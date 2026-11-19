
package com.projectestimation.backend.common.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;

@Service
public class OpportunityDocumentStorageService {

    private final OpportunityRepository opportunityRepository;
    private final Path storageRoot;

    private static final DateTimeFormatter BACKUP_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    public OpportunityDocumentStorageService(
            OpportunityRepository opportunityRepository,
            @Value("${proposal.storage.path}") String storagePath) {

        this.opportunityRepository = opportunityRepository;
        this.storageRoot = Path.of(storagePath)
                .toAbsolutePath()
                .normalize();
    }

    /**
     * Returns the folder belonging to an opportunity.
     */
    public Path getOpportunityFolder(Long opportunityId) throws IOException {

        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Opportunity not found: " + opportunityId));

        String safeOpportunityName = opportunity.getOpportunityName()
                .trim()
                .replaceAll("[\\\\/:*?\"<>|]", "_");

        Path opportunityFolder = storageRoot.resolve(safeOpportunityName)
                .normalize()
                .toAbsolutePath();

        if (!opportunityFolder.startsWith(storageRoot)) {
            throw new IOException("Invalid opportunity folder path");
        }

        Files.createDirectories(opportunityFolder);

        return opportunityFolder;
    }

    /**
     * Returns a document category folder for an opportunity.
     * Example: getDocumentFolder(id, "Quality/RTM")
     */
    public Path getDocumentFolder(
            Long opportunityId,
            String relativeFolder) throws IOException {

        if (relativeFolder == null || relativeFolder.isBlank()) {
            throw new IllegalArgumentException(
                    "Document folder is required");
        }

        Path opportunityFolder = getOpportunityFolder(opportunityId);

        Path documentFolder = opportunityFolder.resolve(relativeFolder)
                .normalize()
                .toAbsolutePath();

        if (!documentFolder.startsWith(opportunityFolder)) {
            throw new IOException("Invalid document folder path");
        }

        Files.createDirectories(documentFolder);

        return documentFolder;
    }

    /**
     * Saves a generated document.
     *
     * If a file with the same name already exists:
     * 1. Move the existing file into the opportunity's Backup folder.
     * 2. Save the newly generated file in its original folder.
     */
    public Path saveFile(
            Long opportunityId,
            String relativeFolder,
            String fileName,
            byte[] fileBytes) throws IOException {

        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }

        if (fileBytes == null || fileBytes.length == 0) {
            throw new IOException("Generated document is empty");
        }

        String safeFileName = Path.of(fileName)
                .getFileName()
                .toString();

        Path documentFolder = getDocumentFolder(
                opportunityId,
                relativeFolder);

        Path destination = documentFolder.resolve(safeFileName)
                .normalize()
                .toAbsolutePath();

        if (!destination.startsWith(documentFolder)) {
            throw new IOException("Invalid document file path");
        }

        // Write the new content to a temporary file first.
        Path temporaryFile = Files.createTempFile(
                documentFolder,
                "generated-",
                ".tmp");

        Path backupFile = null;

