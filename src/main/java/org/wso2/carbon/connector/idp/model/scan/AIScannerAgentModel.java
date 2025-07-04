/*
 * Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com). All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.connector.idp.model.scan;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.wso2.carbon.connector.idp.connection.AIConnection;
import org.wso2.carbon.connector.idp.constants.AIConstants;
import org.wso2.carbon.connector.idp.exception.AIConnectorException;
import org.wso2.carbon.connector.idp.model.AIAgentModel;
import org.wso2.carbon.connector.idp.model.AIEngineModel;
import org.wso2.carbon.connector.idp.util.AIUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AIScannerAgentModel extends AIAgentModel {

    private Integer maxTokens = AIConstants.MAX_TOKENS_DEFAULT;
    private String fileContent = "";
    private String schemaRegistryPath = "";

    private static final Gson gson = new Gson();

    private static class ApiResponse {
        private List<Choice> choices;

        public List<Choice> getChoices() {
            return choices;
        }
    }

    private static class Choice {
        private ResponseMessage message;

        public ResponseMessage getMessage() {
            return message;
        }
    }

    private static class ResponseMessage {
        private String content;

        public String getContent() {
            return content;
        }
    }

    public AIScannerAgentModel() {
        this.setBasePrompt(AIConstants.SYSTEM_PROMPT_TEMPLATE);
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public String getSchemaRegistryPath() {
        return schemaRegistryPath;
    }

    public void setSchemaRegistryPath(String schemaRegistryPath) {
        this.schemaRegistryPath = schemaRegistryPath;
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }

   @Override
    public void processRequest(AIConnection connection) throws AIConnectorException {
        try {
            HttpClient sharedHttpClient = connection.getEngine().getHttpClient();
            AIEngineModel engine = connection.getEngine();
            String endpointUrl = engine.getEndpointUrl();
            String apiKey = engine.getApiKey();
            String modelName = engine.getModel();

            List<Map<String, Object>> messages = buildRequestMessages();

            JsonObject schemaObject = AIUtils.getSchemaContentAsJsonObject(getSchemaRegistryPath());

            Map<String, Object> jsonSchemaPayload = Map.of(
                    "name", "document_extraction_schema",
                    "schema", schemaObject
                    // "strict" if needed 
            );
            Map<String, Object> responseFormat = Map.of(
                    "type", "json_schema",
                    "json_schema", jsonSchemaPayload
            );

            Map<String, Object> requestPayload = Map.of(
                    "model", modelName, 
                    "messages", messages,
                    "max_tokens", this.maxTokens,
                    "temperature", 0.0,
                    "response_format", responseFormat 
            );

            String jsonBody = gson.toJson(requestPayload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = sharedHttpClient.send(request, HttpResponse.BodyHandlers.ofString());

             if (response.statusCode() == 200) {
                ApiResponse apiResponse = gson.fromJson(response.body(), ApiResponse.class);
                if (apiResponse == null || apiResponse.getChoices() == null || apiResponse.getChoices().isEmpty()) {
                    throw new AIConnectorException("Failed to process document: LLM response was empty or malformed.");
                }
                String responseContent = apiResponse.getChoices().get(0).getMessage().getContent();
                JsonObject finalJsonResponse = extractJsonFromLlmContent(responseContent);
                setResponse(finalJsonResponse);
            } else {
                throw new AIConnectorException("API request failed with status code " + response.statusCode() + ": " + response.body());
            }
         } 
         catch (AIConnectorException e) {
            throw e;
         }
         catch (IOException e) {
            throw new AIConnectorException("Error occurred while sending request to AI service.", e);
        } catch (Exception e) {
            throw new AIConnectorException(e.getMessage(), e);
        }
    }

    /**
     * Extracts a valid JSON object from the raw string content of an LLM response.
     * This method cleans markdown fences and surrounding text, then parses the result.
     *
     * @param content The raw string content from the LLM.
     * @return A validated JsonObject.
     * @throws AIConnectorException if the content is empty, no JSON object is found, or parsing fails.
     */
    private static JsonObject extractJsonFromLlmContent(String content) throws AIConnectorException {
        if (content == null || content.isBlank()) {
            throw new AIConnectorException("LLM response content is empty or not a string");
        }
        //some open source LLMS may return content with markdown code fences
        String cleanedContent = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        Pattern pattern = Pattern.compile("\\{[\\s\\S]*\\}");
        Matcher matcher = pattern.matcher(cleanedContent);

        String jsonString;
        if (matcher.find()) {
            jsonString = matcher.group(0);
        } else {
            throw new AIConnectorException("No valid JSON object found in the LLM response content.");
        }
        try {
            JsonObject parsedJson = JsonParser.parseString(jsonString).getAsJsonObject();
            if (parsedJson == null || parsedJson.isJsonNull()) {
                throw new AIConnectorException("Parsed content is not a valid JSON object (resulted in null).");
            }
            return parsedJson;
        } catch (JsonSyntaxException e) {
            throw new AIConnectorException("Failed to parse extracted content as JSON. Syntax error.", e);
        } catch (IllegalStateException e) {
            throw new AIConnectorException("Parsed content is not a JSON object.", e);
        }
    }

    private List<Map<String, Object>> buildRequestMessages() throws AIConnectorException {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", getBasePrompt()));

        List<Object> userMessageParts = new ArrayList<>();
        userMessageParts.add(Map.of("type", "text", "text", AIConstants.USER_PROMPT_TEMPLATE));

        if (fileContent != null && fileContent.toLowerCase().startsWith("data:")) {
            String mimeType = fileContent.substring(5, fileContent.indexOf(";")).toLowerCase();
            if (mimeType.equals("application/pdf")) {
                String base64Pdf = fileContent.substring("data:application/pdf;base64,".length());
                List<String> base64Images = AIUtils.pdfToImage(base64Pdf);
                for (String base64Image : Objects.requireNonNull(base64Images)) {
                    userMessageParts.add(createImagePart("data:image/png;base64," + base64Image));
                }
            } else if ((mimeType.equals("image/png") || mimeType.equals("image/jpeg") ||
                       mimeType.equals("image/gif") || mimeType.equals("image/webp"))) {
                userMessageParts.add(createImagePart(fileContent));
            } else {
                throw new AIConnectorException("Unsupported file MIME type: " + mimeType);
            }
        } else {
            throw new AIConnectorException("Invalid or missing Base64 data URI string.");
        }
        messages.add(Map.of("role", "user", "content", userMessageParts));
        return messages;
    }

    private Map<String, Object> createImagePart(String dataUri) {
        return Map.of("type", "image_url", "image_url", Map.of("url", dataUri));
    }
}

