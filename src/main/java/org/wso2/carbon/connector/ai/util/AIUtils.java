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
package org.wso2.carbon.connector.ai.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.connector.ai.constants.AIConstants;
import org.wso2.carbon.connector.ai.exception.AIConnectorException;
import org.wso2.carbon.connector.ai.model.prompt.AIConverserAgentModel;
import org.wso2.carbon.connector.ai.model.prompt.AIRequestSchema;
import org.wso2.carbon.connector.ai.model.scan.AIScannerAgentModel;
import org.wso2.carbon.connector.core.util.ConnectorUtils;
import org.wso2.micro.integrator.registry.MicroIntegratorRegistry;
import org.wso2.micro.integrator.registry.Resource;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;

/**
 * Utility class for AI operations.
 */
public class AIUtils {
    /**
     * This method will convert a given string into JSON object.
     *
     * @param json The JSON data in string format.
     * @return The JSON object.
     */
    public static JsonObject loadJsonData(String json) {
        try {
            return new JsonParser().parse(json).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    /**
     * Reading the schema from the registry
     *
     * @param schemaPath     Registry Path of the Schema
     * @return InputStream
     */
    public static InputStream getSchemaFromRegistry(String schemaPath) throws AIConnectorException {

        if (!schemaPath.isEmpty()) {
            try {
                MicroIntegratorRegistry registry = new MicroIntegratorRegistry();
                Resource resource = registry.getResource(schemaPath);
                return resource.getContentStream();
            } catch (IOException e) {
                throw new AIConnectorException("Error while reading schema from registry", e);
            }
        }
        return null;
    }

    /**
     * Convert PDF to Images and return as Base64 String List
     *
     * @param base64Pdf - Content of the pdf in Base64
     * @return List of images
     */
    public static List<String> pdfToImage(String base64Pdf) throws AIConnectorException {
        try (InputStream pdfInputStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64Pdf))) {
            PDDocument document = PDDocument.load(pdfInputStream);
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            List<BufferedImage> images = new ArrayList<>();
            List<String> encodedImages = new ArrayList<>();

            int numberOfPages = document.getNumberOfPages();

            for (int i = 0; i < numberOfPages; ++i) {
                BufferedImage bImage = pdfRenderer.renderImageWithDPI(i, AIConstants.IMAGE_DPI_DEFAULT, ImageType.RGB);
                images.add(bImage);
            }

            document.close();

            for (BufferedImage image : images) {
                String base64Image = encodeConvertedImage(image);
                encodedImages.add(base64Image);
            }
            return encodedImages;
        } catch (Exception e) {
            throw new AIConnectorException("Error while converting pdf to image , incorrect Base64", e);
        }
    }

    /**
     * Get image message string in OpenAI format
     *
     * @param image base64 string
     * @return Images message string
     */
    public static String getImageMessegeString(String image) {
        return "{\"type\": \"image_url\", \"image_url\": {\"url\": \"data:image/jpeg;base64," +
                image + "\"}}";
    }

    public static String getSchemaContentString(String schemaRegistryPath) throws AIConnectorException {
        // Reading the schema file
        InputStream schemaStream = AIUtils.getSchemaFromRegistry(schemaRegistryPath);
        StringBuilder payloadBuilder = new StringBuilder();

        String schema = "";
        if (schemaStream != null) {
            try {
                schema = IOUtils.toString(schemaStream, String.valueOf(StandardCharsets.UTF_8))
                        .replace("\"", "")
                        .replace("\t", "").replace("\n", "");
            } catch (IOException e) {
                throw new AIConnectorException("Error with the output schema content reading", e);
            }
        }

        // Append common parts of the payload
        payloadBuilder.append("{\"type\": \"text\", \"text\": \" ");

        // Append schema-related text based on whether schema exists
        if (!schema.isEmpty()) {
            payloadBuilder.append(AIConstants.SCHEMA_PROMPT_STRING_1)
                    .append(schema)
                    .append(AIConstants.SCHEMA_PROMPT_STRING_2);
        } else {
            payloadBuilder.append(AIConstants.NO_SCHEMA_PROMPT_STRING);
        }

        payloadBuilder.append("\"}");

        return payloadBuilder.toString();
    }

