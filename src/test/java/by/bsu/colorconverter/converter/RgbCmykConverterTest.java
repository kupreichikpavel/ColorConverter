package by.bsu.colorconverter.converter;

import by.bsu.colorconverter.model.CmykColor;
import by.bsu.colorconverter.model.RgbColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RgbCmykConverterTest {

  private static final double DELTA = 0.0001;

  @Test
  void redRgbShouldConvertToCmyk() {
    RgbColor rgb = new RgbColor(255, 0, 0);

    CmykColor result = RgbCmykConverter.rgbToCmyk(rgb);

    assertEquals(0.0, result.cyan(), DELTA);
    assertEquals(100.0, result.magenta(), DELTA);
    assertEquals(100.0, result.yellow(), DELTA);
    assertEquals(0.0, result.key(), DELTA);
  }

  @Test
  void whiteRgbShouldConvertToCmyk() {
    RgbColor rgb = new RgbColor(255, 255, 255);

    CmykColor result = RgbCmykConverter.rgbToCmyk(rgb);

    assertEquals(0.0, result.cyan(), DELTA);
    assertEquals(0.0, result.magenta(), DELTA);
    assertEquals(0.0, result.yellow(), DELTA);
    assertEquals(0.0, result.key(), DELTA);
  }

  @Test
  void blackRgbShouldConvertToCmyk() {
    RgbColor rgb = new RgbColor(0, 0, 0);

    CmykColor result = RgbCmykConverter.rgbToCmyk(rgb);

    assertEquals(0.0, result.cyan(), DELTA);
    assertEquals(0.0, result.magenta(), DELTA);
    assertEquals(0.0, result.yellow(), DELTA);
    assertEquals(100.0, result.key(), DELTA);
  }

  @Test
  void redCmykShouldConvertToRgb() {
    CmykColor cmyk = new CmykColor(
        0,
        100,
        100,
        0
    );

    RgbColor result = RgbCmykConverter.cmykToRgb(cmyk);

    assertEquals(255.0, result.red(), DELTA);
    assertEquals(0.0, result.green(), DELTA);
    assertEquals(0.0, result.blue(), DELTA);
  }

  @Test
  void rgbShouldRemainSameAfterRgbToCmykToRgbConversion() {
    RgbColor original = new RgbColor(
        120,
        65,
        210
    );

    CmykColor cmyk = RgbCmykConverter.rgbToCmyk(original);

    RgbColor result = RgbCmykConverter.cmykToRgb(cmyk);

    assertEquals(original.red(), result.red(), DELTA);
    assertEquals(original.green(), result.green(), DELTA);
    assertEquals(original.blue(), result.blue(), DELTA);
  }
}