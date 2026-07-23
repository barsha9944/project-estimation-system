package com.projectestimation.backend.projectschedule.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

public class ExcelChartDateAxisFixer {

    public static byte[] fixDateAxis(byte[] workbookBytes) throws Exception {

        Path tempDir = Files.createTempDirectory("xlsx");

        unzip(workbookBytes, tempDir);

        // We'll modify chart1.xml here

        Path chart =
                tempDir.resolve("xl")
                        .resolve("charts")
                        .resolve("chart1.xml");

        if (Files.exists(chart)) {

            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();

            factory.setNamespaceAware(true);

            DocumentBuilder builder =
                    factory.newDocumentBuilder();

            Document document =
                    builder.parse(chart.toFile());

            // Save it back (no changes yet)
            Transformer transformer =
                    TransformerFactory.newInstance()
                            .newTransformer();

            transformer.setOutputProperty(
                    OutputKeys.INDENT,
                    "yes");

            transformer.transform(
                    new DOMSource(document),
                    new StreamResult(chart.toFile()));
        }

        return zip(tempDir);
    }

    private static void unzip(byte[] data, Path target)
            throws IOException {

        try (ZipInputStream zis =
                     new ZipInputStream(
                             new ByteArrayInputStream(data))) {

            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {

                Path file =
                        target.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(file);
                } else {

                    Files.createDirectories(file.getParent());

                    Files.copy(
                            zis,
                            file,
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static byte[] zip(Path folder)
            throws IOException {

        ByteArrayOutputStream out =
                new ByteArrayOutputStream();

        try (ZipOutputStream zos =
                     new ZipOutputStream(out)) {

            Files.walk(folder)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {

                        try {

                            String name =
                                    folder.relativize(path)
                                            .toString()
                                            .replace("\\", "/");

                            zos.putNextEntry(
                                    new ZipEntry(name));

                            Files.copy(path, zos);

                            zos.closeEntry();

                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        return out.toByteArray();
    }
}