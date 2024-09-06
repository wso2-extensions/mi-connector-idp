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
package org.wso2.carbon.connector.ai;

import org.apache.axis2.AxisFault;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.wso2.carbon.connector.ai.connection.AIConnection;
import org.wso2.carbon.connector.ai.constants.AIConstants;
import org.wso2.carbon.connector.ai.model.prompt.AIConverserAgentModel;
import org.wso2.carbon.connector.ai.util.AIUtils;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.connector.core.connection.ConnectionHandler;

/**
 * This class handles execution of a given prompt with AI.
 */
public class AIConverser extends AbstractConnector {

    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        try {
            org.apache.axis2.context.MessageContext msgContext = ((Axis2MessageContext) messageContext).getAxis2MessageContext();

            ConnectionHandler handler = ConnectionHandler.getConnectionHandler();
            AIConnection aiConnection = (AIConnection) handler
                    .getConnection(AIConstants.CONNECTOR_NAME, AIUtils.getConnectionName(messageContext));
            AIConverserAgentModel agent = AIUtils.getAIConverserAgent(messageContext);
            agent.processRequest(aiConnection);
            JsonUtil.getNewJsonPayload(msgContext, agent.getResponse(), true, true);
            msgContext.setProperty(AIConstants.MESSAGE_TYPE_STRING, AIConstants.JSON_CONTENT_TYPE);
            msgContext.setProperty(AIConstants.CONTENT_TYPE_STRING, AIConstants.JSON_CONTENT_TYPE);
        } catch (AxisFault e) {
            handleException("Error processing JSON payload: Invalid response format.", e, messageContext);
        }
    }

}
