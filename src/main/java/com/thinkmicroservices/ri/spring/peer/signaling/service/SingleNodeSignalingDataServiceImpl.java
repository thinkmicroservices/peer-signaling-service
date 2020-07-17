package com.thinkmicroservices.ri.spring.peer.signaling.service;

import com.thinkmicroservices.ri.spring.peer.signaling.models.PeerUser;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

/**
 *
 * @author cwoodward
 */
@Service
@Slf4j
@Profile("single-node")
public class SingleNodeSignalingDataServiceImpl implements SignalingDataService {

     @Autowired
    private MeterRegistry meterRegistry;
     
    private Map<String, PeerUser> peerUserIdMap = new ConcurrentHashMap<>();
    //private List<WebSocketSession> webSocketSessions = new CopyOnWriteArrayList<>();
    private Map<String, WebSocketSession> webSocketSessionIdMap = new ConcurrentHashMap<>();
    private Counter sessionConnectionCounter;
    private Counter sessionDisconnectionCounter;
    private Counter peerRegistrationCounter;
    private Counter peerDeRegistrationCounter;

    @Override
    public void addSignalingSession(WebSocketSession websocketSession) throws Exception {
        logDataSizes("addSignalingSession-pre");
        
        log.debug("conference connection established:" + websocketSession.getId());
        // add to the map to facilitate websocket session lookup by id
        webSocketSessionIdMap.put(websocketSession.getId(), websocketSession);
        logDataSizes("addSignalingSession-post");
        sessionConnectionCounter.increment();
    }

    @Override
    public void removePeerUserSession(WebSocketSession session, String closeStatus) {
       // webSocketSessions.remove(session);
        log.debug("conference  connection closed:" + session.getId() + "/" + closeStatus);
        logDataSizes("removePeerUserSession-pre");
        
        webSocketSessionIdMap.remove(session.getId());

        // remove video user
        Set<String> keySet = peerUserIdMap.keySet();
        for (String key : keySet) {
            PeerUser element = peerUserIdMap.get(key);
            if (element.getSessionId().equals(session.getId())) {
                log.debug("evicting:{}>{}", key, element);
                peerUserIdMap.remove(key);
                log.debug("remove from peerUserIdMap:{}",key);

            }
        }
        sessionDisconnectionCounter.increment();
        logDataSizes("removePeerUserSession-post");

    }

    private void logDataSizes(String prefix) {
        log.debug("{}:peerUserIdMap:{},{}",prefix, peerUserIdMap.size(),peerUserIdMap.keySet());
        log.debug("{}:webSocketSessionIdMap:{},{}", prefix,webSocketSessionIdMap.size(),webSocketSessionIdMap.keySet());
    }

    @Override
    public PeerUser findPeerUserById(String peerId) {
        return peerUserIdMap.get(peerId);
    }

    @Override
    public WebSocketSession findWebSocketSessionById(String webSocketSessionId) {
        return webSocketSessionIdMap.get(webSocketSessionId);
    }

    @Override
    public void registerPeerUser(WebSocketSession webSocketSession, PeerUser peerUser) {

        // add session to video user if it doesnt exist in the map
        if (peerUserIdMap.get(peerUser.getId()) == null) {
            // set session id in in user
            peerUser.setSessionId(webSocketSession.getId());
            // add  user to map
            peerUserIdMap.put(peerUser.getId(), peerUser);

            // set source session in session map
           // webSocketSessionIdMap.put(webSocketSession.getId(), webSocketSession);
            log.debug("registered user:{}", peerUser);
            peerRegistrationCounter.increment();
        }

    }

    @Override
    public void deRegisterPeerUser(WebSocketSession sourceSession, PeerUser peerUser) {
        peerUserIdMap.remove(peerUser.getId());
        webSocketSessionIdMap.remove(sourceSession.getId());
        peerDeRegistrationCounter.increment();
    }

    @Override
    public List<PeerUser> getActivePeerUsers() {
        Set<String> keys = peerUserIdMap.keySet();
        List<PeerUser> list = new ArrayList<>();
        for (String key : keys) {
            list.add(peerUserIdMap.get(key));
        }

        Collections.sort(list);
        return list;

    }

    @Override
    public Collection<WebSocketSession> getActiveWebsocketSessions() {
        
        return this.webSocketSessionIdMap.values();
    }
    
    
    @PostConstruct
    private void initializeMetrics(){
        
        sessionConnectionCounter = Counter.builder("peer-signaling.connection")
                .description("The number of peer-signaling connection events.")
                .register(meterRegistry);
        
        sessionDisconnectionCounter = Counter.builder("peer-signaling.disconnection")
                .description("The number of peer-signaling disconnection events.")
                .register(meterRegistry);
        
        peerRegistrationCounter = Counter.builder("peer-signaling.registration")
                .description("The number of peer-signaling peer registration events.")
                .register(meterRegistry);
        
        peerDeRegistrationCounter = Counter.builder("peer-signaling.deregistration")
                .description("The number of peer-signaling peer de-registration events.")
                .register(meterRegistry);
    }

}
