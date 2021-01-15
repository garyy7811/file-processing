package org.pubanatomy.siutils;

import java.io.Serializable;

/**
 * User: GaryY
 * Date: 2/15/2017
 */
public class RpcError implements Serializable{

    private int    code;
    private String message;

    public RpcError(){
    }

    public RpcError( int code, String message ){
        this.code = code;
        this.message = message;
    }

    public int getCode(){
        return code;
    }

    public void setCode( int code ){
        this.code = code;
    }

    public String getMessage(){
        return message;
    }

    public void setMessage( String message ){
        this.message = message;
    }
}
