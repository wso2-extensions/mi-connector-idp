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
package org.wso2.carbon.connector.ai.model.scan;

import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatMessageContentItem;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.azure.json.JsonOptions;
import com.azure.json.implementation.DefaultJsonReader;
import org.wso2.carbon.connector.ai.connection.AIConnection;
import org.wso2.carbon.connector.ai.constants.AIConstants;
import org.wso2.carbon.connector.ai.exception.AIConnectorException;
import org.wso2.carbon.connector.ai.model.AIAgentModel;
import org.wso2.carbon.connector.ai.model.AIEngineModel;
import org.wso2.carbon.connector.ai.util.AIUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI Agent to scan documents.
 */
public class AIScannerAgentModel extends AIAgentModel {

    private Integer maxTokens = AIConstants.MAX_TOKENS_DEFAULT;
    private String fileName = "";
    private String fileContent = "";
    private String schemaRegistryPath = "";

    public AIScannerAgentModel() {
        this.setBasePrompt("You are an intelligent assistant tasked with analyzing text extracted from " +
                "images using OCR technology. Your goal is to understand the content and provide insights based on the " +
                "extracted text.");
    }

    @Override
    public void processRequest(AIConnection connection) throws AIConnectorException {
        AIEngineModel engine = connection.getEngine();
        List<ChatRequestMessage> chatMessages = new ArrayList<>();
        List<ChatMessageContentItem> contentItems = getChatMessageContentItems();
        chatMessages.add(new ChatRequestSystemMessage(getBasePrompt()));
        chatMessages.add(new ChatRequestUserMessage(contentItems));
        ChatCompletionsOptions chatCompletionsOptions = new ChatCompletionsOptions(chatMessages);
        chatCompletionsOptions.setMaxTokens(maxTokens);
        ChatCompletions chatCompletions = engine.getOpenAIClient().getChatCompletions(
                engine.getOpenaiModel(), chatCompletionsOptions
        );
        String response = chatCompletions.getChoices().get(0).getMessage().getContent();
        setResponse(response.replaceAll("```", "").replace("json", "").trim());
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }

    private List<ChatMessageContentItem> getChatMessageContentItems() throws AIConnectorException {
        List<String> imageRequestList = new ArrayList<>();

        // Perform document type check
        if (fileName.toLowerCase().endsWith("pdf")) {
            List<String> base64Images = AIUtils.pdfToImage(fileContent);

            for (int i = 0; i < Objects.requireNonNull(base64Images).size(); i++) {
                imageRequestList.add(AIUtils.getImageMessegeString(base64Images.get(i)));
            }

        } else {
            Pattern regexPattern = Pattern.compile(AIConstants.IMAGE_INPUT_TYPE_REGEX);
            Matcher matcher = regexPattern.matcher(fileName.toLowerCase());

            if (matcher.matches()) {
                imageRequestList.add(AIUtils.getImageMessegeString(fileContent));
            } else {
                throw new AIConnectorException("Invalid file format with the payload");
            }
        }
        List<ChatMessageContentItem> contentItems = new ArrayList<>();
        try {
            // add schema content
            String schemaContentItem = AIUtils.getSchemaContentString(getSchemaRegistryPath());
            contentItems.add(ChatMessageContentItem.fromJson(DefaultJsonReader
                    .fromString(schemaContentItem, new JsonOptions())));

            // add image content
            for (String image : imageRequestList) {
                contentItems.add(ChatMessageContentItem.fromJson(DefaultJsonReader
                        .fromString(image, new JsonOptions())));
            }
        } catch (IOException e) {
            throw new AIConnectorException("Error occurred in JSON conversion", e);
        }

        return contentItems;
    }
}
