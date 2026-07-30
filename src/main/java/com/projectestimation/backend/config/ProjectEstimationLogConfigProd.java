package com.projectestimation.backend.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
public class ProjectEstimationLogConfigProd {

	@Bean
	Logger logger() {
		return LogManager.getLogger(getClass().getName());
	}
}
