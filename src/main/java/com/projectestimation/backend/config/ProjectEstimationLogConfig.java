package com.projectestimation.backend.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({ "stage", "dev" })
public class ProjectEstimationLogConfig {

	@Bean
	Logger logger() {
		return LogManager.getLogger(getClass().getName());
	}
}
