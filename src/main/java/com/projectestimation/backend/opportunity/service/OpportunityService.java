package com.projectestimation.backend.opportunity.service;

import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.opportunity.dto.*;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.model.OpportunityStatus;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;

    public OpportunityService(OpportunityRepository opportunityRepository) {
        this.opportunityRepository = opportunityRepository;
    }

    public OpportunityResponse createOpportunity(OpportunityCreateRequest request) {
        Opportunity opportunity = new Opportunity();
        applyCreateRequest(opportunity, request);
        Opportunity saved = opportunityRepository.save(opportunity);
        return toResponse(saved);
    }

    public List<OpportunityListResponse> getAllOpportunities() {
        return opportunityRepository.findAll().stream()
                .sorted(Comparator.comparing(Opportunity::getCreatedAt).reversed())
                .map(this::toListResponse)
                .toList();
    }

    public OpportunityResponse getOpportunityById(Long id) {
        Opportunity opportunity = findOpportunityOrThrow(id);
        return toResponse(opportunity);
    }

    public OpportunityResponse updateOpportunity(Long id, OpportunityUpdateRequest request) {
        Opportunity opportunity = findOpportunityOrThrow(id);
        applyUpdateRequest(opportunity, request);
        Opportunity saved = opportunityRepository.save(opportunity);
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
        return new OpportunityResponse(
                opportunity.getId(),
                opportunity.getImplementationType(),
                List.copyOf(opportunity.getPlatforms()),
                List.copyOf(opportunity.getTechnologyCategories()),
                List.copyOf(opportunity.getEnterpriseContexts()),
                opportunity.getOpportunityName(),
                opportunity.getClientName(),
                opportunity.getRequirementSummary(),
                opportunity.getPriority(),
                opportunity.getExpectedDeliveryDate(),
                List.copyOf(opportunity.getComponents()),
                opportunity.getStatus(),
                opportunity.getCreatedAt(),
                opportunity.getUpdatedAt()
        );
    }

    private OpportunityListResponse toListResponse(Opportunity opportunity) {
        return new OpportunityListResponse(
                opportunity.getId(),
                opportunity.getOpportunityName(),
                opportunity.getClientName(),
                opportunity.getImplementationType(),
                opportunity.getPriority(),
                opportunity.getStatus(),
                opportunity.getExpectedDeliveryDate(),
                opportunity.getCreatedAt(),
                opportunity.getUpdatedAt()
        );
    }
}
