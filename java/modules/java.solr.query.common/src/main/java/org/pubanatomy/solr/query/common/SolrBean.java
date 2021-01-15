package org.pubanatomy.solr.query.common;

import org.apache.solr.client.solrj.beans.Field;

import java.io.Serializable;

/**
 * User: flashflexpro@gmail.com
 * Date: 4/2/2015
 * Time: 12:24 PM
 */
public abstract class SolrBean implements Serializable{

    @Field
    public void setJavaClassType( String javaClassType ){
    }

    public abstract String getJavaClassType();

    @Field
    private String uid;

    @Field
    private String[] enstcText;

    public String getUid(){
        return uid;
    }

    public void setUid( String uid ){
        this.uid = uid;
    }

    public String[] getEnstcText(){
        return enstcText;
    }

    public void setEnstcText( String[] enstcText ){
        this.enstcText = enstcText;
    }
}
