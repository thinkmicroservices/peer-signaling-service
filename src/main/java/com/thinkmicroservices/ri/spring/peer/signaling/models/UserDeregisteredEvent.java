 
package com.thinkmicroservices.ri.spring.peer.signaling.models;

import lombok.Data;

/**
 *
 * @author cwoodward
 */
@Data
public class UserDeregisteredEvent {
   public final String event ="user-deregistered";
   public String data;
}
