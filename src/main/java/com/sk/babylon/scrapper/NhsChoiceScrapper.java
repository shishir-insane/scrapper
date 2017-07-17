package com.sk.babylon.scrapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sk.babylon.log.Profiled;
import com.sk.babylon.util.Constants;
import com.sk.babylon.util.Utils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class NhsChoiceScrapper {

    @Value("${nhs.index}")
    private String nhsIndexURL;

    @Value("${nhs.data.file}")
    private String nhsDataFilePath;

    @Value("${nhs.data.refresh}")
    private String nhsDataRefresh;

    @Value("${nhs.data.stopwords}")
    private String nhsStopWordsFilePath;

    @Autowired
    private ApplicationContext context;

    @PostConstruct
    @Profiled
    public void startScrapping() throws IOException, JSONException {
        final Resource resource = context.getResource(nhsDataFilePath);
        if (StringUtils.equalsIgnoreCase(nhsDataRefresh, "true") || !resource.exists()) {
            final JSONObject result = doScraping();
            log.info(result.toString());
            if (null != result) {
                if (!resource.exists()) {
                    final String dataFileName = StringUtils.substringAfterLast(nhsDataFilePath, "/");
                    final File dataFile = new File(context.getResource(nhsStopWordsFilePath).getFile().getParentFile(),
                            dataFileName);
                    dataFile.createNewFile();
                }
                try (BufferedWriter writer = Files.newBufferedWriter(resource.getFile().toPath())) {
                    writer.write(result.toString());
                } catch (final Exception e) {
                    log.error("Error in populating data file", e);
                }
            }
        }
    }

    public JSONObject doScraping() throws IOException, JSONException {
        final JSONObject resultData = new JSONObject();
        for (char c = 'A'; c <= 'Z'; c++) {
            doScrappingOnIndexPage(String.valueOf(c), resultData);
        }
        doScrappingOnIndexPage(Constants.DIGITS_INDEX_TEXT, resultData);
        return resultData;
    }

    private void doScrappingOnIndexPage(final String index, final JSONObject result) throws IOException, JSONException {
        final Document document = getDocumentNodeFromUrl(
                new StringBuilder(nhsIndexURL).append(Constants.URL_INDEX_PARAM).append(index).toString());
        if (null != document) {
            final Elements pageConditions = document.select(Constants.HREF_ELEMENT_SELECTOR);
            for (final Element condition : pageConditions) {
                final String conditionName = condition.text().replaceAll(Constants.NO_BREAK_SPACE, StringUtils.EMPTY);
                final String conditionUrl = condition.attr(Constants.HREAF_TAG);
                if (conditionUrl.toLowerCase().contains(Constants.CONDITION_URL_PART)) {
                    log.info("Scrapping: {} - {}", index, conditionName);
                    result.put(conditionName, doScrappingOnConditionPage(conditionUrl));
                }
            }
        }
    }

    private JSONObject doScrappingOnConditionPage(final String conditionPageUrl) throws IOException, JSONException {
        String title = null;
        JSONObject result = null;
        final Document document = getDocumentNodeFromUrl(Utils.regularizeHttpInUrl(conditionPageUrl, nhsIndexURL));
        if (null != document) {
            result = new JSONObject();
            if (!CollectionUtils.isEmpty(document.select(Constants.HEADING1_TAG))) {
                title = StringUtils.defaultString(document.select(Constants.HEADING1_TAG).first().text());
                log.info("-- Condition Scrapping: {}", title.replaceAll(Constants.NO_BREAK_SPACE, StringUtils.EMPTY));
                result.put(Constants.TITLE_DATA_FIELD, title.replaceAll(Constants.NO_BREAK_SPACE, StringUtils.EMPTY));
            }
            result.put(Constants.INRO_DATA_FIELD, doScrappingOnSubSectionPage(conditionPageUrl, document));
            final Elements furtherSections = document.select(Constants.HREF_SUBELEMENT_SELECTOR);
            if (!CollectionUtils.isEmpty(furtherSections)) {
                for (final Element section : furtherSections) {
                    final String sectionName = section.ownText();
                    log.info("-- Condition Scrapping: {}", sectionName);
                    result.put(sectionName, doScrappingOnSubSectionPage(conditionPageUrl, getDocumentNodeFromUrl(
                            Utils.regularizeHttpInUrl(section.attr(Constants.HREAF_TAG), nhsIndexURL))));
                }
            }
        }
        return result;
    }

    private JSONObject doScrappingOnSubSectionPage(final String url, final Document document) throws JSONException {
        JSONObject subSections = null;
        if (null != document) {
            final Element content = document.select(Constants.DOCUMENT_CONTENT_SELECTOR).first();
            if (null != content) {
                final Elements textElements = content.select(Constants.ELEMENT_HTML_SELECTOR);
                subSections = new JSONObject();
                log.info("---- Sub-section Scrapping: {}", url);
                subSections.put(Constants.URL_FIELD, url);
                if (!CollectionUtils.isEmpty(content.select(Constants.HEADING2_TAG))) {
                    String currentSection = StringUtils
                            .defaultString(document.select(Constants.HEADING2_TAG).first().text());
                    StringBuilder currentText = new StringBuilder();
                    for (final Element textElement : textElements) {
                        if (textElement.tagName().equals(Constants.HEADING3_TAG)
                                || textElement.tagName().equals(Constants.HEADING2_TAG)) {
                            log.info("---- Sub-section Scrapping: {}", currentSection);
                            subSections.put(currentSection, currentText.toString());
                            currentSection = textElement.text();
                            currentText = new StringBuilder();
                        } else {
                            currentText.append(textElement.text() + StringUtils.SPACE);
                        }
                    }
                    log.info("---- Sub-section Scrapping: {}", currentSection);
                    subSections.put(currentSection, currentText.toString());
                }
            }
        }
        return subSections;
    }

    private Document getDocumentNodeFromUrl(final String url) {
        Document document = null;
        if (StringUtils.isNotBlank(url)) {
            try {
                document = Jsoup.connect(Utils.regularizeHttpInUrl(url, nhsIndexURL)).followRedirects(true).get();
            } catch (final IOException e) {
                log.error("Error while fetching content for {}", url, e);
            }
        }
        return document;
    }
}
