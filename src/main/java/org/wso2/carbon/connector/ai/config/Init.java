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
package org.wso2.carbon.connector.ai.config;

import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.SynapseEnvironment;
import org.wso2.carbon.connector.ai.connection.AIConnection;
import org.wso2.carbon.connector.ai.connection.AIConnectionConfiguration;
import org.wso2.carbon.connector.ai.constants.AIConstants;
import org.wso2.carbon.connector.ai.util.AIUtils;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.connector.core.connection.ConnectionHandler;

import java.util.Optional;


public class Init extends AbstractConnector implements ManagedLifecycle {
    @Override
    public void init(SynapseEnvironment synapseEnvironment) {

    }

    @Override
    public void destroy() {
        ConnectionHandler.getConnectionHandler().shutdownConnections(AIConstants.CONNECTOR_NAME);
    }

    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        try {
            AIConnectionConfiguration configuration = getConnectionConfigFromContext(messageContext);
            String connectionName = configuration.getConnectionName();
            ConnectionHandler handler = ConnectionHandler.getConnectionHandler();
            if (!handler.checkIfConnectionExists(AIConstants.CONNECTOR_NAME, connectionName)) {
                AIConnection aiConnection = new AIConnection(configuration);
                handler.createConnection(AIConstants.CONNECTOR_NAME, connectionName, aiConnection);
            } else {
                AIConnection aiConnection = (AIConnection) handler
                        .getConnection(AIConstants.CONNECTOR_NAME, connectionName);
                if (!aiConnection.getAiConnectionConfiguration().equals(configuration)) {
                    aiConnection.setAiConnectionConfiguration(configuration);
                    aiConnection.setEngine(configuration);
                }
            }
        } catch (SynapseException e) {
            handleException("Failed to initiate ai connector configuration.", e, messageContext);
        }
    }

    private AIConnectionConfiguration getConnectionConfigFromContext(MessageContext mc) throws SynapseException {
       Optional<String> openAIKey = AIUtils.getStringParam(mc, AIConstants.KEY_STRING);
       String openAIModel = AIUtils.getStringParam(mc, AIConstants.MODEL_STRING).orElse(AIConstants.MODEL_DEFAULT);
       String openAIEndpoint = AIUtils.getStringParam(mc, AIConstants.ENDPOINT_STRING)
               .orElse(AIConstants.ENDPOINT_DEFAULT);
       Optional<String> connectionName = AIUtils.getStringParam(mc, AIConstants.CONNECTION_NAME);

       if (!openAIKey.isPresent()) {
           throw new SynapseException("OpenAI API key is required.");
       }

       if (!connectionName.isPresent()) {
           throw new SynapseException("Mandatory parameter 'connectionName' is not set.");
       }

       AIConnectionConfiguration connectionConfiguration = new AIConnectionConfiguration();
       connectionConfiguration.setConnectionName(connectionName.get());
       connectionConfiguration.setOpenaiKey(openAIKey.get());
       connectionConfiguration.setOpenaiModel(openAIModel);
       connectionConfiguration.setOpenaiEndpoint(openAIEndpoint);
       return connectionConfiguration;
    }
}
