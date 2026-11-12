
package com.projectestimation.backend.pmp.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PmpDocumentStorageService {

    @Value("${pmp.storage-dir}")
    private String storageDirectory;

    /**
     * Creates and returns the directory for a particular opportunity.
     */
    private Path getOpportunityDirectory(Long opportunityId)
            throws IOException {

        Path directory = Paths.get(storageDirectory)
                .resolve("opportunity-" + opportunityId);

        Files.createDirectories(directory);

        return directory;
    }

    /**
     * Saves the initial PMP document.
     * Does not overwrite an existing PMP.docx file.
     */
    public void saveLatestPmp(
            Long opportunityId,
            byte[] documentBytes) throws IOException {

        if (documentBytes == null || documentBytes.length == 0) {
            throw new IOException("PMP document is empty.");
        }

        Path directory = getOpportunityDirectory(opportunityId);
        Path latestFile = directory.resolve("PMP.docx");

        Files.write(
                latestFile,
                documentBytes,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                java.nio.file.StandardOpenOption.WRITE
        );
    

        if (documentBytes == null || documentBytes.length == 0) {
            throw new IOException("PMP document is empty.");
        }

//        Path directory = getOpportunityDirectory(opportunityId);

        Path initialFile = directory.resolve("PMP.docx");

        if (!Files.exists(initialFile)) {
            Files.write(initialFile, documentBytes);
        }
    }

    /**
     * Finds the latest DOCX file for an opportunity.
     */
    public Path getLatestPmpFile(Long opportunityId)
            throws IOException {

        Path directory = getOpportunityDirectory(opportunityId);

        try (Stream<Path> files = Files.list(directory)) {

            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName()
                            .toString()
                            .toLowerCase()
                            .endsWith(".docx"))
                    .max(Comparator.comparingLong(path -> {
                        try {
                            return Files.getLastModifiedTime(path)
                                    .toMillis();
                        } catch (IOException ex) {
                            return 0L;
                        }
                    }))
                    .orElseThrow(() -> new IOException(
                            "No PMP DOCX file found for opportunity "
                                    + opportunityId));
        }
    }

    /**
     * Reads the latest PMP DOCX and extracts its paragraphs and tables.
     */
    public String readLatestPmpText(Long opportunityId)
            throws IOException {

        Path latestFile = getLatestPmpFile(opportunityId);

        StringBuilder content = new StringBuilder();

        try (InputStream inputStream =
                     Files.newInputStream(latestFile);
             XWPFDocument document =
                     new XWPFDocument(inputStream)) {

            // Extract paragraphs
            for (XWPFParagraph paragraph :
                    document.getParagraphs()) {

                String text = paragraph.getText();

                if (text != null && !text.isBlank()) {
                    content.append(text).append("\n");
                }
            }

            // Extract tables
            for (XWPFTable table : document.getTables()) {

                for (var row : table.getRows()) {

                    for (var cell : row.getTableCells()) {

                        String cellText = cell.getText();

                        if (cellText != null && !cellText.isBlank()) {
                            content.append(cellText).append(" | ");
                        }
                    }

                    content.append("\n");
                }
            }
        }

        return content.toString();
    }
}
