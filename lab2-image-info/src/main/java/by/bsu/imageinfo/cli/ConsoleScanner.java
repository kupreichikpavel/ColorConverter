package by.bsu.imageinfo.cli;

import by.bsu.imageinfo.model.FileStatus;
import by.bsu.imageinfo.model.ImageInfo;
import by.bsu.imageinfo.scan.ScanJob;
import by.bsu.imageinfo.scan.ScanOptions;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Консольный режим: {@code --cli <папка|файл>... [--all] [--flat] [--threads N] [--details] [--quiet]}.
 * Удобен для замера скорости и для проверки без графического интерфейса.
 */
public final class ConsoleScanner {

    private ConsoleScanner() {
    }

    public static void run(String[] args) throws InterruptedException {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        List<Path> roots = new ArrayList<>();
        boolean all = false;
        boolean recursive = true;
        boolean details = false;
        boolean quiet = false;
        int threads = ScanOptions.defaultThreads();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--cli" -> {
                }
                case "--all" -> all = true;
                case "--flat" -> recursive = false;
                case "--details" -> details = true;
                case "--quiet" -> quiet = true;
                case "--threads" -> threads = Integer.parseInt(args[++i]);
                default -> roots.add(Path.of(args[i]));
            }
        }
        if (roots.isEmpty()) {
            out.println("Использование: --cli <папка|файл>... [--all] [--flat] [--threads N] [--details] [--quiet]");
            return;
        }

        ScanJob job = new ScanJob(roots, new ScanOptions(recursive, all, threads));
        job.start();
        job.await();
        List<ImageInfo> results = job.drainAll();
        results.sort(Comparator.comparing(info -> info.path().toString(), String.CASE_INSENSITIVE_ORDER));

        if (!quiet) {
            out.printf("%-34s %-5s %-12s %-10s %-8s %-40s %s%n",
                    "Файл", "Тип", "Размер, px", "dpi", "Глубина", "Сжатие", "Статус");
            for (ImageInfo info : results) {
                out.printf("%-34s %-5s %-12s %-10s %-8s %-40s %s%n",
                        cut(info.fileName(), 34), info.formatText(), info.sizeText(), info.dpiText(),
                        info.bitDepthText(), cut(info.compressionText(), 40), info.status().displayName()
                                + (info.messages().isEmpty() ? "" : ": " + info.messagesText()));
                if (details) {
                    info.extras().forEach((key, value) ->
                            out.println("      " + key + ": " + value.replace("\n", "\n        ")));
                }
            }
        }

        Map<FileStatus, Integer> counts = new EnumMap<>(FileStatus.class);
        results.forEach(info -> counts.merge(info.status(), 1, Integer::sum));
        double seconds = job.elapsedSeconds();
        out.printf(Locale.ROOT, "%nФайлов: %d, время: %.3f с, %.0f файлов/с, потоков: %d%n",
                results.size(), seconds, results.size() / Math.max(seconds, 1e-9), threads);
        out.println("Статусы: " + counts);
    }

    private static String cut(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
