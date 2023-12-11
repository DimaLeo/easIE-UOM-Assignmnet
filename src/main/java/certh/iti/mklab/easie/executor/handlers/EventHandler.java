package certh.iti.mklab.easie.executor.handlers;

import certh.iti.mklab.easie.configuration.Configuration;
import certh.iti.mklab.easie.extractors.dynamicpages.DynamicHTMLExtractor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.util.ArrayList;

public class EventHandler {

    private static EventHandler instance;
    private ExtractionHandler extractionHandler = new ExtractionHandler();

    private EventHandler() {
    }

    public static EventHandler getInstance() {
        if (instance == null) {
            instance = new EventHandler();
        }
        return instance;
    }

    public void executeEvents(Configuration configuration, DynamicHTMLExtractor wrapper) throws InterruptedException, URISyntaxException, IOException, KeyManagementException {
        if (configuration.events instanceof ArrayList) {
            ArrayList<Configuration.Event> list_of_events = (ArrayList<Configuration.Event>) configuration.events;
            for (Configuration.Event event: list_of_events) {
                executeEvent(configuration, wrapper, event);
            }
        } else {
            Configuration.Event event = (Configuration.Event) configuration.events;
            System.out.println(event.extraction_type);
            executeEvent(configuration, wrapper, event);
        }
    }

    private void executeEvent(Configuration configuration, DynamicHTMLExtractor wrapper, Configuration.Event event) throws InterruptedException, URISyntaxException, IOException, KeyManagementException {
        if (event.type.equals("CLICK")) {
            int times_to_repeat = event.times_to_repeat;
            for (int j = 0; j < times_to_repeat; j++) {
                wrapper.setClickEvent(event.selector);
            }
        } else if (event.type.equals("SCROLL_DOWN")) {
            int times_to_repeat = event.times_to_repeat;
            for (int j = 0; j < times_to_repeat; j++) {
                wrapper.setScrollEvent();
            }
            extractionHandler.execute(wrapper, configuration);
        }
    }

}
