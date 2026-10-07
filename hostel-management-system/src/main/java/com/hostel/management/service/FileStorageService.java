package com.hostel.management.service;

import com.hostel.management.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.dir:uploads/profile-photos}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png", "image/jpg");

    // Dangerous extensions to explicitly reject
    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(
            "exe", "bat", "sh", "cmd", "jsp", "jspx", "php", "py", "pl", "cgi", "html", "htm", "js", "jar", "war"
    );

    public String storeProfilePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("Please select an image file to upload", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApiException("Image file size exceeds the 5MB limit. Please choose a smaller photo.", HttpStatus.BAD_REQUEST);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new ApiException("Invalid file name. Please upload a JPG, JPEG, or PNG image.", HttpStatus.BAD_REQUEST);
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();

        if (FORBIDDEN_EXTENSIONS.contains(extension)) {
            throw new ApiException("Executable and script files are strictly prohibited.", HttpStatus.BAD_REQUEST);
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ApiException("Unsupported file format. Please upload JPG, JPEG, or PNG.", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ApiException("Invalid file type. File must be a valid JPG or PNG image.", HttpStatus.BAD_REQUEST);
        }

        // Validate magic bytes (JPEG or PNG header)
        validateImageMagicBytes(file, extension);

        try {
            Path targetDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            String newFilename = UUID.randomUUID().toString() + "." + extension;
            Path destination = targetDir.resolve(newFilename).normalize();

            // Prevent path traversal
            if (!destination.startsWith(targetDir)) {
                throw new ApiException("Invalid file path.", HttpStatus.BAD_REQUEST);
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Saved profile photo successfully: {}", destination);
            return "/uploads/profile-photos/" + newFilename;
        } catch (IOException e) {
            log.error("Failed to store file", e);
            throw new ApiException("Failed to save profile photo: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateImageMagicBytes(MultipartFile file, String extension) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read < 4) {
                throw new ApiException("Uploaded file appears to be corrupted.", HttpStatus.BAD_REQUEST);
            }

            boolean isPng = (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47);
            boolean isJpg = (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF);

            if (!isPng && !isJpg) {
                throw new ApiException("File content does not match a valid JPG or PNG image.", HttpStatus.BAD_REQUEST);
            }
        } catch (IOException e) {
            throw new ApiException("Unable to verify image format.", HttpStatus.BAD_REQUEST);
        }
    }
}
