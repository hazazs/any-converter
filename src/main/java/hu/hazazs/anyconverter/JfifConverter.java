package hu.hazazs.anyconverter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * JFIF is already JPEG-encoded, so changing the container filename to .jpg
 * requires a byte-for-byte copy rather than a lossy image re-encode.
 */
public final class JfifConverter {

    public static final Path DEFAULT_DIRECTORY = Path.of("D:\\downloadz");

    private JfifConverter() {
    }

    public static Summary convertAll(Path directory, BiConsumer<Integer, Integer> onProgress)
            throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IOException("Folder not found: " + directory);
        }

        List<Path> sources;
        try (Stream<Path> files = Files.list(directory)) {
            sources = files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jfif"))
                    .sorted(Comparator.comparing(file -> file.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList();
        }

        int converted = 0;
        int skipped = 0;
        int failed = 0;

        for (int i = 0; i < sources.size(); i++) {
            Path source = sources.get(i);
            String filename = source.getFileName().toString();
            Path destination = source.resolveSibling(filename.substring(0, filename.length() - 5) + ".jpg");

            try {
                if (Files.exists(destination)) {
                    skipped++; // Never overwrite an existing JPG.
                } else if (!isJpeg(source)) {
                    failed++; // A file with a .jfif extension may not contain a JPEG.
                } else {
                    copyWithoutOverwriting(source, destination);
                    converted++;
                }
            } catch (FileAlreadyExistsException ignored) {
                skipped++;
            } catch (IOException | SecurityException ignored) {
                failed++;
            }

            if (onProgress != null) {
                onProgress.accept(i + 1, sources.size());
            }
        }
        return new Summary(sources.size(), converted, skipped, failed);
    }

    private static boolean isJpeg(Path file) throws IOException {
        try (InputStream stream = Files.newInputStream(file)) {
            return stream.read() == 0xFF
                    && stream.read() == 0xD8
                    && stream.read() == 0xFF;
        }
    }

    private static void copyWithoutOverwriting(Path source, Path destination) throws IOException {
        Path temp = Files.createTempFile(source.getParent(), ".any-converter-", ".tmp");
        try {
            Files.copy(source, temp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp, destination); // No REPLACE_EXISTING: never destroy another file.
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public record Summary(int total, int converted, int skipped, int failed) {
    }
}
