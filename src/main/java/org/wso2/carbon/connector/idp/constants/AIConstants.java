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
package org.wso2.carbon.connector.idp.constants;

public class AIConstants {
    public static final String CONNECTOR_NAME = "idp";
    public static final String CONNECTION_TYPE = "connectionType";
    public static final String OPEN_AI= "OPEN_AI";
    public static final String OLLAMA = "OLLAMA";
    public static final String CUSTOM_LLM = "CUSTOM_LLM";
    public static final String CONNECTION_NAME = "name";
    public static final String KEY_STRING = "apiKey";
    public static final String MODEL_STRING = "model";
    public static final String MODEL_LLM_DEFAULT= "gpt-4.1-mini";
    public static final String MODEL_OLLAMA_DEFAULT = "qwen2.5vl:3b";
    public static final String MODEL_OPEN_AI_DEFAULT= "gpt-4.1-mini";
    public static final String ENDPOINT_STRING = "endpointUrl";
    public static final String ENDPOINT_LLM_DEFAULT= "https://api.openai.com/v1/chat/completions";
    public static final String ENDPOINT_OLLAMA_DEFAULT= "http://localhost:11434/v1/chat/completions";
    public static final String ENDPOINT_OPEN_AI_DEFAULT = "https://api.openai.com/v1/chat/completions";
    public static final String RESPONSE_VARIABLE= "responseVariable";
    public static final String OVERWRITE_BODY = "overwriteBody";
    public static final String MESSAGE_TYPE_STRING = "messageType";
    public static final String CONTENT_TYPE_STRING = "ContentType";
    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String MAX_TOKENS = "maxTokens";
    public static final String FILE_CONTENT = "fileContent";
    public static final String MIME_TYPE = "mimeType";
    public static final String CONTENT_FORMAT = "contentFormat";
    public static final Integer MAX_TOKENS_DEFAULT = 4096;
    public static final Integer IMAGE_DPI_DEFAULT = 300;
    public static final String SCANNER_OUTPUT_SCHEMA = "idpSchema";
    public static final String REGISTRY_PATH = "gov:mi-resources/idp-schemas/";
    public static final String REGISTRY_PATH_BELOW_4_4_0 = "gov:idp-schemas/";
    public static final String SYSTEM_PROMPT_TEMPLATE =
            "You are an expert AI assistant specialized in analyzing multiple images and extracting structured data. " +
            "Your task is to accurately populate the provided JSON schema using the given images. " +
            "Each field in the schema has a description. Use it to infer the correct value if possible. " +
            "If a field cannot be confidently inferred from the images or its description, return null for that field. " +
            "Field names in the output must exactly match the keys in the schema, including case sensitivity. " +
            "Return only a valid JSON object matching the schema structure. Do not include any other text, comments, or formatting.";
    public static final String USER_PROMPT_TEMPLATE =
            "Please analyze all the provided images thoroughly and populate the following JSON schema based on the information extracted. ";
}

