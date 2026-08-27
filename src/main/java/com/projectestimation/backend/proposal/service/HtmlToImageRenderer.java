package com.projectestimation.backend.proposal.service;

import com.microsoft.playwright.*;
import com.projectestimation.backend.common.exception.ProposalFailedException;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class HtmlToImageRenderer {

    public void renderHtmlToImage(
        String html,
        Path outputFile
) {

    try (
            Playwright playwright =
                    Playwright.create()
    ) {

        Browser browser =
                playwright.chromium()
                        .launch(
                                new BrowserType.LaunchOptions()
                                        .setHeadless(true)
                        );

        Page page =
                browser.newPage();

        page.setViewportSize(
                3000,
                1800
        );

        page.setContent(html);

        page.waitForTimeout(2000);

        Locator locator =
                page.locator(
                        "#diagram-container"
                );

        locator.screenshot(
                new Locator.ScreenshotOptions()
                        .setPath(outputFile)
        );

        browser.close();

    } catch (Exception ex) {
ex.printStackTrace();
        throw new ProposalFailedException(
                "Failed to render HTML to image",
                ex
        );
    }
}
}
