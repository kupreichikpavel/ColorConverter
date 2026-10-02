package by.bsu.imageinfo.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ImageInfo {

    private final Path path;
    private final String fileName;
    private final long fileSize;
    private final ImageFormat format;
    private final ImageFormat extensionFormat;
    private final long width;
    private final long height;
    private final Double dpiX;
    private final Double dpiY;
    private final String resolutionNote;
    private final int bitDepth;
    private final String bitDepthDetail;
    private final String colorModel;
    private final String compression;
    private final FileStatus status;
    private final List<String> messages;
    private final Map<String, String> extras;
    private final long parseNanos;

    private ImageInfo(Builder b) {
        this.path = b.path;
        this.fileName = b.path.getFileName() == null ? b.path.toString() : b.path.getFileName().toString();
        this.fileSize = b.fileSize;
        this.format = b.format;
        this.extensionFormat = b.extensionFormat;
        this.width = b.width;
        this.height = b.height;
        this.dpiX = b.dpiX;
        this.dpiY = b.dpiY;
        this.resolutionNote = b.resolutionNote;
        this.bitDepth = b.bitDepth;
        this.bitDepthDetail = b.bitDepthDetail;
        this.colorModel = b.colorModel;
        this.compression = b.compression;
        this.status = b.status;
        this.messages = List.copyOf(b.messages);
        this.extras = Collections.unmodifiableMap(new LinkedHashMap<>(b.extras));
        this.parseNanos = b.parseNanos;
    }

    public static Builder builder(Path path) {
        return new Builder(path);
    }

    public Path path() {
        return path;
    }

    public String fileName() {
        return fileName;
    }

    public long fileSize() {
        return fileSize;
    }

    public ImageFormat format() {
        return format;
    }

    public ImageFormat extensionFormat() {
        return extensionFormat;
    }

    public long width() {
        return width;
    }

    public long height() {
        return height;
    }

    public Double dpiX() {
        return dpiX;
    }

    public Double dpiY() {
        return dpiY;
    }

    public String resolutionNote() {
        return resolutionNote;
    }

    public int bitDepth() {
        return bitDepth;
    }

    public String bitDepthDetail() {
        return bitDepthDetail;
    }

    public String colorModel() {
        return colorModel;
    }

    public String compression() {
        return compression;
    }

    public FileStatus status() {
        return status;
    }

    public List<String> messages() {
        return messages;
    }

    public Map<String, String> extras() {
        return extras;
    }

    public long parseNanos() {
        return parseNanos;
    }

    public boolean hasSize() {
        return width > 0 && height > 0;
    }

    public long pixelCount() {
        return hasSize() ? width * height : -1;
    }

    public String formatText() {
        return format == null ? "—" : format.displayName();
    }

    public String sizeText() {
        return hasSize() ? width + " × " + height : "—";
    }

    public String dpiText() {
        if (dpiX == null || dpiY == null) {
            return "—";
        }
        String x = Fmt.dpi(dpiX);
        String y = Fmt.dpi(dpiY);
        return x.equals(y) ? x : x + " × " + y;
    }

    public double dpiSortKey() {
        return dpiX == null ? -1 : dpiX;
    }

    public String bitDepthText() {
        return bitDepth > 0 ? bitDepth + " бит" : "—";
    }

    public String compressionText() {
        return compression == null ? "—" : compression;
    }

    public String messagesText() {
        return String.join("; ", messages);
    }

    public static final class Builder {

        private final Path path;
        private long fileSize;
        private ImageFormat format = ImageFormat.UNKNOWN;
        private ImageFormat extensionFormat;
        private long width = -1;
        private long height = -1;
        private Double dpiX;
        private Double dpiY;
        private String resolutionNote;
        private int bitDepth = -1;
        private String bitDepthDetail;
        private String colorModel;
        private String compression;
        private FileStatus status = FileStatus.OK;
        private final List<String> messages = new ArrayList<>();
        private final Map<String, String> extras = new LinkedHashMap<>();
        private long parseNanos;

        private Builder(Path path) {
            this.path = path;
        }

        public Builder fileSize(long value) {
            this.fileSize = value;
            return this;
        }

        public Builder format(ImageFormat value) {
            this.format = value;
            return this;
        }

        public Builder extensionFormat(ImageFormat value) {
            this.extensionFormat = value;
            return this;
        }

        public Builder size(long w, long h) {
            this.width = w;
            this.height = h;
            return this;
        }

        public Builder dpi(double x, double y) {
            this.dpiX = x;
            this.dpiY = y;
            return this;
        }

        public Builder resolutionNote(String note) {
            this.resolutionNote = note;
            return this;
        }

        public Builder bitDepth(int bits, String detail) {
            this.bitDepth = bits;
            this.bitDepthDetail = detail;
            return this;
        }

        public Builder colorModel(String value) {
            this.colorModel = value;
            return this;
        }

        public Builder compression(String value) {
            this.compression = value;
            return this;
        }

        public Builder extra(String key, Object value) {
            if (value != null) {
                extras.put(key, String.valueOf(value));
            }
            return this;
        }

        public Builder warning(String message) {
            return issue(FileStatus.WARNING, message);
        }

        public Builder corrupted(String message) {
            return issue(FileStatus.CORRUPTED, message);
        }

        public Builder issue(FileStatus severity, String message) {
            if (severity.isWorseThan(status)) {
                status = severity;
            }
            if (message != null && !messages.contains(message)) {
                messages.add(message);
            }
            return this;
        }

        public Builder parseNanos(long value) {
            this.parseNanos = value;
            return this;
        }

        public FileStatus status() {
            return status;
        }

        public ImageInfo build() {
            return new ImageInfo(this);
        }
    }
}
