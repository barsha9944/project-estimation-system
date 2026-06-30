package com.projectestimation.backend.estimation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.estimation.dto.ActorCalculationRequest;
import com.projectestimation.backend.estimation.dto.ActorCalculationResponse;
import com.projectestimation.backend.estimation.dto.ActorDto;
import com.projectestimation.backend.estimation.dto.EnvironmentalFactorCalculationRequest;
import com.projectestimation.backend.estimation.dto.EnvironmentalFactorCalculationResponse;
import com.projectestimation.backend.estimation.dto.EnvironmentalFactorDto;
import com.projectestimation.backend.estimation.dto.TechnicalFactorCalculationRequest;
import com.projectestimation.backend.estimation.dto.TechnicalFactorCalculationResponse;
import com.projectestimation.backend.estimation.dto.TechnicalFactorDto;
import com.projectestimation.backend.estimation.dto.UseCaseCalculationRequest;
import com.projectestimation.backend.estimation.dto.UseCaseCalculationResponse;
import com.projectestimation.backend.estimation.dto.UseCaseDto;
import com.projectestimation.backend.estimation.model.EstimationActor;
import com.projectestimation.backend.estimation.model.EstimationAnalysis;
import com.projectestimation.backend.estimation.model.EstimationEnvironmentalFactor;
import com.projectestimation.backend.estimation.model.EstimationTechnicalFactor;
import com.projectestimation.backend.estimation.model.EstimationUseCase;
import com.projectestimation.backend.estimation.repository.EstimationActorRepository;
import com.projectestimation.backend.estimation.repository.EstimationAnalysisRepository;
import com.projectestimation.backend.estimation.repository.EstimationEnvironmentalFactorRepository;
import com.projectestimation.backend.estimation.repository.EstimationTechnicalFactorRepository;
import com.projectestimation.backend.estimation.repository.EstimationUseCaseRepository;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;

@Service
@Transactional
public class CalculationService {

    private final OpportunityRepository opportunityRepository;
    private final EstimationAnalysisRepository estimationAnalysisRepository;
    private final EstimationActorRepository estimationActorRepository;
    private final EstimationUseCaseRepository estimationUseCaseRepository;
    private final EstimationTechnicalFactorRepository estimationTechnicalFactorRepository;
    private final EstimationEnvironmentalFactorRepository estimationEnvironmentalFactorRepository;

    public CalculationService(
            OpportunityRepository opportunityRepository,
            EstimationAnalysisRepository estimationAnalysisRepository,
            EstimationActorRepository estimationActorRepository,
            EstimationUseCaseRepository estimationUseCaseRepository,
            EstimationTechnicalFactorRepository estimationTechnicalFactorRepository,
            EstimationEnvironmentalFactorRepository estimationEnvironmentalFactorRepository) {

        this.opportunityRepository = opportunityRepository;
        this.estimationAnalysisRepository = estimationAnalysisRepository;
        this.estimationActorRepository = estimationActorRepository;
        this.estimationUseCaseRepository = estimationUseCaseRepository;
        this.estimationTechnicalFactorRepository = estimationTechnicalFactorRepository;
        this.estimationEnvironmentalFactorRepository = estimationEnvironmentalFactorRepository;
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

        EstimationAnalysis analysis =
                estimationAnalysisRepository
                        .findByOpportunityId(request.getOpportunityId())
                        .orElseGet(() -> {

                            Opportunity opportunity =
                                    opportunityRepository
                                            .findById(request.getOpportunityId())
                                            .orElseThrow(() ->
                                                    new ResourceNotFoundException("Opportunity not found"));

                            EstimationAnalysis newAnalysis =
                                    new EstimationAnalysis();

                            newAnalysis.setOpportunity(opportunity);

                            return estimationAnalysisRepository.save(newAnalysis);
                        });

        // Delete previous use cases
        estimationUseCaseRepository.deleteByEstimationAnalysisId(
                analysis.getId());

        int simple = 0;
        int average = 0;
        int complex = 0;

        for (UseCaseDto dto : request.getUseCases()) {

            EstimationUseCase useCase =
                    new EstimationUseCase();

            useCase.setEstimationAnalysis(analysis);
            useCase.setUseCaseName(dto.getUseCaseName());
            useCase.setComplexity(dto.getComplexity());

            estimationUseCaseRepository.save(useCase);

            if (dto.getComplexity() == null) {
                continue;
            }

            switch (dto.getComplexity().trim().toUpperCase()) {

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

        analysis.setUucp(uucp);

        estimationAnalysisRepository.save(analysis);

        return new UseCaseCalculationResponse(
                simple,
                average,
                complex,
                uucp);
    }
    
    public TechnicalFactorCalculationResponse calculate(
            TechnicalFactorCalculationRequest request
    ) {

        EstimationAnalysis analysis =
                estimationAnalysisRepository
                        .findByOpportunityId(
                                request.getOpportunityId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Estimation Analysis not found"
                                ));

        estimationTechnicalFactorRepository
                .deleteByEstimationAnalysisId(
                        analysis.getId()
                );

        double total = 0;

        for (TechnicalFactorDto dto : request.getTechnicalFactors()) {

            EstimationTechnicalFactor factor =
                    new EstimationTechnicalFactor();

            factor.setEstimationAnalysis(
                    analysis
            );

            factor.setFactorName(
                    dto.getFactorName()
            );

            factor.setMultiplier(
                    dto.getMultiplier()
            );

            factor.setMagnitude(
                    dto.getMagnitude()
            );

            estimationTechnicalFactorRepository
                    .save(
                            factor
                    );

            total +=
                    dto.getMultiplier()
                    * dto.getMagnitude();
        }

        double tcf =
                0.6
                + (total / 100.0);

        analysis.setTcf(
                tcf
        );

        estimationAnalysisRepository.save(
                analysis
        );

        return new TechnicalFactorCalculationResponse(
                tcf
        );
    }
    
    public EnvironmentalFactorCalculationResponse calculate(
            EnvironmentalFactorCalculationRequest request
    ) {

        EstimationAnalysis analysis =
                estimationAnalysisRepository
                        .findByOpportunityId(request.getOpportunityId())
                        .orElseThrow(() ->
                                new RuntimeException("Estimation Analysis not found"));

        estimationEnvironmentalFactorRepository
                .deleteByEstimationAnalysisId(
                        analysis.getId()
                );

        double weightedSum = 0;

        for (EnvironmentalFactorDto dto : request.getEnvironmentalFactors()) {

            EstimationEnvironmentalFactor factor =
                    new EstimationEnvironmentalFactor();

            factor.setEstimationAnalysis(analysis);
            factor.setFactorName(dto.getFactorName());
            factor.setMultiplier(dto.getMultiplier());
            factor.setMagnitude(dto.getMagnitude());
            factor.setDescription(dto.getDescription());

            estimationEnvironmentalFactorRepository.save(factor);

            weightedSum +=
                    dto.getMultiplier()
                    * dto.getMagnitude();
        }

        double ef =
                1.4
                + (-0.03 * weightedSum);

        analysis.setEf(ef);

        estimationAnalysisRepository.save(analysis);

        return new EnvironmentalFactorCalculationResponse(
                ef
        );
    }
}