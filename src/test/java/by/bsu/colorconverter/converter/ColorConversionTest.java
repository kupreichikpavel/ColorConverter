package by.bsu.colorconverter.converter;

import by.bsu.colorconverter.model.CmykColor;
import by.bsu.colorconverter.model.LabColor;
import by.bsu.colorconverter.model.RgbColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColorConversionTest {

  private static final double DELTA = 0.01;

  @Test
  void redRgbShouldConvertCorrectlyToCmykAndLab() {
    RgbColor rgb = new RgbColor(255, 0, 0);

    CmykColor cmyk = ColorConverter.rgbToCmyk(rgb);

    LabColor lab = ColorConverter.rgbToLab(rgb);

    assertEquals(0.0, cmyk.cyan(), DELTA);
    assertEquals(100.0, cmyk.magenta(), DELTA);
    assertEquals(100.0, cmyk.yellow(), DELTA);
    assertEquals(0.0, cmyk.key(), DELTA);

    assertEquals(53.24, lab.l(), DELTA);
    assertEquals(80.09, lab.a(), DELTA);
    assertEquals(67.20, lab.b(), DELTA);
  }

  @Test
  void blackRgbShouldConvertCorrectlyToCmykAndLab() {
    RgbColor rgb = new RgbColor(0, 0, 0);

    CmykColor cmyk = ColorConverter.rgbToCmyk(rgb);

    LabColor lab = ColorConverter.rgbToLab(rgb);

    assertEquals(0.0, cmyk.cyan(), DELTA);
    assertEquals(0.0, cmyk.magenta(), DELTA);
    assertEquals(0.0, cmyk.yellow(), DELTA);
    assertEquals(100.0, cmyk.key(), DELTA);

    assertEquals(0.0, lab.l(), DELTA);
    assertEquals(0.0, lab.a(), DELTA);
    assertEquals(0.0, lab.b(), DELTA);
  }

  @Test
  void whiteRgbShouldConvertCorrectlyToCmykAndLab() {
    RgbColor rgb = new RgbColor(255, 255, 255);

    CmykColor cmyk = ColorConverter.rgbToCmyk(rgb);

    LabColor lab = ColorConverter.rgbToLab(rgb);

    assertEquals(0.0, cmyk.cyan(), DELTA);
    assertEquals(0.0, cmyk.magenta(), DELTA);
    assertEquals(0.0, cmyk.yellow(), DELTA);
    assertEquals(0.0, cmyk.key(), DELTA);

    assertEquals(100.0, lab.l(), DELTA);
    assertEquals(0.0, lab.a(), DELTA);
    assertEquals(0.0, lab.b(), DELTA);
  }

  @Test
  void greenRgbShouldConvertCorrectlyToLab() {
    RgbColor rgb = new RgbColor(0, 255, 0);

    LabColor lab = ColorConverter.rgbToLab(rgb);

    assertEquals(87.74, lab.l(), DELTA);
    assertEquals(-86.18, lab.a(), DELTA);
    assertEquals(83.18, lab.b(), DELTA);
  }

  @Test
  void blueRgbShouldConvertCorrectlyToLab() {
    RgbColor rgb = new RgbColor(0, 0, 255);

    LabColor lab = ColorConverter.rgbToLab(rgb);

    assertEquals(32.30, lab.l(), DELTA);
    assertEquals(79.19, lab.a(), DELTA);
    assertEquals(-107.86, lab.b(), DELTA);
  }

  @Test
  void rgbShouldRemainSameAfterCmykRoundTrip() {
    RgbColor original = new RgbColor(120, 65, 210);

    CmykColor cmyk = ColorConverter.rgbToCmyk(original);

    RgbColor result = ColorConverter.cmykToRgb(cmyk);

    assertEquals(original.red(), result.red(), DELTA);

    assertEquals(original.green(), result.green(), DELTA);

    assertEquals(original.blue(), result.blue(), DELTA);
  }

  @Test
  void rgbShouldRemainApproximatelySameAfterLabRoundTrip() {
    RgbColor original = new RgbColor(120, 65, 210);

    LabColor lab = ColorConverter.rgbToLab(original);

    RgbColor result = ColorConverter.labToRgb(lab);

    assertEquals(original.red(), result.red(), 0.1);

    assertEquals(original.green(), result.green(), 0.1);

    assertEquals(original.blue(), result.blue(), 0.1);
  }

  @Test
  void cmykLabRoundTripShouldPreserveColor() {
    CmykColor original = new CmykColor(20, 40, 10, 15);

    RgbColor originalRgb = ColorConverter.cmykToRgb(original);

    LabColor lab = ColorConverter.cmykToLab(original);

    CmykColor convertedCmyk = ColorConverter.labToCmyk(lab);

    RgbColor resultRgb = ColorConverter.cmykToRgb(convertedCmyk);

    assertEquals(originalRgb.red(), resultRgb.red(), 0.1);

    assertEquals(originalRgb.green(), resultRgb.green(), 0.1);

    assertEquals(originalRgb.blue(), resultRgb.blue(), 0.1);
  }
}