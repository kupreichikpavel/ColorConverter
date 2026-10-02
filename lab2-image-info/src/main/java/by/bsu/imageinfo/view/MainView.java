package by.bsu.imageinfo.view;

import by.bsu.imageinfo.model.FileStatus;
import by.bsu.imageinfo.model.Fmt;
import by.bsu.imageinfo.model.ImageFormat;
import by.bsu.imageinfo.model.ImageInfo;
import by.bsu.imageinfo.scan.ScanOptions;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.function.Function;

public final class MainView extends BorderPane {

    final Button openFolderButton = new Button("Открыть папку…");
    final Button openFilesButton = new Button("Добавить файлы…");
    final Button stopButton = new Button("Остановить");
    final Button exportButton = new Button("Экспорт в CSV…");
    final Button clearButton = new Button("Очистить");
    final CheckBox recursiveBox = new CheckBox("Включая подпапки");
    final CheckBox allFilesBox = new CheckBox("Проверять все файлы по сигнатуре");
    final Spinner<Integer> threadsSpinner = new Spinner<>(1, 64, ScanOptions.defaultThreads());

    final TextField searchField = new TextField();
    final ComboBox<Object> statusFilter = new ComboBox<>();
    final ComboBox<Object> formatFilter = new ComboBox<>();
    final Label shownLabel = new Label();

    final TableView<ImageInfo> table = new TableView<>();
    TableColumn<ImageInfo, ImageInfo> nameColumn;
    final DetailsPane details = new DetailsPane();

    final ProgressBar progressBar = new ProgressBar(0);
    final Label progressLabel = new Label("Готово к работе");
    final Label speedLabel = new Label();
    final Label countersLabel = new Label();

    static final String ALL = "Все";

    public MainView() {
        getStyleClass().add("main");
        setTop(buildTop());
        setCenter(buildCenter());
        setBottom(buildStatusBar());
    }

    private VBox buildTop() {
        openFolderButton.getStyleClass().add("primary");
        stopButton.setDisable(true);
        recursiveBox.setSelected(true);
        allFilesBox.setTooltip(new Tooltip("Без галочки берутся только файлы с расширениями jpg, jpeg, gif, tif, tiff, "
                + "bmp, png, pcx.\nС галочкой проверяется каждый файл папки: формат определяется по содержимому."));
        threadsSpinner.setPrefWidth(76);
        threadsSpinner.setEditable(true);
        threadsSpinner.setTooltip(new Tooltip("Сколько файлов читается одновременно"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(8,
                openFolderButton, openFilesButton, stopButton,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                recursiveBox, allFilesBox,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                new Label("Потоков:"), threadsSpinner,
                spacer, clearButton, exportButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getStyleClass().add("toolbar");

        searchField.setPromptText("Поиск по имени файла");
        searchField.setPrefWidth(260);
        statusFilter.getItems().add(ALL);
        statusFilter.getItems().addAll((Object[]) FileStatus.values());
        statusFilter.setValue(ALL);
        formatFilter.getItems().add(ALL);
        for (ImageFormat format : ImageFormat.values()) {
            if (format != ImageFormat.UNKNOWN) {
                formatFilter.getItems().add(format);
            }
        }
        formatFilter.setValue(ALL);
        shownLabel.getStyleClass().add("muted");

        HBox filters = new HBox(8, searchField, new Label("Статус:"), statusFilter,
                new Label("Формат:"), formatFilter, shownLabel);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.getStyleClass().add("filters");

        return new VBox(actions, filters);
    }

    private SplitPane buildCenter() {
        buildTable();
        SplitPane split = new SplitPane(table, details);
        split.setDividerPositions(0.64);
        SplitPane.setResizableWithParent(details, false);
        return split;
    }

    private HBox buildStatusBar() {
        progressBar.setPrefWidth(240);
        progressBar.setMinWidth(120);
        speedLabel.getStyleClass().add("muted");
        speedLabel.setMinWidth(0);
        progressLabel.setMinWidth(Region.USE_PREF_SIZE);
        countersLabel.setMinWidth(Region.USE_PREF_SIZE);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(12, progressBar, progressLabel, speedLabel, spacer, countersLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 12, 8, 12));
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    private void buildTable() {
        table.setPlaceholder(new Label("Откройте папку или перетащите сюда файлы и папки"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ImageInfo, ImageInfo> name = column("Имя файла", 230,
                Comparator.comparing(ImageInfo::fileName, NaturalOrder.INSTANCE)
                        .thenComparing(info -> info.path().toString()),
                ImageInfo::fileName);
        nameColumn = name;
        TableColumn<ImageInfo, ImageInfo> format = column("Формат", 60,
                Comparator.comparing(ImageInfo::formatText), ImageInfo::formatText);
        TableColumn<ImageInfo, ImageInfo> size = column("Размер, px", 100,
                Comparator.comparingLong(ImageInfo::pixelCount), ImageInfo::sizeText);
        TableColumn<ImageInfo, ImageInfo> dpi = column("DPI", 70,
                Comparator.comparingDouble(ImageInfo::dpiSortKey), ImageInfo::dpiText);
        TableColumn<ImageInfo, ImageInfo> depth = column("Глубина", 75,
                Comparator.comparingInt(ImageInfo::bitDepth), ImageInfo::bitDepthText);
        TableColumn<ImageInfo, ImageInfo> compression = column("Сжатие", 190,
                Comparator.comparing(ImageInfo::compressionText), ImageInfo::compressionText);
        TableColumn<ImageInfo, ImageInfo> fileSize = column("Объём", 75,
                Comparator.comparingLong(ImageInfo::fileSize), info -> Fmt.bytes(info.fileSize()));
        TableColumn<ImageInfo, ImageInfo> status = new TableColumn<>("Статус");
        status.setPrefWidth(120);
        status.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        status.setComparator(Comparator.comparing(ImageInfo::status));
        status.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(ImageInfo item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeIf(style -> style.startsWith("status-"));
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item.status().shortName());
                getStyleClass().add(item.status().styleClass());
            }
        });

        name.setMinWidth(170);
        status.setMinWidth(135);
        status.setMaxWidth(150);
        format.setMinWidth(65);
        format.setMaxWidth(70);
        size.setMinWidth(90);
        size.setMaxWidth(120);
        dpi.setMinWidth(50);
        dpi.setMaxWidth(90);
        depth.setMinWidth(72);
        depth.setMaxWidth(80);
        compression.setMinWidth(130);
        fileSize.setMinWidth(65);
        fileSize.setMaxWidth(85);
        table.getColumns().addAll(name, status, format, size, dpi, depth, compression, fileSize);

        table.setRowFactory(view -> new TableRow<>() {
            @Override
            protected void updateItem(ImageInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.messages().isEmpty()) {
                    setTooltip(null);
                } else {
                    setTooltip(new Tooltip(String.join("\n", item.messages())));
                }
            }
        });
    }

    private static TableColumn<ImageInfo, ImageInfo> column(String title, double width,
                                                            Comparator<ImageInfo> comparator,
                                                            Function<ImageInfo, String> text) {
        TableColumn<ImageInfo, ImageInfo> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        column.setComparator(comparator);
        column.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(ImageInfo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : text.apply(item));
            }
        });
        return column;
    }
}
