package com.projectestimation.backend.rtm.model;

import com.projectestimation.backend.opportunity.model.Opportunity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "requirements_traceability_matrices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rtm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "opportunity_id",
            nullable = false,
            unique = true
    )
    private Opportunity opportunity;

    @Column(name = "rtm_data", columnDefinition = "TEXT")
    private String rtmData;
}
