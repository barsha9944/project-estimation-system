package com.projectestimation.backend.opportunity.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Table(name = "opportunity_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpportunityFile {

    @Id
    @Column(name = "file_id", length = 255, nullable = false)
    private String fileId;

//    @Column(name = "opportunity_id_fk")
//    private Long opportunityIdFK;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "stored_location", length = 1000)
    private String storedLocation;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;
    
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "opportunity_id_fk",
            nullable = false,
            unique = true
    )
    private Opportunity opportunity;
}
