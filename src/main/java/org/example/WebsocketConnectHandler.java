package org.example;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

public class WebsocketConnectHandler implements RequestHandler<WebsocketConnectRequest, WebsocketResponse> {

    private static final DynamoDbClient DYNAMO_DB_CLIENT = DynamoDbClient.builder()
            .region(Region.US_EAST_1)
            .build();

    @Override
    public WebsocketResponse handleRequest(WebsocketConnectRequest websocketConnectRequest, Context context) {
        String connectionId = websocketConnectRequest.getRequestContext().get("connectionId").toString();
        String loginToken = websocketConnectRequest.getHeaders().get("Login-Token");

        int userId = this.authenticate(loginToken);
        System.out.println("Connection Id: " + connectionId);
        System.out.println("Login Token: " + loginToken);

        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName("connections-xuerong")
                .item(Map.of(
                        "UserId", AttributeValue.builder()
                                .s(String.valueOf(userId))
                                .build(),
                        "ConnectionId", AttributeValue.builder()
                                .s(connectionId)
                                .build()))
                .build();
        DYNAMO_DB_CLIENT.putItem(putItemRequest);
        return new WebsocketResponse();
    }

    public int authenticate(String loginToken) {
        if (loginToken.equals("123")) {
            return 1;
        } else if (loginToken.equals("456")) {
            return 2;
        } else {
            throw new RuntimeException("Invalid login token");
        }
    }
}
