
package com.thinkmicroservices.ri.spring.peer.signaling.models;

import lombok.Data;

/**
 *
 * @author cwoodward
 */
@Data
public class InitiateCallEvent extends SessionRoutingInfo{
    private final String event="initiate-call";
    private String callerDisplayName;
    
}
