
package com.projectestimation.backend.car.repository;

import com.projectestimation.backend.car.model.CarAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarActionRepository
        extends JpaRepository<CarAction, Long> {

    List<CarAction> findByCarAnalysisId(Long carAnalysisId);

    List<CarAction> findByStatus(String status);
}
