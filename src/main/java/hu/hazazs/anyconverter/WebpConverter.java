package hu.hazazs.anyconverter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

/**
 * Re-encodes static WebP images as JPEG. JPEG has no transparency, so transparent
 * pixels are composited over white. Existing .jpg files are never overwritten.
 */
public final class WebpConverter {

    private WebpConverter() {
    }

    public static JfifConverter.Summary convertAll(
            Path directory, BiConsumer<Integer, Integer> onProgress) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IOException("Folder not found: " + directory);
        }

        List<Path> sources;
        try (Stream<Path> files = Files.list(directory)) {
            sources = files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString()
                            .toLowerCase(Locale.ROOT).endsWith(".webp"))
                    .sorted(Comparator.comparing(file -> file.getFileName().toString()
                            .toLowerCase(Locale.ROOT)))
                    .toList();
        }

        int converted = 0;
        int skipped = 0;
        int failed = 0;
        for (int i = 0; i < sources.size(); i++) {
            Path source = sources.get(i);
            String name = source.getFileName().toString();
            Path target = source.resolveSibling(name.substring(0, name.length() - 5) + ".jpg");
            try {
                if (Files.exists(target)) {
                    skipped++;
                } else {
                    convertOne(source, target);
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
        return new JfifConverter.Summary(sources.size(), converted, skipped, failed);
    }

    private static void convertOne(Path source, Path destination) throws IOException {
        BufferedImage decoded = ImageIO.read(source.toFile());
        if (decoded == null) {
            throw new IOException("Invalid or unsupported WebP file: " + source.getFileName());
        }

        // Remove alpha before JPEG encoding; Graphics2D blends transparent areas
        // with a white background instead of turning them black.
        BufferedImage rgb = new BufferedImage(
                decoded.getWidth(), decoded.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            graphics.drawImage(decoded, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        Path temp = Files.createTempFile(source.getParent(), ".any-converter-", ".tmp");
        try {
            if (!ImageIO.write(rgb, "jpg", temp.toFile())) {
                throw new IOException("JPEG encoder not found");
            }
            Files.move(temp, destination); // No REPLACE_EXISTING.
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /**
     * Headless smoke check used by CI to verify that the shaded JAR includes
     * the WebP ImageIO service registration.
     */
    public static void main(String[] args) {
        if (!ImageIO.getImageReadersByFormatName("webp").hasNext()) {
            throw new IllegalStateException("WebP image reader missing from runnable JAR");
        }
        System.out.println("WebP image reader available");
    }
}
