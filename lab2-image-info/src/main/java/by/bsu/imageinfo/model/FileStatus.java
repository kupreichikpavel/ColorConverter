package by.bsu.imageinfo.model;

public enum FileStatus {

    OK("OK", "OK", "ok"),
    WARNING("Предупреждение", "Внимание", "warning"),
    UNSUPPORTED("Не изображение", "Не изображение", "unsupported"),
    CORRUPTED("Файл повреждён", "Повреждён", "corrupted"),
    ERROR("Ошибка чтения", "Ошибка", "error");

    private final String displayName;
    private final String shortName;
    private final String styleClass;

    FileStatus(String displayName, String shortName, String styleClass) {
        this.displayName = displayName;
        this.shortName = shortName;
        this.styleClass = styleClass;
    }

    public String shortName() {
        return shortName;
    }

    public String displayName() {
        return displayName;
    }

    public String styleClass() {
        return "status-" + styleClass;
    }

    public boolean isWorseThan(FileStatus other) {
        return ordinal() > other.ordinal();
    }

    @Override
    public String toString() {
        return displayName;
    }
}
