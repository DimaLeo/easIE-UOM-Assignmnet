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
package certh.iti.mklab.easie.configuration;

import certh.iti.mklab.easie.executor.handlers.ExtractionHandler;
import certh.iti.mklab.easie.extractors.dynamicpages.DynamicHTMLExtractor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author vasgat
 */
public final class Configuration {

    public URL url;

    public HashSet<String> group_of_urls;

    public String source_name;

    public String entity_name;

    public String table_selector;

    public ArrayList<ScrapableField> metrics;

    public ArrayList<ScrapableField> entity_info;

    public String next_page_selector;

    public boolean dynamic_page;

    public Object events;

    public Store store;

    public Configuration crawl;


    public void executeEvents(DynamicHTMLExtractor wrapper) throws InterruptedException, URISyntaxException, IOException, KeyManagementException {
        List<Configuration.Event> events = this.events instanceof ArrayList ?
                (ArrayList<Configuration.Event>) this.events : Collections.singletonList((Configuration.Event) this.events);

        for (Configuration.Event event : events) {
            int times_to_repeat = event.times_to_repeat != null ? event.times_to_repeat : 1;

            if (event.type.equals("CLICK")) {
                handleClickEvent(wrapper, event, times_to_repeat);
            } else if (event.type.equals("SCROLL_DOWN")) {
                handleScrollDownEvent(wrapper, times_to_repeat);
            }
        }
    }

    private void handleClickEvent(DynamicHTMLExtractor wrapper, Configuration.Event event, int times_to_repeat)
            throws URISyntaxException, IOException, InterruptedException, KeyManagementException {

        ExtractionHandler extractionHandler = new ExtractionHandler();

        if (event.extraction_type.equals("AFTER_ALL_EVENTS")) {
            performClickEvents(wrapper, event.selector, times_to_repeat);
            extractionHandler.execute(wrapper, this);
        } else {
            extractionHandler.execute(wrapper, this);
            performClickEvents(wrapper, event.selector, times_to_repeat);
        }
    }

    private void handleScrollDownEvent(DynamicHTMLExtractor wrapper, int times_to_repeat)
            throws InterruptedException, URISyntaxException, IOException, KeyManagementException {

        ExtractionHandler extractionHandler = new ExtractionHandler();

        if (times_to_repeat != 1) {
            wrapper.setScrollEvent(times_to_repeat);
        } else {
            wrapper.setScrollEvent();
        }
        extractionHandler.execute(wrapper, this);
    }

    private void performClickEvents(DynamicHTMLExtractor wrapper, String selector, int times_to_repeat) throws InterruptedException {
        for (int i = 0; i < times_to_repeat; i++) {
            wrapper.setClickEvent(selector);
        }
    }


//    public void executeEvents(DynamicHTMLExtractor wrapper) throws InterruptedException, URISyntaxException, IOException, KeyManagementException {
//        if (this.events instanceof ArrayList) {
//            for (Configuration.Event event: (ArrayList<Configuration.Event>) this.events) {
//                this.executeEvent(wrapper, event);
//            }
//        } else {
//            Configuration.Event event = (Configuration.Event) this.events;
//            System.out.println(event.extraction_type);
//            this.executeEvent(wrapper, event);
//        }
//    }
//
//    private void executeEvent(DynamicHTMLExtractor wrapper, Configuration.Event event) throws InterruptedException, URISyntaxException, IOException, KeyManagementException {
//
//        ExtractionHandler extractionHandler = new ExtractionHandler();
//
//        if (event.equals("CLICK")) {
//            for (int j = 0; j < event.times_to_repeat; j++) {
//                wrapper.setClickEvent(event.selector);
//            }
//        } else if (event.equals("SCROLL_DOWN")) {
//            for (int j = 0; j < event.times_to_repeat; j++) {
//                wrapper.setScrollEvent();
//            }
//            extractionHandler.execute(wrapper, this);
//        }
//    }

    public void setEvents(ArrayList<Event> events) {
        this.events = events;
    }

    public void setEvents(Event events) {
        this.events = events;
    }

    public static class ScrapableField {

        public Object label;

        public Object value;

        public Object citeyear;

        public void setLabel(ExtractionProperties ep) {
            label = ep;
        }

        public void setValue(ExtractionProperties ep) {
            value = ep;
        }

        public void setCiteyear(ExtractionProperties ep) {
            citeyear = ep;
        }
    }

    public static class ExtractionProperties {

        public String selector;

        public String type;

        public Object citeyear;

        public ReplaceField replace;

    }

    public static class ReplaceField {

        public List<String> regex;

        public List<String> with;
    }

    public static class Event {

        public String type;

        public String selector;

        public Integer times_to_repeat;

        public String extraction_type;
    }

    public static class URL {

        public String base_url;

        public String relative_url;
    }

    public class Store {

        public String format;

        public String database;

        public String hd_path;

        public String companies_collection;

        public String metrics_collection;

        public DBCreadentials db_credentials;

        public String wikirate_metric_designer;
    }

    public class DBCreadentials {

        public String username;

        public String password;

        public String server_address;

        public String db;
    }
}
