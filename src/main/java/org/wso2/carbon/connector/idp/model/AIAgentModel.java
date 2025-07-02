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
package org.wso2.carbon.connector.idp.model;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonObject;

import org.wso2.carbon.connector.idp.connection.AIConnection;
import org.wso2.carbon.connector.idp.exception.AIConnectorException;

/**
 * Base class of any AI Agent.
 */
public abstract class AIAgentModel {
    private String basePrompt = "";

    private final List<String> userMessages = new ArrayList<>();

    private JsonObject response = null;

    private String responseVariable= "";

    private Boolean overwriteBody= false;

    public String getResponseVariable() {
        return responseVariable;
    }

    public void setResponseVariable(String responseVariable) {
        this.responseVariable = responseVariable;
    }

    public Boolean getOverwriteBody() {
        return overwriteBody;
    }

    public void setOverwriteBody(Boolean overwriteBody) {
        this.overwriteBody = overwriteBody;
    }

    public String getBasePrompt() {
        return basePrompt;
    }

    public void setBasePrompt(String basePrompt) {
        this.basePrompt = basePrompt;
    }

    public List<String> getUserMessages() {
        return userMessages;
    }

    public void addUserMessage(String userMessage) {
        this.userMessages.add(userMessage);
    }

    public void setResponse(JsonObject response) {
        this.response = response;
    }

    public JsonObject getResponse() {
        return response;
    }
    
    public abstract void processRequest(AIConnection connection) throws AIConnectorException;
}
