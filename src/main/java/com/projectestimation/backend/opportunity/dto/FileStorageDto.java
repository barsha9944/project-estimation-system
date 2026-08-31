package com.projectestimation.backend.opportunity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileStorageDto {

	@JsonProperty("id")
	private String fileId;
	private String fileName;

	private String storedLocation;
	@JsonProperty("fileSize")
	private Long fileSizeInBytes;

	private String originalFileName;
	private boolean isActive;
}
