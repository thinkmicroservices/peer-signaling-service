package com.thinkmicroservices.ri.spring.peer.signaling;

import com.thinkmicroservices.ri.spring.peer.signaling.handler.PeerSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {

    @Autowired
    private PeerSocketHandler peerSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        //map the path to the peer socket handler 
        registry.addHandler(peerSocketHandler, "/peer")
            .setAllowedOrigins("*");
    }
}
