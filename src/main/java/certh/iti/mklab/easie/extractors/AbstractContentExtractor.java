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
package certh.iti.mklab.easie.extractors;

import certh.iti.mklab.easie.FIELD_TYPE;
import certh.iti.mklab.easie.configuration.Configuration.ExtractionProperties;
import certh.iti.mklab.easie.configuration.Configuration.ReplaceField;
import certh.iti.mklab.easie.configuration.Configuration.ScrapableField;
import certh.iti.mklab.easie.exception.HTMLElementNotFoundException;
import certh.iti.mklab.easie.exception.PostProcessingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.bson.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author vasgat
 */
public abstract class AbstractContentExtractor {

    protected Element document;
    protected String source;

    protected abstract List run(List<ScrapableField> fields, FIELD_TYPE type) throws HTMLElementNotFoundException;

    public List getExtractedFields(List<ScrapableField> fields, FIELD_TYPE type) throws HTMLElementNotFoundException {
        return run(fields, type);
    }

    /**
     * Returns the content of a specific field in the document
     *
     * @param field
     * @return a Pair of String, Object that corresponds to field name and field
     * value accordingly
     */
    protected Document getSelectedElement(ScrapableField field, Object e) throws PostProcessingException {
        Element element = (Element) e;

        String fieldName;
        Integer fieldCiteyear;

        fieldName = extractFieldName(field, element);
        fieldCiteyear = getFieldCityYearValue(field, element);

        Document extractedContent = new Document();
        extractedContent
                .append("name", fieldName)
                .append("citeyear", fieldCiteyear);

        processFieldValue(field.value, extractedContent);

        return extractedContent;
    }

    private static void processFieldValue(Object fieldValue, Document extractedContent) {
        if (fieldValue instanceof String) {
            convertFieldToExtractedContent(fieldValue, extractedContent);
        } else {
            handleNonStringContent(fieldValue, extractedContent);
        }
    }

    private static void handleNonStringContent(Object fieldValue, Document extractedContent) {
        ExtractionProperties properties = (ExtractionProperties) fieldValue;

        switch (properties.type) {
            case "text":
                extractedContent.append("type", "textual").append("value", fieldValue);
                break;
            case "numerical":
                handleNumericalContent(extractedContent, fieldValue);
                break;
            case "boolean":
                handleBooleanContent(extractedContent,(Boolean) fieldValue);
                break;
            case "categorical":
                extractedContent.append("type", "categorical").append("value", fieldValue);
                break;
            case "link":
            case "src":
                extractedContent.append("type", "link").append("value", fieldValue);
                break;
            default:
                extractedContent.append("type", "other").append("value", fieldValue);
                break;
        }
    }

    private String extractFieldName(ScrapableField field, Element element) throws PostProcessingException {
        String fieldName;
        if (field.label instanceof String) {
            fieldName = (String) field.label;
        } else {
            fieldName = extractContent((ExtractionProperties) field.label, element).toString();
        }
        return fieldName;
    }

    private static void convertFieldToExtractedContent(Object fieldValue, Document extractedContent) {
        if (isFalseValue(fieldValue)) {
            handleBooleanContent(extractedContent, false);
        } else if (isTrueValue(fieldValue)) {
            handleBooleanContent(extractedContent, true);
        } else if (isNumericalValue(fieldValue)) {
            handleNumericalContent(extractedContent, fieldValue);
        } else {
            handleTextualContent(extractedContent, fieldValue);
        }
    }

    private static boolean isFalseValue(Object fieldValue) {
        String trimmedValue = fieldValue.toString().trim().toLowerCase();
        return trimmedValue.equals("0") || trimmedValue.equals("false") || trimmedValue.equals("no");
    }

    private static boolean isTrueValue(Object fieldValue) {
        String trimmedValue = fieldValue.toString().trim().toLowerCase();
        return trimmedValue.equals("1") || trimmedValue.equals("true") || trimmedValue.equals("yes");
    }

