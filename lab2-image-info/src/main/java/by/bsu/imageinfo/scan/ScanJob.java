package by.bsu.imageinfo.scan;

import by.bsu.imageinfo.model.ImageFormat;
import by.bsu.imageinfo.model.ImageInfo;
import by.bsu.imageinfo.reader.ImageInfoReader;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сканирование папки в фоне.
 *
 * <p>Отдельный поток-координатор обходит папку и отдаёт каждый файл в пул
 * рабочих потоков. Рабочий поток читает заголовки файла библиотекой
 * metadata-extractor и кладёт результат в неблокирующую очередь.
 * Интерфейс забирает результаты пачками ({@link #drainTo}) — сам он
 * никогда не ждёт диска. Класс не зависит от JavaFX и используется
 * также консольным режимом.</p>
 */
public final class ScanJob {

    private final List<Path> roots;
    private final ScanOptions options;
    private final ImageInfoReader reader = new ImageInfoReader();

    private final Queue<ImageInfo> results = new ConcurrentLinkedQueue<>();
    private final AtomicInteger total = new AtomicInteger();
    private final AtomicInteger processed = new AtomicInteger();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicBoolean enumerationDone = new AtomicBoolean();
    private final AtomicBoolean finished = new AtomicBoolean();

    private volatile long startNanos;
    private volatile long endNanos;
    private volatile String fatalError;
    private ExecutorService workers;

    public ScanJob(List<Path> roots, ScanOptions options) {
        this.roots = List.copyOf(roots);
        this.options = options;
    }

    public void start() {
        startNanos = System.nanoTime();
        workers = Executors.newFixedThreadPool(options.threads(), daemonFactory("scan-worker"));
        Thread coordinator = daemonFactory("scan-coordinator").newThread(this::run);
        coordinator.start();
    }

    private void run() {
        try {
            for (Path root : roots) {
                if (cancelled.get()) {
                    break;
                }
                if (Files.isDirectory(root)) {
                    walk(root);
                } else if (Files.isRegularFile(root)) {
                    submit(root);
                }
            }
        } catch (IOException | RuntimeException e) {
            fatalError = "Ошибка обхода папки: " + e.getMessage();
        } finally {
            enumerationDone.set(true);
            workers.shutdown();
            try {
                while (!workers.awaitTermination(200, TimeUnit.MILLISECONDS)) {
                    if (cancelled.get()) {
                        workers.shutdownNow();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            endNanos = System.nanoTime();
            finished.set(true);
        }
    }

    private void walk(Path root) throws IOException {
        int depth = options.recursive() ? Integer.MAX_VALUE : 1;
        Files.walkFileTree(root, EnumSet.noneOf(FileVisitOption.class), depth, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (cancelled.get()) {
                    return FileVisitResult.TERMINATE;
                }
                if (attrs.isRegularFile() && accept(file)) {
                    submit(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean accept(Path file) {
        return options.allFiles() || ImageFormat.hasImageExtension(file.getFileName().toString());
    }

    private void submit(Path file) {
        total.incrementAndGet();
        workers.execute(() -> {
            if (cancelled.get()) {
                return;
            }
            ImageInfo info = reader.read(file);
            results.add(info);
            processed.incrementAndGet();
        });
    }

    public void cancel() {
        cancelled.set(true);
    }

    /** Переносит накопленные результаты в список; возвращает их количество. */
    public int drainTo(List<ImageInfo> target, int max) {
        int count = 0;
        ImageInfo info;
        while (count < max && (info = results.poll()) != null) {
            target.add(info);
            count++;
        }
        return count;
    }

    public List<ImageInfo> drainAll() {
        List<ImageInfo> list = new ArrayList<>();
        drainTo(list, Integer.MAX_VALUE);
        return list;
    }

    public int total() {
        return total.get();
    }

    public int processed() {
        return processed.get();
    }

    public boolean isEnumerationDone() {
        return enumerationDone.get();
    }

    public boolean isFinished() {
        return finished.get();
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public String fatalError() {
        return fatalError;
    }

    public double elapsedSeconds() {
        long end = finished.get() ? endNanos : System.nanoTime();
        return (end - startNanos) / 1e9;
    }

    /** Ожидание завершения (для консольного режима). */
    public void await() throws InterruptedException {
        while (!finished.get()) {
            Thread.sleep(20);
        }
    }

    private static ThreadFactory daemonFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
