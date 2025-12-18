package bartek.fileorganizer;

import bartek.fileorganizer.config.ConfigService;
import bartek.fileorganizer.core.DirectoryWatcher;
import bartek.fileorganizer.core.FileProcessor;
import bartek.fileorganizer.model.AppConfig;
import bartek.fileorganizer.model.Rule;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.bootstrapfx.BootstrapFX;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.MenuItem;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

@Slf4j
public class FileOrganizerApp extends Application {

    private AppConfig currentConfig;
    private Thread watcherThread;
    private Label pathLabel;

    private final ObservableList<String> eventsLog = FXCollections.observableArrayList();

    @Override
    public void init() {
        ConfigService configService = new ConfigService();
        try {
            currentConfig = configService.loadConfig();
            log.info("Loaded config for: {}", currentConfig.sourceDirectory());
            currentConfig.rules().forEach(config -> log.info("Rule: {} -> {}", config.extension(), config.nameContains()));
        } catch (IOException e) {
            log.error("Failed to load configuration: {}", e.getMessage());
        }
    }

    @Override
    public void start(Stage stage) {

        Platform.setImplicitExit(false);

        BorderPane root = configureUI();


        if (currentConfig != null) {
            startDirectoryWatcher();
        }

        Scene scene = new Scene(root, 700, 550);
        scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
        String css = Objects.requireNonNull(getClass().getResource("/styles.css")).toExternalForm();
        scene.getStylesheets().add(css);

        Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/appIcon.png")));
        stage.getIcons().add(icon);


        stage.setTitle("File Organizer");
        stage.setMinWidth(700);
        stage.setMinHeight(550);
        stage.setScene(scene);
        stage.show();

        addToSystemTray(stage);
    }

    private BorderPane configureUI() {
        BorderPane root = new BorderPane();


        root.setPadding(new Insets(10));

        VBox header = new VBox(5);

        Label titleLabel = new Label("File Organizer");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");


        TabPane tabPane = new TabPane();

        Tab eventsTab = new Tab("Events Log");
        eventsTab.setContent(buildEventsLogView());
        eventsTab.setClosable(false);
        tabPane.getTabs().add(eventsTab);

        Tab optionsTab = new Tab("Options");
        VBox settingsPane = new VBox(5);
        settingsPane.setPadding(new Insets(10));

        optionsTab.setContent(buildOptionsView());
        optionsTab.setClosable(false);
        tabPane.getTabs().add(optionsTab);

        tabPane.setPadding(new Insets(10,0,0,0));

        pathLabel = new Label();
        updatePathLabel();
        header.getChildren().addAll(titleLabel, pathLabel);

        root.setCenter(tabPane);

        root.setTop(header);
        return root;
    }

    private void startDirectoryWatcher() {

        if (watcherThread != null && watcherThread.isAlive()) {
            watcherThread.interrupt();
            log.info("Stopped existing directory watcher thread");
        }

        if (currentConfig != null) {
            DirectoryWatcher directoryWatcher = new DirectoryWatcher(currentConfig, msg ->
            {
                Platform.runLater(() -> {
                    eventsLog.addFirst(msg);

                    if (eventsLog.size() > 100) {
                        eventsLog.remove(100);
                    }
                });
            });
            watcherThread = new Thread(directoryWatcher);
            watcherThread.setDaemon(true);
            watcherThread.setName("Watcher-Thread");
            watcherThread.start();
            log.info("Started new directory watcher thread");
        }
    }

    private void addToSystemTray(Stage stage) {

        if (!SystemTray.isSupported()) {
            log.warn("System tray is not supported");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();

            URL imgUrl = getClass().getResource("/appIcon.png");
            if (imgUrl == null) {
                log.error("Tray icon image not found");
                return;
            }
            BufferedImage trayImage = ImageIO.read(imgUrl);

            PopupMenu popup = new PopupMenu();

            MenuItem openItem = new MenuItem("Open");
            openItem.addActionListener(e -> {
                Platform.runLater(() -> {
                    stage.show();
                    stage.toFront();
                });
            });

            MenuItem exitItem = new MenuItem("Close");
            exitItem.addActionListener(_ -> {
                tray.remove(tray.getTrayIcons()[0]);
                Platform.exit();
                System.exit(0);
            });


            popup.add(openItem);
            popup.addSeparator();
            popup.add(exitItem);

            TrayIcon trayIcon = new TrayIcon(trayImage, "File Organizer", popup);
            trayIcon.setImageAutoSize(true);

            trayIcon.addActionListener(e -> Platform.runLater(stage::show));

            tray.add(trayIcon);

            stage.setOnCloseRequest(e -> {
                e.consume();
                stage.hide();
                trayIcon.displayMessage(
                        "File Organizer",
                        "Application minimized to tray. Double-click the tray icon to restore.",
                        TrayIcon.MessageType.INFO
                );
                log.info("Application minimized to tray");
            });
        } catch (AWTException | IOException e) {
            log.error("Failed to add to system tray: {}", e.getMessage());
        }
    }

