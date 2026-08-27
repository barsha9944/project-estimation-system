package com.projectestimation.backend.opportunity.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.constants.ProjectConstants;
import com.projectestimation.backend.opportunity.dto.FileStorageDto;
import com.projectestimation.backend.opportunity.dto.OpportunityCreateRequest;
import com.projectestimation.backend.opportunity.dto.OpportunityListResponse;
import com.projectestimation.backend.opportunity.dto.OpportunityResponse;
import com.projectestimation.backend.opportunity.dto.OpportunityUpdateRequest;
import com.projectestimation.backend.opportunity.mapper.FileStorageMapper;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.model.OpportunityFile;
import com.projectestimation.backend.opportunity.model.OpportunityStatus;
import com.projectestimation.backend.opportunity.repository.OpportunityFileRepository;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.util.FileStorage;
import com.projectestimation.backend.util.FileUploadUtil;

import jakarta.validation.Valid;

@Validated
@Service
public class OpportunityService {
	private static final Logger log = LogManager.getLogger(OpportunityService.class);
	private final OpportunityRepository opportunityRepository;
	private final FileUploadUtil fileUploadUtil;
	private final Environment environment;
	private final OpportunityFileRepository opportunityFileRepository;
	private final SequenceService sequenceService;

	public OpportunityService(OpportunityRepository opportunityRepository, FileUploadUtil fileUploadUtil,
			Environment environment, OpportunityFileRepository opportunityFileRepository,
			SequenceService sequenceService) {
		this.opportunityRepository = opportunityRepository;
		this.fileUploadUtil = fileUploadUtil;
		this.environment = environment;
		this.opportunityFileRepository = opportunityFileRepository;
		this.sequenceService = sequenceService;
	}

	public OpportunityResponse createOpportunity(OpportunityCreateRequest request) {
		Opportunity opportunity = new Opportunity();
		OpportunityFile oOpportunityFile = null;
		try {
			applyCreateRequest(opportunity, request);
			long opportunityId = sequenceService.getNextValue();
			log.info("opportunityId :: {}", opportunityId);
			if (opportunityId == 0) {
				throw new RuntimeException("Error occured ");
			}
			opportunity.setId(opportunityId);
			log.info("req summery :: {}", opportunity.getRequirementSummary());
			if (opportunity.getRequirementSummary() == null || opportunity.getRequirementSummary() == "") {
				log.info("req summery mot present");
				if (request.attachmendDta() != null) {
					FileStorageDto storage = request.attachmendDta();
					Path srcPath = Paths.get(storage.getStoredLocation(), storage.getFileName());
					log.info("sorce path : {}", srcPath);
					Path destPath = Paths
							.get(environment.getProperty("proposal.storage.path").concat("/").concat("opportunity-")
									.concat(String.valueOf(opportunityId)).concat("/").concat("uploaded_files"));
					log.info("destPath : {}", destPath);
					if (!Files.isDirectory(destPath)) {
						Files.createDirectories(destPath);
					}
					Path destination = destPath.resolve(storage.getFileName());
					log.info("destination : {}", destination);
					Files.copy(srcPath, destination, StandardCopyOption.REPLACE_EXISTING);
					oOpportunityFile = FileStorageMapper.toOppotunityFileEntity(storage);
					oOpportunityFile.setStoredLocation(destination.toString());
					oOpportunityFile.setOpportunity(opportunity);
					oOpportunityFile.setCreatedBy(null);
					oOpportunityFile.setCreatedOn(LocalDateTime.now());
					oOpportunityFile.setUpdatedOn(LocalDateTime.now());

				} else {
					throw new RuntimeException("Eithr requirment summery or requirmrment document must be present");
				}
			}
			if (oOpportunityFile != null) {
				opportunity.setOpportunityFile(oOpportunityFile);
			}
			Opportunity saved =  opportunityRepository.save(opportunity);

			return toResponse(saved);
		} catch (Exception e) {
			log.error("Error occured while creating opportunity :: ", e);
			throw new RuntimeException("Unable to create opportunity ", e);
		}
	}

