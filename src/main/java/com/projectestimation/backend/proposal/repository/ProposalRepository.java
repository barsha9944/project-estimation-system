package com.projectestimation.backend.proposal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.projectestimation.backend.proposal.dto.ProposalDetailWithCountDto;
import com.projectestimation.backend.proposal.model.Proposal;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

	@Query("SELECT p FROM Proposal p WHERE p.opportunity.id = :opportunityId ORDER BY p.version DESC")
	List<Proposal> findByOpportunityId(@Param("opportunityId") Long opportunityId);

	Optional<Proposal> findFirstByOpportunity_IdOrderByVersionDesc(Long opportunityId);

//    @Query( "SELECT p.id AS proposal_id, p.opportunity_id,p.title,p.proposal_type,COUNT(*) OVER (PARTITION BY p.opportunity_id) AS total_proposals_for_opportunity SFROM proposals p ORDER BY p.opportunity_id, p.id")
	List<Proposal> findAllByOrderByCreatedAtDesc();

//	@Query("""
//			    SELECT new com.projectestimation.backend.proposal.dto.ProposalDetailWithCountDto(
//			        p.id,
//			        p.opportunity.id,
//			        p.proposalType,
//			        p.createdAt,
//			        COUNT(p2)
//			    )
//			    FROM Proposal p
//			    JOIN Proposal p2
//			        ON p.opportunity.id = p2.opportunity.id
//			    GROUP BY
//			        p.id,
//			        p.opportunity.id,
//			        p.proposalType,
//			        p.createdAt
//			    ORDER BY
//			        p.opportunity.id,
//			        p.createdAt DESC
//			""")
//	  @Query( "SELECT p.id AS proposal_id, p.opportunity_id,p.title,p.proposal_type,COUNT(*) OVER (PARTITION BY p.opportunity_id) AS total_proposals_for_opportunity SFROM proposals p ORDER BY p.opportunity_id, p.id")
//	List<ProposalDetailWithCountDto> getProposalDetailsWithCount();
//	@Query(value = """
//		    SELECT p.id,
//		           p.opportunity_id,
//		           p.proposal_type,
//		           p.created_at,
//		           pc.proposal_count
//		    FROM proposals p
//		    JOIN (
//		        SELECT opportunity_id,
//		               COUNT(*) AS proposal_count
//		        FROM proposals
//		        GROUP BY opportunity_id
//		        HAVING COUNT(*) >= 1
//		    ) pc
//		    ON p.opportunity_id = pc.opportunity_id
//		    """,
//		    nativeQuery = true)
//		List<Object[]> findProposalDetailsWithCount();
	
	@Query(value = """
		    SELECT
		        p.id,
		        p.opportunity_id,
		        p.proposal_type,
		        p.created_at,
		        pc.proposal_count
		    FROM proposals p
		    INNER JOIN (
		        SELECT
		            opportunity_id,
		            COUNT(*) AS proposal_count
		        FROM proposals
		        GROUP BY opportunity_id
		    ) pc
		    ON p.opportunity_id = pc.opportunity_id
		    ORDER BY p.opportunity_id, p.created_at DESC
		    """,
		    nativeQuery = true)
		List<Object[]> findProposalDetailsWithCount();
}