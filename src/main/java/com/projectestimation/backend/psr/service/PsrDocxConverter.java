package com.projectestimation.backend.psr.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.projectestimation.backend.common.exception.ProposalFailedException;
import com.projectestimation.backend.config.PsrProperties;

@Component
public class PsrDocxConverter {

    private final PsrProperties psrProperties;

    public PsrDocxConverter(
            PsrProperties psrProperties
    ) {
        this.psrProperties = psrProperties;
    }

    public ConversionResult convertMarkdownToDocx(
            String markdown,
            String baseFileName
    ) {

        try {

            // ============================================
            // PSR STORAGE DIRECTORY
            // ============================================

            Path storageDir =
                    Files.createDirectories(
                            Path.of(
                                    psrProperties.getStorageDir()
                            )
                    );

            // ============================================
            // SAFE FILE NAME
            // ============================================

            String safeBaseName =
                    sanitizeFileName(baseFileName);

            Path markdownFile =
                    storageDir.resolve(
                            safeBaseName + ".md"
                    );

            Path docxFile =
                    storageDir.resolve(
                            safeBaseName + ".docx"
                    );

         // ============================================
         // COPY BEAS LOGO
         // ============================================

         Path logoFile = copyBeasLogo(storageDir);


         // ============================================
         // ADD LOGO TO PSR MARKDOWN
         // ============================================

         String psrMarkdown =
                 "![BEAS Logo](" 
                 + logoFile.getFileName()
                 + "){width=2.5in}\n\n"
                 + markdown;


         // ============================================
         // WRITE MARKDOWN
         // ============================================

         Files.writeString(
                 markdownFile,
                 psrMarkdown
         );

            // ============================================
            // MARKDOWN → DOCX
            // ============================================

            executePandoc(
                    markdownFile,
                    docxFile
            );

            // ============================================
            // READ GENERATED DOCX
            // ============================================

            byte[] docxBytes =
                    Files.readAllBytes(
                            docxFile
                    );

            return new ConversionResult(
                    docxBytes,
                    docxFile.toString()
            );

        } catch (Exception ex) {

            throw new ProposalFailedException(
                    "Failed to convert PSR Markdown to DOCX using Pandoc",
                    ex
            );
        }
    }


    private void executePandoc(
        Path markdownFile,
        Path docxFile
	) throws IOException, InterruptedException {
	
	    ProcessBuilder processBuilder =
	            new ProcessBuilder(
	                    "pandoc",
	                    "--from=markdown+raw_html+fenced_divs",
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
	
	    Process process =
	            processBuilder.start();
	
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
	                "Pandoc did not generate a valid PSR DOCX file"
	        );
	    }
	}


    private String sanitizeFileName(
            String baseFileName
    ) {

        return baseFileName.replaceAll(
                "[^a-zA-Z0-9._-]",
                "_"
        );
    }


    public record ConversionResult(
            byte[] docxBytes,
            String generatedDocPath
    ) {
    }
    
    private Path copyBeasLogo(Path storageDir)
            throws IOException {

        ClassPathResource logoResource =
                new ClassPathResource(
                        "psr/beas-logo.png"
                );

        if (!logoResource.exists()) {

            throw new IOException(
                    "BEAS logo not found in classpath: "
                            + "psr/beas-logo.png"
            );
        }

        Path logoFile =
                storageDir.resolve(
                        "beas-logo.png"
                );

        try (InputStream inputStream =
                     logoResource.getInputStream()) {

            Files.copy(
                    inputStream,
                    logoFile,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
        }

        return logoFile;
    }
}