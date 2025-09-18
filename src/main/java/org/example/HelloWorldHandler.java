package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class HelloWorldHandler implements RequestHandler<Request, Response> {

    @Override
    public Response handleRequest(Request request, Context context) {
        int sum = request.getQueryStringParameters().values().stream()
                .mapToInt(value -> Integer.parseInt(value.toString())).sum();
        return new Response(String.valueOf(sum));
    }
}
