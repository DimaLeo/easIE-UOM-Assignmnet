/*
 * Copyright 2016 vasgat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package certh.iti.mklab.easie.extractors.staticpages;

import certh.iti.mklab.easie.URLPatterns;
import certh.iti.mklab.easie.configuration.Configuration.ScrapableField;
import certh.iti.mklab.easie.extractors.AbstractHTMLExtractor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * PaginationItearator object extends AbstractHTMLExtractor and is responsible
 * for extracting content that is distributed to different pages
 *
 * @author vasgat
 */
public class PaginationIterator extends AbstractHTMLExtractor {

    private Pagination pagination;
    private String base_url;
    private String relative_url;
    private int numThreads;
    private String frontPattern;
    private String rearPattern;

    /**
     * Creates a new PaginationIterator
     *
     * @param next_page_selector next Page CSS selector in the page
     * @throws URISyntaxException
     * @throws IOException
     */
    public PaginationIterator(String base_url, String relative_url, String next_page_selector) throws URISyntaxException, IOException, NoSuchAlgorithmException, KeyManagementException {
        this.pagination = new Pagination(next_page_selector);
        this.base_url = base_url;
        this.relative_url = relative_url;
        this.numThreads = 20;
    }

    private List<HashMap> extractData(String frontPattern, String rearPattern, String tableSelector, List<ScrapableField> pageFields, List<ScrapableField> tableFields) throws URISyntaxException, IOException, InterruptedException, KeyManagementException, NoSuchAlgorithmException {
        ArrayList<HashMap> extractedFields = new ArrayList<>();

        if (pagination.thereisPattern(base_url, relative_url)) {
            extractedFields.addAll(pagination.multiThreadPagination(frontPattern, rearPattern, tableSelector, pageFields, tableFields));
        } else {
            extractedFields.addAll(pagination.singleThreadPagination(new SingleStaticPageExtractor(base_url + relative_url, tableSelector, pageFields, tableFields), base_url));
        }

        return extractedFields;
    }

    /**
     * Extracts data from the defined fields of each page until no next page exists
     *
     * @param fields List of fields we want to extract
     * @return the extracted data fields as a List of HashMaps
     * @throws URISyntaxException
     * @throws IOException
     * @throws Exception
     */
    @Override
    public List<HashMap> extractFields(List<ScrapableField> fields) throws URISyntaxException, IOException, InterruptedException, KeyManagementException, NoSuchAlgorithmException {
        return extractData(frontPattern, rearPattern, null, null, fields);
    }

    /**
     * Extracts data from a table from each page until no next page exists
     *
     * @param tableSelector CSS table selector
     * @param fields        List of table fields we want to extract
     * @return the extracted table fields as a List of HashMaps
     * @throws URISyntaxException
     * @throws IOException
     * @throws Exception
     */
    @Override
    public List<HashMap> extractTable(String tableSelector, List<ScrapableField> fields) throws URISyntaxException, IOException, InterruptedException, KeyManagementException, NoSuchAlgorithmException {
        return extractData(frontPattern, rearPattern, tableSelector, null, fields);
    }

    public Pair extractFieldsOrTable(String frontPattern, String rearPattern, String tableSelector, List<ScrapableField> cfields, List<ScrapableField> sfields) throws URISyntaxException, IOException, InterruptedException, KeyManagementException, NoSuchAlgorithmException {
        ArrayList<HashMap> extractedCFields = new ArrayList<>();
        ArrayList<HashMap> extractedSFields = new ArrayList<>();
        ArrayList temp;

        if (pagination.thereisPattern(base_url, relative_url)) {
            temp = (ArrayList) pagination.multiThreadPagination(frontPattern, rearPattern, tableSelector, cfields, sfields);
        } else {
            temp = pagination.singleThreadPagination(new SingleStaticPageExtractor(base_url + relative_url, tableSelector, cfields, sfields), base_url);
        }

        for (int i = 0; i < temp.size(); i++) {
            extractedCFields.addAll((ArrayList) (((Pair) temp.get(i)).getKey()));
            extractedSFields.addAll((ArrayList) (((Pair) temp.get(i)).getValue()));
        }

        return Pair.of(extractedCFields, extractedSFields);
    }

    @Override
    public Pair extractFields(List<ScrapableField> cfields, List<ScrapableField> sfields) throws URISyntaxException, IOException, InterruptedException, KeyManagementException, NoSuchAlgorithmException {
        return extractFieldsOrTable(frontPattern, rearPattern, null, cfields, sfields);
    }

    @Override
    public Pair extractTable(String tableSelector, List<ScrapableField> cfields, List<ScrapableField> sfields) throws URISyntaxException, IOException, InterruptedException, KeyManagementException, NoSuchAlgorithmException {
        return extractFieldsOrTable(frontPattern, rearPattern, tableSelector, cfields, sfields);
    }


}
