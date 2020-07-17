package com.thinkmicroservices.ri.spring.peer.signaling.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkmicroservices.ri.spring.peer.signaling.models.CallRequestEvent;
import com.thinkmicroservices.ri.spring.peer.signaling.models.CallRequestResponseEvent;
import com.thinkmicroservices.ri.spring.peer.signaling.models.InitiateCallEvent;
import com.thinkmicroservices.ri.spring.peer.signaling.models.UserDeregisteredEvent;
import com.thinkmicroservices.ri.spring.peer.signaling.models.UserRegisteredEvent;
import com.thinkmicroservices.ri.spring.peer.signaling.models.PeerUser;
import com.thinkmicroservices.ri.spring.peer.signaling.service.SignalingDataService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.CloseStatus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Component
@Slf4j
public class PeerSocketHandler extends TextWebSocketHandler {

    private ObjectMapper mapper = new ObjectMapper();

    private static final int EVENT_TYPE_PREFIX_LENGTH = 6;

    private static final int EVENT_TYPE_INDEX = 0;

    private static final int EVENT_DESTINATION_SESSION_ID = 1;
    private static final int EVENT_JSON_MESSAGE_INDEX = 2;

    private static final String SUCCESS = "success";
    private static final String FAILURE = "failure";

    @Autowired
    private SignalingDataService signalingDataService;
    
    @Autowired
    private MeterRegistry meterRegistry;
    private Counter peerAnswerCounter;
    private Counter peerCallRequestCounter;
    private Counter peerAcceptCallCounter;
    private Counter peerCallRequestCanceledCounter;
    private Counter peerCallRequestResponseCounter;
    private Counter peerCandidateCounter;
    private Counter peerDeRegisterCounter;
 
