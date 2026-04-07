package com.streetmonopoly.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private static final int MAX_WIDTH = 800;
    private static final float JPEG_QUALITY = 0.80f;

    private final Path uploadDir;

    public ImageController(@Value("${app.upload-dir:./uploads}") String uploadPath) throws IOException {
        this.uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    @PostMapping("/upload")
    public Map<String, String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed");
        }

        // Read the uploaded image
        BufferedImage original = ImageIO.read(file.getInputStream());
        if (original == null) {
            throw new RuntimeException("Could not read image file");
        }

        // Resize if wider than MAX_WIDTH, maintaining aspect ratio
        BufferedImage resized = resizeImage(original, MAX_WIDTH);

        // Always save as JPEG for consistent compression
        String filename = UUID.randomUUID() + ".jpg";
        Path target = uploadDir.resolve(filename);

        writeJpeg(resized, target, JPEG_QUALITY);

        String imageUrl = "/api/images/" + filename;
        return Map.of("url", imageUrl, "filename", filename);
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) throws MalformedURLException {
        // Sanitise filename to prevent directory traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        Path filePath = uploadDir.resolve(filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String mediaType = "image/jpeg";
        if (filename.endsWith(".png")) mediaType = "image/png";
        else if (filename.endsWith(".webp")) mediaType = "image/webp";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(resource);
    }

    /**
     * Resize an image to fit within maxWidth, preserving aspect ratio.
     * If the image is already smaller, it is returned unchanged.
     * Handles transparency by drawing onto a white background (for JPEG output).
     */
    private BufferedImage resizeImage(BufferedImage original, int maxWidth) {
        int origWidth = original.getWidth();
        int origHeight = original.getHeight();

        if (origWidth <= maxWidth) {
            // Still need to flatten alpha for JPEG
            return flattenAlpha(original);
        }

        double scale = (double) maxWidth / origWidth;
        int newWidth = maxWidth;
        int newHeight = (int) Math.round(origHeight * scale);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, newWidth, newHeight);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(original, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return resized;
    }

    /**
     * Flatten any alpha channel onto a white background for JPEG output.
     */
    private BufferedImage flattenAlpha(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_RGB) {
            return image;
        }
        BufferedImage flat = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = flat.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return flat;
    }

    /**
     * Write a BufferedImage as JPEG with the specified quality (0.0 - 1.0).
     */
    private void writeJpeg(BufferedImage image, Path target, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new RuntimeException("No JPEG writer available");
        }

        ImageWriter writer = writers.next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(quality);

        try (OutputStream os = Files.newOutputStream(target);
             ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
    }
}
