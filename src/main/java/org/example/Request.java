package org.example;

import java.util.Map;

import lombok.Data;

@Data
public class Request {
    Map<String, Object> queryStringParameters;
}
