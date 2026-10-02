package by.bsu.imageinfo;

import by.bsu.imageinfo.cli.ConsoleScanner;
import javafx.application.Application;

import java.util.Arrays;

/**
 * Точка входа. Отдельный класс без наследования от Application нужен,
 * чтобы jar с JavaFX запускался без модульного пути.
 * С аргументом {@code --cli} работает в консольном режиме.
 */
public class Launcher {

    public static void main(String[] args) throws InterruptedException {
        if (Arrays.asList(args).contains("--cli")) {
            ConsoleScanner.run(args);
            return;
        }
        Application.launch(MainApp.class, args);
    }
}
