package by.bsu.colorconverter.converter;

import by.bsu.colorconverter.model.CmykColor;
import by.bsu.colorconverter.model.LabColor;
import by.bsu.colorconverter.model.RgbColor;
import by.bsu.colorconverter.model.XyzColor;

public final class ColorConverter {

    private ColorConverter() {
    }

    public static CmykColor rgbToCmyk(RgbColor rgb) {
        return RgbCmykConverter.rgbToCmyk(rgb);
    }

    public static RgbColor cmykToRgb(CmykColor cmyk) {
        return RgbCmykConverter.cmykToRgb(cmyk);
    }

    public static LabColor rgbToLab(RgbColor rgb) {
        XyzColor xyz =
                RgbXyzConverter.rgbToXyz(rgb);

        return XyzLabConverter.xyzToLab(xyz);
    }

    public static RgbColor labToRgb(LabColor lab) {
        XyzColor xyz =
                XyzLabConverter.labToXyz(lab);

        return RgbXyzConverter.xyzToRgb(xyz);
    }

    public static LabColor cmykToLab(CmykColor cmyk) {
        RgbColor rgb =
                RgbCmykConverter.cmykToRgb(cmyk);

        return rgbToLab(rgb);
    }

    public static CmykColor labToCmyk(LabColor lab) {
        RgbColor rgb = labToRgb(lab);

        return RgbCmykConverter.rgbToCmyk(rgb);
    }
}