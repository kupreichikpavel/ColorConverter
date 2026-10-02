package by.bsu.imageinfo;

import by.bsu.imageinfo.model.FileStatus;
import by.bsu.imageinfo.model.Glossary;
import by.bsu.imageinfo.model.ImageFormat;
import by.bsu.imageinfo.model.ImageInfo;
import by.bsu.imageinfo.reader.ImageInfoReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageInfoReaderTest {

    @TempDir
    Path dir;

    private final ImageInfoReader reader = new ImageInfoReader();

    private ImageInfo read(String name, byte[] data) throws IOException {
        Path file = dir.resolve(name);
        Files.write(file, data);
        return reader.read(file);
    }

    @Test
    void bmp8() throws IOException {
        ImageInfo info = read("a.bmp", TestImages.bmp8(33, 20, 3780));
        assertEquals(FileStatus.OK, info.status(), info.messagesText());
        assertEquals(ImageFormat.BMP, info.format());
        assertEquals(33, info.width());
        assertEquals(20, info.height());
        assertEquals(8, info.bitDepth());
        assertEquals("96", info.dpiText());
        assertEquals("нет (BI_RGB)", info.compression());
        assertTrue(info.extras().get(Glossary.PALETTE_SIZE).startsWith("256"));
    }

    @Test
    void bmp24TopDownWithoutResolution() throws IOException {
        ImageInfo info = read("b.bmp", TestImages.bmp24(10, 7));
        assertEquals(24, info.bitDepth());
        assertEquals(7, info.height());
        assertNull(info.dpiX());
        assertEquals("сверху вниз", info.extras().get(Glossary.BMP_ROW_ORDER));
    }

    @Test
    void pngRgbWithPhys() throws IOException {
        ImageInfo info = read("a.png", TestImages.png(640, 480, 8, 2, 11811));
        assertEquals(FileStatus.OK, info.status(), info.messagesText());
        assertEquals(640, info.width());
        assertEquals(480, info.height());
        assertEquals(24, info.bitDepth());
        assertEquals("300", info.dpiText());
        assertEquals("Deflate", info.compression());
        assertTrue(info.extras().get(Glossary.PNG_FILTER).startsWith("0"));
    }

    @Test
    void pngDepthIsBitsTimesChannels() throws IOException {
        assertEquals(64, read("rgba.png", TestImages.png(4, 4, 16, 6, null)).bitDepth());
        assertEquals(4, read("p.png", TestImages.png(4, 4, 4, 3, null)).bitDepth());
    }

    @Test
    void jpegBaseline() throws IOException {
        ImageInfo info = read("a.jpg", TestImages.jpeg(1920, 1080, 3, 72, false));
        assertEquals(FileStatus.OK, info.status(), info.messagesText());
        assertEquals(1920, info.width());
        assertEquals(1080, info.height());
        assertEquals(24, info.bitDepth());
        assertEquals("72", info.dpiText());
        assertTrue(info.compression().startsWith("DCT базовый"));
        assertTrue(info.extras().get(Glossary.JPEG_SUBSAMPLING).startsWith("4:2:0"));
    }

    @Test
    void jpegProgressiveGray() throws IOException {
        ImageInfo info = read("g.jpg", TestImages.jpeg(100, 50, 1, 300, true));
        assertEquals(8, info.bitDepth());
        assertEquals("оттенки серого", info.colorModel());
        assertTrue(info.compression().startsWith("DCT прогрессивный"));
    }

    @Test
    void gifDepthComesFromPaletteSize() throws IOException {
        ImageInfo info = read("a.gif", TestImages.gif(300, 200, 4, 3));
        assertEquals(FileStatus.OK, info.status(), info.messagesText());
        assertEquals(300, info.width());
        assertEquals(4, info.bitDepth());
        assertEquals("LZW", info.compression());
        assertNull(info.dpiX());
        assertTrue(info.extras().get(Glossary.GIF_FRAMES).startsWith("3"));
    }

    @Test
    void tiffBothByteOrders() throws IOException {
        for (boolean little : new boolean[]{true, false}) {
            ImageInfo info = read("a" + little + ".tif", TestImages.tiff(12, 9, 150, little));
            assertEquals(ImageFormat.TIFF, info.format());
            assertEquals(FileStatus.OK, info.status(), info.messagesText());
            assertEquals(12, info.width());
            assertEquals(9, info.height());
            assertEquals(24, info.bitDepth());
            assertEquals("150", info.dpiText());
            assertEquals("нет", info.compression());
        }
    }

    @Test
    void pcx8() throws IOException {
        ImageInfo info = read("a.pcx", TestImages.pcx8(101, 40, 200));
        assertEquals(FileStatus.OK, info.status(), info.messagesText());
        assertEquals(101, info.width());
        assertEquals(40, info.height());
        assertEquals(8, info.bitDepth());
        assertEquals("200", info.dpiText());
        assertEquals("RLE (PCX)", info.compression());
    }

    @Test
    void textRenamedToJpgIsNotAnImage() throws IOException {
        ImageInfo info = read("fake.jpg", "просто текст\n".repeat(20).getBytes(StandardCharsets.UTF_8));
        assertEquals(FileStatus.UNSUPPORTED, info.status());
        assertTrue(info.messagesText().contains("текстовый"));
    }

    @Test
    void pngRenamedToJpgIsDetectedBySignature() throws IOException {
        ImageInfo info = read("photo.jpg", TestImages.png(5, 5, 8, 2, null));
        assertEquals(ImageFormat.PNG, info.format());
        assertEquals(FileStatus.WARNING, info.status());
        assertEquals(5, info.width());
    }

    @Test
    void emptyFile() throws IOException {
        assertEquals(FileStatus.UNSUPPORTED, read("empty.png", new byte[0]).status());
    }

    @Test
    void damagedFilesNeverCrashTheReader() throws IOException {
        Random random = new Random(7);
        byte[][] samples = {
                TestImages.bmp8(20, 20, 0), TestImages.png(20, 20, 8, 6, 2835),
                TestImages.jpeg(20, 20, 3, 72, false), TestImages.gif(20, 20, 4, 2),
                TestImages.tiff(20, 20, 72, false), TestImages.pcx8(20, 20, 72)
        };
        for (byte[] sample : samples) {
            for (int i = 0; i < 200; i++) {
                byte[] damaged = TestImages.truncate(sample, 1 + random.nextInt(sample.length));
                for (int k = 0; k < 4; k++) {
                    int position = random.nextInt(Math.min(damaged.length, 200));
                    damaged[position] = (byte) random.nextInt(256);
                }
                assertNotNull(read("fuzz.bin", damaged).status());
            }
        }
    }
}