    private static boolean isNumericalValue(Object fieldValue) {
        String cleanedValue = fieldValue.toString().replaceAll("[0-9\\.,]", "").trim();
        return cleanedValue.equals("") && isNumeric(fieldValue.toString());
    }

    private static void handleBooleanContent(Document extractedContent, boolean value) {
        extractedContent.append("value", value).append("type", "boolean");
    }

    private static void handleNumericalContent(Document extractedContent, Object fieldValue) {
        try {
            double numericalValue = Double.parseDouble(fieldValue.toString().replaceAll("[^0-9\\.]", "").trim());
            extractedContent.append("value", numericalValue).append("type", "numerical");
        } catch (NumberFormatException nfe) {
            // Handle the case where parsing to double fails
            extractedContent.append("value", null).append("type", "numerical");
        }
    }

    private static void handleTextualContent(Document extractedContent, Object fieldValue) {
        extractedContent.append("value", fieldValue).append("type", "textual");
    }

    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    private Integer getFieldCityYearValue(ScrapableField field, Element element) {
        Integer fieldCiteyear;

        try {
            if (field.citeyear instanceof Double) {
                fieldCiteyear = ((Double) field.citeyear).intValue();
            } else {
                fieldCiteyear = Integer.parseInt((String) extractContent((ExtractionProperties) field.citeyear, element));
            }
        } catch (Exception ex) {
            fieldCiteyear = Calendar.getInstance().get(Calendar.YEAR);

        }
        return fieldCiteyear;
    }

    protected Object extractContent(ExtractionProperties extractionProperties, Element element) throws PostProcessingException {
        Object extractedContent;

        if (extractionProperties.type.equals("text") || extractionProperties.type.equals("numerical") || extractionProperties.type.equals("boolean") || extractionProperties.type.equals("categorical")) {
            extractedContent = element.select(extractionProperties.selector).text();
        } else if (extractionProperties.type.equals("link")) {
            extractedContent = element.select(extractionProperties.selector).attr("href");
            if (extractedContent.equals("")) {
                extractedContent = element.select(extractionProperties.selector).attr("data-tab-content");
                if (!extractedContent.equals("")) {
                    extractedContent = source + "#" + extractedContent;
                }
            }
        } else if (extractionProperties.type.equals("image")) {
            extractedContent = element.select(extractionProperties.selector).attr("src");
        } else if (extractionProperties.type.equals("html")) {
            extractedContent = element.select(extractionProperties.selector).html();
        } else if (extractionProperties.type.equals("list")) {
            extractedContent = extractList(element, extractionProperties.selector);
        } else {
            extractedContent = element.select(extractionProperties.selector).attr(extractionProperties.type);
        }

        if (extractedContent.equals("")) {
            System.out.println("WARNING: No content found in the specified element:|" + element.cssSelector() + " " + extractionProperties.selector + "| Please check the correction of the defined extraction rule or for possible changes in the source!");
        }
        if (extractedContent instanceof String && extractionProperties.replace != null) {
            extractedContent = processContent(
                    (String) extractedContent,
                    extractionProperties.replace
            );
        }

        return extractedContent;
    }

    protected String processContent(String content, ReplaceField replaceField) throws PostProcessingException {
        if (replaceField.regex.size() == replaceField.with.size()) {
            for (int i = 0; i < replaceField.regex.size(); i++) {
                content = content.replaceAll(
                        replaceField.regex.get(i),
                        replaceField.with.get(i)
                );
            }
        } else {
            throw new PostProcessingException("regex and with arrays need to be the same size in the replace field");
        }

        return content;
    }

    protected List extractList(Element element, String listSelector) {
        List list = new ArrayList();
        Elements elements = element.select(listSelector);
        for (int i = 0; i < elements.size(); i++) {
            list.add(elements.get(i).text()/*.attr("href")*/);
        }
        return list;
    }
}
