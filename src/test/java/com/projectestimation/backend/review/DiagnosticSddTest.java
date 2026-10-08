package com.projectestimation.backend.review;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.sdd.model.Sdd;
import com.projectestimation.backend.sdd.repository.SddRepository;
import com.projectestimation.backend.srs.model.Srs;
import com.projectestimation.backend.srs.repository.SrsRepository;

@SpringBootTest
@ActiveProfiles("dev")
public class DiagnosticSddTest {

    @Autowired
    private SddRepository sddRepository;

    @Autowired
    private SrsRepository srsRepository;

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Test
    @Transactional
    void inspectDatabaseRecords() {
        System.out.println("========== DATABASE INSPECTION START ==========");

        List<Opportunity> opportunities = opportunityRepository.findAll();
        System.out.println("Total Opportunities: " + opportunities.size());
        for (Opportunity opp : opportunities) {
            System.out.println("Opportunity ID: " + opp.getId() + " | Name: " + opp.getOpportunityName());
        }

        System.out.println("\n--- SRS RECORDS ---");
        List<Srs> srsList = srsRepository.findAll();
        for (Srs s : srsList) {
            System.out.println("SRS ID: " + s.getId() + " | Opp ID: " + s.getOpportunity().getId() + " | Opp Name: " + s.getOpportunity().getOpportunityName());
            String srsData = s.getSrsData();
            System.out.println("  SRS length: " + (srsData != null ? srsData.length() : 0));
            if (srsData != null && srsData.length() > 0) {
                System.out.println("  SRS Snippet: " + srsData.substring(0, Math.min(300, srsData.length())));
                System.out.println("  Contains 'Netzr': " + srsData.contains("Netzr"));
                System.out.println("  Contains 'Sealed Bid': " + srsData.contains("Sealed Bid"));
            }
        }

        System.out.println("\n--- SDD RECORDS ---");
        List<Sdd> sddList = sddRepository.findAll();
        for (Sdd s : sddList) {
            System.out.println("SDD ID: " + s.getId() + " | Opp ID: " + s.getOpportunity().getId() + " | Opp Name: " + s.getOpportunity().getOpportunityName());
            String sddData = s.getSddData();
            System.out.println("  SDD length: " + (sddData != null ? sddData.length() : 0));
            if (sddData != null && sddData.length() > 0) {
                System.out.println("  SDD Snippet: " + sddData.substring(0, Math.min(300, sddData.length())));
                System.out.println("  Contains 'Netzr': " + sddData.contains("Netzr"));
                System.out.println("  Contains 'Sealed Bid': " + sddData.contains("Sealed Bid"));
                System.out.println("  Contains 'Reverse Auction': " + sddData.contains("Reverse Auction"));
                System.out.println("  Contains 'Data Subject Request': " + sddData.contains("Data Subject Request"));
            }
        }

        System.out.println("========== DATABASE INSPECTION END ==========");
    }
}
