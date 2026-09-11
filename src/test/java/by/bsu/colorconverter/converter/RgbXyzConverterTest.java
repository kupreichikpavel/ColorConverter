package by.bsu.colorconverter.converter;

import by.bsu.colorconverter.model.RgbColor;
import by.bsu.colorconverter.model.XyzColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RgbXyzConverterTest {

  private static final double DELTA = 0.001;

  @Test
  void redRgbShouldConvertToXyz() {
    RgbColor rgb = new RgbColor(255, 0, 0);

    XyzColor result = RgbXyzConverter.rgbToXyz(rgb);

    assertEquals(41.2453, result.x(), DELTA);
    assertEquals(21.2671, result.y(), DELTA);
    assertEquals(1.9334, result.z(), DELTA);
  }

  @Test
  void blackRgbShouldConvertToXyz() {
    RgbColor rgb = new RgbColor(0, 0, 0);

    XyzColor result = RgbXyzConverter.rgbToXyz(rgb);

    assertEquals(0.0, result.x(), DELTA);
    assertEquals(0.0, result.y(), DELTA);
    assertEquals(0.0, result.z(), DELTA);
  }

  @Test
  void rgbShouldRemainApproximatelySameAfterRoundTrip() {
    RgbColor original = new RgbColor(
        120,
        65,
        210
    );

    XyzColor xyz = RgbXyzConverter.rgbToXyz(original);

    RgbColor result = RgbXyzConverter.xyzToRgb(xyz);

    assertEquals(original.red(), result.red(), 0.1);
    assertEquals(original.green(), result.green(), 0.1);
    assertEquals(original.blue(), result.blue(), 0.1);
  }
}
