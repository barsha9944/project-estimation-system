package com.projectestimation.backend.estimation.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class EstimationHtmlParser {

    public String extractActorTable(
            String html
    ) {

        Document doc = Jsoup.parse(html);

        return doc.select("table")
                  .get(0)
                  .outerHtml();
    }

    public String extractUseCaseTable(
            String html
    ) {

        Document doc = Jsoup.parse(html);

        return doc.select("table")
                  .get(1)
                  .outerHtml();
    }
}