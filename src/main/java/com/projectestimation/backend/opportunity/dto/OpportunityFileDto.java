package com.projectestimation.backend.opportunity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpportunityFileDto {

	private String fileId;

	private Long opportunityId;

	private String fileName;

	private String storedLocation;

	private Long fileSize;

	private String originalFileName;

	private Boolean isActive;

	private LocalDateTime createdOn;

	private LocalDateTime updatedOn;

	private String createdBy;

	private String updatedBy;
}