    private Counter peerOfferCounter;
    private Counter peerRegisterCounter;
    private Counter peerRemoteHangupCounter;

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws InterruptedException, IOException {

        InetSocketAddress localAddress = session.getLocalAddress();
        log.debug(">message: {}-{}", localAddress, message);

        if (message.getPayload().startsWith("event-")) {
            processEvent(session, message);
        } else {
            log.warn("Missing event prefix");

        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        this.signalingDataService.addSignalingSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        this.signalingDataService.removePeerUserSession(session, status.getReason());
        try {
            // now send an updated userlist to all active users
            broadcast(this.getRegisteredUserList());
        } catch (IOException ex) {
            log.warn("erro broadcasting user list");
        }
    }

    /**
     * Splits the incoming message into the event, destination session id and
     * the json message payload. The event value is used to call the appropriate
     * event handler with the destination session id.
     *
     * @param session
     * @param message
     */
    private void processEvent(WebSocketSession session, TextMessage message) {

        // event messages are in the form  <event-type|JSON-message>
        //get event type and JSON by splitting the message on the 
        String rawMessage = message.getPayload();
        log.debug("processEvent:{}", rawMessage);
        String[] splitMessage = rawMessage.split("\\|");
        log.debug("splitMessage.length:{}", splitMessage.length);

        String eventType = splitMessage[EVENT_TYPE_INDEX].substring(EVENT_TYPE_PREFIX_LENGTH);

        String destinationSessionId = splitMessage[EVENT_DESTINATION_SESSION_ID];
        String jsonMessage = splitMessage[EVENT_JSON_MESSAGE_INDEX];

        log.debug("processing event");
        log.debug("event-type:{}", eventType);

        log.debug("destination session id:{}", destinationSessionId);
        log.debug("json-message:{}", jsonMessage);

        //PeerUser vu = peerUserIdMap.get(destinationSessionId);
        log.info("signalingDataService:{}", this.signalingDataService);
        PeerUser vu = this.signalingDataService.findPeerUserById(destinationSessionId);

        WebSocketSession destinationSession = null;
        if (vu != null) {
            log.debug("destination video user:{}", vu);
            //destinationSession = webSocketSessionIdMap.get(vu.getSessionId());
            destinationSession = this.signalingDataService.findWebSocketSessionById(vu.getSessionId());
        }
        switch (eventType) {

            case "register-user":
                registerUser(session, jsonMessage);
                break;

            case "de-register-user":
                deRegisterUser(session, jsonMessage);
                break;

            case "get-active-users":
                getRegisteredUserList(session, jsonMessage);
                break;

            case "initiate-call":
                initiateCall(session, destinationSession, jsonMessage);
                break;

            case "call-request":
                callRequest(session, destinationSession, jsonMessage);
                break;

            case "call-request-response":
                callRequestResponse(session, destinationSession, jsonMessage);
                break;

            case "accept-call":
                acceptCall(session, destinationSession, jsonMessage);
                break;

            case "offer":
                offer(session, destinationSession, jsonMessage);
                break;
            case "answer":
                answer(session, destinationSession, jsonMessage);
                break;
            case "candidate":
                candidate(session, destinationSession, jsonMessage);
                break;

            case "call-request-canceled":
                callRequestCanceled(session, destinationSession, jsonMessage);
                break;
                
            case "remote-hangup":
                remoteHangup(session, destinationSession, jsonMessage);
                break;

            default:
                log.warn("Ignoring event '{}'- not a known event type ", eventType);
        }

    }

    /**
     *
     * @param session
     * @param jsonMessage
     */
    private void registerUser(WebSocketSession sourceSession, String jsonMessage) {
        log.debug("register:{}", jsonMessage);
        try {
            PeerUser peerUser = mapper.readValue(jsonMessage, PeerUser.class);
            log.debug("register user:{}", peerUser);
            peerUser.setHostdIpAddress(sourceSession.getLocalAddress().getAddress());

            // check if the 'user id' has already been registered
            PeerUser existingPeerUser=this.signalingDataService.findPeerUserById(peerUser.getId());
            if(existingPeerUser != null){
             // a user has already been registered for this id and we can't allow
             // it to be registered again. We will close this session with a
             // PolicyVolation status.
                log.warn("Attempt to register while a peer with the same id exists:{}",existingPeerUser);
                try{
                   
                sourceSession.sendMessage(new TextMessage(
                        "{"
                        + "\"event\":\"duplicate-registration\","
                        + "\"data\":"
                        + "{}"
                        + "}"
                )
                );
 
                sourceSession.close(CloseStatus.POLICY_VIOLATION);
                }catch(Exception ex){
                    log.warn(ex.getMessage());
                }
                // bail out 
                return;
            }

            // the peer does not exist so register it
            this.signalingDataService.registerPeerUser(sourceSession, peerUser);

            UserRegisteredEvent urEvent = new UserRegisteredEvent();
            urEvent.setData("success");
            sourceSession.sendMessage(new TextMessage(mapper.writeValueAsString(urEvent)));
            peerRegisterCounter.increment(); // update metrics
            // now send an updated list to all active users
            broadcast(this.getRegisteredUserList());

        } catch (IOException ioex) {
            log.warn("unable to register user:{}", jsonMessage);

            try {
                UserRegisteredEvent urEvent = new UserRegisteredEvent();
                urEvent.setData("failure");
                sourceSession.sendMessage(new TextMessage(mapper.writeValueAsString(urEvent)));

            } catch (IOException ioex2) {
                log.error("unable to send user exception:{}", ioex2.getMessage());
            }
        }
    }

    /**
     *
     * @param session
     * @param jsonMessage
     */
    private void deRegisterUser(WebSocketSession sourceSession, String jsonMessage) {
        log.debug("deregister:{}", jsonMessage);
        try {
            PeerUser peerUser = mapper.readValue(jsonMessage, PeerUser.class);
            this.signalingDataService.deRegisterPeerUser(sourceSession, peerUser);
            peerDeRegisterCounter.increment(); // update metrics
            UserDeregisteredEvent udEvent = new UserDeregisteredEvent();
            udEvent.setData(SUCCESS);
            log.debug("deregistered user:{}", peerUser);
            // now send an updated list to all active users
            broadcast(this.getRegisteredUserList());
        } catch (IOException ioexed) {
            log.warn("unable to deregister user:{}", jsonMessage);

            try {
                UserDeregisteredEvent udEvent = new UserDeregisteredEvent();
                udEvent.setData(FAILURE);
                sourceSession.sendMessage(new TextMessage(mapper.writeValueAsString(udEvent)));

            } catch (IOException ioex2) {
                log.error("unable to send user exception:{}", ioex2.getMessage());
            }
        }
    }

    /**
     *
     * @param session
     * @param jsonMessage
     */
    private void getRegisteredUserList(WebSocketSession sourceSession, String jsonMessage) {
        log.debug("getRegisteredUserList:{}", jsonMessage);
        try {
            sourceSession.sendMessage(new TextMessage(this.getRegisteredUserList()));

        } catch (IOException ioexed) {
            log.warn("unable to deregister user:{}", jsonMessage);

            try {
                sourceSession.sendMessage(new TextMessage(
                        "{"
                        + "\"event\":\"active-user-list\","
                        + "\"data\":"
                        + "{}"
                        + "}"
                )
                );

            } catch (IOException ioex2) {
                log.error("unable to send user exception:{}", ioex2.getMessage());
            }
        }
    }

    /**
     *
     * @return
     */
    private String getRegisteredUserList() {

        String userListJson = "{}";
        try {
            userListJson = mapper.writeValueAsString(getSortedActivePeerUserList());
        } catch (JsonProcessingException ex) {
            log.warn("Unable to find any users");
        }
        String json = "{"
                + "\"event\":\"active-user-list\","
                + "\"data\":"
                + userListJson
                + "}";
        return json;
    }

    /**
     *
     * @param sourceSession
     * @param destinationSession
     * @param jsonMessage
     */
    private void initiateCall(WebSocketSession sourceSession, WebSocketSession destinationSession, String jsonMessage) {
        log.debug("initiate call:{},{}, {}", sourceSession.getId(), destinationSession.getId(), jsonMessage);
        try {

            InitiateCallEvent initiateCallEvent = mapper.readValue(jsonMessage, InitiateCallEvent.class);
            log.debug("InitiateCallEvent:{}", initiateCallEvent);

            String callerDisplayName = initiateCallEvent.getCallerDisplayName();

            // get callee web socket session by id
            destinationSession.sendMessage(new TextMessage(mapper.writeValueAsString(initiateCallEvent)));
            peerCallRequestCounter.increment();// update metrics
            
        } catch (IOException ioex) {
            log.error("ex", ioex);
        }

    }

    /**
     *
     * @param sourceSession
     * @param destinationSession
     * @param jsonMessage
     */
    private void callRequest(WebSocketSession sourceSession, WebSocketSession destinationSession, String jsonMessage) {
        log.debug("call request:{},{}, {}", sourceSession.getId(), destinationSession.getId(), jsonMessage);
        try {
            CallRequestEvent callRequestEvent = mapper.readValue(jsonMessage, CallRequestEvent.class);

            log.debug("callRequestEvent:{}", callRequestEvent);
            destinationSession.sendMessage(new TextMessage(mapper.writeValueAsString(callRequestEvent)));
        } catch (IOException ioex) {
            log.error("call request ex", ioex);
        }

    }

    /**
     *
     * @param sourceSession
     * @param destinationSession
     * @param jsonMessage
     */
    private void callRequestResponse(WebSocketSession sourceSession, WebSocketSession destinationSession, String jsonMessage) {
        log.debug("call request response:{},{}, {}", sourceSession.getId(), destinationSession.getId(), jsonMessage);
        try {
            CallRequestResponseEvent callRequestResponseEvent = mapper.readValue(jsonMessage, CallRequestResponseEvent.class);

            log.debug("callRequestResponseEvent:{}", callRequestResponseEvent);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                log.error(ex.getMessage());
            }
            destinationSession.sendMessage(new TextMessage(mapper.writeValueAsString(callRequestResponseEvent)));
            peerCallRequestResponseCounter.increment(); // update metrics
        } catch (IOException ioex) {
            log.error("call request ex", ioex);
        }

    }

