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
package org.wso2.carbon.connector.idp;

import com.google.gson.JsonObject;
import org.apache.synapse.MessageContext;
import org.wso2.integration.connector.core.AbstractConnectorOperation;
import org.wso2.integration.connector.core.ConnectException;
import org.wso2.integration.connector.core.connection.ConnectionHandler;
import org.wso2.carbon.connector.idp.connection.AIConnection;
import org.wso2.carbon.connector.idp.constants.AIConstants;
import org.wso2.carbon.connector.idp.model.scan.AIScannerAgentModel;
import org.wso2.carbon.connector.idp.util.AIUtils;

/**
 * THis class handles scanning documents with AI.
 */
public class AIScanner extends AbstractConnectorOperation {
    
    @Override
    public void execute(MessageContext messageContext, String responseVariable, Boolean overwriteBody) throws ConnectException {
        try {
            ConnectionHandler handler = ConnectionHandler.getConnectionHandler();
            AIConnection aiConnection = (AIConnection) handler
                    .getConnection(AIConstants.CONNECTOR_NAME, AIUtils.getConnectionName(messageContext));
            AIScannerAgentModel agent = AIUtils.getAIScannerAgent(messageContext);
            agent.processRequest(aiConnection);
            JsonObject resultJSON = agent.getResponse();
            handleConnectorResponse(messageContext, responseVariable, overwriteBody, resultJSON, null, null);
        
        } catch (Exception e) {
           handleException("Error processing the document", e, messageContext);
        }
        
    }
}
