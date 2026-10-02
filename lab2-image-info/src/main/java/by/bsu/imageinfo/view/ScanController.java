package by.bsu.imageinfo.view;

import by.bsu.imageinfo.model.FileStatus;
import by.bsu.imageinfo.model.Fmt;
import by.bsu.imageinfo.model.ImageInfo;
import by.bsu.imageinfo.scan.ScanJob;
import by.bsu.imageinfo.scan.ScanOptions;
import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Связывает интерфейс со сканированием.
 *
 * <p>Сканирование идёт в фоновых потоках ({@link ScanJob}). UI-поток каждые
 * ~50 мс забирает из очереди готовые результаты пачкой и обновляет прогресс-бар.
 * Таблица пересобирается не на каждый файл, а не чаще раза в 100–400 мс:
 * фильтр + одна сортировка O(n log n) + {@code setAll}. Инкрементальная
 * вставка в SortedList на сотнях тысяч строк квадратична и «вешает» UI,
 * поэтому она не используется.</p>
 */
public final class ScanController {

    private static final long REFRESH_NANOS = 50_000_000L;
    private static final int MAX_BATCH = 20_000;

    private final MainView view;
    /** Все результаты текущего сканирования. */
    private final List<ImageInfo> all = new ArrayList<>();
    /** То, что показано в таблице (после фильтра; сортирует сама таблица). */
    private final ObservableList<ImageInfo> visible = FXCollections.observableArrayList();
    private final Map<FileStatus, Integer> counters = new EnumMap<>(FileStatus.class);
    private final List<ImageInfo> batch = new ArrayList<>();

    private ScanJob job;
    private File lastDirectory;
    private long lastRefresh;
    private long lastRebuild;
    private long lastRebuildCost;
    private boolean dirty;
    private boolean rebuilding;

