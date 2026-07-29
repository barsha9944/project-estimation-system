package com.projectestimation.backend.common.ai.gateway;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.projectestimation.backend.common.exception.AiGenerationFailedException;

@Service
public class AiGateway {

    private final List<AiProvider> providers;

    public AiGateway(List<AiProvider> providers) {
        this.providers = providers.stream()
                .sorted(Comparator.comparingInt(AiProvider::getPriority))
                .toList();
    }

    public String generateContent(
            String prompt,
            String responseMimeType,
            int maxOutputTokens) {

        AiGenerationFailedException lastException = null;

        for (AiProvider provider : providers) {

            try {

                System.out.println("Trying provider : "
                        + provider.getName());

                return provider.generateContent(
                        prompt,
                        responseMimeType,
                        maxOutputTokens
                );

            } catch (AiGenerationFailedException ex) {

            	System.out.println("Provider failed : " + provider.getName());
            	System.out.println("Status : " + ex.getStatusCode());
            	System.out.println("Message : " + ex.getMessage());
                lastException = ex;

                if (shouldTryNextProvider(ex)) {

                	System.out.println(
                	        "Provider " + provider.getName()
                	        + " failed. Trying next provider..."
                	);

                	System.out.println("Trying next provider...");
                    continue;
                }

                throw ex;
            }
        }

        throw lastException;
    }

    public String generateJsonContent(
            String prompt,
            int maxOutputTokens) {

        return generateContent(
                prompt,
                "application/json",
                maxOutputTokens
        );
    }

    public String generateJsonContent(
            String prompt) {

        return generateJsonContent(
                prompt,
                2048
        );
    }

    private boolean shouldTryNextProvider(
            AiGenerationFailedException ex) {

        Integer status = ex.getStatusCode();

        if (status == null) {
            return false;
        }

        return status == 404
        		|| status == 408
                || status == 429
                || status == 500
                || status == 502
                || status == 503
                || status == 504;
    }
    
    public String generateContentWithImages(
            String prompt,
            List<Path> imagePaths,
            String responseMimeType,
            int maxOutputTokens) {

    	AiGenerationFailedException lastException = null;

    	for (AiProvider provider : providers) {

    	    try {

    	        System.out.println("Trying provider : "
    	                + provider.getName());

    	        return provider.generateContentWithImages(
    	                prompt,
    	                imagePaths,
    	                responseMimeType,
    	                maxOutputTokens
    	        );

    	    } catch (AiGenerationFailedException ex) {
    	    	
    	    	System.out.println("Provider failed : " + provider.getName());
    	    	System.out.println("Status : " + ex.getStatusCode());
    	    	System.out.println("Message : " + ex.getMessage());

    	        lastException = ex;

    	        if (shouldTryNextProvider(ex)) {

    	        	System.out.println(
    	        	        "Provider " + provider.getName()
    	        	        + " failed. Trying next provider..."
    	        	);
    	        	
    	        	System.out.println("Trying next provider...");

    	            continue;
    	        }

    	        throw ex;
    	    }
    	}

    	throw lastException;
    }
}