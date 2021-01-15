package org.pubanatomy.reporting.solr.bean;

import org.apache.solr.client.solrj.beans.Field;

/**
 * User: flashflexpro@gmail.com
 * Date: 11/25/2014
 * Time: 6:20 PM
 */
public class CSReportingPresDurationSolr extends CSReportingSlideCollection{

    public static final String JAVA_TYPE = "CSReportingPresDurationSolr";
    /**
     * unlike slide which has a start, an end which defines an atom duration, presentation can have many slides
     * because user can jump between slides in different presentations,
     * <p/>
     * so this is an accumulation of all its slides's duration
     */


    @Field( "query_presentationName__sw_t_f_f" )
    protected String query_presentationName;
    @Field( "presentationName__si_t_t_f" )
    protected String presentationName;


    @Field( "presentationId__l_t_t_f" )
    protected Long presentationId;

    @Override
    public String getJavaClassType(){
        return JAVA_TYPE;
    }

    public String getQuery_presentationName(){
        return query_presentationName;
    }

    public void setQuery_presentationName( String query_presentationName ){
        this.query_presentationName = query_presentationName;
    }

    public String getPresentationName(){
        return presentationName;
    }

    public void setPresentationName( String presentationName ){
        this.presentationName = presentationName;
    }

    public Long getPresentationId(){
        return presentationId;
    }

    public void setPresentationId( Long presentationId ){
        this.presentationId = presentationId;
    }

    @Override
    public Long getEndTime(){
        return null;
    }

    @Override
    public void setEndTime( Long endTime ){
        throw new IllegalArgumentException();
    }

    @Override
    public Long getStartTime(){
        return null;
    }

    @Override
    public void setStartTime( Long startTime ){
        throw new IllegalArgumentException();
    }
}
