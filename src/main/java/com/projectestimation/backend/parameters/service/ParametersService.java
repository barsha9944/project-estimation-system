package com.projectestimation.backend.parameters.service;

import com.projectestimation.backend.common.exception.BadRequestException;
import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.parameters.dto.ParametersCreateRequest;
import com.projectestimation.backend.parameters.dto.ParametersResponse;
import com.projectestimation.backend.parameters.dto.ParametersUpdateRequest;
import com.projectestimation.backend.parameters.model.ComplexityLevel;
import com.projectestimation.backend.parameters.model.Parameters;
import com.projectestimation.backend.parameters.repository.ParametersRepository;
import org.springframework.stereotype.Service;

@Service
public class ParametersService {

    private final ParametersRepository parametersRepository;
    private final OpportunityRepository opportunityRepository;

    public ParametersService(ParametersRepository parametersRepository,
                             OpportunityRepository opportunityRepository) {
        this.parametersRepository = parametersRepository;
        this.opportunityRepository = opportunityRepository;
    }

    public ParametersResponse createParameters(Long opportunityId, ParametersCreateRequest request) {
        Opportunity opportunity = findOpportunityOrThrow(opportunityId);

        if (parametersRepository.findByOpportunityId(opportunityId).isPresent()) {
            throw new BadRequestException("Parameters already exist for this opportunity");
        }

        Parameters parameters = new Parameters();
        parameters.setOpportunity(opportunity);
        applyRequestFields(parameters, request.complexity(), request.riskFactor(),
                request.productivityFactor(), request.hourlyRate(), request.teamSize());

        Parameters saved = parametersRepository.save(parameters);
        return toResponse(saved);
    }

    public ParametersResponse getParametersByOpportunityId(Long opportunityId) {
        findOpportunityOrThrow(opportunityId);
        Parameters parameters = findParametersByOpportunityOrThrow(opportunityId);
        return toResponse(parameters);
    }

    public ParametersResponse updateParameters(Long opportunityId, ParametersUpdateRequest request) {
        findOpportunityOrThrow(opportunityId);
        Parameters parameters = findParametersByOpportunityOrThrow(opportunityId);
        applyRequestFields(parameters, request.complexity(), request.riskFactor(),
                request.productivityFactor(), request.hourlyRate(), request.teamSize());

        Parameters saved = parametersRepository.save(parameters);
        return toResponse(saved);
    }

    private Opportunity findOpportunityOrThrow(Long opportunityId) {
        return opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
    }

    private Parameters findParametersByOpportunityOrThrow(Long opportunityId) {
        return parametersRepository.findByOpportunityId(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Parameters not found for this opportunity"));
    }

    private void applyRequestFields(Parameters parameters,
                                    ComplexityLevel complexity,
                                    Double riskFactor,
                                    Double productivityFactor,
                                    Double hourlyRate,
                                    Integer teamSize) {
        parameters.setComplexity(complexity);
        parameters.setRiskFactor(riskFactor);
        parameters.setProductivityFactor(productivityFactor);
        parameters.setHourlyRate(hourlyRate);
        parameters.setTeamSize(teamSize);
    }

    private ParametersResponse toResponse(Parameters parameters) {
        return new ParametersResponse(
                parameters.getId(),
                parameters.getOpportunity().getId(),
                parameters.getComplexity(),
                parameters.getRiskFactor(),
                parameters.getProductivityFactor(),
                parameters.getHourlyRate(),
                parameters.getTeamSize(),
                parameters.getCreatedAt(),
                parameters.getUpdatedAt()
        );
    }
}
