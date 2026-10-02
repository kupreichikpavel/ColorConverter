package by.bsu.imageinfo.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class Fmt {

    private Fmt() {
    }

    public static String number(double value) {
        return number(value, 2);
    }

    public static String dpi(double value) {
        return number(value, 1);
    }

    public static String number(double value, int scale) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "—";
        }
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros();
        return rounded.scale() < 0 ? rounded.setScale(0).toPlainString() : rounded.toPlainString();
    }

    public static String bytes(long size) {
        if (size < 1024) {
            return size + " Б";
        }
        String[] units = {"КБ", "МБ", "ГБ", "ТБ"};
        double value = size;
        int unit = -1;
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024;
            unit++;
        }
        return String.format(Locale.ROOT, value >= 100 ? "%.0f %s" : "%.1f %s", value, units[unit]);
    }

    public static String hex(long value, int digits) {
        return String.format("0x%0" + digits + "X", value);
    }

    public static String hexBytes(byte[] data, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(count, data.length); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(String.format("%02X", data[i] & 0xFF));
        }
        return sb.toString();
    }

    public static String colors(long count) {
        long mod100 = count % 100;
        long mod10 = count % 10;
        String word;
        if (mod100 >= 11 && mod100 <= 14) {
            word = "цветов";
        } else if (mod10 == 1) {
            word = "цвет";
        } else if (mod10 >= 2 && mod10 <= 4) {
            word = "цвета";
        } else {
            word = "цветов";
        }
        return count + " " + word;
    }
}
