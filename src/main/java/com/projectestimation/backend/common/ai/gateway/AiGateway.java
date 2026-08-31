package com.projectestimation.backend.common.ai.gateway;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.projectestimation.backend.common.exception.AiGenerationFailedException;
import com.projectestimation.backend.opportunity.dto.GeminiFile;

@Service
public class AiGateway {
	private static final Logger log = LogManager.getLogger(AiGateway.class);
	private final List<AiProvider> providers;

	public AiGateway(List<AiProvider> providers) {
		this.providers = providers.stream().sorted(Comparator.comparingInt(AiProvider::getPriority)).toList();
	}

	public String generateContent(String prompt, String responseMimeType, int maxOutputTokens) {

		AiGenerationFailedException lastException = null;

		for (AiProvider provider : providers) {

			try {

				log.info("Trying provider : " + provider.getName());

				return provider.generateContent(prompt, responseMimeType, maxOutputTokens);

			} catch (AiGenerationFailedException ex) {

				log.info("Provider failed : " + provider.getName());
				log.info("Status : " + ex.getStatusCode());
				log.info("Message : " + ex.getMessage());
				lastException = ex;

				if (shouldTryNextProvider(ex)) {

					log.info("Provider " + provider.getName() + " failed. Trying next provider...");

					log.info("Trying next provider...");
					continue;
				}

				throw ex;
			}
		}

		throw lastException;
	}

	public String generateJsonContent(String prompt, int maxOutputTokens) {

		return generateContent(prompt, "application/json", maxOutputTokens);
	}

	public String generateJsonContent(String prompt) {

		return generateJsonContent(prompt, 2048);
	}

	private boolean shouldTryNextProvider(AiGenerationFailedException ex) {

		Integer status = ex.getStatusCode();

		if (status == null) {
			return false;
		}

		return status == 404 || status == 408 || status == 429 || status == 500 || status == 502 || status == 503
				|| status == 504;
	}

	public String generateContentWithImages(String prompt, List<Path> imagePaths, String responseMimeType,
			int maxOutputTokens) {

		AiGenerationFailedException lastException = null;

		for (AiProvider provider : providers) {

			try {

				log.info("Trying provider : " + provider.getName());

				return provider.generateContentWithImages(prompt, imagePaths, responseMimeType, maxOutputTokens);

			} catch (AiGenerationFailedException ex) {

				log.info("Provider failed : " + provider.getName());
				log.info("Status : " + ex.getStatusCode());
				log.info("Message : " + ex.getMessage());

				lastException = ex;

				if (shouldTryNextProvider(ex)) {

					log.info("Provider " + provider.getName() + " failed. Trying next provider...");

					log.info("Trying next provider...");

					continue;
				}

				throw ex;
			}
		}

		throw lastException;
	}

	public String generateRequirmentSunnary(String fileReaderPrompt, GeminiFile fileUri, int proposalMaxOutputTokens) {
		AiGenerationFailedException lastException = null;

		for (AiProvider provider : providers) {

			try {

				log.info("Trying provider : " + provider.getName());

				return provider.generateContentFromFile(fileReaderPrompt, fileUri, proposalMaxOutputTokens);

			} catch (AiGenerationFailedException ex) {

				log.info("Provider failed : " + provider.getName());
				log.info("Status : " + ex.getStatusCode());
				log.info("Message : " + ex.getMessage());
				lastException = ex;

				if (shouldTryNextProvider(ex)) {

					log.info("Provider " + provider.getName() + " failed. Trying next provider...");

					log.info("Trying next provider...");
					continue;
				}

				throw ex;
			}
		}

		throw lastException;
	}
}