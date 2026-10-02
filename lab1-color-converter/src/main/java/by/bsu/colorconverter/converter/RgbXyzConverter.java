package by.bsu.colorconverter.converter;

import by.bsu.colorconverter.model.RgbColor;
import by.bsu.colorconverter.model.XyzColor;

public final class RgbXyzConverter {

    private RgbXyzConverter() {
    }


    public static XyzColor rgbToXyz(RgbColor rgb) {
        double r = inverseGammaCorrection(rgb.red() / 255.0) * 100.0;
        double g = inverseGammaCorrection(rgb.green() / 255.0) * 100.0;
        double b = inverseGammaCorrection(rgb.blue() / 255.0) * 100.0;

        double x =
                0.412453 * r +
                0.357580 * g +
                0.180423 * b;

        double y =
                0.212671 * r +
                0.715160 * g +
                0.072169 * b;

        double z =
                0.019334 * r +
                0.119193 * g +
                0.950227 * b;

        return new XyzColor(x, y, z);
    }

    public static RgbColor xyzToRgb(XyzColor xyz) {
        double x = xyz.x() / 100.0;
        double y = xyz.y() / 100.0;
        double z = xyz.z() / 100.0;

        double rLinear =
                3.2406 * x
                - 1.5372 * y
                - 0.4986 * z;

        double gLinear =
                -0.9689 * x
                + 1.8758 * y
                + 0.0415 * z;

        double bLinear =
                0.0557 * x
                - 0.2040 * y
                + 1.0570 * z;

        double r = gammaCorrection(rLinear) * 255.0;
        double g = gammaCorrection(gLinear) * 255.0;
        double b = gammaCorrection(bLinear) * 255.0;

        return new RgbColor(r, g, b);
    }

    private static double inverseGammaCorrection(double value) {
        if (value >= 0.04045) {
            return Math.pow(
                    (value + 0.055) / 1.055,
                    2.4
            );
        }

        return value / 12.92;
    }

    private static double gammaCorrection(double value) {
        if (value >= 0.0031308) {
            return 1.055 * Math.pow(value, 1.0 / 2.4) - 0.055;
        }

        return 12.92 * value;
    }
}