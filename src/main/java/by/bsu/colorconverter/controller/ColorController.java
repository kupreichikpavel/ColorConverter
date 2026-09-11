package by.bsu.colorconverter.controller;

import by.bsu.colorconverter.converter.ColorConverter;
import by.bsu.colorconverter.model.CmykColor;
import by.bsu.colorconverter.model.LabColor;
import by.bsu.colorconverter.model.RgbColor;
import by.bsu.colorconverter.util.RgbRangeUtil;
import by.bsu.colorconverter.view.ColorComponentControl;
import by.bsu.colorconverter.view.MainView;
import javafx.scene.paint.Color;

public final class ColorController {

    private final MainView view;
    private boolean updating = false;

    public ColorController(MainView view) {
        this.view = view;

        bindEvents();
        updateFromRgb();
    }

    private void bindEvents() {
        bindRgbEvents();
        bindCmykEvents();
        bindLabEvents();
        bindColorPicker();
    }

    private void bindRgbEvents() {
        addListener(
            view.getRedControl(),
            this::updateFromRgb
        );

        addListener(
            view.getGreenControl(),
            this::updateFromRgb
        );

        addListener(
            view.getBlueControl(),
            this::updateFromRgb
        );
    }

    private void bindCmykEvents() {
        addListener(
            view.getCyanControl(),
            this::updateFromCmyk
        );

        addListener(
            view.getMagentaControl(),
            this::updateFromCmyk
        );

        addListener(
            view.getYellowControl(),
            this::updateFromCmyk
        );

        addListener(
            view.getKeyControl(),
            this::updateFromCmyk
        );
    }

    private void bindLabEvents() {
        addListener(
            view.getLControl(),
            this::updateFromLab
        );

        addListener(
            view.getAControl(),
            this::updateFromLab
        );

        addListener(
            view.getLabBControl(),
            this::updateFromLab
        );
    }

    private void bindColorPicker() {
        view.getColorPicker().setOnAction(
            event -> updateFromColorPicker()
        );
    }

    private void addListener(
        ColorComponentControl control,
        Runnable action
    ) {
        control.getSlider()
            .valueProperty()
            .addListener(
                (observable, oldValue, newValue) -> action.run()
            );
    }

    private void updateFromRgb() {
        if (updating) {
            return;
        }

        updating = true;

        try {
            RgbColor rgb = readRgb();

            CmykColor cmyk =
                ColorConverter.rgbToCmyk(rgb);

            LabColor lab =
                ColorConverter.rgbToLab(rgb);

            setCmyk(cmyk);
            setLab(lab);

            updatePreview(rgb);

            clearWarning();

        } finally {
            updating = false;
        }
    }

    private void updateFromCmyk() {
        if (updating) {
            return;
        }

        updating = true;

        try {
            CmykColor cmyk = readCmyk();

            RgbColor rgb =
                ColorConverter.cmykToRgb(cmyk);

            LabColor lab =
                ColorConverter.rgbToLab(rgb);

            setRgb(rgb);
            setLab(lab);

            updatePreview(rgb);

            clearWarning();

        } finally {
            updating = false;
        }
    }

    private void updateFromLab() {
        if (updating) {
            return;
        }

        updating = true;

        try {
            LabColor lab = readLab();

            RgbColor calculatedRgb =
                ColorConverter.labToRgb(lab);

            boolean clipped =
                RgbRangeUtil.isOutsideRange(calculatedRgb);

            RgbColor rgb =
                RgbRangeUtil.clamp(calculatedRgb);

            CmykColor cmyk =
                ColorConverter.rgbToCmyk(rgb);

            setRgb(rgb);
            setCmyk(cmyk);

            updatePreview(rgb);

            if (clipped) {
                showClippingWarning();
            } else {
                clearWarning();
            }

        } finally {
            updating = false;
        }
    }

    private void updateFromColorPicker() {
        if (updating) {
            return;
        }

        updating = true;

        try {
            Color selectedColor =
                view.getColorPicker().getValue();

            RgbColor rgb = new RgbColor(
                Math.round(selectedColor.getRed() * 255.0),
                Math.round(selectedColor.getGreen() * 255.0),
                Math.round(selectedColor.getBlue() * 255.0)
            );

            CmykColor cmyk =
                ColorConverter.rgbToCmyk(rgb);

            LabColor lab =
                ColorConverter.rgbToLab(rgb);

            setRgb(rgb);
            setCmyk(cmyk);
            setLab(lab);

            updatePreview(rgb);

            clearWarning();

        } finally {
            updating = false;
        }
    }

    private RgbColor readRgb() {
        return new RgbColor(
            view.getRedControl().getValue(),
            view.getGreenControl().getValue(),
            view.getBlueControl().getValue()
        );
    }

    private CmykColor readCmyk() {
        return new CmykColor(
            view.getCyanControl().getValue(),
            view.getMagentaControl().getValue(),
            view.getYellowControl().getValue(),
            view.getKeyControl().getValue()
        );
    }

    private LabColor readLab() {
        return new LabColor(
            view.getLControl().getValue(),
            view.getAControl().getValue(),
            view.getLabBControl().getValue()
        );
    }

    private void setRgb(RgbColor rgb) {
        view.getRedControl()
            .setValue(rgb.red());

        view.getGreenControl()
            .setValue(rgb.green());

        view.getBlueControl()
            .setValue(rgb.blue());
    }

    private void setCmyk(CmykColor cmyk) {
        view.getCyanControl()
            .setValue(cmyk.cyan());

        view.getMagentaControl()
            .setValue(cmyk.magenta());

        view.getYellowControl()
            .setValue(cmyk.yellow());

        view.getKeyControl()
            .setValue(cmyk.key());
    }

    private void setLab(LabColor lab) {
        view.getLControl()
            .setValue(lab.l());

        view.getAControl()
            .setValue(lab.a());

        view.getLabBControl()
            .setValue(lab.b());
    }

    private void updatePreview(RgbColor rgb) {
        RgbColor safeRgb =
            RgbRangeUtil.clamp(rgb);

        int red = (int) Math.round(safeRgb.red());
        int green = (int) Math.round(safeRgb.green());
        int blue = (int) Math.round(safeRgb.blue());

        Color color = Color.rgb(
            red,
            green,
            blue
        );

        view.setPreviewColor(color);

        view.getColorPicker()
            .setValue(color);
    }

    private void showClippingWarning() {
        view.setWarning(
            "Предупреждение: выбранный цвет LAB "
                + "выходит за цветовой диапазон RGB. "
                + "Некоторые значения RGB были ограничены "
                + "диапазоном 0–255."
        );
    }

    private void clearWarning() {
        view.setWarning("");
    }
}