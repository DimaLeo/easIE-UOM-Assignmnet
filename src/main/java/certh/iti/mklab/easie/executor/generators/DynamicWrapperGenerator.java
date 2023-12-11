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
package certh.iti.mklab.easie.executor.generators;

import certh.iti.mklab.easie.configuration.Configuration;
import certh.iti.mklab.easie.exception.RelativeURLException;
import certh.iti.mklab.easie.executor.handlers.EventHandler;
import certh.iti.mklab.easie.extractors.dynamicpages.DynamicHTMLExtractor;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.util.HashSet;
import java.util.Iterator;

/**
 *
 * @author vasgat
 */
public class DynamicWrapperGenerator extends WrapperGenerator {

    private String ChromeDriverPath;
    private final EventHandler eventHandler;

    public DynamicWrapperGenerator(Configuration configuration, String ChromeDriverPath) {
        super(configuration);
        this.ChromeDriverPath = ChromeDriverPath;

        this.eventHandler = EventHandler.getInstance();
    }

    @Override
    public void execute() throws InterruptedException, URISyntaxException, IOException, RelativeURLException, KeyManagementException {

        if (configuration.url.relative_url != null) {
            DynamicHTMLExtractor wrapper = new DynamicHTMLExtractor(
                    configuration.url.base_url,
                    configuration.url.relative_url,
                    ChromeDriverPath
            );
            if (configuration.events != null) {
                eventHandler.executeEvents(configuration, wrapper);
            } else {
                extraction_handler.execute(wrapper, configuration);
            }

            wrapper.browser_emulator.close();
        } else if (configuration.group_of_urls != null && configuration.url.base_url != null && configuration.url.relative_url == null) {
            HashSet<String> group_of_urls = configuration.group_of_urls;

            Iterator<String> it = group_of_urls.iterator();
            while (it.hasNext()) {
                String current_url = it.next().replace(configuration.url.base_url, "");

                DynamicHTMLExtractor wrapper = new DynamicHTMLExtractor(
                        configuration.url.base_url,
                        current_url,
                        ChromeDriverPath
                );

                if (configuration.events != null) {
                    eventHandler.executeEvents(configuration, wrapper);
                } else {
                    extraction_handler.execute(wrapper, configuration);
                }

                wrapper.browser_emulator.close();
            }

        } else {
            throw new RelativeURLException("relative_url is not defined");
        }
    }


}
