package by.bsu.imageinfo.view;

import by.bsu.imageinfo.model.FileStatus;
import by.bsu.imageinfo.model.Fmt;
import by.bsu.imageinfo.model.Glossary;
import by.bsu.imageinfo.model.ImageInfo;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Map;

public final class DetailsPane extends ScrollPane {

    private static final double PREVIEW_WIDTH = 380;
    private static final double PREVIEW_HEIGHT = 240;

    private final VBox content = new VBox(14);
    private final CheckBox showExplanations = new CheckBox("Показывать пояснения");
    private ImageInfo current;

    public DetailsPane() {
        getStyleClass().add("details");
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        content.setPadding(new Insets(16));
        content.getStyleClass().add("details-content");
        setContent(content);
        showExplanations.setSelected(true);
        showExplanations.selectedProperty().addListener((obs, oldValue, newValue) -> show(current));
        show(null);
    }

    public void show(ImageInfo info) {
        current = info;
        content.getChildren().clear();
        if (info == null) {
            Label hint = new Label("Выберите файл в таблице, чтобы увидеть все его характеристики "
                    + "с пояснениями: что означает каждая и откуда она берётся в файле.");
            hint.setWrapText(true);
            hint.getStyleClass().add("muted");
            content.getChildren().add(hint);
            return;
        }

        Label title = new Label(info.fileName());
        title.getStyleClass().add("details-title");
        title.setWrapText(true);
        Label path = new Label(info.path().toAbsolutePath().toString());
        path.getStyleClass().add("muted");
        path.setWrapText(true);
        content.getChildren().addAll(new VBox(2, title, path), preview(info), showExplanations);

        if (!info.messages().isEmpty() || info.status() != FileStatus.OK) {
            content.getChildren().add(statusBox(info));
        }

        VBox main = section("Основные характеристики");
        main.getChildren().addAll(
                row(Glossary.FORMAT, formatText(info)),
                row(Glossary.IMAGE_SIZE, info.hasSize() ? info.sizeText() + " пикселей" : "—"),
                row(Glossary.RESOLUTION, resolutionText(info)),
                row(Glossary.BIT_DEPTH, info.bitDepth() > 0
                        ? info.bitDepthText() + (info.bitDepthDetail() == null ? "" : " (" + info.bitDepthDetail() + ")")
                        : "—"),
                row(Glossary.COLOR_MODEL, info.colorModel() == null ? "—" : info.colorModel()),
                row(Glossary.COMPRESSION, info.compressionText()),
                row(Glossary.FILE_SIZE, Fmt.bytes(info.fileSize()) + " (" + info.fileSize() + " байт)"));
        content.getChildren().add(main);

        if (!info.extras().isEmpty()) {
            VBox extra = section("Дополнительно — " + info.formatText());
            for (Map.Entry<String, String> e : info.extras().entrySet()) {
                extra.getChildren().add(row(e.getKey(), e.getValue()));
            }
            content.getChildren().add(extra);
        }
    }

    private StackPane preview(ImageInfo info) {
        StackPane box = new StackPane();
        box.getStyleClass().add("preview");
        box.setMinHeight(PREVIEW_HEIGHT + 16);
        box.setPrefHeight(PREVIEW_HEIGHT + 16);

        if (info.format() == null || !info.format().previewSupported()
                || info.status() == FileStatus.UNSUPPORTED || info.status() == FileStatus.ERROR) {
            box.getChildren().add(previewNote(info.format() != null && !info.format().previewSupported()
                    && info.status() != FileStatus.UNSUPPORTED
                    ? "JavaFX не умеет показывать " + info.formatText()
                    + ".\nХарактеристики прочитаны из заголовка вручную."
                    : "Предпросмотр недоступен"));
            box.setMinHeight(64);
            box.setPrefHeight(64);
            return box;
        }

        Image image = new Image(info.path().toUri().toString(), PREVIEW_WIDTH, PREVIEW_HEIGHT, true, true, true);
        ImageView view = new ImageView(image);
        view.setPreserveRatio(true);
        Label loading = previewNote("Загрузка…");
        box.getChildren().addAll(loading, view);
        image.progressProperty().addListener((obs, oldValue, value) -> {
            if (value.doubleValue() >= 1.0) {
                box.getChildren().remove(loading);
            }
        });
        image.errorProperty().addListener((obs, oldValue, error) -> {
            if (error) {
                box.getChildren().setAll(previewNote("Не удалось отобразить изображение"));
                box.setMinHeight(64);
                box.setPrefHeight(64);
            }
        });
        return box;
    }

    private static Label previewNote(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted");
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER);
        return label;
    }

    private VBox statusBox(ImageInfo info) {
        VBox box = new VBox(4);
        box.getStyleClass().addAll("status-box", info.status().styleClass());
        Label head = new Label(info.status().displayName());
        head.getStyleClass().add("status-box-title");
        box.getChildren().add(head);
        for (String message : info.messages()) {
            Label line = new Label(message);
            line.setWrapText(true);
            box.getChildren().add(line);
        }
        return box;
    }

    private VBox section(String title) {
        VBox section = new VBox(10);
        Label label = new Label(title);
        label.getStyleClass().add("section-title");
        section.getChildren().add(label);
        return section;
    }

    private VBox row(String key, String value) {
        Label name = new Label(key);
        name.getStyleClass().add("prop-name");
        name.setWrapText(true);
        name.setMinWidth(170);
        name.setPrefWidth(170);
        name.setMaxWidth(170);

        VBox row = new VBox(3);
        row.getStyleClass().add("prop-row");
        boolean multiline = value.contains("\n");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add(multiline ? "prop-value-mono" : "prop-value");
        valueLabel.setWrapText(!multiline);

        if (multiline) {
            row.getChildren().addAll(name, valueLabel);
        } else {
            HBox line = new HBox(10, name, valueLabel);
            HBox.setHgrow(valueLabel, Priority.ALWAYS);
            valueLabel.setMaxWidth(Double.MAX_VALUE);
            row.getChildren().add(line);
        }
        String explanation = Glossary.explain(key);
        if (showExplanations.isSelected() && !explanation.isEmpty()) {
            Label hint = new Label(explanation);
            hint.getStyleClass().add("prop-hint");
            hint.setWrapText(true);
            row.getChildren().add(hint);
        }
        return row;
    }

    private static String formatText(ImageInfo info) {
        String text = info.formatText();
        if (info.extensionFormat() != null && info.format() != null && info.extensionFormat() != info.format()
                && info.format() != by.bsu.imageinfo.model.ImageFormat.UNKNOWN) {
            text += " (расширение указывает на " + info.extensionFormat().displayName() + ")";
        }
        return text;
    }

    private static String resolutionText(ImageInfo info) {
        if (info.dpiX() != null) {
            return info.dpiText() + " dpi";
        }
        return info.resolutionNote() == null ? "—" : "— " + info.resolutionNote();
    }
}
