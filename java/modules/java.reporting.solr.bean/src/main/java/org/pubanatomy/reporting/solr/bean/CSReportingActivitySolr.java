package org.pubanatomy.reporting.solr.bean;

import org.apache.solr.client.solrj.beans.Field;

/**
 * User: flashflexpro@gmail.com
 * Date: 11/12/2014
 * Time: 11:26 AM
 */
public class CSReportingActivitySolr extends CSReportingSolrAbstract{

    public static final String JAVA_TYPE = "CSReportingActivitySolr";
    @Field( "pauseReason__si_t_t_f" )
    protected String pauseReason;

    @Field( "pauseBeginOrEnd__i_t_t_f" )
    protected Integer pauseBeginOrEnd;

    @Field( "recTimeStamp__l_t_t_f" )
    protected Long recTimeStamp;

    @Field( "toSlideIndexOfPres__i_t_t_f" )
    protected Integer toSlideIndexOfPres;

    @Field( "query_toSlideName__sw_t_f_f" )
    protected String query_toSlideName;

    @Field( "toSlideName__si_t_t_f" )
    protected String toSlideName;


    @Field( "toSlideRefId__l_t_t_f" )
    protected Long toSlideRefId;


    @Field( "fromSlideIndexOfPres__i_t_t_f" )
    protected Integer fromSlideIndexOfPres;


    @Field( "query_fromSlideName__sw_t_f_f" )
    protected String query_fromSlideName;
    @Field( "fromSlideName__si_t_t_f" )
    protected String fromSlideName;


    @Field( "fromSlideRefId__l_t_t_f" )
    protected Long fromSlideRefId;

    @Field( "clientTime__l_t_t_f" )
    protected Long clientTime;

    @Override
    public String getJavaClassType(){
        return JAVA_TYPE;
    }

    public String getPauseReason(){
        return pauseReason;
    }

    public void setPauseReason( String pauseReason ){
        this.pauseReason = pauseReason;
    }

    public Integer getPauseBeginOrEnd(){
        return pauseBeginOrEnd;
    }

    public void setPauseBeginOrEnd( Integer pauseBeginOrEnd ){
        this.pauseBeginOrEnd = pauseBeginOrEnd;
    }

    public Long getRecTimeStamp(){
        return recTimeStamp;
    }

    public void setRecTimeStamp( Long recTimeStamp ){
        this.recTimeStamp = recTimeStamp;
    }

    public Integer getToSlideIndexOfPres(){
        return toSlideIndexOfPres;
    }

    public void setToSlideIndexOfPres( Integer toSlideIndexOfPres ){
        this.toSlideIndexOfPres = toSlideIndexOfPres;
    }

    public String getQuery_toSlideName(){
        return query_toSlideName;
    }

    public void setQuery_toSlideName( String query_toSlideName ){
        this.query_toSlideName = query_toSlideName;
    }

    public String getToSlideName(){
        return toSlideName;
    }

    public void setToSlideName( String toSlideName ){
        this.toSlideName = toSlideName;
    }

    public Long getToSlideRefId(){
        return toSlideRefId;
    }

    public void setToSlideRefId( Long toSlideRefId ){
        this.toSlideRefId = toSlideRefId;
    }

    public Integer getFromSlideIndexOfPres(){
        return fromSlideIndexOfPres;
    }

    public void setFromSlideIndexOfPres( Integer fromSlideIndexOfPres ){
        this.fromSlideIndexOfPres = fromSlideIndexOfPres;
    }

    public String getQuery_fromSlideName(){
        return query_fromSlideName;
    }

    public void setQuery_fromSlideName( String query_fromSlideName ){
        this.query_fromSlideName = query_fromSlideName;
    }

    public String getFromSlideName(){
        return fromSlideName;
    }

    public void setFromSlideName( String fromSlideName ){
        this.fromSlideName = fromSlideName;
    }

    public Long getFromSlideRefId(){
        return fromSlideRefId;
    }

    public void setFromSlideRefId( Long fromSlideRefId ){
        this.fromSlideRefId = fromSlideRefId;
    }

    public Long getClientTime(){
        return clientTime;
    }

    public void setClientTime( Long clientTime ){
        this.clientTime = clientTime;
    }
}
