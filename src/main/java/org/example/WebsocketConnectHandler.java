package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class WebsocketConnectHandler implements RequestHandler<WebsocketConnectRequest, WebsocketResponse> {
    @Override
    public WebsocketResponse handleRequest(WebsocketConnectRequest websocketConnectRequest, Context context) {
        String connectionId = websocketConnectRequest.getRequestContext().get("connectionId").toString();
        String loginToken = websocketConnectRequest.getHeaders().get("Login-Token");

        int userId = this.authenticate(loginToken);
        System.out.println("Connection Id: " + connectionId);
        System.out.println("Login Token: " + loginToken);

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
