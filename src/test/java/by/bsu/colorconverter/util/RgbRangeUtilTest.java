package by.bsu.colorconverter.util;

import by.bsu.colorconverter.model.RgbColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RgbRangeUtilTest {

  @Test
  void validRgbShouldNotBeOutsideRange() {
    RgbColor rgb = new RgbColor(120, 65, 210);

    assertFalse(RgbRangeUtil.isOutsideRange(rgb));
  }

  @Test
  void rgbAbove255ShouldBeOutsideRange() {
    RgbColor rgb = new RgbColor(280, 50, 100);

    assertTrue(RgbRangeUtil.isOutsideRange(rgb));
  }

  @Test
  void negativeRgbShouldBeOutsideRange() {
    RgbColor rgb = new RgbColor(-15, 50, 100);

    assertTrue(RgbRangeUtil.isOutsideRange(rgb));
  }

  @Test
  void rgbShouldBeClampedToValidRange() {
    RgbColor rgb = new RgbColor(280, -20, 100);

    RgbColor result = RgbRangeUtil.clamp(rgb);

    assertEquals(255.0, result.red());
    assertEquals(0.0, result.green());
    assertEquals(100.0, result.blue());
  }

  @Test
  void rgbOnBoundaryShouldBeValid() {
    RgbColor rgb =
        new RgbColor(
            0,
            255,
            255
        );

    assertFalse(
        RgbRangeUtil.isOutsideRange(rgb)
    );
  }

  @Test
  void clampShouldNotChangeValidRgb() {
    RgbColor original =
        new RgbColor(
            100,
            150,
            200
        );

    RgbColor result =
        RgbRangeUtil.clamp(original);

    assertEquals(
        original.red(),
        result.red()
    );

    assertEquals(
        original.green(),
        result.green()
    );

    assertEquals(
        original.blue(),
        result.blue()
    );
  }
}