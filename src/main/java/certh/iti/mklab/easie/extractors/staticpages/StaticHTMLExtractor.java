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

import certh.iti.mklab.easie.FIELD_TYPE;
import certh.iti.mklab.easie.extractors.FieldExtractor;
import certh.iti.mklab.easie.configuration.Configuration.ScrapableField;
import certh.iti.mklab.easie.extractors.AbstractHTMLExtractor;
import certh.iti.mklab.easie.exception.HTMLElementNotFoundException;
import certh.iti.mklab.easie.extractors.TableFieldExtractor;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;
import org.jsoup.nodes.Element;

/**
 * StaticHTMLExtractor Object extends AbstractHTMLExtractor and is responsible
 * for extracting data from defined fields (by css selectors) from a static
 * webpage or fields from a table.
 *
 * @author vasgat
 */
public class StaticHTMLExtractor implements AbstractHTMLExtractor {

    public String base_url;
    public String relative_url;
    private String source;
    public StaticHTMLFetcher fetcher;

    /**
     * Creates a new StaticHTMLWrapper for a webpage
     *
     * @param base_url:     webpage base url
     * @param relative_url: path to the specific spot in the page
     * @throws URISyntaxException
     * @throws IOException
     */
    public StaticHTMLExtractor(String base_url, String relative_url) throws URISyntaxException, IOException, NoSuchAlgorithmException, KeyManagementException {
        this.base_url = base_url;
        this.source = base_url + relative_url;
        this.fetcher = new StaticHTMLFetcher(base_url, relative_url);
    }

    /**
     * Creates a new StaticHTMLWrapper for a webpage
     *
     * @param FullLink webpage full url
     * @throws URISyntaxException
     * @throws IOException
     */
    public StaticHTMLExtractor(String FullLink) throws URISyntaxException, IOException, NoSuchAlgorithmException, KeyManagementException {
        this.source = FullLink;
        this.fetcher = new StaticHTMLFetcher(FullLink);
    }

    /**
     * extracts data from a list of specified fields from a webpage
     *
     * @param sfields: list of fields
     * @return a HashMap of the extracted fields
     */
    @Override
    public List<Document> extractFields(List<ScrapableField> sfields) {
        FieldExtractor extractor = new FieldExtractor((Element) fetcher.getHTMLDocument(), source);
        return extractor.getExtractedFields(sfields, FIELD_TYPE.METRIC);
    }

    /**
     * extracts data from the specified table fields
     *
     * @param table_selector: CSS table selector
     * @param fields:         list of table fields
     * @return an ArrayList of HashMap (corresponds to the extracted table
     * fields)
     */
    @Override
    public List<Document> extractTable(String table_selector, List<ScrapableField> fields) {
        TableFieldExtractor extractor = new TableFieldExtractor((Element) fetcher.getHTMLDocument(), table_selector, source);
        try {
            return extractor.getExtractedFields(fields, FIELD_TYPE.METRIC);
        } catch (HTMLElementNotFoundException ex) {
            System.out.println(ex.getMessage());
            return new ArrayList<Document>();
        }
    }

    @Override
    public Pair extractFields(List<ScrapableField> cfields, List<ScrapableField> sfields) {
        FieldExtractor extractor = new FieldExtractor((Element) fetcher.getHTMLDocument(), source);
        return Pair.of(
                extractor.getExtractedFields(cfields, FIELD_TYPE.COMPANY_INFO),
                extractor.getExtractedFields(sfields, FIELD_TYPE.METRIC)
        );
    }

    @Override
    public Pair extractTable(String table_selector, List<ScrapableField> cfields, List<ScrapableField> sfields) {
        TableFieldExtractor extractor = new TableFieldExtractor((Element) fetcher.getHTMLDocument(), table_selector, source);
        List extracted_company_info = new ArrayList();
        List extracted_metric_info = new ArrayList();
        try {
            extracted_company_info = extractor.getExtractedFields(cfields, FIELD_TYPE.COMPANY_INFO);
            extracted_metric_info = extractor.getExtractedFields(sfields, FIELD_TYPE.METRIC);
        } catch (HTMLElementNotFoundException ex) {
            System.out.println(ex.getMessage());
        }

        return Pair.of(
                extracted_company_info,
                extracted_metric_info
        );
    }
}
