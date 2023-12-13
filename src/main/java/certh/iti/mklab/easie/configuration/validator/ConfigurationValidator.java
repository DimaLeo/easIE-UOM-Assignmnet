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
package certh.iti.mklab.easie.configuration.validator;

import certh.iti.mklab.easie.exception.IllegalSchemaException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author vasgat
 */
public class ConfigurationValidator {

    private JsonValidator VALIDATOR;
    private JsonNode ConfigurationSchema;

    public ConfigurationValidator(String path) throws IOException {
        this.VALIDATOR = JsonSchemaFactory.byDefault().getValidator();
        this.ConfigurationSchema = JsonLoader.fromFile(
                new File(path + "/ConfigurationSchema.json")
        );
    }

    public ConfigurationValidator() throws IOException {
        this.VALIDATOR = JsonSchemaFactory.byDefault().getValidator();
        this.ConfigurationSchema = JsonLoader.fromFile(
                new File("ConfigurationSchema.json")
        );
    }

    public void validate(File configurationFile) throws IOException, ProcessingException, IllegalSchemaException {
        JsonNode configuration = JsonLoader.fromFile(configurationFile);
        performValidation(configuration);
    }

    public void validate(String configurationFile) throws IOException, ProcessingException, IllegalSchemaException {
        JsonNode configuration = JsonLoader.fromString(configurationFile);
        performValidation(configuration);
    }

    private void performValidation(JsonNode configuration) throws IOException, ProcessingException, IllegalSchemaException {
        ProcessingReport report = VALIDATOR.validate(ConfigurationSchema, configuration);

        if (!report.isSuccess()) {
            Iterator<ProcessingMessage> iterator = report.iterator();
            ProcessingMessage message = iterator.next();
            JSONObject jsonObject = new JSONObject(message.asJson().toString());
            throw new IllegalSchemaException(jsonObject.toString(4));
        }
    }

    public void setConfigurationSchema(File ConfigurationSchemaFile) throws IOException {
        this.ConfigurationSchema = JsonLoader.fromFile(ConfigurationSchemaFile);
    }

    public void setConfigurationSchema(String ConfigurationSchema) throws IOException {
        this.ConfigurationSchema = JsonLoader.fromString(ConfigurationSchema);
    }
}
