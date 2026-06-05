package com.projectestimation.backend.proposal.ai;

import java.util.List;

public record AiProposalResult(
        String markdownContent,
        String architectureHtml,
        List<String> processFlowHtmls
) {
}