    /**
     *
     * @param sourceSession
     * @param destinationSession
     * @param jsonMessage
     */
    private void acceptCall(WebSocketSession sourceSession, WebSocketSession destinationSession, String jsonMessage) {
        peerAcceptCallCounter.increment(); // update metrics
    }

    /**
     *
     * @param session
     * @param destinationSession
     * @param jsonMessage
     */
    private void offer(WebSocketSession session, WebSocketSession destinationSession, String jsonMessage) {
        try {
            destinationSession.sendMessage(new TextMessage(jsonMessage));
            peerOfferCounter.increment(); // update metrics
        } catch (IOException ex) {
            log.error("error in offer:{}", ex);
        }
    }

    /**
     *
     * @param session
     * @param destinationSession
     * @param jsonMessage
     */
    private void answer(WebSocketSession session, WebSocketSession destinationSession, String jsonMessage) {
        try {
            destinationSession.sendMessage(new TextMessage(jsonMessage));
            peerAnswerCounter.increment(); // update metrics
        } catch (IOException ex) {
            log.error("error in answer:{}", ex);
        }
    }

    /**
     *
     * @param session
     * @param destinationSession
     * @param jsonMessage
     */
    private void candidate(WebSocketSession session, WebSocketSession destinationSession, String jsonMessage) {
        try {
            destinationSession.sendMessage(new TextMessage(jsonMessage));
            peerCandidateCounter.increment(); //update metrics
        } catch (IOException ex) {
            log.error("error in candidate:{}", ex);
        }
    }

