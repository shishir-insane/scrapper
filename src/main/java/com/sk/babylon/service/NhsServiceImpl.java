package com.sk.babylon.service;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.sk.babylon.stemmer.Stemmer;

import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.PTBTokenizer;

/**
 * The Class NhsServiceImpl.
 */
@Service
public class NhsServiceImpl implements NhsService {

    @Value("${nhs.data.file}")
    private String nhsDataFilePath;

    @Value("${nhs.data.stopwords}")
    private String nhsStopWordsFilePath;

    @Autowired
    private ApplicationContext context;

    private JSONObject nhsData;
    private Set<String> stopwords;

    /**
     * Inits the.
     *
     * @throws FileNotFoundException
     *             the file not found exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws ParseException
     *             the parse exception
     */
    @PostConstruct
    public void init() throws FileNotFoundException, IOException, ParseException {
        final JSONParser parser = new JSONParser();
        final Resource dataFileResource = context.getResource(nhsDataFilePath);
        final Resource stopWordFileResource = context.getResource(nhsStopWordsFilePath);
        final Object obj = parser.parse(new FileReader(dataFileResource.getFile()));
        nhsData = (JSONObject) obj;

        stopwords = new HashSet<>();
        try (BufferedReader in = new BufferedReader(new FileReader(stopWordFileResource.getFile()))) {
            String word;
            while ((word = in.readLine()) != null) {
                stopwords.add(word);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sk.babylon.service.NhsService#getDataForChoice(java.lang.String)
     */
    @Override
    public JSONObject getDataForChoice(final String query) {
        final JSONObject result = new JSONObject();
        final Set<String> bagOfWords = extractNounsFromQuery(query);
        final Object response = searchKeywordsInDataCache(nhsData, bagOfWords, 0);
        result.put("query", query);
        result.put("response", response);
        return result;
    }

    /**
     * Extract nouns from query.
     *
     * @param query
     *            the query
     * @return the sets the
     */
    private Set<String> extractNounsFromQuery(final String query) {
        final Iterator<Word> words = PTBTokenizer.newPTBTokenizer(new StringReader(query.toLowerCase()));
        final Stemmer stemmer = new Stemmer();
        final HashSet<String> bagOfWords = new HashSet<>();
        boolean insideBrackets = false;
        while (words.hasNext()) {
            final Word token = words.next();
            if (token.word().equals("-LRB-")) {
                insideBrackets = true;
            } else if (token.word().equals("-RRB-")) {
                insideBrackets = false;
            }
            if (!stopwords.contains(token.word()) && token.word().length() >= 2 && !insideBrackets) {
                bagOfWords.add(stemmer.stem(token.word()));
            }
        }
        return bagOfWords;
    }

    /**
     * Search keywords in data cache.
     *
     * @param data
     *            the data
     * @param keywords
     *            the keywords
     * @param depth
     *            the depth
     * @return the object
     */
    private Object searchKeywordsInDataCache(final JSONObject data, final Set<String> keywords, final int depth) {
        if (depth == 2) {
            return data;
        }
        final JSONObject jsonData = data;
        int maxWords = 0;
        JSONObject bestSubtree = null;
        final List<String> keysSortedByLength = new ArrayList<>();
        for (final Object key : jsonData.keySet()) {
            keysSortedByLength.add((String) key);
        }
        Collections.sort(keysSortedByLength, (s1, s2) -> s1.length() - s2.length());
        for (final String key : keysSortedByLength) {
            final Set<String> keyPreprocessed = extractNounsFromQuery(key);
            keyPreprocessed.retainAll(keywords);
            if (keyPreprocessed.size() > maxWords) {
                maxWords = keyPreprocessed.size();
                bestSubtree = (JSONObject) jsonData.get(key);
            }
        }
        if (maxWords > 0) {
            return searchKeywordsInDataCache(bestSubtree, keywords, depth + 1);
        } else {
            if (depth == 0) {
                return null;
            } else {
                return data;
            }
        }
    }

}
