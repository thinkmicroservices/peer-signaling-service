 
package com.thinkmicroservices.ri.spring.peer.signaling.models;

import lombok.Data;

/**
 *
 * @author cwoodward
 */
@Data
public class CallRequestResponseEvent {
    private String event = "call-request-response";
    private boolean accepted;
    
}
