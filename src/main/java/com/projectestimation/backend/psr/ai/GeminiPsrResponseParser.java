package com.projectestimation.backend.psr.ai;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.projectestimation.backend.common.exception.ProposalFailedException;

@Component
public class GeminiPsrResponseParser {

    private static final Pattern MARKDOWN_FENCE =
            Pattern.compile(
                    "^```(?:markdown)?\\s*\\n?([\\s\\S]*?)\\n?```\\s*$",
                    Pattern.CASE_INSENSITIVE
            );

    public String parse(String rawResponse) {

        if (rawResponse == null || rawResponse.isBlank()) {
            throw new ProposalFailedException(
                    "Gemini returned an empty PSR response"
            );
        }

        String markdown = rawResponse.trim();

        var matcher = MARKDOWN_FENCE.matcher(markdown);

        if (matcher.matches()) {
            markdown = matcher.group(1).trim();
        }

        if (!markdown.contains("#")) {
            throw new ProposalFailedException(
                    "Gemini PSR response is not valid Markdown"
            );
        }

        return markdown;
    }
}