	public List<OpportunityListResponse> getAllOpportunities() {
		return opportunityRepository.findAll().stream()
				.sorted(Comparator.comparing(Opportunity::getCreatedAt).reversed()).map(this::toListResponse).toList();
	}

	public OpportunityResponse getOpportunityById(Long id) {
		Opportunity opportunity = findOpportunityOrThrow(id);
		return toResponse(opportunity);
	}

	public OpportunityResponse updateOpportunity(Long id, OpportunityUpdateRequest request) {
		Opportunity opportunity = findOpportunityOrThrow(id);
		applyUpdateRequest(opportunity, request);
		Opportunity saved = opportunityRepository.save(opportunity);
		if (saved != null) {
			FileStorageDto storageDto = request.opportunityFileStorage();
			if (storageDto != null) {
				Path filePath = Paths.get(storageDto.getStoredLocation(), storageDto.getFileName()).normalize();
				Path destPath = Paths.get(environment.getProperty("proposal.storage.path")
						.concat("opportunity-" + saved.getId()).concat("req_files"));
				try {

					if (!Files.isDirectory(destPath)) {
						Files.createDirectory(destPath);
					}
					Files.move(filePath, destPath, StandardCopyOption.REPLACE_EXISTING);

				} catch (IOException e) {
					// TODO Auto-generated catch block

				}

			}
		}
		return toResponse(saved);
	}

	private Opportunity findOpportunityOrThrow(Long id) {
		return opportunityRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
	}

	private void applyCreateRequest(Opportunity opportunity, OpportunityCreateRequest request) {
		opportunity.setImplementationType(request.implementationType());
		opportunity.setPlatforms(copyList(request.platforms()));
		opportunity.setTechnologyCategories(copyList(request.technologyCategories()));
		opportunity.setEnterpriseContexts(copyList(request.enterpriseContexts()));
		opportunity.setOpportunityName(request.opportunityName().trim());
		opportunity.setClientName(request.clientName().trim());
		opportunity.setRequirementSummary(request.requirementSummary().trim());
		opportunity.setPriority(request.priority());
		opportunity.setExpectedDeliveryDate(request.expectedDeliveryDate());
		opportunity.setComponents(copyList(request.components()));
		opportunity.setStatus(OpportunityStatus.NEW);
	}

	private void applyUpdateRequest(Opportunity opportunity, OpportunityUpdateRequest request) {
		opportunity.setImplementationType(request.implementationType());
		opportunity.setPlatforms(copyList(request.platforms()));
		opportunity.setTechnologyCategories(copyList(request.technologyCategories()));
		opportunity.setEnterpriseContexts(copyList(request.enterpriseContexts()));
		opportunity.setOpportunityName(request.opportunityName().trim());
		opportunity.setClientName(request.clientName().trim());
		opportunity.setRequirementSummary(request.requirementSummary().trim());
		opportunity.setPriority(request.priority());
		opportunity.setExpectedDeliveryDate(request.expectedDeliveryDate());
		opportunity.setComponents(copyList(request.components()));
		if (request.status() != null) {
			opportunity.setStatus(request.status());
		}
	}

	private List<String> copyList(List<String> source) {
		return source == null ? new ArrayList<>() : new ArrayList<>(source);
	}

	private OpportunityResponse toResponse(Opportunity opportunity) {
		return new OpportunityResponse(opportunity.getId(), opportunity.getImplementationType(),
				List.copyOf(opportunity.getPlatforms()), List.copyOf(opportunity.getTechnologyCategories()),
				List.copyOf(opportunity.getEnterpriseContexts()), opportunity.getOpportunityName(),
				opportunity.getClientName(), opportunity.getRequirementSummary(), opportunity.getPriority(),
				opportunity.getExpectedDeliveryDate(), List.copyOf(opportunity.getComponents()),
				opportunity.getStatus(), opportunity.getCreatedAt(), opportunity.getUpdatedAt());
	}

	private OpportunityListResponse toListResponse(Opportunity opportunity) {
		return new OpportunityListResponse(opportunity.getId(), opportunity.getOpportunityName(),
				opportunity.getClientName(), opportunity.getImplementationType(), opportunity.getPriority(),
				opportunity.getStatus(), opportunity.getExpectedDeliveryDate(), opportunity.getCreatedAt(),
				opportunity.getUpdatedAt());
	}

