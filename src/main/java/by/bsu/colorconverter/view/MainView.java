package by.bsu.colorconverter.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public final class MainView extends BorderPane {

    private final ColorComponentControl redControl =
            new ColorComponentControl(
                    "R",
                    0,
                    255,
                    255,
                    0
            );

    private final ColorComponentControl greenControl =
            new ColorComponentControl(
                    "G",
                    0,
                    255,
                    0,
                    0
            );

    private final ColorComponentControl blueControl =
            new ColorComponentControl(
                    "B",
                    0,
                    255,
                    0,
                    0
            );

    private final ColorComponentControl cyanControl =
            new ColorComponentControl(
                    "C",
                    0,
                    100,
                    0,
                    2
            );

    private final ColorComponentControl magentaControl =
            new ColorComponentControl(
                    "M",
                    0,
                    100,
                    100,
                    2
            );

    private final ColorComponentControl yellowControl =
            new ColorComponentControl(
                    "Y",
                    0,
                    100,
                    100,
                    2
            );

    private final ColorComponentControl keyControl =
            new ColorComponentControl(
                    "K",
                    0,
                    100,
                    0,
                    2
            );

    private final ColorComponentControl lControl =
            new ColorComponentControl(
                    "L",
                    0,
                    100,
                    53.24,
                    2
            );

    private final ColorComponentControl aControl =
            new ColorComponentControl(
                    "a",
                    -128,
                    127,
                    80.09,
                    2
            );

    private final ColorComponentControl labBControl =
            new ColorComponentControl(
                    "b",
                    -128,
                    127,
                    67.20,
                    2
            );

    private final ColorPicker colorPicker =
            new ColorPicker(Color.RED);

    private final Rectangle colorPreview =
            new Rectangle(220, 100);

    private final Label warningLabel =
            new Label();

    public MainView() {
        configureView();
    }

    private void configureView() {
        setPadding(new Insets(25));

        Label title = new Label(
                "Color Converter — Variant 10"
        );

        title.setStyle(
                "-fx-font-size: 26px;" +
                "-fx-font-weight: bold;"
        );

        colorPreview.setFill(Color.RED);

        colorPreview.setArcWidth(15);
        colorPreview.setArcHeight(15);

        colorPreview.setStroke(Color.GRAY);

        Label pickerLabel =
                new Label("Выбор цвета:");

        HBox pickerBox = new HBox(
                10,
                pickerLabel,
                colorPicker
        );

        pickerBox.setAlignment(Pos.CENTER);

        VBox topBox = new VBox(
                15,
                title,
                colorPreview,
                pickerBox
        );

        topBox.setAlignment(Pos.CENTER);

        VBox rgbSection = createSection(
                "RGB",
                redControl,
                greenControl,
                blueControl
        );

        VBox cmykSection = createSection(
                "CMYK",
                cyanControl,
                magentaControl,
                yellowControl,
                keyControl
        );

        VBox labSection = createSection(
                "LAB",
                lControl,
                aControl,
                labBControl
        );

        HBox modelsBox = new HBox(
                30,
                rgbSection,
                cmykSection,
                labSection
        );

        modelsBox.setAlignment(Pos.TOP_CENTER);

        modelsBox.setPadding(
                new Insets(30, 0, 20, 0)
        );

        warningLabel.setStyle(
                "-fx-text-fill: #d97706;" +
                "-fx-font-size: 14px;"
        );

        warningLabel.setWrapText(true);

        VBox center = new VBox(
                modelsBox,
                warningLabel
        );

        center.setAlignment(Pos.TOP_CENTER);

        setTop(topBox);
        setCenter(center);
    }

    private VBox createSection(
            String title,
            ColorComponentControl... controls
    ) {
        Label sectionTitle =
                new Label(title);

        sectionTitle.setStyle(
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;"
        );

        VBox box = new VBox(15);

        box.setPadding(new Insets(20));

        box.setStyle(
                "-fx-border-color: #cccccc;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;"
        );

        box.getChildren().add(sectionTitle);
        box.getChildren().addAll(controls);

        return box;
    }

    public ColorComponentControl getRedControl() {
        return redControl;
    }

    public ColorComponentControl getGreenControl() {
        return greenControl;
    }

    public ColorComponentControl getBlueControl() {
        return blueControl;
    }

    public ColorComponentControl getCyanControl() {
        return cyanControl;
    }

    public ColorComponentControl getMagentaControl() {
        return magentaControl;
    }

    public ColorComponentControl getYellowControl() {
        return yellowControl;
    }

    public ColorComponentControl getKeyControl() {
        return keyControl;
    }

    public ColorComponentControl getLControl() {
        return lControl;
    }

    public ColorComponentControl getAControl() {
        return aControl;
    }

    public ColorComponentControl getLabBControl() {
        return labBControl;
    }

    public ColorPicker getColorPicker() {
        return colorPicker;
    }

    public void setPreviewColor(Color color) {
        colorPreview.setFill(color);
    }

    public void setWarning(String message) {
        warningLabel.setText(message);
    }
}