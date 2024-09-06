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
package org.wso2.carbon.connector.ai.model;

import org.wso2.carbon.connector.ai.connection.AIConnection;
import org.wso2.carbon.connector.ai.constants.AIConstants;
import org.wso2.carbon.connector.ai.exception.AIConnectorException;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class of any AI Agent.
 */
public abstract class AIAgentModel {
    private String basePrompt = "";

    private String retryPrompt = "";

    private final List<String> userMessages = new ArrayList<>();

    private int retryCount = AIConstants.RETRY_COUNT_DEFAULT;

    private int remainingRetries = AIConstants.RETRY_COUNT_DEFAULT;
    private String response = null;

    public String getBasePrompt() {
        return basePrompt;
    }

    public void setBasePrompt(String basePrompt) {
        this.basePrompt = basePrompt;
    }

    public String getRetryPrompt() {
        return retryPrompt;
    }

    public void setRetryPrompt(String retryPrompt) {
        this.retryPrompt = retryPrompt;
    }

    public List<String> getUserMessages() {
        return userMessages;
    }

    public void addUserMessage(String userMessage) {
        this.userMessages.add(userMessage);
    }

    public int getRetryCounter() {
        return remainingRetries;
    }

    public void setRetryCounter() {
        remainingRetries--;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
        this.remainingRetries = retryCount;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }

    /**
     * Implementation should call the AI endpoint with necessary parameters and set the response
     */
    public abstract void processRequest(AIConnection connection) throws AIConnectorException;
}
