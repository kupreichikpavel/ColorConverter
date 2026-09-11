package by.bsu.colorconverter.util;

import by.bsu.colorconverter.model.RgbColor;

public final class RgbRangeUtil {

    private static final double MIN = 0.0;
    private static final double MAX = 255.0;

    private RgbRangeUtil() {
    }

    public static boolean isOutsideRange(RgbColor rgb) {
        return rgb.red() < MIN
                || rgb.red() > MAX
                || rgb.green() < MIN
                || rgb.green() > MAX
                || rgb.blue() < MIN
                || rgb.blue() > MAX;
    }

    public static RgbColor clamp(RgbColor rgb) {
        return new RgbColor(
                clamp(rgb.red()),
                clamp(rgb.green()),
                clamp(rgb.blue())
        );
    }

    private static double clamp(double value) {
        return Math.max(MIN, Math.min(MAX, value));
    }
}