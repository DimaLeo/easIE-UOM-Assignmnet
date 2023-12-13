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
package certh.iti.mklab.easie.executor.handlers;

import certh.iti.mklab.easie.companymatching.CompanyMatcher;
import com.mongodb.client.MongoCollection;

import java.io.*;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.bson.Document;
import org.bson.json.JsonWriterSettings;

/**
 * @author vasgat
 */
public class StoreUtils {

    private List<Document> extracted_company_info;
    private List<ArrayList<Document>> extracted_metrics;
    private int number_of_metrics;
    private String entity_name;

    public StoreUtils(List<Document> extracted_companies, List<ArrayList<Document>> extracted_metrics, String entity_name) {
        this.extracted_company_info = extracted_companies;
        this.extracted_metrics = extracted_metrics;
        this.entity_name = entity_name;
    }

    public void toMongoDB(MongoCollection companiesCollection, MongoCollection metricsCollection, String sourceName) throws UnknownHostException, IOException {
        for (int i = 0; i < extracted_company_info.size(); i++) {
            Document companyInfo = extracted_company_info.get(i);

            if (companyInfo.size() == 0 || !companyInfo.containsKey("company_name")) {
                continue;
            }

            processCompanyInfo(companiesCollection, metricsCollection, sourceName, companyInfo, extracted_metrics.get(i));
        }
    }

    private void processCompanyInfo(
            MongoCollection companiesCollection,
            MongoCollection metricsCollection,
            String sourceName,
            Document companyInfo,
            ArrayList<Document> metrics) throws IOException {

        CompanyMatcher companyMatcher = new CompanyMatcher(
                companiesCollection,
                (String) companyInfo.get("company_name"),
                (String) companyInfo.get("country"),
                (String) companyInfo.get("website")
        );

        Iterator<Map.Entry<String, Object>> iter = companyInfo.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Object> entry = iter.next();
            String key = entry.getKey();

            if (!key.equals("company_name") && !key.equals("country") && !key.equals("website")) {
                companyMatcher.insertInfo(key, entry.getValue().toString());
            }
        }

        for (Document metric : metrics) {
            metric.append("company_id", companyMatcher.getId()).append("source_name", sourceName);
            metricsCollection.insertOne(metric);
            metric.remove("company_id");
            metric.remove("_id");
        }
    }

    public void toMongoDB(MongoCollection metricsCollection) {
        if (extracted_company_info != null) {
            number_of_metrics = 0;

            for (int j = 0; j < extracted_company_info.size(); j++) {
                Document company = extracted_company_info.get(j);
                ArrayList<Document> tempMetrics = extracted_metrics.get(j);
                ArrayList<Document> metrics = new ArrayList<>();

                for (Document tempMetric : tempMetrics) {
                    if (!tempMetric.getString("name").equals("crawl_to")) {
                        number_of_metrics++;
                        metrics.add(tempMetric);
                    }
                }

                Document json = new Document().append("company", company).append("metrics", metrics);
                metricsCollection.insertOne(json);
            }
        } else {
            for (int j = 0; j < extracted_metrics.size(); j++) {
                ArrayList<Document> tempMetrics = extracted_metrics.get(j);

                Document json = new Document();
                for (int i = 0; i < tempMetrics.size(); i++) {
                    if (!tempMetrics.get(i).getString("name").equals("crawl_to")) {
                        number_of_metrics++;
                        json.append(tempMetrics.get(i).getString("name"), tempMetrics.get(i).getString("value"));
                    }
                }
                json.append("source", tempMetrics.get(0).getString("source"));
                json.append("citeyear", tempMetrics.get(0).getString("citeyear"));
                metricsCollection.insertOne(json);
            }
        }
    }

    public void toJSONFile(String filePath) throws IOException {
        PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(
                        new FileOutputStream(filePath), "UTF-8"));
        writer.write(exportJson());

        writer.close();
        /*PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(
                        new FileOutputStream(filePath), "UTF-8"));*/

        //writer.println(exportJson());
        //writer.close();
    }

    public void toCSVFile(String filePath, String metric_designer) throws
            FileNotFoundException, UnsupportedEncodingException, IOException {
        PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(
                        new FileOutputStream(filePath), "UTF-8"));

        writer.println(exportCSV(metric_designer));
        writer.close();
    }

    /**
     * @returns the extracted data into JSON format
     */
    public String exportJson() {
        ArrayList<Document> results = new ArrayList<>();

        if (extracted_company_info != null) {
            for (int j = 0; j < extracted_company_info.size(); j++) {
                Document company = extracted_company_info.get(j);
                ArrayList<Document> metrics = processMetrics(extracted_metrics.get(j));
                results.add(new Document(entity_name, company).append("metrics", metrics));
            }
        } else {
            for (ArrayList<Document> temp_metrics : extracted_metrics) {
                ArrayList<Document> metrics = processMetrics(temp_metrics);
                results.addAll(metrics);
            }
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        Date date = new Date(System.currentTimeMillis());

        return new Document("results", results)
                .append("timestamp", formatter.format(date))
                .toJson(JsonWriterSettings.builder().indent(true).build());
    }

    private ArrayList<Document> processMetrics(ArrayList<Document> tempMetrics) {
        ArrayList<Document> metrics = new ArrayList<>();

        for (Document tempMetric : tempMetrics) {
            if (!tempMetric.getString("name").equals("crawl_to")) {
                number_of_metrics++;
                metrics.add(Document.parse(tempMetric.toJson()));
            }
        }

        if (tempMetrics.size() > 0) {
            Document json = new Document();
            json.append("source", tempMetrics.get(0).getString("source"));
            json.append("citeyear", tempMetrics.get(0).getString("citeyear"));
            metrics.add(json);
        }

        return metrics;
    }

    public String exportCSV(String wikirate_metric_designer) {
        String csv = "";

        if (extracted_company_info != null) {

            for (int j = 0; j < extracted_company_info.size(); j++) {
                if (extracted_company_info.isEmpty()) {
                    continue;
                }

                Document company = Document.parse(extracted_company_info.get(j).toJson());

                ArrayList<Document> temp_metrics = extracted_metrics.get(j);
                for (int i = 0; i < temp_metrics.size(); i++) {
                    if (!temp_metrics.get(i).getString("name").equals("crawl_to")) {
                        Document current_metric = Document.parse(temp_metrics.get(i).toJson());
                        String metric_string = wikirate_metric_designer + "+" + current_metric.getString("name") + ",\"" + company.getString("company_name").replace("\"", "") + "\"," + current_metric.get("citeyear") + "," + current_metric.get("value") + ",\"" + current_metric.get("source") + "\"\n";

                        csv += metric_string;
                        number_of_metrics++;
                    }
                }
            }
        }
        return csv;
    }

    public int getNumberOfExtractedMetrics() {
        if (number_of_metrics == 0) {
            exportJson();
        }
        return number_of_metrics;
    }
}
