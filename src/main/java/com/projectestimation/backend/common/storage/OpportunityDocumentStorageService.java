
package com.projectestimation.backend.common.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;

@Service
public class OpportunityDocumentStorageService {

    private final OpportunityRepository opportunityRepository;
    private final Path storageRoot;

    public OpportunityDocumentStorageService(
            OpportunityRepository opportunityRepository,
            @Value("${proposal.storage.path}") String storagePath) {

        this.opportunityRepository = opportunityRepository;
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
    }

    /**
     * Returns the folder belonging to an opportunity.
     * The folder name is based on the opportunity name, matching
     * the folder structure created by OpportunityService.
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
     * Example: getDocumentFolder(id, "Quality/PSR")
     */
    public Path getDocumentFolder(
            Long opportunityId,
            String relativeFolder) throws IOException {

        if (relativeFolder == null || relativeFolder.isBlank()) {
            throw new IllegalArgumentException("Document folder is required");
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
     * Saves newly generated document bytes in the requested folder.
     * Existing files with the same name are replaced.
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

        String safeFileName = Path.of(fileName).getFileName().toString();

        Path documentFolder = getDocumentFolder(
                opportunityId,
                relativeFolder);

        Path destination = documentFolder.resolve(safeFileName)
                .normalize()
                .toAbsolutePath();

        if (!destination.startsWith(documentFolder)) {
            throw new IOException("Invalid document file path");
        }

        return Files.write(destination, fileBytes);
    }
}
