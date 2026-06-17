package com.example.Cricbook.service;

import com.example.Cricbook.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final long MAX_BYTES = 5L * 1024L * 1024L;
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png", "image/webp");

    public String saveFile(MultipartFile file, Long turfId) throws IOException {
        validateImage(file);

        Path uploadRoot = Paths.get("uploads").toAbsolutePath().normalize();
        Path directory = uploadRoot.resolve("turf").resolve(String.valueOf(turfId)).normalize();
        if (!directory.startsWith(uploadRoot.resolve("turf").normalize())) {
            throw ApiException.badRequest("Invalid upload target.");
        }
        Files.createDirectories(directory);

        String fileName = UUID.randomUUID() + extensionFor(file);
        Path path = directory.resolve(fileName).normalize();
        if (!path.startsWith(directory)) {
            throw ApiException.badRequest("Invalid upload filename.");
        }
        Files.write(path, file.getBytes());

        String relative = uploadRoot.relativize(path).toString().replace("\\", "/");
        return "/uploads/" + relative;
    }

    private void validateImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Please choose an image to upload.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw ApiException.badRequest("Image must be 5 MB or smaller.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw ApiException.badRequest("Only JPG, PNG, and WEBP images are supported.");
        }
        if (!matchesDeclaredImageType(file.getBytes(), contentType.toLowerCase(Locale.ROOT))) {
            throw ApiException.badRequest("Uploaded file does not look like a valid image.");
        }
    }

    private String extensionFor(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null && original.lastIndexOf('.') >= 0) {
            String ext = original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.]", "");
            if (List.of(".jpg", ".jpeg", ".png", ".webp").contains(ext)) {
                return ext;
            }
        }
        return switch (file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    private boolean matchesDeclaredImageType(byte[] bytes, String contentType) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        if ("image/jpeg".equals(contentType)) {
            return bytes.length >= 3
                    && (bytes[0] & 0xFF) == 0xFF
                    && (bytes[1] & 0xFF) == 0xD8
                    && (bytes[2] & 0xFF) == 0xFF;
        }
        if ("image/png".equals(contentType)) {
            byte[] signature = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            if (bytes.length < signature.length) {
                return false;
            }
            for (int i = 0; i < signature.length; i++) {
                if (bytes[i] != signature[i]) {
                    return false;
                }
            }
            return true;
        }
        if ("image/webp".equals(contentType)) {
            return bytes.length >= 12
                    && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                    && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
        }
        return false;
    }
}
