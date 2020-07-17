 
package com.thinkmicroservices.ri.spring.peer.signaling.models;

import lombok.Data;

/**
 *
 * @author cwoodward
 */
@Data
public class SessionRoutingInfo {
    private String callerDisplayName;
    private String callerId;
    private String calleeId;
}
