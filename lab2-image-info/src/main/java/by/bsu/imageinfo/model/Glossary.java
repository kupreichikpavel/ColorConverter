package by.bsu.imageinfo.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Названия всех выводимых характеристик и пояснения к ним:
 * что это, зачем нужно и в каком месте файла хранится
 * (значения оттуда читает библиотека metadata-extractor).
 */
public final class Glossary {

    // ---------- основные колонки ----------
    public static final String FILE_NAME = "Имя файла";
    public static final String FORMAT = "Формат";
    public static final String IMAGE_SIZE = "Размер изображения";
    public static final String RESOLUTION = "Разрешение";
    public static final String BIT_DEPTH = "Глубина цвета";
    public static final String COMPRESSION = "Сжатие";
    public static final String COLOR_MODEL = "Цветовая модель";
    public static final String FILE_SIZE = "Размер файла";
    public static final String STATUS = "Статус";

    // ---------- общие ----------
    public static final String PALETTE_SIZE = "Размер палитры";
    public static final String INTERLACE = "Чересстрочность";
    public static final String TRANSPARENCY = "Прозрачность";
    public static final String ORIENTATION = "Ориентация";
    public static final String RAW_RESOLUTION = "Разрешение, как записано в файле";
    public static final String DATE = "Дата";
    public static final String SOFTWARE = "Программа";
    public static final String MAKE = "Производитель камеры";
    public static final String MODEL = "Модель камеры";

    // ---------- BMP ----------
    public static final String BMP_HEADER = "Заголовок DIB";
    public static final String BMP_ROW_ORDER = "Порядок строк";

    // ---------- PNG ----------
    public static final String PNG_COLOR_TYPE = "Тип цвета (color type)";
    public static final String PNG_BITS_PER_CHANNEL = "Битов на канал";
    public static final String PNG_FILTER = "Метод фильтрации";
    public static final String PNG_GAMMA = "Гамма (gAMA)";

    // ---------- JPEG ----------
    public static final String JPEG_SOF = "Маркер кадра (SOF)";
    public static final String JPEG_SUBSAMPLING = "Субдискретизация цветности";
    public static final String JPEG_HUFFMAN = "Таблицы Хаффмана";
    public static final String JPEG_JFIF = "JFIF";

    // ---------- GIF ----------
    public static final String GIF_VERSION = "Версия GIF";
    public static final String GIF_COLOR_RESOLUTION = "Цветовое разрешение";
    public static final String GIF_FRAMES = "Кадров";
    public static final String GIF_LOOP = "Повторов анимации";

    // ---------- TIFF ----------
    public static final String TIFF_PLANAR = "Планарная конфигурация";

    // ---------- PCX ----------
    public static final String PCX_VERSION = "Версия PCX";
    public static final String PCX_PLANES = "Цветовых плоскостей";
    public static final String PCX_BYTES_PER_LINE = "Байт в строке плоскости";

    private static final Map<String, String> EXPLANATIONS = new HashMap<>();