    /**
     *
     * @param session
     * @param destinationSession
     * @param jsonMessage
     */
    private void callRequestCanceled(WebSocketSession session, WebSocketSession destinationSession, String jsonMessage) {
        log.debug("call-request-canceled:{}", jsonMessage);
        try {
            destinationSession.sendMessage(new TextMessage(jsonMessage));
            peerCallRequestCanceledCounter.increment(); // update metrics
        } catch (IOException ex) {
            log.error("error in candidate:{}", ex);
        }
    }
    
    /**
     *
     * @param session
     * @param destinationSession
     * @param jsonMessage
     */
    private void remoteHangup(WebSocketSession session, WebSocketSession destinationSession, String jsonMessage) {
        log.debug("remote-hangup:{}", jsonMessage);
        try {
            destinationSession.sendMessage(new TextMessage(jsonMessage));
            peerRemoteHangupCounter.increment();
        } catch (IOException ex) {
            log.error("error in candidate:{}", ex);
        }
    }

    /**
     *
     * @return
     */
    private List<PeerUser> getSortedActivePeerUserList() {
        return this.signalingDataService.getActivePeerUsers();

    }

    /**
     * broadcast the supplied json message to all connected users
     *
     * @param jsonMessage
     * @throws IOException
     */
    public void broadcast(String jsonMessage) throws IOException {

        for (WebSocketSession session : this.signalingDataService.getActiveWebsocketSessions()) {
            log.debug("broadcast to session.id{}-{}", session.getId(), jsonMessage);

            session.sendMessage(new TextMessage(jsonMessage));
        }

    }

    @PostConstruct
    public void initializeMetrics() {
        log.info("Signaling Data Service impl: {}", this.signalingDataService);
        log.debug("initializeMetrics");
        
        
        peerAcceptCallCounter = Counter.builder("peer-signaling.peer.accept.call")
                .description("The number of peer-signaling peer answer messages.")
                .register(meterRegistry);
        
        peerAnswerCounter = Counter.builder("peer-signaling.peer.answer")
                .description("The number of peer-signaling peer answer messages.")
                .register(meterRegistry);
        
        peerCallRequestCounter = Counter.builder("peer-signaling.peer.call.request")
                .description("The number of peer-signaling peer call request messages.")
                .register(meterRegistry);
        
        peerCallRequestCanceledCounter = Counter.builder("peer-signaling.peer.call.request.canceled")
                .description("The number of peer-signaling peer call request canceled messages.")
                .register(meterRegistry);
        
        peerCallRequestResponseCounter = Counter.builder("peer-signaling.peer.call.request.response")
                .description("The number of peer-signaling peer call request response messages.")
                .register(meterRegistry);
        
        peerCandidateCounter = Counter.builder("peer-signaling.peer.candidate")
                .description("The number of peer-signaling peer candidate messages.")
                .register(meterRegistry);
        
        peerRegisterCounter = Counter.builder("peer-signaling.peer.register.user")
                .description("The number of peer-signaling peer register user messages.")
                .register(meterRegistry);
        
        peerDeRegisterCounter = Counter.builder("peer-signaling.peer.deregister.user")
                .description("The number of peer-signaling peer de-register user messages.")
                .register(meterRegistry);
        
        peerOfferCounter = Counter.builder("peer-signaling.peer.offer")
                .description("The number of peer-signaling peer offer messages.")
                .register(meterRegistry);
        
        peerRemoteHangupCounter = Counter.builder("peer-signaling.peer.remote.hangup")
                .description("The number of peer-signaling peer remote hangup messages.")
                .register(meterRegistry);
    }

}
