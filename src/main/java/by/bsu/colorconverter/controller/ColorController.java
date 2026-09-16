package by.bsu.colorconverter.controller;

import by.bsu.colorconverter.converter.ColorConverter;
import by.bsu.colorconverter.model.CmykColor;
import by.bsu.colorconverter.model.LabColor;
import by.bsu.colorconverter.model.RgbColor;
import by.bsu.colorconverter.util.RgbRangeUtil;
import by.bsu.colorconverter.view.ColorComponentControl;
import by.bsu.colorconverter.view.InfoWindow;
import by.bsu.colorconverter.view.MainView;
import javafx.scene.paint.Color;


public final class ColorController {


  private final MainView view;

  private boolean updating;


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

    view.getInfoButton()
        .setOnAction(
            event -> InfoWindow.show()
        );
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

    view.getColorPicker()
        .setOnAction(
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
            (observable, oldValue, newValue) ->
                action.run()
        );
  }


  private void updateFromRgb() {

    if (updating) {
      return;
    }

    updating = true;

    try {

      RgbColor rgb = readRgb();

      setCmyk(
          ColorConverter.rgbToCmyk(rgb)
      );

      setLab(
          ColorConverter.rgbToLab(rgb)
      );

      updatePreview(rgb);

      view.setWarning("");

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

      CmykColor cmyk =
          readCmyk();

      RgbColor rgb =
          ColorConverter.cmykToRgb(cmyk);

      setRgb(rgb);

      setLab(
          ColorConverter.rgbToLab(rgb)
      );

      updatePreview(rgb);

      view.setWarning("");

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

      LabColor lab =
          readLab();

      RgbColor calculated =
          ColorConverter.labToRgb(lab);

      boolean clipped =
          RgbRangeUtil.isOutsideRange(calculated);

      RgbColor rgb =
          RgbRangeUtil.clamp(calculated);

      setRgb(rgb);

      setCmyk(
          ColorConverter.rgbToCmyk(rgb)
      );

      updatePreview(rgb);

      if (clipped) {

        view.setWarning(
            "LAB цвет выходит за диапазон RGB. Значения RGB были ограничены."
        );

      } else {

        view.setWarning("");
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

      Color color =
          view.getColorPicker()
              .getValue();

      RgbColor rgb =
          new RgbColor(
              color.getRed() * 255,
              color.getGreen() * 255,
              color.getBlue() * 255
          );

      setRgb(rgb);

      setCmyk(
          ColorConverter.rgbToCmyk(rgb)
      );

      setLab(
          ColorConverter.rgbToLab(rgb)
      );

      updatePreview(rgb);


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

    RgbColor safe =
        RgbRangeUtil.clamp(rgb);

    Color color =
        Color.rgb(
            (int) safe.red(),
            (int) safe.green(),
            (int) safe.blue()
        );

    view.setPreviewColor(color);

    view.getColorPicker()
        .setValue(color);
  }
}