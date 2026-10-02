package by.bsu.imageinfo.scan;

public record ScanOptions(boolean recursive, boolean allFiles, int threads) {

    public ScanOptions {
        threads = Math.max(1, Math.min(threads, 64));
    }

    public static int defaultThreads() {
        int cores = Runtime.getRuntime().availableProcessors();
        return Math.max(4, Math.min(32, cores * 2));
    }
}
