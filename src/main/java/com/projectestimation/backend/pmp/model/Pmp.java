package com.projectestimation.backend.pmp.model;

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
@Table(name = "project_management_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pmp {

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

    @Column(columnDefinition = "TEXT")
    private String pmpData;
}