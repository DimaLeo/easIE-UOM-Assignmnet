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

import certh.iti.mklab.easie.configuration.Configuration;
import com.mongodb.MongoClient;
import certh.iti.mklab.easie.MongoUtils;
import certh.iti.mklab.easie.configuration.Configuration.Store;
import com.mongodb.client.MongoCollection;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

/**
 * DataHandler transforms the extracted data into Snippet Objects and stores
 * them into a mongodb or in the drive
 *
 * @author vasgat
 */
public class DataHandler {

    private List<Document> extracted_company_info;
    private List<ArrayList<Document>> extracted_metrics;
    private StoreUtils storeUtils;
    private String entity_name;

    /**
     * Creates SnippetHandler Object
     *
     * @param extracted_company_info: extracted company fields
     * @param extracted_metrics:      extracted snippet fields
     * @throws Exception
     */
    public DataHandler(List<Document> extracted_company_info, List<ArrayList<Document>> extracted_metrics, String entity_name) {
        this.extracted_company_info = extracted_company_info;
        this.extracted_metrics = extracted_metrics;
        this.entity_name = entity_name;
        storeUtils = new StoreUtils(extracted_company_info, extracted_metrics, entity_name);
    }

    /**
     * Stores the extracted data (companies_fields and extracted_metrics) into
     * mongodb or drive
     *
     * @param store       contains information about where the data are going to be
     *                    stored
     * @param source_name
     * @throws UnknownHostException
     * @throws FileNotFoundException
     * @throws Exception
     */
    public void store(Store store, String sourceName) throws UnknownHostException, FileNotFoundException, IOException {
        if (store.companies_collection != null && extracted_company_info != null) {
            storeCompaniesAndMetrics(store, sourceName);
        } else if (store.companies_collection == null && store.metrics_collection != null) {
            storeMetrics(store);
        } else if ("json".equals(store.format)) {
            storeUtils.toJSONFile(store.hd_path);
        } else {
            storeUtils.toCSVFile(store.hd_path, store.wikirate_metric_designer);
        }
    }

    private void storeCompaniesAndMetrics(Store store, String sourceName) throws IOException {
        MongoClient client = createMongoClient(store.db_credentials);
        MongoCollection companiesCollection = connectToMongo(client, store.database, store.companies_collection);
        MongoCollection metricsCollection = connectToMongo(client, store.database, store.metrics_collection);

        storeUtils.toMongoDB(companiesCollection, metricsCollection, sourceName);

        client.close();
    }

    private void storeMetrics(Store store) throws UnknownHostException {
        MongoClient client = createMongoClient(store.db_credentials);
        MongoCollection metricsCollection = connectToMongo(client, store.database, store.metrics_collection);

        storeUtils.toMongoDB(metricsCollection);

        client.close();
    }

    private MongoClient createMongoClient(Configuration.DBCreadentials credentials) {
        if (credentials != null) {
            return MongoUtils.newClient(
                    credentials.server_address,
                    credentials.username,
                    credentials.password,
                    credentials.db
            );
        } else {
            return MongoUtils.newClient();
        }
    }

    private MongoCollection connectToMongo(MongoClient client, String database, String collection) {
        return MongoUtils.connect(client, database, collection);
    }

    public String exportJson() {
        return storeUtils.exportJson();
    }

    public int getNumberOfExtractedMetrics() {
        return storeUtils.getNumberOfExtractedMetrics();
    }

}
