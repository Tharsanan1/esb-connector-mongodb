/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.connector.operations;

import com.mongodb.MongoException;
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.MessageContext;
import org.bson.BsonInvalidOperationException;
import org.bson.Document;
import org.wso2.carbon.connector.connection.MongoConnection;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.connector.core.connection.ConnectionHandler;
import org.wso2.carbon.connector.exception.MongoConnectorException;
import org.wso2.carbon.connector.utils.MongoConstants;
import org.wso2.carbon.connector.utils.MongoUtils;
import org.wso2.carbon.connector.utils.SimpleMongoClient;

/**
 * Class mediator for inserting one document.
 * For more information, see https://docs.mongodb.com/manual/reference/method/db.collection.insertOne
 */
public class InsertOne extends AbstractConnector {

    private static final String COLLECTION = "collection";
    private static final String DOCUMENT = "document";
    private static final String INSERT_ONE_RESULT = "{\"InsertOneResult\":\"Successful\"}";
    private static final String INVALID_MONGODB_CONFIG_MESSAGE = "MongoDB connection has not been instantiated.";
    private static final String EMPTY_DOCUMENT_MESSAGE = "The document to be inserted cannot be null or empty.";
    private static final String INVALID_DOCUMENT_MESSAGE = "The document to be inserted cannot be a JSON array. Please provide a JSON object.";
    private static final String ERROR_MESSAGE = "Error occurred while inserting the document to the database";

    @Override
    public void connect(MessageContext messageContext) throws ConnectException {

        ConnectionHandler handler = ConnectionHandler.getConnectionHandler();
        SimpleMongoClient simpleMongoClient;

        String collection = (String) getParameter(messageContext, COLLECTION);
        String stringDocument = (String) getParameter(messageContext, DOCUMENT);

        if (StringUtils.isEmpty(stringDocument)) {
            MongoConnectorException e = new MongoConnectorException(EMPTY_DOCUMENT_MESSAGE);
            MongoUtils.handleError(messageContext, e, MongoConstants.MONGODB_CONNECTIVITY, e.getMessage());
        }

        try {
            String connectionName = MongoUtils.getConnectionName(messageContext);
            MongoConnection mongoConnection = (MongoConnection) handler.getConnection(MongoConstants.CONNECTOR_NAME, connectionName);
            simpleMongoClient = mongoConnection.getSimpleMongoClient();

            if (simpleMongoClient == null) {
                throw new MongoConnectorException(INVALID_MONGODB_CONFIG_MESSAGE);
            }

            Document document = Document.parse(stringDocument);

            simpleMongoClient.insertOneDocument(collection, document);

            if (log.isDebugEnabled()) {
                log.debug(INSERT_ONE_RESULT);
            }
            MongoUtils.setPayload(messageContext, INSERT_ONE_RESULT);

        } catch (BsonInvalidOperationException e) {
            MongoUtils.handleError(messageContext, e, MongoConstants.MONGODB_CONNECTIVITY, INVALID_DOCUMENT_MESSAGE);

        } catch (IllegalArgumentException e) {
            MongoUtils.handleError(messageContext, e, MongoConstants.MONGODB_CONNECTIVITY, ERROR_MESSAGE);

        } catch (MongoException e) {
            MongoUtils.handleError(messageContext, e, e.getCode(), ERROR_MESSAGE);

        } catch (Exception e) {
            MongoUtils.handleError(messageContext, e, MongoConstants.MONGODB_UNKNOWN_EXCEPTION, ERROR_MESSAGE);
        }
    }
}