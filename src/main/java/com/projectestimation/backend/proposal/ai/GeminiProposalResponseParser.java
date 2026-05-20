package com.projectestimation.backend.proposal.ai;

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

    private static final List<String> REQUIRED_SECTIONS = List.of(
            "Introduction",
            "Scope of Work",
            "Solution Architecture",
            "Technology Stack",
            "Quality Assurance",
            "Project Governance",
            "Commercials",
            "Organization Capabilities"
    );

    public AiProposalResult parse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new ProposalFailedException("Gemini returned an empty proposal response");
        }

        String markdown = normalizeMarkdown(rawResponse);
        validateMarkdown(markdown);
        return new AiProposalResult(markdown);
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

    private void validateMarkdown(String markdown) {
        if (!markdown.contains("#")) {
            throw new ProposalFailedException("Gemini response is not valid Markdown: missing headings");
        }

        for (String section : REQUIRED_SECTIONS) {
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
