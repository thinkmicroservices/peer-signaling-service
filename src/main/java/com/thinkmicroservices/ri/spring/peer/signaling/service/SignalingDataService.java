 
package com.thinkmicroservices.ri.spring.peer.signaling.service;

import com.thinkmicroservices.ri.spring.peer.signaling.models.PeerUser;
import java.util.Collection;
import java.util.List;
import org.springframework.web.socket.WebSocketSession;

/**
 *
 * @author cwoodward
 */
public interface SignalingDataService {
    
    /**
     * add the websocket  peer session 
     * @param websocketSession
     * @throws Exception 
     */
    public void addSignalingSession(WebSocketSession websocketSession) throws Exception;
     
    /**
     * 
     * @param session
     * @param closeStatus 
     */
    public void removePeerUserSession(WebSocketSession session, String closeStatus);
    
    /**
     * 
     * @param id
     * @return 
     */
    public PeerUser findPeerUserById(String id);
    
    /**
     * 
     * @param id
     * @return 
     */
    public WebSocketSession findWebSocketSessionById(String id);
    
    /**
     * 
     * @param sourceSession
     * @param peerUser 
     */
    public void registerPeerUser(WebSocketSession webSocketSession, PeerUser peerUser);
    
    
    /**
     * 
     * @param sourceSession
     * @param peerUser 
     */
    public void deRegisterPeerUser(WebSocketSession sourceSession, PeerUser peerUser);
    
    
    /**
     * 
     * @return 
     */
    public List<PeerUser> getActivePeerUsers();
    
    /**
     * 
     * @return 
     */
    public  Collection<WebSocketSession> getActiveWebsocketSessions();
}
