package org.pubanatomy.reporting.dto;

import java.io.Serializable;

/**
 * User: flashflexpro@gmail.com
 * Date: 11/25/2014
 * Time: 9:00 PM
 */
public abstract class DtoReportingAbstract implements Serializable{

    private String uid;
    private String clientSessionId;
    private String showSessionId;
    private String flexClientId;

    public String getUid(){
        return uid;
    }

    public void setUid( String uid ){
        this.uid = uid;
    }

    public String getClientSessionId(){
        return clientSessionId;
    }

    public void setClientSessionId( String clientSessionId ){
        this.clientSessionId = clientSessionId;
    }

    public String getShowSessionId(){
        return showSessionId;
    }

    public void setShowSessionId( String showSessionId ){
        this.showSessionId = showSessionId;
    }

    public String getFlexClientId(){
        return flexClientId;
    }

    public void setFlexClientId( String flexClientId ){
        this.flexClientId = flexClientId;
    }
}
