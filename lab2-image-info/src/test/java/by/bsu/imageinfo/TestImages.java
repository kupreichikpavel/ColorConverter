package by.bsu.imageinfo;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;

final class TestImages {

    private TestImages() {
    }

    static byte[] bmp8(int width, int height, int pixelsPerMeter) {
        int stride = (width + 3) / 4 * 4;
        int palette = 256 * 4;
        int offset = 14 + 40 + palette;
        int size = offset + stride * height;
        ByteBuffer b = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        b.put((byte) 'B').put((byte) 'M').putInt(size).putInt(0).putInt(offset);
        b.putInt(40).putInt(width).putInt(height).putShort((short) 1).putShort((short) 8)
                .putInt(0).putInt(stride * height).putInt(pixelsPerMeter).putInt(pixelsPerMeter)
                .putInt(256).putInt(0);
        for (int i = 0; i < 256; i++) {
            b.put((byte) i).put((byte) i).put((byte) i).put((byte) 0);
        }
        return b.array();
    }

    static byte[] bmp24(int width, int height) {
        int stride = (width * 3 + 3) / 4 * 4;
        int offset = 54;
        int size = offset + stride * height;
        ByteBuffer b = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        b.put((byte) 'B').put((byte) 'M').putInt(size).putInt(0).putInt(offset);
        b.putInt(40).putInt(width).putInt(-height).putShort((short) 1).putShort((short) 24)
                .putInt(0).putInt(0).putInt(0).putInt(0).putInt(0).putInt(0);
        return b.array();
    }

    static byte[] png(int width, int height, int bitDepth, int colorType, Integer pixelsPerMeter) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        ByteBuffer ihdr = ByteBuffer.allocate(13);
        ihdr.putInt(width).putInt(height).put((byte) bitDepth).put((byte) colorType)
                .put((byte) 0).put((byte) 0).put((byte) 0);
        chunk(out, "IHDR", ihdr.array());
        if (colorType == 3) {
            byte[] plte = new byte[4 * 3];
            chunk(out, "PLTE", plte);
        }
        if (pixelsPerMeter != null) {
            ByteBuffer phys = ByteBuffer.allocate(9);
            phys.putInt(pixelsPerMeter).putInt(pixelsPerMeter).put((byte) 1);
            chunk(out, "pHYs", phys.array());
        }
        chunk(out, "IDAT", new byte[]{0x78, 0x01, 0x01, 0x00, 0x00, (byte) 0xFF, (byte) 0xFF});
        chunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static void chunk(ByteArrayOutputStream out, String type, byte[] data) {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        out.writeBytes(ByteBuffer.allocate(4).putInt(data.length).array());
        out.writeBytes(typeBytes);
        out.writeBytes(data);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        out.writeBytes(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }

    static byte[] jpeg(int width, int height, int components, int dpi, boolean progressive) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[]{(byte) 0xFF, (byte) 0xD8});
        out.writeBytes(new byte[]{(byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0, 1, 1, 1,
                (byte) (dpi >> 8), (byte) dpi, (byte) (dpi >> 8), (byte) dpi, 0, 0});
        out.writeBytes(new byte[]{(byte) 0xFF, (byte) 0xDB, 0, 67, 0});
        byte[] table = new byte[64];
        Arrays.fill(table, (byte) 1);
        out.writeBytes(table);
        int length = 8 + components * 3;
        out.writeBytes(new byte[]{(byte) 0xFF, (byte) (progressive ? 0xC2 : 0xC0), 0, (byte) length, 8,
                (byte) (height >> 8), (byte) height, (byte) (width >> 8), (byte) width, (byte) components});
        for (int i = 1; i <= components; i++) {
            int sampling = (i == 1 && components == 3) ? 0x22 : 0x11;
            out.writeBytes(new byte[]{(byte) i, (byte) sampling, (byte) (i == 1 ? 0 : 1)});
        }
        out.writeBytes(new byte[]{(byte) 0xFF, (byte) 0xDA, 0, 8, 1, 1, 0, 0, 63, 0});
        out.writeBytes(new byte[]{0x12, 0x34, 0x56, 0x78});
        out.writeBytes(new byte[]{(byte) 0xFF, (byte) 0xD9});
        return out.toByteArray();
    }