	public FileStorage uploadRequirmentFile(@Valid MultipartFile requirmentFile) {
		long allowedSize;
		FileStorage returnResponse = null;
		String destinationPath = environment.getProperty("file.upload-dir");
		Set<String> allowedExtensions = Set.of(ProjectConstants.EXTENTION_PDF, ProjectConstants.EXTENTION_DOC,
				ProjectConstants.EXTENTION_DOCX);
		try {
			String allowedSizeString = environment.getProperty("spring.servlet.multipart.max-file-size");
			if (allowedSizeString == null || allowedSizeString.isBlank()) {
				throw new IllegalArgumentException("File size configuration is missing");
			}
			allowedSizeString = allowedSizeString.trim().toUpperCase();
			// Convert MB or KB to Bytes cleanly
			if (allowedSizeString.endsWith(ProjectConstants.SIZE_IN_MB)) {

				long size = Long.parseLong(allowedSizeString.substring(0, allowedSizeString.length() - 2).trim());

				allowedSize = DataSize.ofMegabytes(size).toBytes();

			} else if (allowedSizeString.endsWith(ProjectConstants.SIZE_IN_KB)) {

				long size = Long.parseLong(allowedSizeString.substring(0, allowedSizeString.length() - 2).trim());

				allowedSize = DataSize.ofKilobytes(size).toBytes();

			} else {
				throw new IllegalArgumentException("Unsupported file size format: " + allowedSizeString);
			}

			if (requirmentFile != null) {
				FileStorageDto fileStorageDto = fileUploadUtil.uploadFile(destinationPath, requirmentFile,
						allowedExtensions, allowedSize);
				if (fileStorageDto != null) {
					String fileId = UUID.randomUUID().toString();
					FileStorage storage = FileStorageMapper.toEntity(fileStorageDto);
					storage.setId(fileId);
					returnResponse = opportunityFileRepository.save(storage);
				}

			}

		} catch (Exception e) {
			log.error("Exception Occured while uploading file ", e);
			throw e;
		}
		return returnResponse;
	}

	public FileStorageDto getFileById(String id) {

		FileStorage fileStorage = opportunityFileRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("File not found with id: " + id));

		return FileStorageMapper.toDto(fileStorage);
	}

//	public String generateRequirmentSummery() {
//		
//	}

	public void deleteFileById(String id) {

		FileStorage fileStorage = opportunityFileRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("File not found with id: " + id));

		try {
			Path filePath = Paths.get(fileStorage.getStoredLocation(), fileStorage.getFileName()).normalize();

			// Delete physical file
			Files.deleteIfExists(filePath);

			// Delete database record
			opportunityFileRepository.delete(fileStorage);

		} catch (IOException ex) {
			log.error("Error deleting file: {}", fileStorage.getFileName(), ex);

			throw new RuntimeException("Unable to delete file: " + fileStorage.getFileName(), ex);
		}
	}

	public ResponseEntity<Resource> viewFileById(String opportunityFileId) {
		FileStorage fileStorage = opportunityFileRepository.findById(opportunityFileId)
				.orElseThrow(() -> new RuntimeException("File not found with id: " + opportunityFileId));

		try {
			Path filePath = Paths.get(fileStorage.getStoredLocation(), fileStorage.getFileName()).normalize();
			Resource resource = new UrlResource(filePath.toUri());

			if (!resource.exists() || !resource.isReadable()) {
				return ResponseEntity.notFound().build();
			}
			String contentType = Files.probeContentType(filePath);

			if (contentType == null) {
				contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
			}

			String disposition = fileStorage.getOriginalFileName().toLowerCase().endsWith(".pdf") ? "inline"
					: "attachment";

			return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
					.header(HttpHeaders.CONTENT_DISPOSITION,
							disposition + "; filename=\"" + resource.getFilename() + "\"")
					.body(resource);
		} catch (Exception ex) {
			log.error("Error deleting file: {}", fileStorage.getFileName(), ex);

			throw new RuntimeException("Unable to delete file: " + fileStorage.getFileName(), ex);
		}

	}
}
