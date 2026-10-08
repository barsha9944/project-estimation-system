package com.projectestimation.backend.opportunity.model;

import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "project_teams")
public class ProjectTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id", nullable = false, unique = true)
    private Opportunity opportunity;

    @Column(nullable = false)
    private String projectManager;

    @Column(nullable = false)
    private String teamLead;

    // Multiple developers
    @ElementCollection
    @CollectionTable(
            name = "project_team_developers",
            joinColumns = @JoinColumn(name = "team_id")
    )
    @Column(name = "developer_name")
    private List<String> developers;

    @Column(nullable = false)
    private String tester;

    // Only ONE database developer
    @Column(nullable = false)
    private String databaseDevelopers;

    @Column(nullable = false)
    private String admin;

    @Column(nullable = false)
    private String hr;

    public ProjectTeam() {
    }

    public Long getId() {
        return id;
    }

    public Opportunity getOpportunity() {
        return opportunity;
    }

    public String getProjectManager() {
        return projectManager;
    }

    public String getTeamLead() {
        return teamLead;
    }

    public List<String> getDevelopers() {
        return developers;
    }

    public String getTester() {
        return tester;
    }

    public String getDatabaseDevelopers() {
        return databaseDevelopers;
    }

    public String getAdmin() {
        return admin;
    }

    public String getHr() {
        return hr;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOpportunity(Opportunity opportunity) {
        this.opportunity = opportunity;
    }

    public void setProjectManager(String projectManager) {
        this.projectManager = projectManager;
    }

    public void setTeamLead(String teamLead) {
        this.teamLead = teamLead;
    }

    public void setDevelopers(List<String> developers) {
        this.developers = developers;
    }

    public void setTester(String tester) {
        this.tester = tester;
    }

    public void setDatabaseDevelopers(String databaseDevelopers) {
        this.databaseDevelopers = databaseDevelopers;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }

    public void setHr(String hr) {
        this.hr = hr;
    }
}