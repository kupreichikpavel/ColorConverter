package by.bsu.imageinfo.model;

import java.util.List;
import java.util.Locale;

/** Поддерживаемые форматы и их расширения. */
public enum ImageFormat {

    JPEG("JPEG", true, "jpg", "jpeg", "jpe", "jfif"),
    GIF("GIF", true, "gif"),
    TIFF("TIFF", false, "tif", "tiff"),
    BMP("BMP", true, "bmp", "dib"),
    PNG("PNG", true, "png"),
    PCX("PCX", false, "pcx"),
    UNKNOWN("—", false);

    private final String displayName;
    private final boolean previewSupported;
    private final List<String> extensions;

    ImageFormat(String displayName, boolean previewSupported, String... extensions) {
        this.displayName = displayName;
        this.previewSupported = previewSupported;
        this.extensions = List.of(extensions);
    }

    public String displayName() {
        return displayName;
    }

    /** Умеет ли JavaFX показать картинку этого формата (используется только для превью). */
    public boolean previewSupported() {
        return previewSupported;
    }

    public List<String> extensions() {
        return extensions;
    }

    /** Формат, которому соответствует расширение файла, или {@code null}. */
    public static ImageFormat fromFileName(String fileName) {
        String extension = extensionOf(fileName);
        if (extension.isEmpty()) {
            return null;
        }
        for (ImageFormat format : values()) {
            if (format.extensions.contains(extension)) {
                return format;
            }
        }
        return null;
    }

    public static boolean hasImageExtension(String fileName) {
        return fromFileName(fileName) != null;
    }

    public static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
