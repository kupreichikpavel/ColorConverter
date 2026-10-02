package by.bsu.imageinfo.reader;

import by.bsu.imageinfo.model.FileStatus;
import by.bsu.imageinfo.model.Fmt;
import by.bsu.imageinfo.model.Glossary;
import by.bsu.imageinfo.model.ImageFormat;
import by.bsu.imageinfo.model.ImageInfo;
import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.imaging.png.PngChunkType;
import com.drew.lang.Rational;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.bmp.BmpHeaderDirectory;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.gif.GifAnimationDirectory;
import com.drew.metadata.gif.GifControlDirectory;
import com.drew.metadata.gif.GifHeaderDirectory;
import com.drew.metadata.gif.GifImageDirectory;
import com.drew.metadata.jfif.JfifDirectory;
import com.drew.metadata.jpeg.HuffmanTablesDirectory;
import com.drew.metadata.jpeg.JpegComponent;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.pcx.PcxDirectory;
import com.drew.metadata.png.PngDirectory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public final class ImageInfoReader {

    private static final double INCH_IN_METERS = 0.0254;
    private static final double INCH_IN_CM = 2.54;

    public ImageInfo read(Path path) {
        long started = System.nanoTime();
        ImageInfo.Builder b = ImageInfo.builder(path);
        String name = path.getFileName() == null ? path.toString() : path.getFileName().toString();
        ImageFormat byExtension = ImageFormat.fromFileName(name);
        b.extensionFormat(byExtension);

        try (InputStream in = new BufferedInputStream(Files.newInputStream(path), 64 * 1024)) {
            long size = Files.size(path);
            b.fileSize(size);
            if (size == 0) {
                b.issue(FileStatus.UNSUPPORTED, "Пустой файл (0 байт)");
                return b.parseNanos(System.nanoTime() - started).build();
            }

            FileType type = FileTypeDetector.detectFileType(in);
            ImageFormat format = toFormat(type);
            b.format(format);
            if (format == ImageFormat.UNKNOWN) {
                b.issue(FileStatus.UNSUPPORTED, describeUnknown(type, in));
                return b.parseNanos(System.nanoTime() - started).build();
            }
            if (byExtension != null && byExtension != format) {
                b.warning("Расширение ." + ImageFormat.extensionOf(name) + " не соответствует содержимому: "
                        + "по сигнатуре это " + format.displayName());
            }

            FileType readAs = format == ImageFormat.TIFF ? FileType.Tiff : type;
            Metadata metadata = ImageMetadataReader.readMetadata(in, size, readAs);
            switch (format) {
                case JPEG -> readJpeg(metadata, b);
                case PNG -> readPng(metadata, b);
                case GIF -> readGif(metadata, b);
                case BMP -> readBmp(metadata, b);
                case TIFF -> readTiff(metadata, b);
                case PCX -> readPcx(metadata, b);
                default -> {
                }
            }
            for (Directory directory : metadata.getDirectories()) {
                for (String error : directory.getErrors()) {
                    b.warning(directory.getName() + ": " + error);
                }
            }
        } catch (NoSuchFileException e) {
            b.issue(FileStatus.ERROR, "Файл не найден");
        } catch (AccessDeniedException e) {
            b.issue(FileStatus.ERROR, "Нет доступа к файлу");
        } catch (ImageProcessingException | IOException e) {
            b.corrupted("Не удалось прочитать заголовок: " + e.getMessage());
        } catch (RuntimeException e) {
            b.corrupted("Ошибка разбора файла: " + e.getClass().getSimpleName()
                    + (e.getMessage() == null ? "" : " — " + e.getMessage()));
        }
        return b.parseNanos(System.nanoTime() - started).build();
    }

    private static void readJpeg(Metadata metadata, ImageInfo.Builder b) {
        JpegDirectory jpeg = metadata.getFirstDirectoryOfType(JpegDirectory.class);
        if (jpeg == null) {
            b.corrupted("Не найден маркер начала кадра SOF");
            return;
        }
        Integer width = jpeg.getInteger(JpegDirectory.TAG_IMAGE_WIDTH);
        Integer height = jpeg.getInteger(JpegDirectory.TAG_IMAGE_HEIGHT);
        Integer precision = jpeg.getInteger(JpegDirectory.TAG_DATA_PRECISION);
        Integer components = jpeg.getInteger(JpegDirectory.TAG_NUMBER_OF_COMPONENTS);
        Integer sof = jpeg.getInteger(JpegDirectory.TAG_COMPRESSION_TYPE);

        size(b, width, height);
        if (precision != null && components != null) {
            b.bitDepth(precision * components, precision + " бит × " + components
                    + (components == 1 ? " компонента" : " компоненты"));
            b.colorModel(switch (components) {
                case 1 -> "оттенки серого";
                case 3 -> "YCbCr";
                case 4 -> "CMYK";
                default -> components + " компонент";
            });
        }
        if (sof != null) {
            b.compression(jpegCompression(sof));
            b.extra(Glossary.JPEG_SOF, "SOF" + sof);
        }
        if (components != null && components == 3) {
            b.extra(Glossary.JPEG_SUBSAMPLING, subsampling(jpeg));
        }
        HuffmanTablesDirectory huffman = metadata.getFirstDirectoryOfType(HuffmanTablesDirectory.class);
        if (huffman != null) {
            try {
                b.extra(Glossary.JPEG_HUFFMAN, huffman.getNumberOfTables());
            } catch (com.drew.metadata.MetadataException ignored) {
            }
        }

        JfifDirectory jfif = metadata.getFirstDirectoryOfType(JfifDirectory.class);
        ExifIFD0Directory exif = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        boolean resolved = false;
        if (jfif != null) {
            Integer units = jfif.getInteger(JfifDirectory.TAG_UNITS);
            Integer x = jfif.getInteger(JfifDirectory.TAG_RESX);
            Integer y = jfif.getInteger(JfifDirectory.TAG_RESY);
            b.extra(Glossary.JPEG_JFIF, "единицы " + units + ", плотность " + x + " × " + y);
            if (units != null && x != null && y != null && x > 0 && y > 0) {
                if (units == 1) {
                    b.dpi(x, y);
                    resolved = true;
                } else if (units == 2) {
                    b.dpi(x * INCH_IN_CM, y * INCH_IN_CM);
                    resolved = true;
                } else {
                    b.resolutionNote("в JFIF задано только соотношение сторон пикселя " + x + ":" + y);
                }
            }
        }
        if (exif != null) {
            readCameraInfo(exif, b);
            if (!resolved) {
                resolved = exifResolution(exif, b);
            }
        }
        if (!resolved && jfif == null) {
            b.resolutionNote("не указано (нет JFIF и Exif)");
        }
    }

    private static String jpegCompression(int sof) {
        return switch (sof) {
            case 0 -> "DCT базовый, с потерями";
            case 1, 9 -> "DCT расширенный, с потерями";
            case 2, 10 -> "DCT прогрессивный, с потерями";
            case 3, 11 -> "Lossless, без потерь";
            case 5, 13 -> "DCT дифф. последовательный";
            case 6, 14 -> "DCT дифф. прогрессивный";
            case 7, 15 -> "Lossless дифф., без потерь";
            default -> "SOF" + sof;
        } + (sof >= 9 ? ", арифм." : "");
    }

    private static String subsampling(JpegDirectory jpeg) {
        JpegComponent y = jpeg.getComponent(0);
        JpegComponent cb = jpeg.getComponent(1);
        if (y == null || cb == null || cb.getHorizontalSamplingFactor() == 0 || cb.getVerticalSamplingFactor() == 0) {
            return "—";
        }
        int h = y.getHorizontalSamplingFactor() / cb.getHorizontalSamplingFactor();
        int v = y.getVerticalSamplingFactor() / cb.getVerticalSamplingFactor();
        String scheme;
        if (h == 1 && v == 1) {
            scheme = "4:4:4";
        } else if (h == 2 && v == 1) {
            scheme = "4:2:2";
        } else if (h == 2 && v == 2) {
            scheme = "4:2:0";
        } else if (h == 1 && v == 2) {
            scheme = "4:4:0";
        } else if (h == 4 && v == 1) {
            scheme = "4:1:1";
        } else {
            scheme = "нестандартная";
        }
        return scheme + " (Y " + y.getHorizontalSamplingFactor() + "×" + y.getVerticalSamplingFactor()
                + ", Cb/Cr " + cb.getHorizontalSamplingFactor() + "×" + cb.getVerticalSamplingFactor() + ")";
    }

    private static void readPng(Metadata metadata, ImageInfo.Builder b) {
        b.resolutionNote("не указано (нет чанка pHYs)");
        boolean headerFound = false;
        for (PngDirectory png : metadata.getDirectoriesOfType(PngDirectory.class)) {
            PngChunkType chunk = png.getPngChunkType();
            if (PngChunkType.IHDR.equals(chunk)) {
                headerFound = true;
                Integer bits = png.getInteger(PngDirectory.TAG_BITS_PER_SAMPLE);
                Integer colorType = png.getInteger(PngDirectory.TAG_COLOR_TYPE);
                size(b, png.getInteger(PngDirectory.TAG_IMAGE_WIDTH), png.getInteger(PngDirectory.TAG_IMAGE_HEIGHT));
                if (bits != null && colorType != null) {
                    int channels = switch (colorType) {
                        case 2 -> 3;
                        case 4 -> 2;
                        case 6 -> 4;
                        default -> 1;
                    };
                    b.bitDepth(bits * channels, channels > 1
                            ? bits + " бит × " + channels + " канала"
                            : bits + " бит на " + (colorType == 3 ? "индекс палитры" : "пиксель"));
                    b.colorModel(pngColorType(colorType));
                    b.extra(Glossary.PNG_COLOR_TYPE, colorType + " — " + pngColorType(colorType));
                    b.extra(Glossary.PNG_BITS_PER_CHANNEL, bits);
                }
                Integer compression = png.getInteger(PngDirectory.TAG_COMPRESSION_TYPE);
                b.compression(compression != null && compression == 0 ? "Deflate" : "метод " + compression);
                Integer filter = png.getInteger(PngDirectory.TAG_FILTER_METHOD);
                b.extra(Glossary.PNG_FILTER, filter + (filter != null && filter == 0
                        ? " — адаптивная фильтрация строк (None, Sub, Up, Average, Paeth)" : ""));
                Integer interlace = png.getInteger(PngDirectory.TAG_INTERLACE_METHOD);
                b.extra(Glossary.INTERLACE, interlace != null && interlace == 1 ? "Adam7" : "нет");
            } else if (PngChunkType.PLTE.equals(chunk)) {
                Integer colors = png.getInteger(PngDirectory.TAG_PALETTE_SIZE);
                if (colors != null) {
                    b.extra(Glossary.PALETTE_SIZE, Fmt.colors(colors));
                }
            } else if (PngChunkType.pHYs.equals(chunk)) {
                Integer x = png.getInteger(PngDirectory.TAG_PIXELS_PER_UNIT_X);
                Integer y = png.getInteger(PngDirectory.TAG_PIXELS_PER_UNIT_Y);
                Integer unit = png.getInteger(PngDirectory.TAG_UNIT_SPECIFIER);
                if (x != null && y != null && unit != null && unit == 1) {
                    b.dpi(x * INCH_IN_METERS, y * INCH_IN_METERS);
                    b.resolutionNote(null);
                    b.extra(Glossary.RAW_RESOLUTION, x + " × " + y + " пикселей на метр");
                } else if (x != null && y != null) {
                    b.resolutionNote("единицы не заданы, соотношение сторон пикселя " + x + ":" + y);
                }
            } else if (PngChunkType.gAMA.equals(chunk)) {
                Double gamma = png.getDoubleObject(PngDirectory.TAG_GAMMA);
                if (gamma != null) {
                    b.extra(Glossary.PNG_GAMMA, Fmt.number(gamma));
                }
            } else if (PngChunkType.tRNS.equals(chunk)) {
                b.extra(Glossary.TRANSPARENCY, "есть (чанк tRNS)");
            }
        }
        if (!headerFound) {
            b.corrupted("Не найден чанк IHDR");
        }
    }

    private static String pngColorType(int type) {
        return switch (type) {
            case 0 -> "оттенки серого";
            case 2 -> "RGB";
            case 3 -> "индексированный (палитра)";
            case 4 -> "оттенки серого + альфа";
            case 6 -> "RGBA";
            default -> "неизвестный";
        };
    }

    private static void readGif(Metadata metadata, ImageInfo.Builder b) {
        GifHeaderDirectory header = metadata.getFirstDirectoryOfType(GifHeaderDirectory.class);
        if (header == null) {
            b.corrupted("Не найден заголовок GIF");
            return;
        }
        size(b, header.getInteger(GifHeaderDirectory.TAG_IMAGE_WIDTH), header.getInteger(GifHeaderDirectory.TAG_IMAGE_HEIGHT));
        b.compression("LZW");
        b.resolutionNote("формат GIF не хранит разрешение");
        b.extra(Glossary.GIF_VERSION, header.getString(GifHeaderDirectory.TAG_GIF_FORMAT_VERSION));

        Boolean global = header.getBooleanObject(GifHeaderDirectory.TAG_HAS_GLOBAL_COLOR_TABLE);
        Integer tableSize = header.getInteger(GifHeaderDirectory.TAG_COLOR_TABLE_SIZE);
        int depth = 0;
        if (Boolean.TRUE.equals(global) && tableSize != null) {
            depth = Integer.numberOfTrailingZeros(tableSize);
            b.extra(Glossary.PALETTE_SIZE, Fmt.colors(tableSize) + " (глобальная палитра)");
        }
        Integer colorResolution = header.getInteger(GifHeaderDirectory.TAG_BITS_PER_PIXEL);
        if (colorResolution != null) {
            b.extra(Glossary.GIF_COLOR_RESOLUTION, colorResolution + " бит на канал");
        }

        var frames = metadata.getDirectoriesOfType(GifImageDirectory.class);
        if (!frames.isEmpty()) {
            GifImageDirectory first = frames.iterator().next();
            if (depth == 0) {
                Integer localBits = first.getInteger(GifImageDirectory.TAG_LOCAL_COLOUR_TABLE_BITS_PER_PIXEL);
                if (localBits != null) {
                    depth = localBits;
                    b.extra(Glossary.PALETTE_SIZE, Fmt.colors(1L << localBits) + " (локальная палитра кадра)");
                }
            }
            b.extra(Glossary.INTERLACE, Boolean.TRUE.equals(first.getBooleanObject(GifImageDirectory.TAG_IS_INTERLACED))
                    ? "да" : "нет");
        }
        if (depth > 0) {
            b.bitDepth(depth, depth + " бит на индекс палитры (до " + Fmt.colors(1L << depth) + ")");
            b.colorModel("индексированный (палитра)");
        }
        b.extra(Glossary.GIF_FRAMES, frames.size() + (frames.size() > 1 ? " (анимация)" : ""));

        GifAnimationDirectory animation = metadata.getFirstDirectoryOfType(GifAnimationDirectory.class);
        if (animation != null) {
            Integer loops = animation.getInteger(GifAnimationDirectory.TAG_ITERATION_COUNT);
            b.extra(Glossary.GIF_LOOP, loops == null || loops == 0 ? "бесконечно" : loops);
        }
        boolean transparent = false;
        for (GifControlDirectory control : metadata.getDirectoriesOfType(GifControlDirectory.class)) {
            transparent |= Boolean.TRUE.equals(control.getBooleanObject(GifControlDirectory.TAG_TRANSPARENT_COLOR_FLAG));
        }
        b.extra(Glossary.TRANSPARENCY, transparent ? "есть прозрачный цвет" : "нет");
    }

    private static void readBmp(Metadata metadata, ImageInfo.Builder b) {
        BmpHeaderDirectory bmp = metadata.getFirstDirectoryOfType(BmpHeaderDirectory.class);
        if (bmp == null) {
            b.corrupted("Не найден заголовок BMP");
            return;
        }
        Integer height = bmp.getInteger(BmpHeaderDirectory.TAG_IMAGE_HEIGHT);
        size(b, bmp.getInteger(BmpHeaderDirectory.TAG_IMAGE_WIDTH), height == null ? null : Math.abs(height));

        Integer bits = bmp.getInteger(BmpHeaderDirectory.TAG_BITS_PER_PIXEL);
        if (bits != null) {
            b.bitDepth(bits, bits + " бит на пиксель");
            b.colorModel(switch (bits) {
                case 1, 2, 4, 8 -> "индексированный (палитра)";
                case 16 -> "RGB High Color";
                case 24 -> "RGB True Color";
                case 32 -> "RGB + альфа/резерв";
                default -> "—";
            });
        }
        Integer compression = bmp.getInteger(BmpHeaderDirectory.TAG_COMPRESSION);
        if (compression != null) {
            b.compression(bmpCompression(compression));
        }

        Integer x = bmp.getInteger(BmpHeaderDirectory.TAG_X_PIXELS_PER_METER);
        Integer y = bmp.getInteger(BmpHeaderDirectory.TAG_Y_PIXELS_PER_METER);
        if (x != null && y != null && x > 0 && y > 0) {
            b.dpi(x * INCH_IN_METERS, y * INCH_IN_METERS);
            b.extra(Glossary.RAW_RESOLUTION, x + " × " + y + " пикселей на метр");
        } else {
            b.resolutionNote("не указано (biXPelsPerMeter = 0)");
        }

        Integer palette = bmp.getInteger(BmpHeaderDirectory.TAG_PALETTE_COLOUR_COUNT);
        if (bits != null && bits <= 8) {
            int colors = palette != null && palette > 0 ? palette : 1 << bits;
            b.extra(Glossary.PALETTE_SIZE, Fmt.colors(colors) + " (записи по 4 байта: B, G, R, резерв)");
        }
        Integer headerSize = bmp.getInteger(BmpHeaderDirectory.TAG_HEADER_SIZE);
        if (headerSize != null) {
            b.extra(Glossary.BMP_HEADER, headerName(headerSize));
        }
        if (height != null) {
            b.extra(Glossary.BMP_ROW_ORDER, height < 0 ? "сверху вниз" : "снизу вверх");
        }
    }

    private static String bmpCompression(int code) {
        return switch (code) {
            case 0 -> "нет (BI_RGB)";
            case 1 -> "RLE 8 бит (BI_RLE8)";
            case 2 -> "RLE 4 бита (BI_RLE4)";
            case 3 -> "нет, битовые маски (BI_BITFIELDS)";
            case 4 -> "JPEG (BI_JPEG)";
            case 5 -> "PNG (BI_PNG)";
            case 6 -> "нет, маски с альфой (BI_ALPHABITFIELDS)";
            default -> "код " + code;
        };
    }

    private static String headerName(int size) {
        return switch (size) {
            case 12 -> "BITMAPCOREHEADER (OS/2 1.x), 12 байт";
            case 40 -> "BITMAPINFOHEADER, 40 байт";
            case 64 -> "OS/2 2.x, 64 байта";
            case 108 -> "BITMAPV4HEADER, 108 байт";
            case 124 -> "BITMAPV5HEADER, 124 байта";
            default -> size + " байт";
        };
    }

    private static void readTiff(Metadata metadata, ImageInfo.Builder b) {
        ExifIFD0Directory ifd = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (ifd == null) {
            b.corrupted("Не найден первый каталог IFD");
            return;
        }
        size(b, ifd.getInteger(ExifIFD0Directory.TAG_IMAGE_WIDTH), ifd.getInteger(ExifIFD0Directory.TAG_IMAGE_HEIGHT));

        int[] bitsPerSample = ifd.getIntArray(ExifIFD0Directory.TAG_BITS_PER_SAMPLE);
        Integer samples = ifd.getInteger(ExifIFD0Directory.TAG_SAMPLES_PER_PIXEL);
        int channels = samples == null ? 1 : samples;
        if (bitsPerSample == null) {
            bitsPerSample = new int[]{1};
        }
        int total = 0;
        StringBuilder detail = new StringBuilder();
        for (int i = 0; i < bitsPerSample.length; i++) {
            total += bitsPerSample[i];
            detail.append(i == 0 ? "" : ", ").append(bitsPerSample[i]);
        }
        if (bitsPerSample.length == 1 && channels > 1) {
            total *= channels;
        }
        b.bitDepth(total, channels + (channels == 1 ? " канал" : " канала") + ", бит на канал: " + detail);

        Integer compression = ifd.getInteger(ExifIFD0Directory.TAG_COMPRESSION);
        b.compression(tiffCompression(compression == null ? 1 : compression));
        Integer photometric = ifd.getInteger(ExifIFD0Directory.TAG_PHOTOMETRIC_INTERPRETATION);
        if (photometric != null) {
            b.colorModel(tiffPhotometric(photometric));
        }
        if (!exifResolution(ifd, b)) {
            b.resolutionNote("не указано (нет тегов XResolution/YResolution)");
        }
        readCameraInfo(ifd, b);

        Integer planar = ifd.getInteger(ExifIFD0Directory.TAG_PLANAR_CONFIGURATION);
        if (planar != null) {
            b.extra(Glossary.TIFF_PLANAR, planar == 2 ? "раздельные плоскости" : "каналы пикселя вместе");
        }
    }

    private static String tiffCompression(int code) {
        return switch (code) {
            case 1 -> "нет";
            case 2 -> "CCITT 1D (RLE)";
            case 3 -> "CCITT Group 3 (факс)";
            case 4 -> "CCITT Group 4 (факс)";
            case 5 -> "LZW";
            case 6, 7 -> "JPEG";
            case 8, 32946 -> "Deflate";
            case 32773 -> "PackBits (RLE)";
            case 34712 -> "JPEG 2000";
            case 50000 -> "Zstandard";
            default -> "код " + code;
        };
    }

    private static String tiffPhotometric(int code) {
        return switch (code) {
            case 0 -> "оттенки серого (0 = белый)";
            case 1 -> "оттенки серого (0 = чёрный)";
            case 2 -> "RGB";
            case 3 -> "индексированный (палитра)";
            case 4 -> "маска прозрачности";
            case 5 -> "CMYK";
            case 6 -> "YCbCr";
            case 8 -> "CIE L*a*b*";
            default -> "код " + code;
        };
    }

    private static void readPcx(Metadata metadata, ImageInfo.Builder b) {
        PcxDirectory pcx = metadata.getFirstDirectoryOfType(PcxDirectory.class);
        if (pcx == null) {
            b.corrupted("Не найден заголовок PCX");
            return;
        }
        Integer xMin = pcx.getInteger(PcxDirectory.TAG_XMIN);
        Integer yMin = pcx.getInteger(PcxDirectory.TAG_YMIN);
        Integer xMax = pcx.getInteger(PcxDirectory.TAG_XMAX);
        Integer yMax = pcx.getInteger(PcxDirectory.TAG_YMAX);
        if (xMin != null && yMin != null && xMax != null && yMax != null) {
            size(b, xMax - xMin + 1, yMax - yMin + 1);
        }
        Integer bits = pcx.getInteger(PcxDirectory.TAG_BITS_PER_PIXEL);
        Integer planes = pcx.getInteger(PcxDirectory.TAG_COLOR_PLANES);
        if (bits != null && planes != null) {
            b.bitDepth(bits * planes, bits + " бит × " + planes + (planes == 1 ? " плоскость" : " плоскости"));
            b.colorModel(bits == 8 && planes >= 3 ? "RGB (по плоскости на канал)" : "индексированный (палитра)");
            b.extra(Glossary.PCX_PLANES, planes);
        }
        b.compression("RLE (PCX)");

        Integer hDpi = pcx.getInteger(PcxDirectory.TAG_HORIZONTAL_DPI);
        Integer vDpi = pcx.getInteger(PcxDirectory.TAG_VERTICAL_DPI);
        if (hDpi != null && vDpi != null && hDpi > 0 && vDpi > 0) {
            b.dpi(hDpi, vDpi);
        } else {
            b.resolutionNote("не указано (HDpi/VDpi = 0)");
        }
        Integer version = pcx.getInteger(PcxDirectory.TAG_VERSION);
        if (version != null) {
            b.extra(Glossary.PCX_VERSION, version + " — " + switch (version) {
                case 0 -> "PC Paintbrush 2.5";
                case 2 -> "PC Paintbrush 2.8 с палитрой";
                case 3 -> "PC Paintbrush 2.8 без палитры";
                case 4 -> "PC Paintbrush for Windows";
                case 5 -> "PC Paintbrush 3.0 и новее";
                default -> "неизвестная";
            });
        }
        Integer bytesPerLine = pcx.getInteger(PcxDirectory.TAG_BYTES_PER_LINE);
        if (bytesPerLine != null) {
            b.extra(Glossary.PCX_BYTES_PER_LINE, bytesPerLine);
        }
    }

    private static boolean exifResolution(Directory ifd, ImageInfo.Builder b) {
        Rational x = ifd.getRational(ExifIFD0Directory.TAG_X_RESOLUTION);
        Rational y = ifd.getRational(ExifIFD0Directory.TAG_Y_RESOLUTION);
        if (x == null || y == null || x.doubleValue() <= 0 || y.doubleValue() <= 0) {
            return false;
        }
        Integer unit = ifd.getInteger(ExifIFD0Directory.TAG_RESOLUTION_UNIT);
        int u = unit == null ? 2 : unit;
        if (u == 2) {
            b.dpi(x.doubleValue(), y.doubleValue());
            b.extra(Glossary.RAW_RESOLUTION, Fmt.number(x.doubleValue()) + " × " + Fmt.number(y.doubleValue())
                    + " точек на дюйм (теги 282/283)");
            return true;
        }
        if (u == 3) {
            b.dpi(x.doubleValue() * INCH_IN_CM, y.doubleValue() * INCH_IN_CM);
            b.extra(Glossary.RAW_RESOLUTION, Fmt.number(x.doubleValue()) + " × " + Fmt.number(y.doubleValue())
                    + " точек на сантиметр (теги 282/283)");
            return true;
        }
        b.resolutionNote("единицы не заданы (ResolutionUnit = 1)");
        return false;
    }

    private static void readCameraInfo(Directory ifd, ImageInfo.Builder b) {
        text(ifd, ExifIFD0Directory.TAG_MAKE, Glossary.MAKE, b);
        text(ifd, ExifIFD0Directory.TAG_MODEL, Glossary.MODEL, b);
        text(ifd, ExifIFD0Directory.TAG_SOFTWARE, Glossary.SOFTWARE, b);
        text(ifd, ExifIFD0Directory.TAG_DATETIME, Glossary.DATE, b);
        Integer orientation = ifd.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
        if (orientation != null) {
            b.extra(Glossary.ORIENTATION, orientation + " — " + switch (orientation) {
                case 1 -> "нормальная";
                case 3 -> "повёрнута на 180°";
                case 6 -> "повёрнута на 90° по часовой";
                case 8 -> "повёрнута на 90° против часовой";
                default -> "с отражением";
            });
        }
    }

    private static void text(Directory directory, int tag, String key, ImageInfo.Builder b) {
        String value = directory.getString(tag);
        if (value != null && !value.isBlank()) {
            b.extra(key, value.trim());
        }
    }

    private static void size(ImageInfo.Builder b, Integer width, Integer height) {
        if (width != null && height != null) {
            b.size(width, height);
        }
    }

    private static ImageFormat toFormat(FileType type) {
        return switch (type) {
            case Jpeg -> ImageFormat.JPEG;
            case Png -> ImageFormat.PNG;
            case Gif -> ImageFormat.GIF;
            case Bmp -> ImageFormat.BMP;
            case Tiff, Arw, Cr2, Nef, Orf, Rw2 -> ImageFormat.TIFF;
            case Pcx -> ImageFormat.PCX;
            default -> ImageFormat.UNKNOWN;
        };
    }

    private static String describeUnknown(FileType type, InputStream in) throws IOException {
        if (type != FileType.Unknown) {
            return "Формат " + type.getName() + " (" + type.getLongName() + ") не входит в задание";
        }
        in.mark(512);
        byte[] head = in.readNBytes(512);
        in.reset();
        int printable = 0;
        for (byte value : head) {
            int c = value & 0xFF;
            if (c == '\t' || c == '\n' || c == '\r' || c >= 0x20) {
                printable++;
            }
        }
        if (head.length > 0 && printable >= head.length * 0.95) {
            return "Похоже на текстовый файл";
        }
        return "Сигнатура " + Fmt.hexBytes(head, 8) + " не распознана";
    }
}
