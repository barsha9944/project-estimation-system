package com.projectestimation.backend.proposal.ai;

import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class ProposalStaticContentProvider {

    public String load(String fileName) {

        try (
                InputStream is =
                        getClass()
                                .getClassLoader()
                                .getResourceAsStream(
                                        "proposal/static-content/" + fileName
                                )
        ) {

            if (is == null) {
                return "";
            }

            return new String(
                    is.readAllBytes(),
                    StandardCharsets.UTF_8
            );

        } catch (Exception ex) {

            return "";
        }
    }
}