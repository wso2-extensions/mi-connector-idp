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
package org.wso2.carbon.connector.idp.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.synapse.MessageContext;
import org.wso2.integration.connector.core.util.ConnectorUtils;
import org.wso2.carbon.connector.idp.constants.AIConstants;
import org.wso2.carbon.connector.idp.exception.AIConnectorException;
import org.wso2.carbon.connector.idp.model.scan.AIScannerAgentModel;
import org.wso2.micro.integrator.registry.MicroIntegratorRegistry;
import org.wso2.micro.integrator.registry.Resource;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;


public class AIUtils {

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
                String jsonPath_1 = "gov:mi-resources/idp-schemas/" + schemaPath + "/" + schemaPath + ".json";
                String jsonPath_2 = "gov:idp-schemas/" + schemaPath + "/" + schemaPath + ".json"; // for backward compatibility

                if (registry.isResourceExists(jsonPath_1)) {
                    Resource resource = registry.getResource(jsonPath_1);
                    return resource.getContentStream();
                } else if (registry.isResourceExists(jsonPath_2)) {
                    Resource resource = registry.getResource(jsonPath_2);
                    return resource.getContentStream();
                } else {
                    throw new AIConnectorException("Schema not found in the registry: " + schemaPath);
                }
            } catch (IOException e) {
                throw new AIConnectorException("Error while reading schema from registry", e);
            }
        }
        return null;
    }

    /**
     * Converts a Base64 encoded PDF string into a List of Base64 encoded PNG image strings.
     *
     * @param base64Pdf Content of the PDF in Base64.
     * @return A List of strings, where each string is a Base64 encoded PNG image of a page.
     * @throws AIConnectorException if there is an error reading or converting the PDF data.
     */
    public static List<String> pdfToImage(String base64Pdf) throws AIConnectorException {
        List<String> encodedImages = new ArrayList<>();

        try {
            final byte[] pdfData = Base64.getDecoder().decode(base64Pdf);
            try (PDDocument document = Loader.loadPDF(pdfData)) {
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                for (int page = 0; page < document.getNumberOfPages(); ++page) {
                    BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        ImageIO.write(bufferedImage, "png", baos); 
                        String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
                        encodedImages.add(base64Image);
                    }
                }
            } 
            return encodedImages;
        } catch (IllegalArgumentException e) {
            throw new AIConnectorException("Error converting PDF: The provided string is not valid Base64.", e);
        } catch (IOException e) {
            throw new AIConnectorException("Error during I/O operation while converting PDF to image.", e);
        }
    }

    public static String getSchemaContentString(String schemaRegistryPath) throws AIConnectorException {
        InputStream schemaStream = AIUtils.getSchemaFromRegistry(schemaRegistryPath);
        String schema = "";
        if (schemaStream != null) {
            try {
                schema = IOUtils.toString(schemaStream, String.valueOf(StandardCharsets.UTF_8))
                        .replace("\"", "")
                        .replace("\t", "")
                        .replace("\n", "");
            } catch (IOException e) {
                throw new AIConnectorException("Error with the output schema content reading", e);
            }
        }
        return schema;
    }

    public static String constructDataUriIfBase64(String fileContent, String mimeType, String contentFormat) {
        if (contentFormat != null && contentFormat.trim().equalsIgnoreCase("base64")
                && StringUtils.isNotBlank(fileContent) && StringUtils.isNotBlank(mimeType)) {
            return "data:" + mimeType + ";base64," + fileContent;
        }
        return fileContent;
    }


    public static AIScannerAgentModel getAIScannerAgent(MessageContext mc) throws AIConnectorException {
        AIScannerAgentModel agent = new AIScannerAgentModel();

        Integer maxTokens = getIntegerParam(mc, AIConstants.MAX_TOKENS).orElse(AIConstants.MAX_TOKENS_DEFAULT);
        String schemaFile = getStringParam(mc, AIConstants.SCANNER_OUTPUT_SCHEMA).orElse("");
        String fileContent = getStringParam(mc, AIConstants.FILE_CONTENT).orElse("");
        String mimeType = getStringParam(mc, AIConstants.MIME_TYPE).orElse("");
        String contentFormat = getStringParam(mc, AIConstants.CONTENT_FORMAT).orElse("");

        fileContent = constructDataUriIfBase64(fileContent, mimeType, contentFormat);
        agent.setFileContent(fileContent);

        if (!(maxTokens > 0)) {
            throw new AIConnectorException("Invalid number of tokens.");
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

