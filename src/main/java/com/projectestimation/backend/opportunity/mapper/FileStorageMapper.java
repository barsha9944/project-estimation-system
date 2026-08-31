package com.projectestimation.backend.opportunity.mapper;

import com.projectestimation.backend.opportunity.dto.FileStorageDto;
import com.projectestimation.backend.opportunity.model.OpportunityFile;
import com.projectestimation.backend.util.FileStorage;

public class FileStorageMapper {

	public static FileStorage toEntity(FileStorageDto dto) {

		if (dto == null) {
			return null;
		}

		return FileStorage.builder().fileName(dto.getFileName()).storedLocation(dto.getStoredLocation())
				.fileSize(dto.getFileSizeInBytes()).originalFileName(dto.getOriginalFileName()).isActive(dto.isActive())
				.build();
	}

	public static OpportunityFile toOppotunityFileEntity(FileStorageDto dto) {

		if (dto == null) {
			return null;
		}

		return OpportunityFile.builder().fileId(dto.getFileId()).fileName(dto.getFileName()).storedLocation(dto.getStoredLocation())
				.fileSize(dto.getFileSizeInBytes()).originalFileName(dto.getOriginalFileName()).isActive(dto.isActive())
				.build();
	}

	public static FileStorageDto toDto(FileStorage entity) {

		if (entity == null) {
			return null;
		}

		return FileStorageDto.builder().fileId(entity.getId().toString()).fileName(entity.getFileName())
				.storedLocation(entity.getStoredLocation()).fileSizeInBytes(entity.getFileSize())
				.originalFileName(entity.getOriginalFileName()).isActive(entity.isActive()).build();
	}
}