    private VBox createSection(String title, Node content) {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10, 0, 20, 0));

        Label headerLabel = new Label(title);
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Separator separator = new Separator();

        section.getChildren().addAll(headerLabel, separator, content);
        return section;
    }

    private Node buildEventsLogView() {
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        Label label = new Label("Events Log");
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        ListView<String> listView = new ListView<>(eventsLog);
        listView.setPlaceholder(new Label("Waiting for events..."));

        VBox.setVgrow(listView, Priority.ALWAYS);

        layout.getChildren().addAll(label, listView);


        return listView;
    }

    private Node buildOptionsView() {
        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(20));

        //Section 1
        HBox folderControls = new HBox(10);
        TextField pathField = new TextField(currentConfig.sourceDirectory());
        pathField.getStyleClass().add("path-field");
        pathField.setEditable(false);
        pathField.setPrefWidth(300);
        Button changeBtn = getChangeDirectoryButton(pathField);
        folderControls.getChildren().addAll(pathField, changeBtn);

        VBox sourceFolderSection = createSection("Source folder", folderControls);

        //Section 2 - Rules List
        TableView<Rule> rulesTable = new TableView<>();
        rulesTable.setPrefHeight(200);
        rulesTable.setItems(FXCollections.observableArrayList(currentConfig.rules()));
        rulesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        rulesTable.setPlaceholder(new Label("No rules defined yet. Click 'Add Rule' to start!"));
        rulesTable.getStyleClass().add("table-striped");

        TableColumn<Rule, String> extCol = new TableColumn<>("Extension");
        extCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().extension()));

        TableColumn<Rule, String> nameCol = new TableColumn<>("Name Contains");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().nameContains()));

        TableColumn<Rule, String> targetCol = new TableColumn<>("Target Folder");
        targetCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().targetFolder()));

        rulesTable.getColumns().addAll(extCol, nameCol, targetCol);

        VBox rulesSection = createSection("Manage Rules", rulesTable);
        rulesSection.setPadding(new Insets(10));
        Button addRuleBtn = getAddRuleButton(rulesTable);
        Button removeRuleBtn = getRemoveRuleButton(rulesTable);
        Button saveBtn = getSaveConfigButton(pathField, rulesTable);
        Button scanExistingBtn = getCleanNowButton();
        HBox rulesButtons = new HBox(10, addRuleBtn, removeRuleBtn, saveBtn,scanExistingBtn);
        rulesSection.getChildren().add(rulesButtons);


        mainLayout.getChildren().addAll(sourceFolderSection, rulesSection);

        return mainLayout;
    }

    private Button getChangeDirectoryButton(TextField pathField) {
        Button changeBtn = new Button("Change...");
        changeBtn.getStyleClass().addAll("btn", "btn-secondary");
        changeBtn.setOnAction(
                _ -> {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Select Source Folder");
                    File selectedDirectory = directoryChooser.showDialog(null);
                    if (selectedDirectory != null) {
                        pathField.setText(selectedDirectory.getAbsolutePath());
                    }
                }
        );
        return changeBtn;
    }

    private Button getCleanNowButton() {
        Button scanBtn = new Button("Clean Now");
        scanBtn.getStyleClass().addAll("btn", "btn-info");
        scanBtn.setGraphic(new Label("🔍"));
        scanBtn.getGraphic().setStyle("-fx-text-fill: white;");
        scanBtn.setOnAction(
                _ -> {
                    scanBtn.setDisable(true);
                    FileProcessor tempProcessor = new FileProcessor(currentConfig, msg -> {
                        Platform.runLater(() -> {
                            eventsLog.addFirst(msg);
                            if (eventsLog.size() > 100) {
                                eventsLog.remove(100);
                            }
                        });
                    });

                    Thread scanThread = new Thread(() -> {
                        tempProcessor.scanExistingFiles();
                        Platform.runLater(() -> {
                            scanBtn.setDisable(false);

                            showStyledAlert(Alert.AlertType.INFORMATION, "Scan", "Cleaning complete!");
                        });
                    });
                    scanThread.setDaemon(true);
                    scanThread.start();
                }

        );
        return scanBtn;
    }

    private Button getAddRuleButton(TableView<Rule> rulesTable) {
        Button addBtn = new Button("Add Rule");
        addBtn.getStyleClass().addAll("btn", "btn-success");
        addBtn.setGraphic(new Label("➕"));
        addBtn.getGraphic().setStyle("-fx-text-fill: white;");
        addBtn.setOnAction(
                _ -> {
                    Dialog<Rule> dialog = new Dialog<>();
                    dialog.setTitle("New rule");
                    dialog.setHeaderText("Define new move rule");

                    ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
                    dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

                    GridPane grid = new GridPane();
                    grid.setHgap(10);
                    grid.setVgap(10);
                    grid.setPadding(new Insets(20, 150, 10, 10));

                    TextField extField = new TextField();
                    extField.setPromptText("np. pdf");
                    TextField nameField = new TextField();
                    nameField.setPromptText("np. invoice");
                    TextField folderField = new TextField();
                    folderField.setPromptText("np. documents");

                    grid.add(new Label("Extension:"), 0, 0);
                    grid.add(extField, 1, 0);
                    grid.add(new Label("Name contains:"), 0, 1);
                    grid.add(nameField, 1, 1);
                    grid.add(new Label("Target folder:"), 0, 2);
                    grid.add(folderField, 1, 2);

                    dialog.getDialogPane().setContent(grid);

                    Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
                    try {
                        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/appIcon.png"))));
                    } catch (Exception _) {
                    }


                    Platform.runLater(extField::requestFocus);

                    dialog.setResultConverter(dialogButton -> {
                        if (dialogButton == addButtonType) {
                            String ext = extField.getText().trim();
                            String name = nameField.getText().trim();
                            String folder = folderField.getText().trim();

                            if (folder.isEmpty()) {
                                showStyledAlert(Alert.AlertType.ERROR, "Error", "Folder is empty!");
                                return null;
                            } else {
                                String finalExt = ext.isEmpty() ? null : ext;
                                String finalName = name.isEmpty() ? null : name;
                                return new Rule(finalExt, folder, finalName);
                            }
                        }
                        return null;
                    });

                    dialog.showAndWait().ifPresent(newRule -> {
                        rulesTable.getItems().add(newRule);
                        log.info("Added new rule: {} -> {}", newRule.extension(), newRule.targetFolder());
                    });
                }
        );
        return addBtn;
    }

    private static Button getRemoveRuleButton(TableView<Rule> rulesTable) {
        Button removeBtn = new Button("Remove Selected Rule");
        removeBtn.getStyleClass().addAll("btn", "btn-danger");
        removeBtn.setGraphic(new Label("🗑"));
        removeBtn.getGraphic().setStyle("-fx-text-fill: white;");

        removeBtn.disableProperty().bind(
                rulesTable.getSelectionModel().selectedItemProperty().isNull()
        );

        removeBtn.setOnAction(
                _ -> {
                    Rule selectedRule = rulesTable.getSelectionModel().getSelectedItem();
                    if (selectedRule != null) {
                        rulesTable.getItems().remove(selectedRule);
                        log.info("Removed rule: {} -> {}", selectedRule.extension(), selectedRule.targetFolder());
                    }
                }
        );
        return removeBtn;
    }

    private Button getSaveConfigButton(TextField pathField, TableView<Rule> rulesTable) {
        Button saveBtn = new Button("Save & Apply");
        saveBtn.getStyleClass().addAll("btn", "btn-primary");
        saveBtn.setGraphic(new Label("💾"));
        saveBtn.getGraphic().setStyle("-fx-text-fill: white;");

        saveBtn.setOnAction(
                e -> {
                    try {
                        String newPath = pathField.getText().trim();

                        List<Rule> newRules = List.copyOf(rulesTable.getItems());

                        AppConfig newConfig = new AppConfig(newPath, newRules);

                        ConfigService configService = new ConfigService();
                        configService.saveConfig(newConfig);


                        this.currentConfig = newConfig;

                        updatePathLabel();
                        this.startDirectoryWatcher();

                        showStyledAlert(Alert.AlertType.INFORMATION, "Success", "Configuration saved and watcher restarted!");

                    } catch (IOException ex) {
                        log.error("Failed to save config", ex);
                        showStyledAlert(Alert.AlertType.ERROR, "Error", "Configuration save failed!");

                    }
                }
        );
        return saveBtn;
    }

    private void updatePathLabel() {
        if (pathLabel != null) {
            String dir = (currentConfig != null) ? currentConfig.sourceDirectory() : "Not set";
            pathLabel.setText("Watching folder: " + dir);
        }
    }

    private void showStyledAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();

        try {
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/appIcon.png"))));
        } catch (Exception _) {
        }

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());

        alert.showAndWait();
    }

    @Override
    public void stop() {
        log.info("Stopping application");
    }
}
