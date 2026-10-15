package hu.hazazs.anyconverter;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JfifConverterTest {

    @TempDir
    Path folder;

    @Test
    void convertsAllJfifLosslesslyAndReportsProgress() throws IOException {
        Path first = folder.resolve("photo.jfif");
        Path second = folder.resolve("other.JFIF");
        writeJpeg(first);
        writeJpeg(second);
        Files.writeString(folder.resolve("invalid.jfif"), "not a JPEG");
        Files.writeString(folder.resolve("ignore.png"), "not a JFIF");
        Path subdirectory = Files.createDirectory(folder.resolve("subfolder"));
        writeJpeg(subdirectory.resolve("nested.jfif"));

        List<String> progress = new ArrayList<>();
        var result = JfifConverter.convertAll(folder,
                (done, total) -> progress.add(done + "/" + total));

        assertEquals(3, result.total());
        assertEquals(2, result.converted());
        assertEquals(0, result.skipped());
        assertEquals(1, result.failed());
        assertArrayEquals(Files.readAllBytes(first), Files.readAllBytes(folder.resolve("photo.jpg")));
        assertArrayEquals(Files.readAllBytes(second), Files.readAllBytes(folder.resolve("other.jpg")));
        assertTrue(Files.exists(first), "Originals must not be deleted");
        assertFalse(Files.exists(folder.resolve("invalid.jpg")));
        assertFalse(Files.exists(subdirectory.resolve("nested.jpg")));
        assertEquals(List.of("1/3", "2/3", "3/3"), progress);
    }

    @Test
    void neverOverwritesAnExistingJpg() throws IOException {
        writeJpeg(folder.resolve("photo.jfif"));
        Path existing = folder.resolve("photo.jpg");
        Files.writeString(existing, "keep this original jpg");
        var result = JfifConverter.convertAll(folder, null);

        assertEquals(1, result.total());
        assertEquals(0, result.converted());
        assertEquals(1, result.skipped());
        assertEquals(0, result.failed());
        assertEquals("keep this original jpg", Files.readString(existing));
    }

    @Test
    void emptyFolderAndMissingFolderAreHandled() throws IOException {
        var result = JfifConverter.convertAll(folder, null);
        assertEquals(0, result.total());
        assertThrows(IOException.class,
                () -> JfifConverter.convertAll(folder.resolve("missing"), null));
    }

    private static void writeJpeg(Path path) throws IOException {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        assertTrue(ImageIO.write(image, "jpg", path.toFile()));
    }
}
