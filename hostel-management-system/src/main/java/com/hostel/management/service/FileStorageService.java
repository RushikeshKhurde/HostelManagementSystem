package com.hostel.management.service;

import com.hostel.management.exception.ApiException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.dir:uploads/profile-photos}")
    private String uploadDir;

    @Value("${app.complaint.upload.dir:uploads/complaint-images}")
    private String complaintUploadDir;

    @Value("${app.feedback.upload.dir:uploads/feedback-images}")
    private String feedbackUploadDir;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png", "image/jpg");

    private static final long MAX_COMPLAINT_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_COMPLAINT_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_COMPLAINT_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/jpg", "image/webp");

    private static final long MAX_FEEDBACK_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_FEEDBACK_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_FEEDBACK_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/jpg", "image/webp");

    // Dangerous extensions to explicitly reject
    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(
            "exe", "bat", "sh", "cmd", "jsp", "jspx", "php", "py", "pl", "cgi", "html", "htm", "js", "jar", "war"
    );

    @Getter
    public static class StoredFile {
        private final String filename;
        private final String originalFilename;
        private final String contentType;
        private final long size;

        public StoredFile(String filename, String originalFilename, String contentType, long size) {
            this.filename = filename;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.size = size;
        }
    }

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

    public StoredFile storeComplaintImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        if (file.getSize() > MAX_COMPLAINT_FILE_SIZE) {
            throw new ApiException("Image file size exceeds the 5MB limit. Please choose a smaller photo.", HttpStatus.BAD_REQUEST);
        }

        String rawOriginalFilename = file.getOriginalFilename();
        if (rawOriginalFilename == null || !rawOriginalFilename.contains(".")) {
            throw new ApiException("Invalid file name. Please upload a JPG, JPEG, PNG, or WebP image.", HttpStatus.BAD_REQUEST);
        }

        // Sanitize original filename to strip directory traversal / paths
        String originalFilename = Paths.get(rawOriginalFilename).getFileName().toString();
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();

        if (FORBIDDEN_EXTENSIONS.contains(extension)) {
            throw new ApiException("Executable and script files are strictly prohibited.", HttpStatus.BAD_REQUEST);
        }

        if (!ALLOWED_COMPLAINT_EXTENSIONS.contains(extension)) {
            throw new ApiException("Unsupported file format. Please upload JPG, JPEG, PNG, or WebP.", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_COMPLAINT_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ApiException("Invalid file type. File must be a valid JPG, PNG, or WebP image.", HttpStatus.BAD_REQUEST);
        }

        // Validate magic bytes (JPEG, PNG, or WebP header)
        validateMagicBytesWithWebp(file, extension);

        try {
            Path targetDir = Paths.get(complaintUploadDir).toAbsolutePath().normalize();
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

            log.info("Saved complaint image successfully: {}", destination);
            return new StoredFile(newFilename, originalFilename, contentType, file.getSize());
        } catch (IOException e) {
            log.error("Failed to store complaint image", e);
            throw new ApiException("Failed to save complaint image: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Resource loadComplaintImageAsResource(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new ApiException("Image filename cannot be empty.", HttpStatus.BAD_REQUEST);
        }

        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new ApiException("Invalid image filename.", HttpStatus.BAD_REQUEST);
        }

        try {
            Path targetDir = Paths.get(complaintUploadDir).toAbsolutePath().normalize();
            Path filePath = targetDir.resolve(filename).normalize();

            if (!filePath.startsWith(targetDir)) {
                throw new ApiException("Invalid file path.", HttpStatus.BAD_REQUEST);
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ApiException("Image not found or is unreadable.", HttpStatus.NOT_FOUND);
            }

            return resource;
        } catch (MalformedURLException e) {
            throw new ApiException("Image not found.", HttpStatus.NOT_FOUND);
        }
    }

    public StoredFile storeFeedbackImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        if (file.getSize() > MAX_FEEDBACK_FILE_SIZE) {
            throw new ApiException("Image file size exceeds the 5MB limit. Please choose a smaller photo.", HttpStatus.BAD_REQUEST);
        }

        String rawOriginalFilename = file.getOriginalFilename();
        if (rawOriginalFilename == null || !rawOriginalFilename.contains(".")) {
            throw new ApiException("Invalid file name. Please upload a JPG, JPEG, PNG, or WebP image.", HttpStatus.BAD_REQUEST);
        }

        // Sanitize original filename to strip directory traversal / paths
        String originalFilename = Paths.get(rawOriginalFilename).getFileName().toString();
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();

        if (FORBIDDEN_EXTENSIONS.contains(extension)) {
            throw new ApiException("Executable and script files are strictly prohibited.", HttpStatus.BAD_REQUEST);
        }

        if (!ALLOWED_FEEDBACK_EXTENSIONS.contains(extension)) {
            throw new ApiException("Unsupported file format. Please upload JPG, JPEG, PNG, or WebP.", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_FEEDBACK_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ApiException("Invalid file type. File must be a valid JPG, PNG, or WebP image.", HttpStatus.BAD_REQUEST);
        }

        // Validate magic bytes (JPEG, PNG, or WebP header)
        validateMagicBytesWithWebp(file, extension);

        try {
            Path targetDir = Paths.get(feedbackUploadDir).toAbsolutePath().normalize();
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

            log.info("Saved feedback image successfully: {}", destination);
            return new StoredFile(newFilename, originalFilename, contentType, file.getSize());
        } catch (IOException e) {
            log.error("Failed to store feedback image", e);
            throw new ApiException("Failed to save feedback image: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Resource loadFeedbackImageAsResource(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new ApiException("Image filename cannot be empty.", HttpStatus.BAD_REQUEST);
        }

        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new ApiException("Invalid image filename.", HttpStatus.BAD_REQUEST);
        }

        try {
            Path targetDir = Paths.get(feedbackUploadDir).toAbsolutePath().normalize();
            Path filePath = targetDir.resolve(filename).normalize();

            if (!filePath.startsWith(targetDir)) {
                throw new ApiException("Invalid file path.", HttpStatus.BAD_REQUEST);
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ApiException("Image not found or is unreadable.", HttpStatus.NOT_FOUND);
            }

            return resource;
        } catch (MalformedURLException e) {
            throw new ApiException("Image not found.", HttpStatus.NOT_FOUND);
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

    private void validateMagicBytesWithWebp(MultipartFile file, String extension) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header);
            if (read < 4) {
                throw new ApiException("Uploaded file appears to be corrupted.", HttpStatus.BAD_REQUEST);
            }

            boolean isPng = read >= 4 && (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47);
            boolean isJpg = read >= 3 && (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF);
            boolean isWebp = read >= 12 &&
                    (header[0] == (byte) 'R' && header[1] == (byte) 'I' && header[2] == (byte) 'F' && header[3] == (byte) 'F') &&
                    (header[8] == (byte) 'W' && header[9] == (byte) 'E' && header[10] == (byte) 'B' && header[11] == (byte) 'P');

            if (!isPng && !isJpg && !isWebp) {
                throw new ApiException("File content does not match a valid JPG, PNG, or WebP image.", HttpStatus.BAD_REQUEST);
            }
        } catch (IOException e) {
            throw new ApiException("Unable to verify image format.", HttpStatus.BAD_REQUEST);
        }
    }
}
