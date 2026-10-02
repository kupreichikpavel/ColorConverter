package by.bsu.colorconverter.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.util.Locale;

public final class ColorComponentControl extends HBox {

    private final Slider slider;
    private final TextField textField;

    private final double min;
    private final double max;
    private final int decimals;

    public ColorComponentControl(
            String name,
            double min,
            double max,
            double initialValue,
            int decimals
    ) {
        this.min = min;
        this.max = max;
        this.decimals = decimals;

        Label label = new Label(name);
        label.setPrefWidth(25);

        slider = new Slider(min, max, initialValue);
        slider.setPrefWidth(180);

        textField = new TextField(format(initialValue));
        textField.setPrefWidth(75);
        textField.setAlignment(Pos.CENTER_RIGHT);

        setSpacing(10);
        setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(
                label,
                slider,
                textField
        );

        slider.valueProperty().addListener(
                (observable, oldValue, newValue) ->
                        textField.setText(
                                format(newValue.doubleValue())
                        )
        );

        textField.setOnAction(event -> commitTextValue());

        textField.focusedProperty().addListener(
                (observable, oldValue, focused) -> {
                    if (!focused) {
                        commitTextValue();
                    }
                }
        );
    }

    private void commitTextValue() {
        try {
            String text = textField
                    .getText()
                    .trim()
                    .replace(',', '.');

            double value = Double.parseDouble(text);

            value = clamp(value);
            value = round(value);

            slider.setValue(value);
            textField.setText(format(value));

            textField.setStyle("");

        } catch (NumberFormatException exception) {
            textField.setText(format(getValue()));

            textField.setStyle(
                    "-fx-border-color: #d9534f;"
            );
        }
    }

    public double getValue() {
        return round(slider.getValue());
    }

    public void setValue(double value) {
        double corrected = round(clamp(value));

        slider.setValue(corrected);
        textField.setText(format(corrected));
    }

    public Slider getSlider() {
        return slider;
    }

    private double clamp(double value) {
        return Math.max(
                min,
                Math.min(max, value)
        );
    }

    private double round(double value) {
        double multiplier = Math.pow(10, decimals);

        return Math.round(value * multiplier)
                / multiplier;
    }

    private String format(double value) {
        return String.format(
                Locale.US,
                "%." + decimals + "f",
                value
        );
    }
}