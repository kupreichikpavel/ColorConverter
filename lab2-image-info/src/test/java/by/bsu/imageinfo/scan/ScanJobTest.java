package by.bsu.imageinfo.scan;

import by.bsu.imageinfo.model.ImageInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScanJobTest {

    @TempDir
    Path dir;

    private void prepare() throws IOException {
        Path sub = Files.createDirectory(dir.resolve("sub"));
        for (int i = 0; i < 50; i++) {
            Files.write(dir.resolve("img" + i + ".gif"), "GIF89a".getBytes());
            Files.write(sub.resolve("deep" + i + ".png"), new byte[]{1, 2, 3});
        }
        Files.writeString(dir.resolve("notes.txt"), "text");
    }

    private List<ImageInfo> run(ScanOptions options) throws InterruptedException {
        ScanJob job = new ScanJob(List.of(dir), options);
        job.start();
        job.await();
        assertEquals(job.total(), job.processed());
        return job.drainAll();
    }

    @Test
    void recursiveScanTakesOnlyImageExtensions() throws Exception {
        prepare();
        assertEquals(100, run(new ScanOptions(true, false, 4)).size());
    }

    @Test
    void flatScanSkipsSubfolders() throws Exception {
        prepare();
        assertEquals(50, run(new ScanOptions(false, false, 2)).size());
    }

    @Test
    void allFilesModeChecksEveryFileBySignature() throws Exception {
        prepare();
        List<ImageInfo> results = run(new ScanOptions(true, true, 8));
        assertEquals(101, results.size());
        assertTrue(results.stream().anyMatch(info -> info.fileName().equals("notes.txt")));
    }
}
