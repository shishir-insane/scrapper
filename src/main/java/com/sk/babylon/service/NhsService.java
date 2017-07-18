package com.sk.babylon.service;

import org.json.simple.JSONObject;

/**
 * The Interface NhsService.
 */
public interface NhsService {

    /**
     * Gets the data for choice.
     *
     * @param query
     *            the query
     * @return the data for choice
     */
    JSONObject getDataForChoice(String query);

}