    /**
     * Receive converted pdf pages as Buffered Images and return In Base64
     *
     * @param image Buffered Image
     * @return Images as Base64
     * @throws IOException
     */
    private static String encodeConvertedImage(BufferedImage image) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * This method will check whether the given response is in the correct format as per the response-schema.
     *
     * @param response The response from OpenAI.
     * @return Whether the response is in the correct format. (true | false)
     */
    public static boolean isInvalidResponse(String response) {
        JsonObject jsonNode = loadJsonData(response);

        if (jsonNode == null || jsonNode.get("data") == null) {
            return true;
        }

        if (jsonNode.get("dataType") != null) {
            String type = jsonNode.get("dataType").getAsString();
            switch (type.toLowerCase()) {
                case "json":
                case "xml":
                case "text":
                    return false;
                default:
                    return true;
            }
        }

        return true;
    }

    public static AIConverserAgentModel getAIConverserAgent(MessageContext mc) throws AIConnectorException {
        Optional<String> prompt = getStringParam(mc, AIConstants.PROMPT_STRING);
        Optional<String> payload = getStringParam(mc, AIConstants.PAYLOAD_STRING);
        String headers = getStringParam(mc, AIConstants.HEADERS_STRING).orElse("");
        Integer retryCount = getIntegerParam(mc, AIConstants.RETRY_COUNT_STRING).orElse(AIConstants.RETRY_COUNT_DEFAULT);

        AIRequestSchema requestSchema = new AIRequestSchema(
                prompt.orElseThrow(() -> new AIConnectorException("Prompt is required.")),
                payload.orElseThrow(() -> new AIConnectorException("Payload is required.")),
                headers
        );

        AIConverserAgentModel agent = new AIConverserAgentModel(requestSchema);
        agent.setRetryCount(retryCount);

        return agent;
    }

    public static AIScannerAgentModel getAIScannerAgent(MessageContext mc) throws AIConnectorException {
        AIScannerAgentModel agent = new AIScannerAgentModel();

        Integer maxTokens = getIntegerParam(mc, AIConstants.MAX_TOKENS).orElse(AIConstants.MAX_TOKENS_DEFAULT);
        String schemaFile = getStringParam(mc, AIConstants.SCANNER_OUTPUT_SCHEMA).orElse("");
        String fileName = getStringParam(mc, AIConstants.FILE_NAME).orElse("");
        String fileContent = getStringParam(mc, AIConstants.FILE_CONTENT).orElse("");

        if (fileName.isEmpty()) {
            throw new AIConnectorException("Cannot find the filename to process");
        } else if (fileContent.isEmpty()) {
            throw new AIConnectorException("Cannot find the content to process");
        }

        agent.setFileName(fileName);
        agent.setFileContent(fileContent);

        if (!(maxTokens > 0)) {
            throw new AIConnectorException("Invalid number of tokens.");
        }

        if (schemaFile.trim().endsWith(".xsd") || schemaFile.trim().
                endsWith(".json") || schemaFile.isEmpty()) {
            agent.setSchemaRegistryPath(schemaFile);
        } else {
            throw new AIConnectorException("Invalid file type, type should be xsd or json");
        }

        agent.setMaxTokens(maxTokens);
        agent.setSchemaRegistryPath(schemaFile);

        return agent;
    }

    /**
     * Read a String parameter
     * @param mc MessageContext.
     * @param parameterKey Key of the parameter.
     * @return Optional String of the parameter value.
     */
    public static Optional<String> getStringParam(MessageContext mc, String parameterKey) {
        String parameter = (String) ConnectorUtils.lookupTemplateParamater(mc, parameterKey);
        if (StringUtils.isNotBlank(parameter)) {
            return Optional.of(parameter);
        }
        return Optional.empty();
    }

    /**
     * Read a Integer parameter
     * @param mc MessageContext.
     * @param parameterKey Key of the parameter.
     * @return Optional Integer of the parameter value.
     */
    public static Optional<Integer> getIntegerParam(MessageContext mc, String parameterKey) {
        Optional<String> parameterValue = getStringParam(mc, parameterKey);
        return parameterValue.map(s -> {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return null;
            }
        });
    }

    /**
     * Retrieves connection name from message context if configured as configKey attribute
     * or from the template parameter
     *
     * @param messageContext Message Context from which the parameters should be extracted from
     * @return connection name
     */
    public static String getConnectionName(MessageContext messageContext) throws AIConnectorException {
        String connectionName = (String) messageContext.getProperty(AIConstants.CONNECTION_NAME);
        if (StringUtils.isBlank(connectionName)) {
            throw new AIConnectorException("Mandatory parameter 'connectionName' is not set.");
        }
        return connectionName;
    }

}
