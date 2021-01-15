package org.pubanatomy.reporting.solr.bean;

import org.apache.solr.client.solrj.beans.Field;

/**
 * User: flashflexpro@gmail.com
 * Date: 11/25/2014
 * Time: 6:20 PM
 */
public abstract class CSReportingSlideCollection extends CSReportingHasUserOwnerDuration{


    @Field( "slideNum__i_t_t_f" )
    protected Integer slideNum;

    @Field( "viewedSlideNum__i_t_t_f" )
    protected Integer viewedSlideNum;

    @Field( "slideIsLibs__b_t_t_t" )
    protected Boolean[] slideIsLibs;


    @Field( "query_slideOwnerClientNames__sw_t_f_t" )
    protected String[] query_slideOwnerClientNames;
    @Field( "slideOwnerClientNames__si_t_t_t" )
    protected String[] slideOwnerClientNames;
    @Field( "slideOwnerClientIds__l_t_t_t" )
    protected Long[] slideOwnerClientIds;


    @Field( "query_slideOwnerGroupNames__sw_t_f_t" )
    protected String[] query_slideOwnerGroupNames;
    @Field( "slideOwnerGroupNames__si_t_t_t" )
    protected String[] slideOwnerGroupNames;
    @Field( "slideOwnerGroupIds__l_t_t_t" )
    protected Long[] slideOwnerGroupIds;


    @Field( "query_slideOwnerEmails__sw_t_f_t" )
    protected String[] query_slideOwnerEmails;
    @Field( "slideOwnerEmails__si_t_t_t" )
    protected String[] slideOwnerEmails;

    @Field( "query_slideOwnerFirstNames__sw_t_f_t" )
    protected String[] query_slideOwnerFirstNames;
    @Field( "slideOwnerFirstNames__si_t_t_t" )
    protected String[] slideOwnerFirstNames;

    @Field( "query_slideOwnerLastNames__sw_t_f_t" )
    protected String[] query_slideOwnerLastNames;
    @Field( "slideOwnerLastNames__si_t_t_t" )
    protected String[] slideOwnerLastNames;

    @Field( "slideOwnerIds__l_t_t_t" )
    protected Long[] slideOwnerIds;


    @Field( "slideIds__l_t_t_t" )
    protected Long[] slideIds;


    @Field( "slideNames__si_t_t_t" )
    protected String[] slideNames;
    @Field( "query_slideNames__sw_t_f_t" )
    protected String[] query_slideNames;


    @Field( "slideRefIds__l_t_t_t" )
    protected Long[] slideRefIds;

    @Field( "slideRefNames__si_t_t_t" )
    protected String[] slideRefNames;
    @Field( "query_slideRefNames__sw_t_f_t" )
    protected String[] query_slideRefNames;

    public Integer getSlideNum(){
        return slideNum;
    }

    public void setSlideNum( Integer slideNum ){
        this.slideNum = slideNum;
    }

    public Integer getViewedSlideNum(){
        return viewedSlideNum;
    }

    public void setViewedSlideNum( Integer viewedSlideNum ){
        this.viewedSlideNum = viewedSlideNum;
    }

    public Boolean[] getSlideIsLibs(){
        return slideIsLibs;
    }

    public void setSlideIsLibs( Boolean[] slideIsLibs ){
        this.slideIsLibs = slideIsLibs;
    }

    public String[] getQuery_slideOwnerClientNames(){
        return query_slideOwnerClientNames;
    }

    public void setQuery_slideOwnerClientNames( String[] query_slideOwnerClientNames ){
        this.query_slideOwnerClientNames = query_slideOwnerClientNames;
    }

    public String[] getSlideOwnerClientNames(){
        return slideOwnerClientNames;
    }

    public void setSlideOwnerClientNames( String[] slideOwnerClientNames ){
        this.slideOwnerClientNames = slideOwnerClientNames;
    }

    public Long[] getSlideOwnerClientIds(){
        return slideOwnerClientIds;
    }

