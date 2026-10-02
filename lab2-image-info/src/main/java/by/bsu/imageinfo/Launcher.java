package by.bsu.imageinfo;

import by.bsu.imageinfo.cli.ConsoleScanner;
import javafx.application.Application;

import java.util.Arrays;

public class Launcher {

    public static void main(String[] args) throws InterruptedException {
        if (Arrays.asList(args).contains("--cli")) {
            ConsoleScanner.run(args);
            return;
        }
        Application.launch(MainApp.class, args);
    }
}
