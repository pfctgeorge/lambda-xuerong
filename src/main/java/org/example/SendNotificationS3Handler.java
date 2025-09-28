package org.example;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.apigatewaymanagementapi.ApiGatewayManagementApiClient;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.GoneException;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.PostToConnectionRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;


@Log4j2
public class SendNotificationS3Handler implements RequestHandler<SQSEvent, Void> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final DynamoDbClient DYNAMO_DB_CLIENT = DynamoDbClient.builder()
            .region(Region.US_EAST_1)
            .build();

    private static final S3Client S3_CLIENT = S3Client.builder()
            .region(Region.US_EAST_1)
            .build();

    private static final S3Presigner S3_PRESIGNER = S3Presigner.builder()
            .region(Region.US_EAST_1)
            .s3Client(S3_CLIENT)
            .build();

    private static final ApiGatewayManagementApiClient API_GATEWAY_MANAGEMENT_API_CLIENT =
            ApiGatewayManagementApiClient.builder()
                    .region(Region.US_EAST_1)
                    .endpointOverride(URI.create("https://g0vlclj5f0.execute-api.us-east-1.amazonaws.com/production/"))
                    .build();

    private static final String BUCKET_NAME = "jianjin-messaging-user-file";

    @Override
    public Void handleRequest(SQSEvent sqsEvent, Context context) {
        for (SQSEvent.SQSMessage record : sqsEvent.getRecords()) {
            var messageBody = record.getBody();
            try {
                SendNotificationSQSMessage sendNotificationSQSMessage = OBJECT_MAPPER.readValue(
                        messageBody,
                        SendNotificationSQSMessage.class);
                this.sendNotification(sendNotificationSQSMessage.getSenderUserId(),
                                      sendNotificationSQSMessage.getUserIdsToNotify(),
                                      sendNotificationSQSMessage.getMessageId());
            } catch (Exception exception) {
                log.warn("Encountered exception when processing SQS record: {}. Reason: {}",
                         messageBody, exception.getMessage(), exception);
                System.out.println(exception.getMessage());
            }
        }
        return null;
    }


    private void sendNotification(int senderUserId,
                                  List<Integer> userIdsToNotify,
                                  int messageId) throws Exception {
        List<UserWebsocketConnection> userWebsocketConnections = this.getConnectionIdsByUserIds(userIdsToNotify);
        if (userWebsocketConnections.isEmpty()) {
            return;
        }
        // Build the GetObject request
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(String.valueOf(messageId))
                .build();

        // Define how long the presigned URL should be valid
        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10)) // URL valid for 10 minutes
                .getObjectRequest(getObjectRequest)
                .build();

        // Generate presigned request
        PresignedGetObjectRequest presignedGetObjectRequest =
                S3_PRESIGNER.presignGetObject(getObjectPresignRequest);

        String url = presignedGetObjectRequest.url().toString();

        MessageNotification messageNotification = new MessageNotification(senderUserId,
                                                                          url);

        for (var userWebsocketConnection : userWebsocketConnections) {
            log.info("Sending message to userWebsocketConnection: {}", userWebsocketConnection);
            try {
                PostToConnectionRequest postToConnectionRequest = PostToConnectionRequest.builder()
                        .connectionId(userWebsocketConnection.getConnectionId())
                        .data(SdkBytes.fromUtf8String(OBJECT_MAPPER.writeValueAsString(messageNotification)))
                        .build();
                this.API_GATEWAY_MANAGEMENT_API_CLIENT.postToConnection(postToConnectionRequest);
            } catch (GoneException goneException) {
                log.info("ConnectionId {} is gone. Deleting connection from DynamoDb", userWebsocketConnection);
                DeleteItemRequest deleteItemRequest = DeleteItemRequest.builder()
                        .tableName("connections-xuerong")
                        .key(Map.of(
                                "UserId",
                                AttributeValue.builder().s(String.valueOf(userWebsocketConnection.getUserId())).build(),
                                "ConnectionId",
                                AttributeValue.builder().s(userWebsocketConnection.getConnectionId()).build()))
                        .build();
                DYNAMO_DB_CLIENT.deleteItem(deleteItemRequest);
            }
        }
    }

    private List<UserWebsocketConnection> getConnectionIdsByUserIds(List<Integer> userIdsToNotify) {

        List<UserWebsocketConnection> userWebsocketConnections = new ArrayList<>();
        for (Integer userId : userIdsToNotify) {
            var queryResponse = DYNAMO_DB_CLIENT.query(QueryRequest.builder()
                                                               .tableName("connections-xuerong")
                                                               .keyConditions(Map.of("UserId",
                                                                                     Condition.builder()
                                                                                             .attributeValueList(
                                                                                                     AttributeValue.builder()
                                                                                                             .s(String.valueOf(
                                                                                                                     userId))
                                                                                                             .build())
                                                                                             .comparisonOperator(
                                                                                                     ComparisonOperator.EQ) // connectionId == userId
                                                                                             .build()))
                                                               .build());
            userWebsocketConnections.addAll(queryResponse.items()
                                                    .stream()
                                                    .map(item ->
                                                                 new UserWebsocketConnection(item.get("UserId").s(),
                                                                                             item.get("ConnectionId")
                                                                                                     .s()))
                                                    .toList());
        }
        return userWebsocketConnections;
    }
}

