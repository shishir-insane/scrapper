package com.sk.babylon.scrapper;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sk.babylon.util.Constants;
import com.sk.babylon.util.Utils;

import lombok.extern.slf4j.Slf4j;

/**
 * The Class NhsChoiceWorker.
 */
@Component
@Slf4j
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NhsChoiceWorker implements Callable<JSONObject> {

    private final String nhsIndexURL;
    private final String index;

    /**
     * Instantiates a new nhs choice worker.
     *
     * @param index
     *            the index
     * @param nhsIndexURL
     *            the nhs index URL
     */
    @Autowired
    public NhsChoiceWorker(final String index, final String nhsIndexURL) {
        log.info(nhsIndexURL);
        log.info(index);
        this.index = index;
        this.nhsIndexURL = nhsIndexURL;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.Callable#call()
     */
    @Override
    public JSONObject call() throws Exception {
        return doScrappingOnIndexPage(index);
    }

    /**
     * Do scrapping on index page.
     *
     * @param index
     *            the index
     * @return the JSON object
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws JSONException
     *             the JSON exception
     */
    @SuppressWarnings("unchecked")
    private JSONObject doScrappingOnIndexPage(final String index) throws IOException, JSONException {
        final JSONObject result = new JSONObject();
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
        return result;
    }

    /**
     * Do scrapping on condition page.
     *
     * @param conditionPageUrl
     *            the condition page url
     * @return the JSON object
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws JSONException
     *             the JSON exception
     */
    @SuppressWarnings("unchecked")
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

    /**
     * Do scrapping on sub section page.
     *
     * @param url
     *            the url
     * @param document
     *            the document
     * @return the JSON object
     * @throws JSONException
     *             the JSON exception
     */
    @SuppressWarnings("unchecked")
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

    /**
     * Gets the document node from url.
     *
     * @param url
     *            the url
     * @return the document node from url
     */
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
