
package com.projectestimation.backend.car.repository;

import com.projectestimation.backend.car.model.CarEffectiveness;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarEffectivenessRepository
        extends JpaRepository<CarEffectiveness, Long> {

    List<CarEffectiveness> findByCarAnalysisIdOrderByReviewDateDesc(
        Long carAnalysisId
    );
}