    static byte[] gif(int width, int height, int bits, int frames) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes("GIF89a".getBytes(StandardCharsets.US_ASCII));
        out.writeBytes(new byte[]{(byte) width, (byte) (width >> 8), (byte) height, (byte) (height >> 8),
                (byte) (0x80 | 0x70 | (bits - 1)), 0, 0});
        out.writeBytes(new byte[3 << bits]);
        for (int f = 0; f < frames; f++) {
            out.writeBytes(new byte[]{0x21, (byte) 0xF9, 4, 1, 10, 0, 0, 0});
            out.writeBytes(new byte[]{0x2C, 0, 0, 0, 0, (byte) width, (byte) (width >> 8),
                    (byte) height, (byte) (height >> 8), 0});
            out.writeBytes(new byte[]{(byte) Math.max(2, bits), 2, 0x4C, 0x01, 0});
        }
        out.write(0x3B);
        return out.toByteArray();
    }

    static byte[] tiff(int width, int height, int dpi, boolean littleEndian) {
        ByteOrder order = littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        int entries = 11;
        int ifdOffset = 8;
        int ifdSize = 2 + entries * 12 + 4;
        int bpsOffset = ifdOffset + ifdSize;
        int xResOffset = bpsOffset + 6;
        int yResOffset = xResOffset + 8;
        int dataOffset = yResOffset + 8;
        int dataSize = width * height * 3;
        ByteBuffer b = ByteBuffer.allocate(dataOffset + dataSize).order(order);
        b.put(littleEndian ? (byte) 'I' : (byte) 'M').put(littleEndian ? (byte) 'I' : (byte) 'M');
        b.putShort((short) 42).putInt(ifdOffset);
        b.putShort((short) entries);
        entry(b, 256, 4, 1, width);
        entry(b, 257, 4, 1, height);
        entry(b, 258, 3, 3, bpsOffset);
        entryShort(b, 259, 1);
        entryShort(b, 262, 2);
        entry(b, 273, 4, 1, dataOffset);
        entryShort(b, 277, 3);
        entry(b, 279, 4, 1, dataSize);
        entry(b, 282, 5, 1, xResOffset);
        entry(b, 283, 5, 1, yResOffset);
        entryShort(b, 296, 2);
        b.putInt(0);
        b.putShort((short) 8).putShort((short) 8).putShort((short) 8);
        b.putInt(dpi).putInt(1);
        b.putInt(dpi).putInt(1);
        return b.array();
    }

    private static void entry(ByteBuffer b, int tag, int type, int count, int value) {
        b.putShort((short) tag).putShort((short) type).putInt(count).putInt(value);
    }

    private static void entryShort(ByteBuffer b, int tag, int value) {
        b.putShort((short) tag).putShort((short) 3).putInt(1).putShort((short) value).putShort((short) 0);
    }

    static byte[] pcx8(int width, int height, int dpi) {
        int bytesPerLine = (width + 1) / 2 * 2;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteBuffer h = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN);
        h.put((byte) 0x0A).put((byte) 5).put((byte) 1).put((byte) 8);
        h.putShort((short) 0).putShort((short) 0).putShort((short) (width - 1)).putShort((short) (height - 1));
        h.putShort((short) dpi).putShort((short) dpi);
        h.position(65);
        h.put((byte) 1).putShort((short) bytesPerLine).putShort((short) 1);
        out.writeBytes(h.array());
        for (int y = 0; y < height; y++) {
            int left = bytesPerLine;
            while (left > 0) {
                int run = Math.min(63, left);
                out.write(0xC0 | run);
                out.write(0x55);
                left -= run;
            }
        }
        out.write(0x0C);
        for (int i = 0; i < 256; i++) {
            out.write(i);
            out.write(i);
            out.write(i);
        }
        return out.toByteArray();
    }

    static byte[] truncate(byte[] data, int length) {
        return Arrays.copyOf(data, length);
    }
}
