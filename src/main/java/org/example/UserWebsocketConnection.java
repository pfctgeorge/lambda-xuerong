package org.example;


import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class UserWebsocketConnection {
    private String userId;
    private String connectionId;
}
