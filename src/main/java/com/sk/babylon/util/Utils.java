package com.sk.babylon.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Utils {

    private Utils() {
        // Hidden constructor
    }

    public static String regularizeHttpInUrl(final String url, final String baseUrl) throws UnsupportedEncodingException {
        String regularizedUrl = null;
        if (!StringUtils.startsWith(url, Constants.HTTP_TAG)) {
            regularizedUrl = new StringBuilder(Constants.HTTP_TAG).append(getHostUrl(baseUrl)).append(url).toString();
        } else {
            regularizedUrl = url;
        }
        // URLEncoder.encode(StringUtils.defaultString(regularizedUrl), StandardCharsets.UTF_8.name());
        return StringUtils.defaultString(regularizedUrl);
    }

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
