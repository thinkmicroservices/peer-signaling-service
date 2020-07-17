
package com.thinkmicroservices.ri.spring.peer.signaling.service;

import com.thinkmicroservices.ri.spring.peer.signaling.models.PeerUser;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

/**
 *
 * @author cwoodward
 */
@Service
@Slf4j
@Profile("multi-node")
public class MultiNodeSignalingDataServiceImpl implements SignalingDataService{

    @Override
    public void addSignalingSession(WebSocketSession websocketSession) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removePeerUserSession(WebSocketSession session, String closeStatus) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PeerUser findPeerUserById(String id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public WebSocketSession findWebSocketSessionById(String id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void registerPeerUser(WebSocketSession webSocketSession, PeerUser peerUser) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deRegisterPeerUser(WebSocketSession sourceSession, PeerUser peerUser) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<PeerUser> getActivePeerUsers() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<WebSocketSession> getActiveWebsocketSessions() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
 
    
}
