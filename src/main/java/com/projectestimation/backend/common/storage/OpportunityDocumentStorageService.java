
package com.projectestimation.backend.common.storage;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

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
    private Path backupExistingFile(
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
}
