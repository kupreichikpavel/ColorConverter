package by.bsu.imageinfo.scan;

/**
 * @param recursive обходить подпапки
 * @param allFiles  проверять все файлы по сигнатуре, а не только с расширениями изображений
 * @param threads   число рабочих потоков
 */
public record ScanOptions(boolean recursive, boolean allFiles, int threads) {

    public ScanOptions {
        threads = Math.max(1, Math.min(threads, 64));
    }

    /** Чтение заголовков — короткие операции ввода-вывода, поэтому потоков берём больше, чем ядер. */
    public static int defaultThreads() {
        int cores = Runtime.getRuntime().availableProcessors();
        return Math.max(4, Math.min(32, cores * 2));
    }
}
