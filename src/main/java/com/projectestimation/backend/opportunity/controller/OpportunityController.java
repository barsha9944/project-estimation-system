package com.projectestimation.backend.opportunity.controller;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartFile;

import com.projectestimation.backend.auth.model.User;
import com.projectestimation.backend.common.response.ApiResponse;
import com.projectestimation.backend.opportunity.dto.FileStorageDto;
import com.projectestimation.backend.opportunity.dto.OpportunityCreateRequest;
import com.projectestimation.backend.opportunity.dto.OpportunityListResponse;
import com.projectestimation.backend.opportunity.dto.OpportunityResponse;
import com.projectestimation.backend.opportunity.dto.OpportunityUpdateRequest;
import com.projectestimation.backend.opportunity.service.OpportunityService;
import com.projectestimation.backend.util.FileStorage;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/opportunities")
@RestControllerAdvice
public class OpportunityController {

	private final OpportunityService opportunityService;

	public OpportunityController(OpportunityService opportunityService) {
		this.opportunityService = opportunityService;
	}

	@PostMapping("/create")
	public ResponseEntity<ApiResponse<OpportunityResponse>> create(@Valid @RequestBody OpportunityCreateRequest request,
			@AuthenticationPrincipal User user) {
		OpportunityResponse response = opportunityService.createOpportunity(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Opportunity created successfully", response));
	}

	// Added by Shinjan on 21-08-2026
	@PostMapping(value = "/uploadrequirmentdocument", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<FileStorage>> uploadRequirmentDocument(
			@Valid @RequestPart("file") MultipartFile requirmentFile, @AuthenticationPrincipal User user) {
		FileStorage response = opportunityService.uploadRequirmentFile(requirmentFile);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Opportunity requirment file uploaded successfully", response));
	}

	@GetMapping("/allopportunities")
	public ResponseEntity<ApiResponse<List<OpportunityListResponse>>> getAll() {
		List<OpportunityListResponse> opportunities = opportunityService.getAllOpportunities();
		return ResponseEntity.ok(ApiResponse.success("Opportunities retrieved successfully", opportunities));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<OpportunityResponse>> getById(@PathVariable Long id) {
		OpportunityResponse response = opportunityService.getOpportunityById(id);
		return ResponseEntity.ok(ApiResponse.success("Opportunity retrieved successfully", response));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<OpportunityResponse>> update(@PathVariable Long id,
			@Valid @RequestBody OpportunityUpdateRequest request) {
		OpportunityResponse response = opportunityService.updateOpportunity(id, request);
		return ResponseEntity.ok(ApiResponse.success("Opportunity updated successfully", response));
	}

	@GetMapping("/requirmentdocument/{id}")
	public ResponseEntity<FileStorageDto> getFileById(@PathVariable String id) {

		return ResponseEntity.ok(opportunityService.getFileById(id));
	}

	@DeleteMapping("/requirmentdocument/delete/{id}")
	public ResponseEntity<Void> deleteFileById(@PathVariable String id) {

		opportunityService.deleteFileById(id);

		return ResponseEntity.noContent().build();
	}

	@GetMapping("/requirmentdocument/viewfile/{id}")
	public ResponseEntity<Resource> viewRequirmentFile(@PathVariable("id") String opportunityId) {
		return opportunityService.viewFileById(opportunityId);
	}
}
