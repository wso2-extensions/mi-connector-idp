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
package org.wso2.carbon.connector.idp.connection;

import org.wso2.integration.connector.core.connection.Connection;
import org.wso2.integration.connector.core.connection.ConnectionConfig;
import org.wso2.carbon.connector.idp.model.AIEngineModel;

public class AIConnection implements Connection {
    private AIConnectionConfiguration aiConnectionConfiguration;
    private AIEngineModel engine;

    @Override
    public void connect(ConnectionConfig connectionConfiguration) {
    }

    @Override
    public void close() {
    }

    public AIConnection(AIConnectionConfiguration aiConnectionConfiguration) {
        this.aiConnectionConfiguration = aiConnectionConfiguration;
    }

    public AIConnectionConfiguration getAiConnectionConfiguration() {
        return aiConnectionConfiguration;
    }

    public void setAiConnectionConfiguration(AIConnectionConfiguration aiConnectionConfiguration) {
        this.aiConnectionConfiguration = aiConnectionConfiguration;
    }

    public AIEngineModel getEngine() {
        if (engine == null) {
            this.engine = createNewAIEngineInstance(this.aiConnectionConfiguration);
        }
        return this.engine;
    }

    public void setEngine(AIConnectionConfiguration connectionConfiguration) {
        this.engine = createNewAIEngineInstance(connectionConfiguration);
    }

    private AIEngineModel createNewAIEngineInstance(AIConnectionConfiguration connectionConfiguration) {
        return new AIEngineModel(
            connectionConfiguration.getApiKey(),
            connectionConfiguration.getModel(),
            connectionConfiguration.getEndpointUrl()
        );
    }
}
