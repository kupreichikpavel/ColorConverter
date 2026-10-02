package by.bsu.imageinfo;

import by.bsu.imageinfo.view.MainView;
import by.bsu.imageinfo.view.ScanController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        MainView view = new MainView();
        ScanController controller = new ScanController(view);

        Scene scene = new Scene(view, 1360, 780);
        scene.getStylesheets().add(Objects.requireNonNull(
                MainApp.class.getResource("app.css")).toExternalForm());

        stage.setTitle("Image Info — характеристики графических файлов (ЛР 2)");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(560);
        stage.show();

        // папку или файлы можно передать аргументами (например, перетащив на ярлык программы)
        List<Path> paths = getParameters().getRaw().stream()
                .filter(arg -> !arg.startsWith("--"))
                .map(Path::of)
                .filter(Files::exists)
                .toList();
        if (!paths.isEmpty()) {
            controller.scan(paths);
        }
    }
}
