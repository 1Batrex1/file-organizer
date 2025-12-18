module bartek.fileorganizer {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    requires org.slf4j;

    requires static lombok;
    requires tools.jackson.databind;
    requires java.desktop;
    requires java.naming;
    requires com.sun.jna;
    requires com.sun.jna.platform;


    opens bartek.fileorganizer to javafx.fxml;

    opens bartek.fileorganizer.model to tools.jackson.databind;
    opens bartek.fileorganizer.config to com.sun.jna , tools.jackson.databind;



    exports bartek.fileorganizer;
}