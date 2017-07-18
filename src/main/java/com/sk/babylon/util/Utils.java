package com.sk.babylon.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * The Class Utils.
 */
@Slf4j
public final class Utils {

    /**
     * Instantiates a new utils.
     */
    private Utils() {
        // Hidden constructor
    }

    /**
     * Regularize http in url.
     *
     * @param url
     *            the url
     * @param baseUrl
     *            the base url
     * @return the string
     * @throws UnsupportedEncodingException
     *             the unsupported encoding exception
     */
    public static String regularizeHttpInUrl(final String url, final String baseUrl)
            throws UnsupportedEncodingException {
        String regularizedUrl = null;
        if (!StringUtils.startsWith(url, Constants.HTTP_TAG)) {
            regularizedUrl = new StringBuilder(Constants.HTTP_TAG).append(getHostUrl(baseUrl)).append(url).toString();
        } else {
            regularizedUrl = url;
        }
        // URLEncoder.encode(StringUtils.defaultString(regularizedUrl),
        // StandardCharsets.UTF_8.name());
        return StringUtils.defaultString(regularizedUrl);
    }

    /**
     * Gets the host url.
     *
     * @param baseUrl
     *            the base url
     * @return the host url
     */
    public static String getHostUrl(final String baseUrl) {
        String hostUrl = null;
        try {
            hostUrl = new URL(baseUrl).getHost();
        } catch (final MalformedURLException e) {
            log.error("{} is invalid.");
        }
        return hostUrl;
    }
}
