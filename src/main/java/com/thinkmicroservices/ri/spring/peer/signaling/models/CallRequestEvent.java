 
package com.thinkmicroservices.ri.spring.peer.signaling.models;

import lombok.Data;

/**
 *
 * @author cwoodward
 */
@Data
public class CallRequestEvent extends SessionRoutingInfo{
    
     private String event="call-request";
}
