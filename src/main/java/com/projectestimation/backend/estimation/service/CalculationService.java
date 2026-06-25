package com.projectestimation.backend.estimation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.estimation.dto.ActorCalculationRequest;
import com.projectestimation.backend.estimation.dto.ActorCalculationResponse;
import com.projectestimation.backend.estimation.dto.ActorDto;
import com.projectestimation.backend.estimation.dto.UseCaseCalculationRequest;
import com.projectestimation.backend.estimation.dto.UseCaseCalculationResponse;
import com.projectestimation.backend.estimation.dto.UseCaseDto;
import com.projectestimation.backend.estimation.model.EstimationActor;
import com.projectestimation.backend.estimation.model.EstimationAnalysis;
import com.projectestimation.backend.estimation.repository.EstimationActorRepository;
import com.projectestimation.backend.estimation.repository.EstimationAnalysisRepository;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;

@Service
@Transactional
public class CalculationService {

    private final OpportunityRepository opportunityRepository;
    private final EstimationAnalysisRepository estimationAnalysisRepository;
    private final EstimationActorRepository estimationActorRepository;

    public CalculationService(
            OpportunityRepository opportunityRepository,
            EstimationAnalysisRepository estimationAnalysisRepository,
            EstimationActorRepository estimationActorRepository) {

        this.opportunityRepository = opportunityRepository;
        this.estimationAnalysisRepository = estimationAnalysisRepository;
        this.estimationActorRepository = estimationActorRepository;
    }

    public ActorCalculationResponse calculate(
            ActorCalculationRequest request) {

        EstimationAnalysis analysis =
                estimationAnalysisRepository
                        .findByOpportunityId(request.getOpportunityId())
                        .orElseGet(() -> {

                            Opportunity opportunity =
                                    opportunityRepository.findById(
                                            request.getOpportunityId())
                                            .orElseThrow(() ->
                                                    new ResourceNotFoundException(
                                                            "Opportunity not found"));

                            EstimationAnalysis newAnalysis =
                                    new EstimationAnalysis();

                            newAnalysis.setOpportunity(opportunity);

                            return estimationAnalysisRepository.save(newAnalysis);
                        });

        // Delete previously saved actors
        estimationActorRepository.deleteByEstimationAnalysisId(
                analysis.getId());

        int simple = 0;
        int average = 0;
        int complex = 0;

        for (ActorDto dto : request.getActors()) {

            EstimationActor actor = new EstimationActor();

            actor.setEstimationAnalysis(analysis);
            actor.setActorName(dto.getActorName());
            actor.setActorType(dto.getActorType());

            estimationActorRepository.save(actor);

            if (dto.getActorType() == null) {
                continue;
            }

            switch (dto.getActorType().trim().toUpperCase()) {

                case "SIMPLE":
                    simple++;
                    break;

                case "AVERAGE":
                    average++;
                    break;

                case "COMPLEX":
                    complex++;
                    break;
            }
        }

        int aw =
                (simple * 1)
                        + (average * 2)
                        + (complex * 3);

        analysis.setActorWeight(aw);

        estimationAnalysisRepository.save(analysis);

        return new ActorCalculationResponse(
                simple,
                average,
                complex,
                aw);
    }

    public UseCaseCalculationResponse calculate(
            UseCaseCalculationRequest request) {

        int simple = 0;
        int average = 0;
        int complex = 0;

        for (UseCaseDto useCase : request.getUseCases()) {

            if (useCase.getComplexity() == null) {
                continue;
            }

            switch (useCase.getComplexity().trim().toUpperCase()) {

                case "SIMPLE":
                    simple++;
                    break;

                case "AVERAGE":
                    average++;
                    break;

                case "COMPLEX":
                    complex++;
                    break;
            }
        }

        int uucp =
                (simple * 5)
                        + (average * 10)
                        + (complex * 15);

        return new UseCaseCalculationResponse(
                simple,
                average,
                complex,
                uucp);
    }
}