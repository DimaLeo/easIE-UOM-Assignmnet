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
package certh.iti.mklab.easie;

import certh.iti.mklab.easie.configuration.Configuration;
import certh.iti.mklab.easie.configuration.ConfigurationReader;
import certh.iti.mklab.easie.exception.IllegalSchemaException;
import certh.iti.mklab.easie.exception.PaginationException;
import certh.iti.mklab.easie.exception.RelativeURLException;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import certh.iti.mklab.easie.executor.WrapperExecutor;
import certh.iti.mklab.easie.executor.handlers.DataHandler;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.google.gson.Gson;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.json.JSONArray;
import org.jsoup.select.Selector.SelectorParseException;

import static java.lang.System.exit;

/**
 * @author vasgat
 */
public class Main {


    public static void main(String[] args) throws URISyntaxException, NoSuchAlgorithmException {
        try {
            ConfigurationReader reader = null;
            if(args.length == 1){
                reader = new ConfigurationReader(args[0], ".");
            }
            else if(args.length == 2){
                reader = new ConfigurationReader(args[0], args[1]);
            }
            else if(args.length == 0) {
                System.out.println("You need to provide the configuration file filepath!");
                exit(1);
            }
            else {
                System.out.println("Maximum two vars can be defined...");
                exit(1);
            }

            Configuration config = reader.getConfiguration();

            WrapperExecutor executor = new WrapperExecutor(config, ".");

            ArrayList companies = (ArrayList) executor.getCompanyInfo();
            ArrayList metrics = executor.getExtractedMetrics();

            DataHandler dh = new DataHandler(companies, metrics, config.entity_name);

            if (config.store != null) {
                dh.store(config.store, config.source_name);
                System.out.println("EXTRACTION TASK COMPLETED");
            } else {
                JSONArray array = new JSONArray(dh.exportJson());
                System.out.println(array.toString(4));
            }
        } catch (IOException | SelectorParseException | RelativeURLException | PaginationException |
                 InterruptedException | IllegalSchemaException | ProcessingException ex) {
            System.out.println(ex.getMessage());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

}
