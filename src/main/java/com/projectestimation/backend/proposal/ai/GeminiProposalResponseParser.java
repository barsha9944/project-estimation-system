package com.projectestimation.backend.proposal.ai;

import com.projectestimation.backend.common.enums.ProposalType;
import com.projectestimation.backend.common.exception.ProposalFailedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class GeminiProposalResponseParser {

    private static final Pattern MARKDOWN_FENCE = Pattern.compile(
            "^```(?:markdown)?\\s*\\n?([\\s\\S]*?)\\n?```\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private List<String> requiredSections(
            ProposalType type
    ) {

        return switch (type) {

            case BASIC -> List.of(
                    "Introduction",
                    "Scope",
                    "Solution Architecture",
                    "Technology Stack",
                    "Commercials",
                    "Organization Capabilities"
            );

            case INTERMEDIATE -> List.of(
                    "Introduction",
                    "Scope",
                    "Solution Architecture",
                    "Technology Stack",
                    "Quality Assurance",
                    "Commercials",
                    "Organization Capabilities"
            );

            case EXPERT -> List.of(
                    "Introduction",
                    "Scope",
                    "Solution Architecture",
                    "Important Process Flows",
                    "Assumptions",
                    "Accountability Distributions",
                    "Technology Stack",
                    "Quality Assurance",
                    "Commercials",
                    "Organization Capabilities"
            );
        };
    }

    public AiProposalResult parse(String rawResponse, ProposalType proposalType) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new ProposalFailedException("Gemini returned an empty proposal response");
        }

        String markdown = normalizeMarkdown(rawResponse);
        validateMarkdown(
                markdown,
                proposalType
        );
        return new AiProposalResult(
                markdown,
                null,
                null
        );
    }

    private String normalizeMarkdown(String rawResponse) {
        String trimmed = rawResponse.trim();

        var fenceMatcher = MARKDOWN_FENCE.matcher(trimmed);
        if (fenceMatcher.matches()) {
            trimmed = fenceMatcher.group(1).trim();
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:markdown)?\\s*\\n?", "");
            trimmed = trimmed.replaceFirst("\\n?```\\s*$", "");
        }

        return trimmed.trim();
    }

    private void validateMarkdown(
            String markdown,
            ProposalType proposalType
    ) {
        if (!markdown.contains("#")) {
            throw new ProposalFailedException("Gemini response is not valid Markdown: missing headings");
        }

        for (
        	    String section :
        	    requiredSections(proposalType)
        	) {
            if (!containsSection(markdown, section)) {
                throw new ProposalFailedException("Gemini Markdown response is missing required section: " + section);
            }
        }

        if (!containsTable(markdown)) {
            throw new ProposalFailedException("Gemini Markdown response must include at least one Markdown table");
        }
    }

    private boolean containsSection(String markdown, String sectionName) {
        return markdown.toLowerCase().contains(sectionName.toLowerCase());
    }

    private boolean containsTable(String markdown) {
        return markdown.lines().anyMatch(line -> line.trim().startsWith("|") && line.trim().contains("|"));
    }
}
