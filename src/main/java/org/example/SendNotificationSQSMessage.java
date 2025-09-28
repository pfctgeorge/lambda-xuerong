package org.example;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SendNotificationSQSMessage {

    int senderUserId;
    List<Integer> userIdsToNotify;
    int messageId;
}
