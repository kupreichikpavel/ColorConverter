package by.bsu.colorconverter.converter;

import by.bsu.colorconverter.model.CmykColor;
import by.bsu.colorconverter.model.RgbColor;

public final class RgbCmykConverter {

    private RgbCmykConverter() {
    }


    public static CmykColor rgbToCmyk(RgbColor rgb) {
        double r = rgb.red() / 255.0;
        double g = rgb.green() / 255.0;
        double b = rgb.blue() / 255.0;

        double k = 1.0 - Math.max(r, Math.max(g, b));

        if (Math.abs(k - 1.0) < 1e-10) {
            return new CmykColor(
                0.0,
                0.0,
                0.0,
                100.0
            );
        }

        double c = (1.0 - r - k) / (1.0 - k);
        double m = (1.0 - g - k) / (1.0 - k);
        double y = (1.0 - b - k) / (1.0 - k);

        return new CmykColor(
            c * 100.0,
            m * 100.0,
            y * 100.0,
            k * 100.0
        );
    }

    public static RgbColor cmykToRgb(CmykColor cmyk) {
        double c = cmyk.cyan() / 100.0;
        double m = cmyk.magenta() / 100.0;
        double y = cmyk.yellow() / 100.0;
        double k = cmyk.key() / 100.0;

        double r = 255.0 * (1.0 - c) * (1.0 - k);
        double g = 255.0 * (1.0 - m) * (1.0 - k);
        double b = 255.0 * (1.0 - y) * (1.0 - k);

        return new RgbColor(r, g, b);
    }
}