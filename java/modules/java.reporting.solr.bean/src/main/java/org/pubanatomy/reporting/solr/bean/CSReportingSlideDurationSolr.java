package org.pubanatomy.reporting.solr.bean;

import org.apache.solr.client.solrj.beans.Field;

/**
 * User: flashflexpro@gmail.com
 * Date: 11/25/2014
 * Time: 6:20 PM
 */
public class CSReportingSlideDurationSolr extends CSReportingHasUserOwnerDuration{


    public static final String JAVA_TYPE = "CSReportingSlideDurationSolr";

    @Override
    public String getJavaClassType(){
        return JAVA_TYPE;
    }


    @Field( "viewedShowIndex__i_t_t_f" )
    protected Integer viewedShowIndex;

    @Field( "pauseReasons__si_t_t_t" )
    protected String[] pauseReasons;
    @Field( "pauseEnds__l_t_t_t" )
    protected Long[] pauseEnds;
    @Field( "pauseStarts__l_t_t_t" )
    protected Long[] pauseStarts;

    @Field( "query_presentationName__sw_t_f_f" )
    protected String query_presentationName;
    @Field( "presentationName__si_t_t_f" )
    protected String presentationName;

    @Field( "presentationId__l_t_t_f" )
    protected Long presentationId;


    @Field( "query_toSlideRefName__sw_t_f_f" )
    protected String query_toSlideRefName;
    @Field( "toSlideRefName__si_t_t_f" )
    protected String toSlideRefName;

    @Field( "toSlidePresIndex__i_t_t_f" )
    protected Integer toSlidePresIndex;

    @Field( "toSlideRefId__l_t_t_f" )
    protected Long toSlideRefId;

    @Field( "query_fromSlideRefName__sw_t_f_f" )
    protected String query_fromSlideRefName;
    @Field( "fromSlideRefName__si_t_t_f" )
    protected String fromSlideRefName;

    @Field( "fromSlidePresIndex__i_t_t_f" )
    protected Integer fromSlidePresIndex;

    @Field( "fromSlideRefId__l_t_t_f" )
    protected Long fromSlideRefId;

    @Field( "slideShowIndex__i_t_t_f" )
    protected Integer slideShowIndex;

    @Field( "presentationIndex__i_t_t_f" )
    protected Integer presentationIndex;


    @Field( "query_slideRefName__sw_t_f_f" )
    protected String query_slideRefName;
    @Field( "slideRefName__si_t_t_f" )
    protected String slideRefName;

    @Field( "slideRefId__l_t_t_f" )
    protected Long slideRefId;


    @Field( "query_slideName__sw_t_f_f" )
    protected String query_slideName;
    @Field( "slideName__si_t_t_f" )
    protected String slideName;

    @Field( "slideId__l_t_t_f" )
    protected Long slideId;


    @Field( "slideIsLib__b_t_t_f" )
    protected Boolean slideIsLib;


    public Integer getViewedShowIndex(){
        return viewedShowIndex;
    }

    public void setViewedShowIndex( Integer viewedShowIndex ){
        this.viewedShowIndex = viewedShowIndex;
    }

    public String[] getPauseReasons(){
        return pauseReasons;
    }

    public void setPauseReasons( String[] pauseReasons ){
        this.pauseReasons = pauseReasons;
    }

    public Long[] getPauseEnds(){
        return pauseEnds;
    }

    public void setPauseEnds( Long[] pauseEnds ){
        this.pauseEnds = pauseEnds;
    }

    public Long[] getPauseStarts(){
        return pauseStarts;
    }

    public void setPauseStarts( Long[] pauseStarts ){
        this.pauseStarts = pauseStarts;
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

    public String getQuery_toSlideRefName(){
        return query_toSlideRefName;
    }

    public void setQuery_toSlideRefName( String query_toSlideRefName ){
        this.query_toSlideRefName = query_toSlideRefName;
    }

    public String getToSlideRefName(){
        return toSlideRefName;
    }

    public void setToSlideRefName( String toSlideRefName ){
        this.toSlideRefName = toSlideRefName;
    }

    public Integer getToSlidePresIndex(){
        return toSlidePresIndex;
    }

    public void setToSlidePresIndex( Integer toSlidePresIndex ){
        this.toSlidePresIndex = toSlidePresIndex;
    }

    public Long getToSlideRefId(){
        return toSlideRefId;
    }

    public void setToSlideRefId( Long toSlideRefId ){
        this.toSlideRefId = toSlideRefId;
    }

    public String getQuery_fromSlideRefName(){
        return query_fromSlideRefName;
    }

    public void setQuery_fromSlideRefName( String query_fromSlideRefName ){
        this.query_fromSlideRefName = query_fromSlideRefName;
    }

    public String getFromSlideRefName(){
        return fromSlideRefName;
    }

    public void setFromSlideRefName( String fromSlideRefName ){
        this.fromSlideRefName = fromSlideRefName;
    }

    public Integer getFromSlidePresIndex(){
        return fromSlidePresIndex;
    }

    public void setFromSlidePresIndex( Integer fromSlidePresIndex ){
        this.fromSlidePresIndex = fromSlidePresIndex;
    }

    public Long getFromSlideRefId(){
        return fromSlideRefId;
    }

    public void setFromSlideRefId( Long fromSlideRefId ){
        this.fromSlideRefId = fromSlideRefId;
    }

    public Integer getSlideShowIndex(){
        return slideShowIndex;
    }

    public void setSlideShowIndex( Integer slideShowIndex ){
        this.slideShowIndex = slideShowIndex;
    }

    public Integer getPresentationIndex(){
        return presentationIndex;
    }

    public void setPresentationIndex( Integer presentationIndex ){
        this.presentationIndex = presentationIndex;
    }

    public String getQuery_slideRefName(){
        return query_slideRefName;
    }

    public void setQuery_slideRefName( String query_slideRefName ){
        this.query_slideRefName = query_slideRefName;
    }

    public String getSlideRefName(){
        return slideRefName;
    }

    public void setSlideRefName( String slideRefName ){
        this.slideRefName = slideRefName;
    }

    public Long getSlideRefId(){
        return slideRefId;
    }

    public void setSlideRefId( Long slideRefId ){
        this.slideRefId = slideRefId;
    }

    public Long getSlideId(){
        return slideId;
    }

    public void setSlideId( Long slideId ){
        this.slideId = slideId;
    }

    public Boolean getSlideIsLib(){
        return slideIsLib;
    }

    public void setSlideIsLib( Boolean slideIsLib ){
        this.slideIsLib = slideIsLib;
    }

    public String getQuery_slideName(){
        return query_slideName;
    }

    public void setQuery_slideName( String query_slideName ){
        this.query_slideName = query_slideName;
    }

    public String getSlideName(){
        return slideName;
    }

    public void setSlideName( String slideName ){
        this.slideName = slideName;
    }
}
