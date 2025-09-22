package org.example;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageNotification {

    int senderUserId;
    Integer groupChatId;
    Integer receiverUserId;
    String getObjectRequestPresignedUrl;
}
