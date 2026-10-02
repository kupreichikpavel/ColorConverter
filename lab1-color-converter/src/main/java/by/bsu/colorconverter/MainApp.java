package by.bsu.colorconverter;

import by.bsu.colorconverter.controller.ColorController;
import by.bsu.colorconverter.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        MainView view = new MainView();

        new ColorController(view);

        Scene scene = new Scene(
            view,
            1150,
            650
        );

        stage.setTitle(
            "Color Converter — Variant 10"
        );

        stage.setScene(scene);

        stage.setMinWidth(1050);
        stage.setMinHeight(600);

        stage.show();
    }
}