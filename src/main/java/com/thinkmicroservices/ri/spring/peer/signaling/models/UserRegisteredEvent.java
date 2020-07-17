 
package com.thinkmicroservices.ri.spring.peer.signaling.models;

import lombok.Data;

/**
 *
 * @author cwoodward
 */
@Data
public class UserRegisteredEvent {
    private final String event = "user-registered";
    private String data;
}
