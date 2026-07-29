package com.projectestimation.backend.constant;

import org.springframework.context.annotation.Profile;

@Profile("dev")
public class ProjectEstimationConstantDev {

	public static final String APP_SECURITY_JWT_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
	public static final String APP_SECURITY_JWT_EXPIRATION_SECONDS = "86400";
}
