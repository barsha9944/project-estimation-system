
package com.projectestimation.backend.car.service;

import com.projectestimation.backend.car.dto.CarActionDto;
import com.projectestimation.backend.car.model.CarAction;
import com.projectestimation.backend.car.model.CarAnalysis;
import com.projectestimation.backend.car.repository.CarActionRepository;

import com.projectestimation.backend.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CarActionPlanService {

    private final CarService carService;
    private final CarActionRepository actionRepository;

    public CarActionDto addAction(
            Long carId,
            CarActionDto request) {

        CarAnalysis car = carService.getCarEntity(carId);

        CarAction action = CarAction.builder()
                .carAnalysis(car)
                .actionCategory(request.getActionCategory())
                .actionDescription(request.getActionDescription())
                .assignedTo(request.getAssignedTo())
                .priority(request.getPriority())
                .targetCompletionDate(
                        request.getTargetCompletionDate()
                )
                .actionTaken(request.getActionTaken())
                .implementationDate(request.getImplementationDate())
                .evidenceReference(request.getEvidenceReference())
                .status(
                        request.getStatus() == null
                                ? "PENDING"
                                : request.getStatus()
                )
                .build();

        car.setStatus("IN_PROGRESS");

        return toDto(actionRepository.save(action));
    }

    public CarActionDto updateAction(
            Long actionId,
            CarActionDto request) {

        CarAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CAR action not found: " + actionId
                ));

        if (request.getActionCategory() != null) {
            action.setActionCategory(request.getActionCategory());
        }

        if (request.getActionDescription() != null) {
            action.setActionDescription(request.getActionDescription());
        }

        if (request.getAssignedTo() != null) {
            action.setAssignedTo(request.getAssignedTo());
        }

        if (request.getPriority() != null) {
            action.setPriority(request.getPriority());
        }

        if (request.getTargetCompletionDate() != null) {
            action.setTargetCompletionDate(
                    request.getTargetCompletionDate()
            );
        }

        if (request.getActionTaken() != null) {
            action.setActionTaken(request.getActionTaken());
        }

        if (request.getImplementationDate() != null) {
            action.setImplementationDate(
                    request.getImplementationDate()
            );
        }

        if (request.getEvidenceReference() != null) {
            action.setEvidenceReference(
                    request.getEvidenceReference()
            );
        }

        if (request.getStatus() != null) {
            action.setStatus(request.getStatus());
        }

        return toDto(actionRepository.save(action));
    }

    @Transactional(readOnly = true)
    public List<CarActionDto> getActions(Long carId) {

        carService.getCarEntity(carId);

        return actionRepository.findByCarAnalysisId(carId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private CarActionDto toDto(CarAction action) {

        return CarActionDto.builder()
                .id(action.getId())
                .actionCategory(action.getActionCategory())
                .actionDescription(action.getActionDescription())
                .assignedTo(action.getAssignedTo())
                .priority(action.getPriority())
                .targetCompletionDate(
                        action.getTargetCompletionDate()
                )
                .actionTaken(action.getActionTaken())
                .implementationDate(action.getImplementationDate())
                .evidenceReference(action.getEvidenceReference())
                .status(action.getStatus())
                .build();
    }
}
