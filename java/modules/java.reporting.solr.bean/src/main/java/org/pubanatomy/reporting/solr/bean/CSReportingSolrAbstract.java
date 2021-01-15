package org.pubanatomy.reporting.solr.bean;

import org.pubanatomy.solr.query.common.SolrBean;
import org.apache.solr.client.solrj.beans.Field;

/**
 * User: flashflexpro@gmail.com
 * Date: 11/12/2014
 * Time: 11:26 AM
 */
public abstract class CSReportingSolrAbstract extends SolrBean{

    /*
     * meeting ??
     */

    @Field( "flexClientId__si_t_t_f" )
    protected String flexClientId;


    @Field( "showSessionId__si_t_t_f" )
    protected String showSessionId;


    @Field( "clientSessionId__si_t_t_f" )
    protected String clientSessionId;

    public String getFlexClientId(){
        return flexClientId;
    }

    public void setFlexClientId( String flexClientId ){
        this.flexClientId = flexClientId;
    }

    public String getShowSessionId(){
        return showSessionId;
    }

    public void setShowSessionId( String showSessionId ){
        this.showSessionId = showSessionId;
    }

    public String getClientSessionId(){
        return clientSessionId;
    }

    public void setClientSessionId( String clientSessionId ){
        this.clientSessionId = clientSessionId;
    }
}
