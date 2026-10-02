package by.bsu.colorconverter.converter;

import by.bsu.colorconverter.model.LabColor;
import by.bsu.colorconverter.model.XyzColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XyzLabConverterTest {

  private static final double DELTA = 0.001;

  @Test
  void redXyzShouldConvertToLab() {
    XyzColor xyz = new XyzColor(
        41.2453,
        21.2671,
        1.9334
    );

    LabColor result = XyzLabConverter.xyzToLab(xyz);

    assertEquals(53.2406, result.l(), DELTA);
    assertEquals(80.0923, result.a(), DELTA);
    assertEquals(67.2028, result.b(), DELTA);
  }

  @Test
  void blackXyzShouldConvertToLab() {
    XyzColor xyz = new XyzColor(
        0,
        0,
        0
    );

    LabColor result = XyzLabConverter.xyzToLab(xyz);

    assertEquals(0.0, result.l(), DELTA);
    assertEquals(0.0, result.a(), DELTA);
    assertEquals(0.0, result.b(), DELTA);
  }

  @Test
  void xyzShouldRemainApproximatelySameAfterRoundTrip() {
    XyzColor original = new XyzColor(
        41.2453,
        21.2671,
        1.9334
    );

    LabColor lab = XyzLabConverter.xyzToLab(original);

    XyzColor result = XyzLabConverter.labToXyz(lab);

    assertEquals(original.x(), result.x(), DELTA);
    assertEquals(original.y(), result.y(), DELTA);
    assertEquals(original.z(), result.z(), DELTA);
  }

  @Test
  void whitePointShouldConvertToLabWhite() {
    XyzColor xyz = new XyzColor(
        95.047,
        100.0,
        108.883
    );

    LabColor result =
        XyzLabConverter.xyzToLab(xyz);

    assertEquals(100.0, result.l(), DELTA);
    assertEquals(0.0, result.a(), DELTA);
    assertEquals(0.0, result.b(), DELTA);
  }

  @Test
  void labWhiteShouldConvertToWhitePoint() {
    LabColor lab =
        new LabColor(100, 0, 0);

    XyzColor result =
        XyzLabConverter.labToXyz(lab);

    assertEquals(95.047, result.x(), DELTA);
    assertEquals(100.0, result.y(), DELTA);
    assertEquals(108.883, result.z(), DELTA);
  }
}