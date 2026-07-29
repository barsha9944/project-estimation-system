package com.projectestimation.backend.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CreateLogDirectory {
	static final Logger logger = LogManager.getLogger(CreateLogDirectory.class);

	public String createFolderIfNotExists(String targetPath, String filename, String baseLocation) {

		String directoryPath = baseLocation + File.separator;
		String logFilePath = null;
		String folderPath = directoryPath + targetPath;
		logger.info("Project Root: " + folderPath);

		Path path = Paths.get(folderPath);
		// File file = new File(folderPath, fileName);
		try {
			if (!Files.exists(path)) {

				Path createdPath = Files.createDirectories(path);

				logger.info("Folder created successfully! " + createdPath);
				logFilePath = folderPath + File.separator + filename;
				Path filePath = Paths.get(logFilePath);
				logger.info("log File Path: " + logFilePath);
				if (!Files.exists(filePath)) {
					Files.createFile(filePath);
					logger.info("Log file created successfully.");
				}
				return logFilePath;

			} else {

				logger.info("Folder already exists!");
				logFilePath = folderPath + File.separator + filename;
				Path filePath = Paths.get(logFilePath);
				logger.info("log File Path: " + logFilePath);
				if (!Files.exists(filePath)) {
					Files.createFile(filePath);
					logger.info("Log file created successfully.");
				} else {
					logger.info("Log file already exists!");
				}
				return logFilePath;
			}
		} catch (IOException e) {
			throw new IllegalStateException("Failed to create log directory", e);
		}

	}

}
