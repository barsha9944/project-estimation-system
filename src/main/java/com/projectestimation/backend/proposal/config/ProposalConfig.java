package com.projectestimation.backend.proposal.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PandocProperties.class)
public class ProposalConfig {
}
