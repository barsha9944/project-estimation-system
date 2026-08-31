package com.projectestimation.backend.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import com.projectestimation.backend.opportunity.dto.FileStorageDto;

import jakarta.validation.Valid;

@Component
@Validated
public class FileUploadUtil {
	private static final Logger log = LogManager.getLogger(FileUploadUtil.class);

	public FileStorageDto uploadFile(String destinationPath, @Valid MultipartFile requirementFile,
			Set<String> allowedExtensions, long allowedFileSizeInBytes) {

		String fileName = null;
		FileStorageDto fileStorageDto = new FileStorageDto();

		try {
			if (requirementFile == null || requirementFile.isEmpty()) {
				throw new IOException("File is empty or not provided");
			}

			// Get and sanitize original filename
			String originalFileName = requirementFile.getOriginalFilename();

			if (originalFileName == null || originalFileName.isBlank()) {
				throw new IOException("Invalid file name");

			}

			fileName = StringUtils.cleanPath(originalFileName);

			// Prevent directory traversal
			if (fileName.contains("..")) {
				throw new IOException("Invalid path sequence in file name: " + fileName);

			}

			// Get extension
			String uploadedFileExtension = getExtension(fileName).toLowerCase();

			// Validate extension
			if (!allowedExtensions.contains(uploadedFileExtension)) {
				throw new IOException("Invalid file type. Allowed extensions are: " + allowedExtensions);

			}

			// Validate file size
			if (requirementFile.getSize() > allowedFileSizeInBytes) {
				throw new IOException("File size exceeds the allowed limit of " + allowedFileSizeInBytes + " bytes");

			}

			// Get filename without extension
			String originalNameWithoutExtension = fileName;

			int lastDotIndex = fileName.lastIndexOf(".");
			if (lastDotIndex > 0) {
				originalNameWithoutExtension = fileName.substring(0, lastDotIndex);
			}

			// Create a unique filename
			String alteredFileName = "Opportunity_" + originalNameWithoutExtension + "_" + UUID.randomUUID() + "."
					+ uploadedFileExtension;

			// Destination directory
			Path destinationDirectory = Paths.get(destinationPath).toAbsolutePath().normalize();

			// Create directory if it does not exist
			Files.createDirectories(destinationDirectory);

			// Final target path
			Path targetPath = destinationDirectory.resolve(alteredFileName).normalize();

			// Extra security check
			if (!targetPath.startsWith(destinationDirectory)) {
				throw new IOException("Invalid file path");
			}

			// Copy file
			long fileUpStatus = Files.copy(requirementFile.getInputStream(), targetPath,
					StandardCopyOption.REPLACE_EXISTING);
			if (fileUpStatus > 0) {
				fileStorageDto.setFileName(alteredFileName);
				fileStorageDto.setOriginalFileName(originalFileName);
				fileStorageDto.setFileSizeInBytes(requirementFile.getSize());
				fileStorageDto.setStoredLocation(destinationPath);
				fileStorageDto.setActive(true);
			}
			return fileStorageDto;

		} catch (Exception ex) {
			log.error("Exception occurred while uploading file :: ", ex);
			return fileStorageDto;
		}
	}

	private static String getExtension(String fileName) {
		int lastDot = fileName.lastIndexOf('.');
		if (lastDot > 0 && lastDot < fileName.length() - 1) {
			return fileName.substring(lastDot + 1);
		}
		return "";
	}

}
