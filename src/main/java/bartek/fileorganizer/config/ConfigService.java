package bartek.fileorganizer.config;

import bartek.fileorganizer.model.AppConfig;
import com.sun.jna.platform.win32.KnownFolders;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;


@Slf4j
public class ConfigService {

    private static final Path CONFIG_PATH = Paths.get("organizer_config.json");
    private final ObjectMapper mapper;

    public ConfigService() {
        this.mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
    }

    public AppConfig loadConfig() throws IOException {
        if (!Files.exists(CONFIG_PATH)) {
            return createDefaultConfig();
        }
        return mapper.readValue(CONFIG_PATH.toFile(), AppConfig.class);
    }

    private AppConfig createDefaultConfig() {


        Path downloads = getDownloadsFolder();

        AppConfig config = new AppConfig(downloads.toString(), Collections.emptyList());

        mapper.writeValue(CONFIG_PATH.toFile(), config);

        return config;
    }

    private OS getOperatingSystem() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            log.info("Operating System: Windows");
            return OS.WINDOWS;
        }
        if (os.contains("mac")) {
            log.info("Operating System: MacOS");
            return OS.MAC;
        }
        if (os.contains("nux") || os.contains("linux")) {
            log.info("Operating System: Linux");
            return OS.LINUX;
        }

        log.error("Operating System: Other");
        return OS.OTHER;
    }

    private Path getDownloadsFolder() {
        OS operatingSystem = getOperatingSystem();

        return switch (operatingSystem) {
            case WINDOWS -> getWindowsDownloadFolder();
            case MAC -> Path.of(System.getProperty("user.home"), "Downloads");
            case LINUX -> getLinuxDownloadFolder();
            case OTHER -> {
                log.error("Operating system is not supported");
                Platform.exit();
                yield Path.of("");
            }
        };
    }

    private static Path getLinuxDownloadFolder() {
        Path xdg = Path.of(
                System.getProperty("user.home"),
                ".config/user-dirs.dirs"
        );

        if (Files.exists(xdg)) {
            try {
                for (String line : Files.readAllLines(xdg)) {
                    if (line.startsWith("XDG_DOWNLOAD_DIR")) {
                        String path = line.split("=")[1]
                                .replace("\"", "")
                                .replace("$HOME", System.getProperty("user.home"));

                        return Path.of(path);
                    }
                }
            } catch (IOException e) {
                log.error("Error reading XDG user dirs: {}", e.getMessage());
            }
        }
        return Path.of(System.getProperty("user.home"), "Downloads");
    }

    private static Path getWindowsDownloadFolder() {
        PointerByReference ppszPath = new PointerByReference();

        WinNT.HRESULT hr = Shell32.INSTANCE.SHGetKnownFolderPath(
                KnownFolders.FOLDERID_Downloads,
                0,
                null,
                ppszPath
        );
        if (!WinNT.S_OK.equals(hr)) {
            throw new RuntimeException("SHGetKnownFolderPath failed: " + hr);
        }

        String path = ppszPath.getValue().getWideString(0);
        Ole32.INSTANCE.CoTaskMemFree(ppszPath.getValue());
        return Path.of(path);
    }

    public void saveConfig(AppConfig config) throws IOException {
        mapper.writeValue(CONFIG_PATH.toFile(), config);
    }

}
