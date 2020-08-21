
package com.thinkmicroservices.ri.spring.peer.signaling.jwt;

import java.util.Date;
import java.util.List;
import lombok.Data;
import lombok.Builder;

/**
 *
 * @author cwoodward
 */
@Data
@Builder
public class JWT {
    
    public static final String ROLE_USER="user";
    public static final String ROLE_ADMIN="admin";
    
    private String subject;
    private String accountId;
    private Date issuedAt;
    private Date expiresAt;
    private List<String> roles;
   
    /**
     * 
     * @return 
     */
    public boolean isTokenExpired(){
         
        if(System.currentTimeMillis()>expiresAt.getTime()){
            return true;
        }
        return false;
    }
    
    /**
     * 
     * @param role
     * @return 
     */
    public boolean hasRole(String role){
        return roles.contains(role);
    }
    
    /**
     * 
     * @param roles
     * @return 
     */
    public boolean hasAllRoles(List<String> roles){
        // loop over the the list of roles
        // and return false if any role is missing
        for(String role : roles){
         if (!hasRole(role)){
             return false;
         }
        }
        // no roles were missing.
        return true;
    }
    
    /**
     * 
     * @return 
     */
    public boolean hasAccountId(){
        if(accountId== null){
            return false;
        }
        return true;
    }
}