        try {
            Files.write(temporaryFile, fileBytes);

            // Back up the previous version before replacing it.
            if (Files.exists(destination)) {
                backupFile = backupExistingFile(
                        opportunityId,
                        destination);
            }

            // Move the newly generated file into its final location.
            moveFile(temporaryFile, destination);

            return destination;

        } catch (IOException exception) {

            // If saving the new file failed after the old file was
            // backed up, attempt to restore the old file.
            if (backupFile != null
                    && Files.exists(backupFile)
                    && !Files.exists(destination)) {

                try {
                    moveFile(backupFile, destination);
                } catch (IOException restoreException) {
                    exception.addSuppressed(restoreException);
                }
            }

            throw exception;

        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    /**
     * Moves an existing document into the opportunity's root Backup folder.
     */
    public Path backupExistingFile(
            Long opportunityId,
            Path existingFile) throws IOException {

        Path opportunityFolder = getOpportunityFolder(opportunityId);

        Path backupFolder = getDocumentFolder(
                opportunityId,
                "Backup");

        if (!backupFolder.startsWith(opportunityFolder)) {
            throw new IOException("Invalid backup folder path");
        }

        String originalFileName = existingFile.getFileName().toString();

        int extensionIndex = originalFileName.lastIndexOf('.');

        String baseName;
        String extension;

        if (extensionIndex > 0) {
            baseName = originalFileName.substring(0, extensionIndex);
            extension = originalFileName.substring(extensionIndex);
        } else {
            baseName = originalFileName;
            extension = "";
        }

        String backupFileName =
                baseName
                + "_backup_"
                + LocalDateTime.now().format(BACKUP_TIMESTAMP)
                + "_"
                + UUID.randomUUID().toString().substring(0, 8)
                + extension;

        Path backupDestination = backupFolder.resolve(backupFileName)
                .normalize()
                .toAbsolutePath();

        if (!backupDestination.startsWith(backupFolder)) {
            throw new IOException("Invalid backup file path");
        }

        moveFile(existingFile, backupDestination);

        return backupDestination;
    }

    /**
     * Uses an atomic move where supported, with a normal move as fallback.
     */
    private void moveFile(
            Path source,
            Path destination) throws IOException {

        try {
            Files.move(
                    source,
                    destination,
                    StandardCopyOption.ATOMIC_MOVE);

        } catch (AtomicMoveNotSupportedException exception) {

            Files.move(
                    source,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Converts an absolute path into an opportunity-relative path.
     * E.g. "D:/proposal-storage/ProjectA/Baseline/SRS/Doc.docx" -> "Baseline/SRS/Doc.docx"
     */
    public String toOpportunityRelativePath(Long opportunityId, Path absolutePath) throws IOException {
        Path opportunityFolder = getOpportunityFolder(opportunityId);
        Path normalizedAbsolute = absolutePath.toAbsolutePath().normalize();
        if (!normalizedAbsolute.startsWith(opportunityFolder)) {
            throw new IllegalArgumentException("Path does not belong to opportunity folder: " + absolutePath);
        }
        return opportunityFolder.relativize(normalizedAbsolute).toString().replace('\\', '/');
    }

    /**
     * Safely resolves an opportunity-relative path to its absolute Path on disk,
     * guaranteeing no directory traversal outside the opportunity root.
     */
    public Path resolveStoredFilePath(Long opportunityId, String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Relative path is required");
        }
        Path opportunityFolder = getOpportunityFolder(opportunityId);
        Path targetPath = opportunityFolder.resolve(relativePath).normalize().toAbsolutePath();
        if (!targetPath.startsWith(opportunityFolder)) {
            throw new IOException("Invalid path sequence detected: " + relativePath);
        }
        return targetPath;
    }

    /**
     * Reads all bytes from an existing physical file specified by opportunity-relative path.
     */
    public byte[] readFile(Long opportunityId, String relativePath) throws IOException {
        Path targetPath = resolveStoredFilePath(opportunityId, relativePath);
        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            throw new ResourceNotFoundException("Document file not found: " + relativePath);
        }
        return Files.readAllBytes(targetPath);
    }

    /**
     * Opens an InputStream for an existing physical file specified by opportunity-relative path.
     */
    public InputStream openInputStream(Long opportunityId, String relativePath) throws IOException {
        Path targetPath = resolveStoredFilePath(opportunityId, relativePath);
        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            throw new ResourceNotFoundException("Document file not found: " + relativePath);
        }
        return Files.newInputStream(targetPath);
    }

    /**
     * Replaces an existing physical file, moving the previous file into the Backup folder,
     * and writes the new content. Returns the opportunity-relative path of the new file.
     */
    public String replaceFile(Long opportunityId, String relativeFolder, String fileName, byte[] newContent) throws IOException {
        Path savedPath = saveFile(opportunityId, relativeFolder, fileName, newContent);
        return toOpportunityRelativePath(opportunityId, savedPath);
    }

    /**
     * Returns whether a physical file exists at the opportunity-relative path.
     */
    public boolean exists(Long opportunityId, String relativePath) {
        try {
            Path targetPath = resolveStoredFilePath(opportunityId, relativePath);
            return Files.exists(targetPath) && Files.isRegularFile(targetPath);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the last modified timestamp of the physical file in LocalDateTime.
     */
    public LocalDateTime getLastModified(Long opportunityId, String relativePath) throws IOException {
        Path targetPath = resolveStoredFilePath(opportunityId, relativePath);
        if (!Files.exists(targetPath)) {
            throw new ResourceNotFoundException("Document file not found: " + relativePath);
        }
        Instant instant = Files.getLastModifiedTime(targetPath).toInstant();
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    /**
     * Computes the SHA-256 checksum of the physical file for change detection.
     */
    public String getFileChecksum(Long opportunityId, String relativePath) throws IOException {
        Path targetPath = resolveStoredFilePath(opportunityId, relativePath);
        if (!Files.exists(targetPath)) {
            throw new ResourceNotFoundException("Document file not found: " + relativePath);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(targetPath);
            byte[] hashBytes = digest.digest(fileBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Finds the latest file in a relative folder matching an extension (e.g. ".docx").
     */
    public Optional<Path> findLatestFile(Long opportunityId, String relativeFolder, String extension) throws IOException {
        Path folder = getDocumentFolder(opportunityId, relativeFolder);
        if (!Files.exists(folder)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(p -> extension == null || p.getFileName().toString().toLowerCase().endsWith(extension.toLowerCase()))
                    .max(Comparator.comparingLong(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toMillis();
                        } catch (IOException e) {
                            return 0L;
                        }
                    }));
        }
    }
}
