package by.bsu.colorconverter.view;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class InfoWindow {

    private InfoWindow() {
    }

    public static void show() {

        Stage stage = new Stage();

        stage.setTitle("Color Converter Information");

        Label infoText = new Label(
                """
                Color Converter — Variant 10
                
                Приложение выполняет преобразование
                цветов между моделями RGB, CMYK и LAB.
                
                
                RGB
                
                RGB состоит из трёх компонентов:
                
                R — Red (красный)
                G — Green (зелёный)
                B — Blue (синий)
                
                Диапазон значений:
                0 - 255
                
                При изменении RGB автоматически
                пересчитываются CMYK и LAB.
                
                
                CMYK
                
                CMYK используется для печати.
                
                C — Cyan
                M — Magenta
                Y — Yellow
                K — Key (Black)
                
                Диапазон значений:
                0 - 100
                
                При изменении CMYK программа
                получает соответствующий RGB цвет
                и пересчитывает LAB.
                
                
                LAB
                
                LAB состоит из:
                
                L — яркость цвета
                a — зелёный ↔ красный
                b — синий ↔ жёлтый
                
                При изменении LAB программа
                переводит цвет обратно в RGB.
                
                
                Схема преобразований:
                
                
                CMYK
                   |
                   v
                  RGB
                   |
                   v
                  XYZ
                   |
                   v
                  LAB
                  
                  
                Обратное преобразование:
                
                
                LAB
                   |
                   v
                  XYZ
                   |
                   v
                  RGB
                   |
                   v
                  CMYK
                
                
                XYZ является внутренней
                промежуточной моделью и
                пользователю не отображается.
                
                
                Пример:
                
                RGB(255, 0, 0)
                
                CMYK:
                C = 0
                M = 100
                Y = 100
                K = 0
                
                LAB:
                L ≈ 53.24
                a ≈ 80.09
                b ≈ 67.20
                
                
                Если выбранный LAB цвет выходит
                за диапазон RGB, приложение
                автоматически ограничивает значения
                и показывает предупреждение.
                """
        );


        infoText.setWrapText(true);

        infoText.setStyle(
                "-fx-font-size: 14px;"
        );


        VBox content = new VBox(infoText);

        content.setPadding(
                new Insets(20)
        );


        ScrollPane scrollPane =
                new ScrollPane(content);

        scrollPane.setFitToWidth(true);


        Scene scene =
                new Scene(
                        scrollPane,
                        600,
                        650
                );


        stage.setScene(scene);

        stage.show();
    }
}