package org.example;

import java.util.Map;

import lombok.Data;

@Data
public class WebsocketResponse {
    private String statusCode = "200";
    private Map<String, String> headers = Map.of();
    private String body = "";
    private boolean isBase64Encoded = false;
}
