package hu.hazazs.anyconverter;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WebpConverterTest {

    // Tiny valid lossless WebP fixtures: one opaque and one with transparent pixels.
    private static final String OPAQUE =
            "UklGRjoAAABXRUJQVlA4TC0AAAAvAUAAAB8gJCC6Wd5b5YZAgHCnXRUhAWGE/3t8UuY/hi0bBgrSNmDR/iL6HzoA";
    private static final String ALPHA =
            "UklGRiQAAABXRUJQVlA4TBcAAAAvAUAAEBcQ8y8gKPJ/tPkP+GAFIvofAQA=";

    @TempDir
    Path folder;

    @Test
    void convertsWebpFilesToReadableJpgAndReportsProgress() throws IOException {
        Path opaque = folder.resolve("opaque.webp");
        Path alpha = folder.resolve("alpha.WEBP");
        Files.write(opaque, Base64.getDecoder().decode(OPAQUE));
        Files.write(alpha, Base64.getDecoder().decode(ALPHA));
        Files.writeString(folder.resolve("bad.webp"), "not a WebP file");
        Files.writeString(folder.resolve("ignore.txt"), "unrelated");
        Path nested = Files.createDirectory(folder.resolve("nested"));
        Files.write(nested.resolve("nested.webp"), Base64.getDecoder().decode(OPAQUE));

        List<String> progress = new ArrayList<>();
        var result = WebpConverter.convertAll(folder,
                (done, total) -> progress.add(done + "/" + total));

        assertEquals(3, result.total());
        assertEquals(2, result.converted());
        assertEquals(0, result.skipped());
        assertEquals(1, result.failed());
        assertTrue(Files.exists(opaque), "Source files are preserved");
        assertTrue(Files.exists(alpha));
        assertFalse(Files.exists(folder.resolve("bad.jpg")));
        assertFalse(Files.exists(nested.resolve("nested.jpg")));
        assertEquals(List.of("1/3", "2/3", "3/3"), progress);

        BufferedImage first = ImageIO.read(folder.resolve("opaque.jpg").toFile());
        BufferedImage second = ImageIO.read(folder.resolve("alpha.jpg").toFile());
        assertNotNull(first);
        assertNotNull(second);
        assertEquals(2, first.getWidth());
        assertEquals(2, second.getHeight());

        // The top-right pixel of the transparent fixture must become white.
        int white = second.getRGB(1, 0) & 0xFFFFFF;
        for (int shift : new int[]{16, 8, 0}) {
            assertTrue(((white >>> shift) & 0xFF) > 220,
                    "Transparent background must be white, not black");
        }
    }

    @Test
    void skipsExistingJpgWithoutOverwriting() throws IOException {
        Files.write(folder.resolve("picture.webp"), Base64.getDecoder().decode(OPAQUE));
        Path target = folder.resolve("picture.jpg");
        Files.writeString(target, "existing image");
        var result = WebpConverter.convertAll(folder, null);

        assertEquals(1, result.total());
        assertEquals(0, result.converted());
        assertEquals(1, result.skipped());
        assertEquals(0, result.failed());
        assertEquals("existing image", Files.readString(target));
    }

    @Test
    void handlesEmptyOrMissingDirectory() throws IOException {
        assertEquals(0, WebpConverter.convertAll(folder, null).total());
        assertThrows(IOException.class,
                () -> WebpConverter.convertAll(folder.resolve("missing"), null));
    }
}
