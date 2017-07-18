package com.sk.babylon.scrapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.sk.babylon.log.Profiled;
import com.sk.babylon.util.Constants;

import lombok.extern.slf4j.Slf4j;

/**
 * The Class NhsChoiceScrapper.
 */
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

    @Value("${nhs.data.thread.count}")
    private int numberOfThreads;

    @Autowired
    private ApplicationContext context;

    /**
     * Start scrapping.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws JSONException
     *             the JSON exception
     */
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

    /**
     * Do scraping.
     *
     * @return the JSON object
     */
    @SuppressWarnings("unchecked")
    public JSONObject doScraping() {
        final JSONObject resultData = new JSONObject();
        final ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        final List<NhsChoiceWorker> tasks = new ArrayList<>(Constants.TOTAL_INDEX_PAGES);
        try {
            for (char c = 'A'; c <= 'Z'; c++) {
                tasks.add(new NhsChoiceWorker(String.valueOf(c), nhsIndexURL));
            }
            tasks.add(new NhsChoiceWorker(Constants.DIGITS_INDEX_TEXT, nhsIndexURL));

            final List<Future<JSONObject>> futures = executorService.invokeAll(tasks);
            for (final Future<JSONObject> future : futures) {
                resultData.putAll(future.get(10, TimeUnit.MINUTES));
            }
            executorService.shutdown();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Error while scrapping", e);
        }

        return resultData;
    }

}