    public void setSlideOwnerClientIds( Long[] slideOwnerClientIds ){
        this.slideOwnerClientIds = slideOwnerClientIds;
    }

    public String[] getQuery_slideOwnerGroupNames(){
        return query_slideOwnerGroupNames;
    }

    public void setQuery_slideOwnerGroupNames( String[] query_slideOwnerGroupNames ){
        this.query_slideOwnerGroupNames = query_slideOwnerGroupNames;
    }

    public String[] getSlideOwnerGroupNames(){
        return slideOwnerGroupNames;
    }

    public void setSlideOwnerGroupNames( String[] slideOwnerGroupNames ){
        this.slideOwnerGroupNames = slideOwnerGroupNames;
    }

    public Long[] getSlideOwnerGroupIds(){
        return slideOwnerGroupIds;
    }

    public void setSlideOwnerGroupIds( Long[] slideOwnerGroupIds ){
        this.slideOwnerGroupIds = slideOwnerGroupIds;
    }

    public String[] getQuery_slideOwnerEmails(){
        return query_slideOwnerEmails;
    }

    public void setQuery_slideOwnerEmails( String[] query_slideOwnerEmails ){
        this.query_slideOwnerEmails = query_slideOwnerEmails;
    }

    public String[] getSlideOwnerEmails(){
        return slideOwnerEmails;
    }

    public void setSlideOwnerEmails( String[] slideOwnerEmails ){
        this.slideOwnerEmails = slideOwnerEmails;
    }

    public String[] getQuery_slideOwnerFirstNames(){
        return query_slideOwnerFirstNames;
    }

    public void setQuery_slideOwnerFirstNames( String[] query_slideOwnerFirstNames ){
        this.query_slideOwnerFirstNames = query_slideOwnerFirstNames;
    }

    public String[] getSlideOwnerFirstNames(){
        return slideOwnerFirstNames;
    }

    public void setSlideOwnerFirstNames( String[] slideOwnerFirstNames ){
        this.slideOwnerFirstNames = slideOwnerFirstNames;
    }

    public String[] getQuery_slideOwnerLastNames(){
        return query_slideOwnerLastNames;
    }

    public void setQuery_slideOwnerLastNames( String[] query_slideOwnerLastNames ){
        this.query_slideOwnerLastNames = query_slideOwnerLastNames;
    }

    public String[] getSlideOwnerLastNames(){
        return slideOwnerLastNames;
    }

    public void setSlideOwnerLastNames( String[] slideOwnerLastNames ){
        this.slideOwnerLastNames = slideOwnerLastNames;
    }

    public Long[] getSlideOwnerIds(){
        return slideOwnerIds;
    }

    public void setSlideOwnerIds( Long[] slideOwnerIds ){
        this.slideOwnerIds = slideOwnerIds;
    }

    public Long[] getSlideRefIds(){
        return slideRefIds;
    }

    public void setSlideRefIds( Long[] slideRefIds ){
        this.slideRefIds = slideRefIds;
    }

    public Long[] getSlideIds(){
        return slideIds;
    }

    public void setSlideIds( Long[] slideIds ){
        this.slideIds = slideIds;
    }

    public String[] getSlideRefNames(){
        return slideRefNames;
    }

    public void setSlideRefNames( String[] slideRefNames ){
        this.slideRefNames = slideRefNames;
    }

    public String[] getQuery_slideRefNames(){
        return query_slideRefNames;
    }

    public void setQuery_slideRefNames( String[] query_slideRefNames ){
        this.query_slideRefNames = query_slideRefNames;
    }

    public String[] getSlideNames(){
        return slideNames;
    }

    public void setSlideNames( String[] slideNames ){
        this.slideNames = slideNames;
    }

    public String[] getQuery_slideNames(){
        return query_slideNames;
    }

    public void setQuery_slideNames( String[] query_slideNames ){
        this.query_slideNames = query_slideNames;
    }
}
