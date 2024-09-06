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

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;

/**
 * AI Engine Model class.
 */
public class AIEngineModel {

    private final String openaiKey;
    private final String openaiModel;
    private final String openaiEndpoint;

    public AIEngineModel(String openaiKey, String openaiModel, String openaiEndpoint) {
        this.openaiKey = openaiKey;
        this.openaiModel = openaiModel;
        this.openaiEndpoint = openaiEndpoint;
    }

    /**
     * Get a OpenAI Client object with Azure utilities.
     */
    public OpenAIClient getOpenAIClient() {
        OpenAIClientBuilder builder = new OpenAIClientBuilder()
                .credential(new AzureKeyCredential(this.openaiKey))
                .endpoint(this.openaiEndpoint);

        return builder.buildClient();
    }

    public String getOpenaiModel() {
        return openaiModel;
    }

    public String getOpenaiKey() {
        return openaiKey;
    }

    public String getOpenaiEndpoint() {
        return openaiEndpoint;
    }
}
