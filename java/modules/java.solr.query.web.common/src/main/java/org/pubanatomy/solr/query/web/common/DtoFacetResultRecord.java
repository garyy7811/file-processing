package org.pubanatomy.solr.query.web.common;

import java.io.Serializable;
import java.util.HashMap;

/**
 * User: flashflexpro@gmail.com
 * Date: 5/6/2015
 * Time: 3:36 PM
 */
public class DtoFacetResultRecord implements Serializable{

    private String uid;

    public String getUid(){
        return uid;
    }

    public void setUid( String uid ){
        this.uid = uid;
    }

    private HashMap<String, Object> properties = new HashMap<>();

    public HashMap<String, Object> getProperties(){
        return properties;
    }

    public void setProperties( HashMap<String, Object> properties ){
        this.properties = properties;
    }
}
