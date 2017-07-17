package com.sk.babylon.controller;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sk.babylon.scrapper.NhsChoiceScrapper;
import com.sk.babylon.service.NhsService;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class NhsController {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private NhsChoiceScrapper scapper;

    @Autowired
    private NhsService nhsService;

    @Value("${nhs.data.file}")
    private String nhsDataFilePath;

    @Value("${nhs.data.stopwords}")
    private String nhsStopWordsFilePath;

    @PostConstruct
    public void initNhsController() throws IOException {
        final Resource resource = context.getResource(nhsDataFilePath);
        if (!resource.exists()) {
            log.error(
                    "Data file doesn't exist. Run the application with -Dspring.profiles.active=data-refresh parameter");
            System.exit(0);
        }
    }

    @RequestMapping("/choice")
    public JSONObject getDataForChoice(final String query) {
        return null;
    }
}
