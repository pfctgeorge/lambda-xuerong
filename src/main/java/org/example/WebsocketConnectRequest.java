package org.example;

import java.util.Map;

import lombok.Data;

@Data
public class WebsocketConnectRequest {
    private Map<String, String> headers;
    private Map<String, Object> requestContext;
}
