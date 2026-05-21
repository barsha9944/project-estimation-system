package com.projectestimation.backend.proposal.service;

import com.projectestimation.backend.common.exception.ProposalFailedException;
import com.projectestimation.backend.proposal.config.PandocProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Component
public class PandocDocxConverter {

    private final PandocProperties pandocProperties;

    public PandocDocxConverter(PandocProperties pandocProperties) {
        this.pandocProperties = pandocProperties;
    }

    public ConversionResult convertMarkdownToDocx(String markdown, String baseFileName) {
        try {
            Path tempDir = Files.createDirectories(Path.of(pandocProperties.getTempDir()));
            String safeBaseName = sanitizeFileName(baseFileName);

            Path markdownFile = tempDir.resolve(safeBaseName + ".md");
            Path docxFile = tempDir.resolve(safeBaseName + ".docx");

            Files.writeString(markdownFile, markdown);
            executePandoc(markdownFile, docxFile);

            byte[] docxBytes = Files.readAllBytes(docxFile);
            return new ConversionResult(docxBytes, docxFile.toString());
        } catch (ProposalFailedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProposalFailedException("Failed to convert Markdown to DOCX using Pandoc", ex);
        } finally {
//            cleanupTempFiles(baseFileName);
        }
    }

    private void executePandoc(Path markdownFile, Path docxFile) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                pandocProperties.getExecutable(),
                markdownFile.toString(),
                "-o",
                docxFile.toString()
        );
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        String processOutput = new String(process.getInputStream().readAllBytes());

        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new ProposalFailedException("Pandoc conversion timed out");
        }

        if (process.exitValue() != 0) {
            throw new ProposalFailedException(
                    "Pandoc conversion failed: " + (processOutput.isBlank() ? "unknown error" : processOutput.trim())
            );
        }

        if (!Files.exists(docxFile) || Files.size(docxFile) == 0) {
            throw new ProposalFailedException("Pandoc did not generate a valid DOCX file");
        }
    }

    private void cleanupTempFiles(String baseFileName) {
        try {
            Path tempDir = Path.of(pandocProperties.getTempDir());
            String safeBaseName = sanitizeFileName(baseFileName);
            Files.deleteIfExists(tempDir.resolve(safeBaseName + ".md"));
            Files.deleteIfExists(tempDir.resolve(safeBaseName + ".docx"));
        } catch (IOException ignored) {
            // Best-effort cleanup of temporary conversion files.
        }
    }

    private String sanitizeFileName(String baseFileName) {
        return baseFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public record ConversionResult(byte[] docxBytes, String generatedDocPath) {
    }
}
