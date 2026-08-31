package com.projectestimation.backend.util;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_storage")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileStorage {

	@Id
	//@GeneratedValue(strategy = GenerationType.IDENTITY)
	private String id;

	@Column(name = "file_name", nullable = false)
	private String fileName;

	@Column(name = "stored_location", nullable = false)
	private String storedLocation;

	@Column(name = "file_size", nullable = false)
	private Long fileSize;

	@Column(name = "original_file_name", nullable = false)
	private String originalFileName;

	@Column(name = "created_on", nullable = false, updatable = false)
	private LocalDateTime createdOn;

	@Column(name = "updated_on")
	private LocalDateTime updatedOn;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "updated_by")
	private String updatedBy;

	@Column(name = "is_active")
	private boolean isActive;

	@PrePersist
	protected void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdOn = now;
		updatedOn = now;
	}

	@PreUpdate
	protected void onUpdate() {
		updatedOn = LocalDateTime.now();
	}
}
