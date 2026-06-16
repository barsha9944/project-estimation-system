package com.projectestimation.backend.proposal.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.projectestimation.backend.common.exception.ProposalFailedException;
import com.projectestimation.backend.proposal.config.PandocProperties;

@Component
public class PandocDocxConverter {

    private final PandocProperties pandocProperties;
    private final HtmlToImageRenderer htmlToImageRenderer;

    public PandocDocxConverter(
            PandocProperties pandocProperties,
            HtmlToImageRenderer htmlToImageRenderer
    ) {
        this.pandocProperties = pandocProperties;
        this.htmlToImageRenderer = htmlToImageRenderer;
    }

    public ConversionResult convertMarkdownToDocx(
            String markdown,
            String baseFileName,
            String architectureHtml,
            List<String> processFlowHtmls
    ) {
        try {
//            Path tempDir = Files.createDirectories(Path.of(pandocProperties.getTempDir()));
//        	
////        	Path tempDir =
////        	        Files.createTempDirectory(
////        	                "proposal-workspace-"
////        	        );
//        	
//            String safeBaseName = sanitizeFileName(baseFileName);
//            
//            Path imagesDir =
//                    tempDir.resolve(
//                            "assets/images"
//                    );
//
//            Files.createDirectories(imagesDir);
//
//            copyProposalImages(imagesDir);
        	
        	Path workspace =
        	        Files.createDirectories(
        	                Path.of(
        	                        pandocProperties.getTempDir()
        	                )
        	        );

        	String safeBaseName =
        	        sanitizeFileName(
        	                baseFileName
        	        );

        	Path imagesDir =
        	        Files.createDirectories(
        	                workspace.resolve(
        	                        "assets/images"
        	                )
        	        );

        	copyProposalImages(imagesDir);

        	generateDynamicImages(
        	        architectureHtml,
        	        processFlowHtmls,
        	        imagesDir,
        	        baseFileName
        	);

//            Path markdownFile = tempDir.resolve(safeBaseName + ".md");
        	Path markdownFile =
        	        workspace.resolve(
        	                safeBaseName + ".md"
        	        );
//            Path docxFile = tempDir.resolve(safeBaseName + ".docx");
        	Path docxFile =
        	        workspace.resolve(
        	                safeBaseName + ".docx"
        	        );

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

    private void executePandoc(
        Path markdownFile,
        Path docxFile
	) throws IOException, InterruptedException {
	
	    ProcessBuilder processBuilder =
	            new ProcessBuilder(
	                    pandocProperties.getExecutable(),
	                    markdownFile.toString(),
	                    "-o",
	                    docxFile.toString()
	            );
	
	    processBuilder.directory(
	            markdownFile
	                    .getParent()
	                    .toFile()
	    );
	
	    processBuilder.redirectErrorStream(true);
	
	    Process process = processBuilder.start();
	
	    String processOutput =
	            new String(
	                    process.getInputStream()
	                            .readAllBytes()
	            );
	
	    boolean finished =
	            process.waitFor(
	                    60,
	                    TimeUnit.SECONDS
	            );
	
	    if (!finished) {
	
	        process.destroyForcibly();
	
	        throw new ProposalFailedException(
	                "Pandoc conversion timed out"
	        );
	    }
	
	    if (process.exitValue() != 0) {
	
	        throw new ProposalFailedException(
	                "Pandoc conversion failed: "
	                        + (
	                        processOutput.isBlank()
	                                ? "unknown error"
	                                : processOutput.trim()
	                )
	        );
	    }
	
	    if (
	            !Files.exists(docxFile)
	                    || Files.size(docxFile) == 0
	    ) {
	
	        throw new ProposalFailedException(
	                "Pandoc did not generate a valid DOCX file"
	        );
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
    
    private void copyProposalImages(
            Path targetDir
    ) throws IOException {

        String[] files = {
                "QualityAssurance.png",
                "ExecutionSchedule.png",
                "OrganisationsCapabilities.png"
        };

        for (String file : files) {

            try (
                    InputStream is =
                            getClass()
                                    .getClassLoader()
                                    .getResourceAsStream(
                                            "proposal/assets/images/" + file
                                    )
            ) {

                if (is == null) {
                    continue;
                }

                Files.copy(
                        is,
                        targetDir.resolve(file),
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        }
    }
    
    public void generateProposalImages(
            String architectureHtml,
            List<String> processFlowHtmls,
            Path proposalDir,
            String baseFileName
    ) {

        generateDynamicImages(
                architectureHtml,
                processFlowHtmls,
                proposalDir,
                baseFileName
        );
    }
    
    private void generateDynamicImages(
            String architectureHtml,
            List<String> processFlowHtmls,
            Path imagesDir,
            String baseFileName
    ) {

        try {

//        	String architectureImageName =
//        	        baseFileName + "-architecture.png";

        	String safeFileName =
        	        baseFileName.replaceAll("[^a-zA-Z0-9._-]", "_");

        	String architectureImageName =
        	        safeFileName + "-architecture.png";
        	
            htmlToImageRenderer.renderHtmlToImage(
                    architectureHtml,
                    imagesDir.resolve(
                    		architectureImageName
                    )
            );

            for (int i = 0; i < processFlowHtmls.size(); i++) {

                htmlToImageRenderer.renderHtmlToImage(
                        processFlowHtmls.get(i),
                        imagesDir.resolve(
                                safeFileName
                                        + "-process-flow-"
                                        + (i + 1)
                                        + ".png"
                        )
                );
            }

        } catch (Exception ex) {

            throw new ProposalFailedException(
                    "Failed to generate dynamic proposal images",
                    ex
            );
        }
    }

    private String sanitizeFileName(String baseFileName) {
        return baseFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public record ConversionResult(byte[] docxBytes, String generatedDocPath) {
    }
}