    private final AnimationTimer refresher = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (now - lastRefresh >= REFRESH_NANOS) {
                lastRefresh = now;
                pullResults();
            }
        }
    };

    public ScanController(MainView view) {
        this.view = view;

        view.table.setItems(visible);
        view.table.getSortOrder().setAll(List.of(view.nameColumn));

        view.table.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, selected) -> {
                    if (!rebuilding) {
                        view.details.show(selected);
                    }
                });

        view.openFolderButton.setOnAction(e -> chooseFolder());
        view.openFilesButton.setOnAction(e -> chooseFiles());
        view.stopButton.setOnAction(e -> stop());
        view.clearButton.setOnAction(e -> clear());
        view.exportButton.setOnAction(e -> exportCsv());

        view.searchField.textProperty().addListener((obs, o, n) -> rebuild());
        view.statusFilter.valueProperty().addListener((obs, o, n) -> rebuild());
        view.formatFilter.valueProperty().addListener((obs, o, n) -> rebuild());

        view.setOnDragOver(this::onDragOver);
        view.setOnDragDropped(this::onDragDropped);

        updateCounters();
    }

    // ---------------------------------------------------------------- действия

    private void chooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Папка с изображениями");
        if (lastDirectory != null && lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(lastDirectory);
        }
        File folder = chooser.showDialog(window());
        if (folder != null) {
            lastDirectory = folder;
            startScan(List.of(folder.toPath()), true);
        }
    }

    private void chooseFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Файлы изображений");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Изображения",
                        "*.jpg", "*.jpeg", "*.jpe", "*.jfif", "*.gif", "*.tif", "*.tiff",
                        "*.bmp", "*.dib", "*.png", "*.pcx"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*"));
        if (lastDirectory != null && lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(lastDirectory);
        }
        List<File> files = chooser.showOpenMultipleDialog(window());
        if (files != null && !files.isEmpty()) {
            lastDirectory = files.get(0).getParentFile();
            startScan(files.stream().map(File::toPath).toList(), false);
        }
    }

    private void onDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    private void onDragDropped(DragEvent event) {
        List<File> files = event.getDragboard().getFiles();
        boolean ok = files != null && !files.isEmpty() && (job == null || job.isFinished());
        if (ok) {
            startScan(files.stream().map(File::toPath).toList(), true);
        }
        event.setDropCompleted(ok);
        event.consume();
    }

    /** Сканирование путей, переданных при запуске. */
    public void scan(List<Path> roots) {
        startScan(roots, true);
    }

    /**
     * @param replace очистить таблицу перед сканированием (папка) или дополнить её (отдельные файлы)
     */
    private void startScan(List<Path> roots, boolean replace) {
        if (job != null && !job.isFinished()) {
            return;
        }
        if (replace) {
            clear();
        }
        ScanOptions options = new ScanOptions(
                view.recursiveBox.isSelected(),
                view.allFilesBox.isSelected(),
                view.threadsSpinner.getValue());
        job = new ScanJob(roots, options);
        setRunning(true);
        view.progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        view.progressLabel.setText("Поиск файлов…");
        job.start();
        refresher.start();
    }

    private void stop() {
        if (job != null) {
            job.cancel();
            view.progressLabel.setText("Остановка…");
        }
    }

    private void clear() {
        if (job != null && !job.isFinished()) {
            return;
        }
        all.clear();
        visible.clear();
        counters.clear();
        view.details.show(null);
        view.progressBar.setProgress(0);
        view.progressLabel.setText("Готово к работе");
        view.speedLabel.setText("");
        updateCounters();
    }

    // ---------------------------------------------------------------- обновление UI

    private void pullResults() {
        if (job == null) {
            refresher.stop();
            return;
        }
        batch.clear();
        job.drainTo(batch, MAX_BATCH);
        if (!batch.isEmpty()) {
            for (ImageInfo info : batch) {
                counters.merge(info.status(), 1, Integer::sum);
            }
            all.addAll(batch);
            dirty = true;
            updateCounters();
        }
        long now = System.nanoTime();
        // пересборка не чаще, чем раз в 5× её собственную длительность (не меньше 150 мс)
        long interval = Math.max(150_000_000L, lastRebuildCost * 5);
        if (dirty && now - lastRebuild >= interval) {
            rebuild();
        }

        int total = job.total();
        int processed = job.processed();
        double seconds = job.elapsedSeconds();
        if (job.isEnumerationDone() && total > 0) {
            view.progressBar.setProgress((double) processed / total);
        }
        view.progressLabel.setText(job.isEnumerationDone()
                ? String.format("Обработано %d из %d", processed, total)
                : String.format("Найдено %d, готово %d…", total, processed));
        view.speedLabel.setText(String.format(Locale.ROOT, "%.2f с, %.0f файл/с",
                seconds, processed / Math.max(seconds, 1e-3)));

        if (job.isFinished()) {
            finish();
        }
    }

    private void finish() {
        List<ImageInfo> rest = job.drainAll();
        if (!rest.isEmpty()) {
            rest.forEach(info -> counters.merge(info.status(), 1, Integer::sum));
            all.addAll(rest);
            updateCounters();
        }
        rebuild();
        refresher.stop();
        setRunning(false);
        view.progressBar.setProgress(job.total() == 0 ? 0 : 1);
        String state = job.isCancelled() ? "Остановлено" : "Готово";
        view.progressLabel.setText(String.format("%s: %d из %d файлов", state, job.processed(), job.total()));
        if (job.total() == 0) {
            view.progressLabel.setText("Изображений не найдено"
                    + (view.allFilesBox.isSelected() ? "" : " (проверьте расширения или включите проверку всех файлов)"));
        }
        if (job.fatalError() != null) {
            new Alert(Alert.AlertType.WARNING, job.fatalError()).showAndWait();
        }
    }

    private void setRunning(boolean running) {
        view.stopButton.setDisable(!running);
        view.openFolderButton.setDisable(running);
        view.openFilesButton.setDisable(running);
        view.clearButton.setDisable(running);
        view.recursiveBox.setDisable(running);
        view.allFilesBox.setDisable(running);
        view.threadsSpinner.setDisable(running);
    }

    private void updateCounters() {
        view.countersLabel.setText(String.format("OK: %d   Внимание: %d   Повреждены: %d   Не изображения: %d%s",
                counters.getOrDefault(FileStatus.OK, 0),
                counters.getOrDefault(FileStatus.WARNING, 0),
                counters.getOrDefault(FileStatus.CORRUPTED, 0),
                counters.getOrDefault(FileStatus.UNSUPPORTED, 0),
                counters.containsKey(FileStatus.ERROR) ? "   Ошибки: " + counters.get(FileStatus.ERROR) : ""));
        updateShown();
    }

    /** Пересобирает видимый список: фильтр, затем сортировка по текущей колонке таблицы. */
    private void rebuild() {
        long started = System.nanoTime();
        dirty = false;
        String query = view.searchField.getText() == null ? "" : view.searchField.getText().trim().toLowerCase(Locale.ROOT);
        Object status = view.statusFilter.getValue();
        Object format = view.formatFilter.getValue();
        boolean noFilter = query.isEmpty()
                && (status == null || status == MainView.ALL)
                && (format == null || format == MainView.ALL);

        List<ImageInfo> list;
        if (noFilter) {
            list = new ArrayList<>(all);
        } else {
            list = new ArrayList<>();
            for (ImageInfo info : all) {
                if ((query.isEmpty() || info.fileName().toLowerCase(Locale.ROOT).contains(query))
                        && (status == null || status == MainView.ALL || info.status() == status)
                        && (format == null || format == MainView.ALL || info.format() == format)) {
                    list.add(info);
                }
            }
        }
        Comparator<ImageInfo> comparator = view.table.getComparator();
        if (comparator != null) {
            list.sort(comparator);
        }

        ImageInfo selected = view.table.getSelectionModel().getSelectedItem();
        rebuilding = true;
        try {
            visible.setAll(list);
            if (selected != null) {
                int index = list.indexOf(selected);
                if (index >= 0) {
                    view.table.getSelectionModel().select(index);
                } else {
                    view.details.show(null);
                }
            }
        } finally {
            rebuilding = false;
        }
        updateShown();
        lastRebuild = System.nanoTime();
        lastRebuildCost = lastRebuild - started;
    }

    private void updateShown() {
        view.shownLabel.setText(visible.size() == all.size()
                ? "Файлов: " + all.size()
                : "Показано " + visible.size() + " из " + all.size());
    }

    // ---------------------------------------------------------------- экспорт

    private void exportCsv() {
        if (view.table.getItems().isEmpty()) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить таблицу");
        chooser.setInitialFileName("image-info.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (Excel)", "*.csv"));
        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write('\uFEFF');
            writer.write("Путь;Имя файла;Формат;Ширина;Высота;DPI X;DPI Y;Глубина цвета, бит;"
                    + "Цветовая модель;Сжатие;Размер файла, байт;Статус;Сообщения\n");
            for (ImageInfo info : view.table.getItems()) {
                writer.write(String.join(";",
                        csv(info.path().toAbsolutePath().toString()), csv(info.fileName()), csv(info.formatText()),
                        info.hasSize() ? String.valueOf(info.width()) : "",
                        info.hasSize() ? String.valueOf(info.height()) : "",
                        info.dpiX() == null ? "" : Fmt.dpi(info.dpiX()),
                        info.dpiY() == null ? "" : Fmt.dpi(info.dpiY()),
                        info.bitDepth() > 0 ? String.valueOf(info.bitDepth()) : "",
                        csv(info.colorModel()), csv(info.compression()),
                        String.valueOf(info.fileSize()), csv(info.status().displayName()), csv(info.messagesText())));
                writer.write('\n');
            }
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Не удалось сохранить файл: " + e.getMessage()).showAndWait();
        }
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"").replace('\n', ' ') + "\"";
    }

    private Window window() {
        Node node = view;
        return node.getScene() == null ? null : node.getScene().getWindow();
    }
}