    static {
        put(FILE_NAME, "Имя файла на диске. Берётся из файловой системы, а не из содержимого файла.");
        put(FORMAT, "Фактический формат, определённый по сигнатуре (magic number) в первых байтах файла, "
                + "а не по расширению: BMP — «BM», PNG — 89 50 4E 47, JPEG — FF D8 FF, GIF — «GIF8», "
                + "TIFF — «II*» или «MM*», PCX — байт 0x0A.");
        put(IMAGE_SIZE, "Ширина × высота в пикселях. Хранится в заголовке: BITMAPINFOHEADER (BMP), чанк IHDR (PNG), "
                + "маркер SOF (JPEG), Logical Screen Descriptor (GIF), теги 256/257 (TIFF), "
                + "Xmax − Xmin + 1 и Ymax − Ymin + 1 (PCX).");
        put(RESOLUTION, "Сколько точек приходится на дюйм при печати (dpi). На экранный вид не влияет, нужно, "
                + "чтобы при печати получился правильный физический размер. Источник: biXPelsPerMeter (BMP, "
                + "пикселей на метр × 0.0254), чанк pHYs (PNG), сегмент JFIF или Exif (JPEG), теги 282/283/296 "
                + "(TIFF), поля HDpi/VDpi (PCX). GIF разрешение не хранит.");
        put(BIT_DEPTH, "Число бит на один пиксель; определяет, сколько цветов можно закодировать (2^n): "
                + "1 бит — чёрно-белое, 8 бит — 256 цветов или оттенков серого, 24 бита — True Color "
                + "(по 8 бит на R, G, B), 32 бита — RGB + альфа.");
        put(COMPRESSION, "Алгоритм упаковки пиксельных данных. RLE, LZW, Deflate, PackBits — без потерь; "
                + "DCT в JPEG — с потерями. Хранится в поле biCompression (BMP), IHDR (PNG), номере маркера SOF "
                + "(JPEG), теге 259 (TIFF); в GIF всегда LZW, в PCX — RLE.");
        put(COLOR_MODEL, "Как трактуются значения пикселей: индексы палитры, оттенки серого, RGB, YCbCr, CMYK.");
        put(FILE_SIZE, "Размер файла на диске.");
        put(STATUS, "OK — заголовок прочитан; «Внимание» — расширение не совпадает с содержимым или библиотека "
                + "сообщила о проблеме; «Повреждён» — заголовок прочитать не удалось; «Не изображение» — "
                + "сигнатура не относится к поддерживаемым форматам.");

        put(PALETTE_SIZE, "Число цветов в таблице цветов (палитре). При глубине ≤ 8 бит пиксель хранит не цвет, "
                + "а номер записи в палитре; сама запись содержит значения R, G, B.");
        put(INTERLACE, "Чересстрочное хранение строк: при загрузке картинка сначала появляется грубо, затем "
                + "уточняется. PNG — Adam7 (7 проходов), GIF — 4 прохода.");
        put(TRANSPARENCY, "Есть ли прозрачные пиксели: чанк tRNS в PNG, флаг прозрачного цвета в GIF.");
        put(ORIENTATION, "Как повернуть картинку при показе (тег 274). Камера пишет пиксели «как сняла», "
                + "а поворот хранит отдельно.");
        put(RAW_RESOLUTION, "Разрешение в тех единицах, в которых оно записано в файле (пиксели на метр, точки на "
                + "дюйм или сантиметр), до пересчёта в dpi.");
        put(DATE, "Дата и время создания/изменения (тег 306 DateTime).");
        put(SOFTWARE, "Программа, создавшая файл (тег 305 Software).");
        put(MAKE, "Производитель камеры или сканера (тег 271 Make).");
        put(MODEL, "Модель устройства (тег 272 Model).");

        put(BMP_HEADER, "Тип информационного заголовка определяется его размером: 12 — BITMAPCOREHEADER, "
                + "40 — BITMAPINFOHEADER, 108 — BITMAPV4HEADER, 124 — BITMAPV5HEADER.");
        put(BMP_ROW_ORDER, "Положительная высота в заголовке — строки хранятся снизу вверх, "
                + "отрицательная — сверху вниз.");

        put(PNG_COLOR_TYPE, "Байт Color type чанка IHDR: 0 — серый, 2 — RGB, 3 — палитра, 4 — серый + альфа, "
                + "6 — RGBA. Определяет число каналов.");
        put(PNG_BITS_PER_CHANNEL, "Байт Bit depth чанка IHDR — бит на канал (1, 2, 4, 8, 16). "
                + "Глубина пикселя = бит на канал × число каналов.");
        put(PNG_FILTER, "Байт Filter method чанка IHDR. Значение 0 — адаптивная фильтрация: перед сжатием каждая "
                + "строка обрабатывается одним из 5 фильтров, чтобы данные лучше сжимались Deflate.");
        put(PNG_GAMMA, "Чанк gAMA — гамма, с которой закодированы яркости; нужна для одинакового вида на разных мониторах.");

        put(JPEG_SOF, "Номер маркера начала кадра SOFn задаёт режим кодирования: SOF0 — базовый DCT, "
                + "SOF2 — прогрессивный, SOF3 — без потерь, SOF9 и выше — с арифметическим кодированием.");
        put(JPEG_SUBSAMPLING, "Во сколько раз цветовые компоненты Cb/Cr хранятся с меньшим разрешением, чем яркость Y "
                + "(глаз менее чувствителен к цвету). 4:4:4 — без прореживания, 4:2:0 — вдвое по обеим осям. "
                + "Считается по коэффициентам дискретизации компонент в SOF.");
        put(JPEG_HUFFMAN, "Число таблиц Хаффмана (сегменты DHT), которыми кодируются квантованные коэффициенты DCT.");
        put(JPEG_JFIF, "Сегмент APP0 «JFIF»: единицы плотности (0 — только соотношение сторон, 1 — точек на дюйм, "
                + "2 — точек на см) и плотность по X и Y.");

        put(GIF_VERSION, "Сигнатура GIF87a или GIF89a. В 89a добавлены прозрачность, анимация и комментарии.");
        put(GIF_COLOR_RESOLUTION, "Поле Color Resolution: сколько бит на канал было у исходного изображения до "
                + "построения палитры. Информационное, на глубину цвета файла не влияет.");
        put(GIF_FRAMES, "Число кадров (блоков Image Descriptor). Больше одного — анимация.");
        put(GIF_LOOP, "Из расширения NETSCAPE2.0: сколько раз проигрывать анимацию (0 — бесконечно).");

        put(TIFF_PLANAR, "Тег 284: каналы пикселя хранятся вместе (RGBRGB…) или раздельными плоскостями (RRR…GGG…).");

        put(PCX_VERSION, "Байт 1 заголовка: версия PC Paintbrush, от неё зависят возможности (5 — 24 бита и 256 цветов).");
        put(PCX_PLANES, "Число цветовых плоскостей. 24-битный PCX хранит R, G, B тремя плоскостями по 8 бит.");
        put(PCX_BYTES_PER_LINE, "Сколько байт занимает строка одной плоскости после распаковки RLE.");
    }

    private Glossary() {
    }

    private static void put(String key, String explanation) {
        EXPLANATIONS.put(key, explanation);
    }

    public static String explain(String key) {
        return EXPLANATIONS.getOrDefault(key, "");
    }
}
