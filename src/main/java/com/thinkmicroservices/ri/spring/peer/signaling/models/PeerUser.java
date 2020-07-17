 
package com.thinkmicroservices.ri.spring.peer.signaling.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.net.InetAddress;
import lombok.Data;

/**
 *
 * @author cwoodward
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PeerUser implements Serializable, Comparable<PeerUser>{
    public String id;
    public String displayName;
    public String sessionId; 
    // the host address the user's websocket is attached to
    public InetAddress hostdIpAddress; 

    @Override
    public int compareTo(PeerUser vu) {
        return this.displayName.compareTo(vu.getDisplayName());
            
        
    }
    
}
