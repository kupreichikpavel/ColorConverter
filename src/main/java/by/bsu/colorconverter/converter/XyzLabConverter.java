package by.bsu.colorconverter.converter;

import by.bsu.colorconverter.model.LabColor;
import by.bsu.colorconverter.model.XyzColor;

public final class XyzLabConverter {

    private static final double XN = 95.047;
    private static final double YN = 100.0;
    private static final double ZN = 108.883;

    private XyzLabConverter() {
    }


    public static LabColor xyzToLab(XyzColor xyz) {
        double x = xyz.x() / XN;
        double y = xyz.y() / YN;
        double z = xyz.z() / ZN;

        double fx = forwardFunction(x);
        double fy = forwardFunction(y);
        double fz = forwardFunction(z);

        double l = 116.0 * fy - 16.0;
        double a = 500.0 * (fx - fy);
        double b = 200.0 * (fy - fz);

        return new LabColor(l, a, b);
    }

    public static XyzColor labToXyz(LabColor lab) {
        double fy = (lab.l() + 16.0) / 116.0;
        double fx = lab.a() / 500.0 + fy;
        double fz = fy - lab.b() / 200.0;

        double x = XN * inverseFunction(fx);
        double y = YN * inverseFunction(fy);
        double z = ZN * inverseFunction(fz);

        return new XyzColor(x, y, z);
    }

    private static double forwardFunction(double value) {
        if (value >= 0.008856) {
            return Math.cbrt(value);
        }

        return 7.787 * value + 16.0 / 116.0;
    }

    private static double inverseFunction(double value) {
        double cube = value * value * value;

        if (cube >= 0.008856) {
            return cube;
        }

        return (value - 16.0 / 116.0) / 7.787;
    }
}