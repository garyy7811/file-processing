package org.pubanatomy.reporting.solr.bean;

import org.apache.solr.client.solrj.beans.Field;

/**
 * User: flashflexpro@gmail.com
 * Date: 11/25/2014
 * Time: 6:20 PM
 */
public class CSReportingShowDurationSolr extends CSReportingSlideCollection{


    public static final String JAVA_TYPE = "CSReportingShowDurationSolr";

    @Field( "preview__b_t_t_f" )
    protected Boolean preview;

    @Field( "processingSource__i_t_t_f" )
    protected int processingSource = 0;

    @Field( "query_prestOwnerClientNames__sw_t_f_t" )
    protected String[] query_prestOwnerClientNames;
    @Field( "prestOwnerClientNames__si_t_t_t" )
    protected String[] prestOwnerClientNames;
    @Field( "prestOwnerClientIds__l_t_t_t" )
    protected Long[] prestOwnerClientIds;
    @Field( "query_prestOwnerGroupNames__sw_t_f_t" )
    protected String[] query_prestOwnerGroupNames;
    @Field( "prestOwnerGroupNames__si_t_t_t" )
    protected String[] prestOwnerGroupNames;
    @Field( "prestOwnerGroupIds__l_t_t_t" )
    protected Long[] prestOwnerGroupIds;


    @Field( "query_prestOwnerEmails__sw_t_f_t" )
    protected String[] query_prestOwnerEmails;
    @Field( "prestOwnerEmails__si_t_t_t" )
    protected String[] prestOwnerEmails;

    @Field( "query_prestOwneFirstNames__sw_t_f_t" )
    protected String[] query_prestOwnerFirstNames;
    @Field( "prestOwnerFirstNames__si_t_t_t" )
    protected String[] prestOwnerFirstNames;

    @Field( "query_prestOwnerLastNames__sw_t_f_t" )
    protected String[] query_prestOwnerLastNames;
    @Field( "prestOwnerLastNames__si_t_t_t" )
    protected String[] prestOwnerLastNames;


    @Field( "prestOwnerIds__l_t_t_t" )
    protected Long[] prestOwnerIds;
    @Field( "query_prestNames__sw_t_f_t" )
    protected String[] query_prestNames;
    @Field( "prestNames__si_t_t_t" )
    protected String[] prestNames;
    @Field( "prestIds__l_t_t_t" )
    protected Long[] prestIds;
    @Field( "query_slidePresNames__sw_t_f_t" )
    protected String[] query_slidePresNames;
    @Field( "slidePresNames__si_t_t_t" )
    protected String[] slidePresNames;
    @Field( "slideIndexOfPres__i_t_t_t" )
    protected Integer[] slideIndexOfPres;
    @Field( "slidePositionOfPres__i_t_t_t" )
    protected Integer[] slidePositionOfPres;
    @Field( "slidePresIds__l_t_t_t" )
    protected Long[] slidePresIds;

    @Field( "appType__si_t_t_f" )
    protected String appType;

    @Field( "appTypeWebViewer__i_t_t_f" )
    protected Integer appTypeWebViewer;

    @Field( "appTypeDesktopViewer__i_t_t_f" )
    protected Integer appTypeDesktopViewer;

    @Field( "appTypeIpadViewer__i_t_t_f" )
    protected Integer appTypeIpadViewer;

    @Field( "appVersion__si_t_t_f" )
    protected String appVersion;
    @Field( "flRy__i_t_t_f" )
    protected Integer flRy;
    @Field( "flRx__i_t_t_f" )
    protected Integer flRx;
    @Field( "flArch__si_t_t_f" )
    protected String flArch;
    @Field( "flV__si_t_t_f" )
    protected String flV;
    @Field( "flL__si_t_t_f" )
    protected String flL;
    @Field( "query_flOs__sw_t_f_f" )
    protected String query_flOs;
    @Field( "flOs__si_t_t_f" )
    protected String flOs;


    @Field( "flUrl__si_t_t_f" )
    protected String flUrl;

    @Field( "lookUpId__si_t_t_f" )
    protected String lookUpId;


    @Override
    public String getJavaClassType(){
        return JAVA_TYPE;
    }

    public Boolean getPreview(){
        return preview;
    }

    public void setPreview( Boolean preview ){
        this.preview = preview;
    }

    public int getProcessingSource(){
        return processingSource;
    }

    public void setProcessingSource( int processingSource ){
        this.processingSource = processingSource;
    }

    public String[] getQuery_prestOwnerClientNames(){
        return query_prestOwnerClientNames;
    }

    public void setQuery_prestOwnerClientNames( String[] query_prestOwnerClientNames ){
        this.query_prestOwnerClientNames = query_prestOwnerClientNames;
    }

    public String[] getPrestOwnerClientNames(){
        return prestOwnerClientNames;
    }

    public void setPrestOwnerClientNames( String[] prestOwnerClientNames ){
        this.prestOwnerClientNames = prestOwnerClientNames;
    }

    public Long[] getPrestOwnerClientIds(){
        return prestOwnerClientIds;
    }

    public void setPrestOwnerClientIds( Long[] prestOwnerClientIds ){
        this.prestOwnerClientIds = prestOwnerClientIds;
    }

    public String[] getQuery_prestOwnerGroupNames(){
        return query_prestOwnerGroupNames;
    }

    public void setQuery_prestOwnerGroupNames( String[] query_prestOwnerGroupNames ){
        this.query_prestOwnerGroupNames = query_prestOwnerGroupNames;
    }

    public String[] getPrestOwnerGroupNames(){
        return prestOwnerGroupNames;
    }

    public void setPrestOwnerGroupNames( String[] prestOwnerGroupNames ){
        this.prestOwnerGroupNames = prestOwnerGroupNames;
    }

    public Long[] getPrestOwnerGroupIds(){
        return prestOwnerGroupIds;
    }

    public void setPrestOwnerGroupIds( Long[] prestOwnerGroupIds ){
        this.prestOwnerGroupIds = prestOwnerGroupIds;
    }

    public String[] getQuery_prestOwnerEmails(){
        return query_prestOwnerEmails;
    }

    public void setQuery_prestOwnerEmails( String[] query_prestOwnerEmails ){
        this.query_prestOwnerEmails = query_prestOwnerEmails;
    }

    public String[] getPrestOwnerEmails(){
        return prestOwnerEmails;
    }

    public void setPrestOwnerEmails( String[] prestOwnerEmails ){
        this.prestOwnerEmails = prestOwnerEmails;
    }

    public String[] getQuery_prestOwnerFirstNames(){
        return query_prestOwnerFirstNames;
    }

    public void setQuery_prestOwnerFirstNames( String[] query_prestOwnerFirstNames ){
        this.query_prestOwnerFirstNames = query_prestOwnerFirstNames;
    }

    public String[] getPrestOwnerFirstNames(){
        return prestOwnerFirstNames;
    }

    public void setPrestOwnerFirstNames( String[] prestOwnerFirstNames ){
        this.prestOwnerFirstNames = prestOwnerFirstNames;
    }

    public String[] getQuery_prestOwnerLastNames(){
        return query_prestOwnerLastNames;
    }

    public void setQuery_prestOwnerLastNames( String[] query_prestOwnerLastNames ){
        this.query_prestOwnerLastNames = query_prestOwnerLastNames;
    }

    public String[] getPrestOwnerLastNames(){
        return prestOwnerLastNames;
    }

    public void setPrestOwnerLastNames( String[] prestOwnerLastNames ){
        this.prestOwnerLastNames = prestOwnerLastNames;
    }

    public Long[] getPrestOwnerIds(){
        return prestOwnerIds;
    }

    public void setPrestOwnerIds( Long[] prestOwnerIds ){
        this.prestOwnerIds = prestOwnerIds;
    }

    public String[] getQuery_prestNames(){
        return query_prestNames;
    }

    public void setQuery_prestNames( String[] query_prestNames ){
        this.query_prestNames = query_prestNames;
    }

    public String[] getPrestNames(){
        return prestNames;
    }

    public void setPrestNames( String[] prestNames ){
        this.prestNames = prestNames;
    }

    public Long[] getPrestIds(){
        return prestIds;
    }

    public void setPrestIds( Long[] prestIds ){
        this.prestIds = prestIds;
    }

    public String[] getQuery_slidePresNames(){
        return query_slidePresNames;
    }

    public void setQuery_slidePresNames( String[] query_slidePresNames ){
        this.query_slidePresNames = query_slidePresNames;
    }

    public String[] getSlidePresNames(){
        return slidePresNames;
    }

    public void setSlidePresNames( String[] slidePresNames ){
        this.slidePresNames = slidePresNames;
    }

    public Integer[] getSlideIndexOfPres(){
        return slideIndexOfPres;
    }

    public void setSlideIndexOfPres( Integer[] slideIndexOfPres ){
        this.slideIndexOfPres = slideIndexOfPres;
    }

    public Integer[] getSlidePositionOfPres(){
        return slidePositionOfPres;
    }

    public void setSlidePositionOfPres( Integer[] slidePositionOfPres ){
        this.slidePositionOfPres = slidePositionOfPres;
    }

    public Long[] getSlidePresIds(){
        return slidePresIds;
    }

    public void setSlidePresIds( Long[] slidePresIds ){
        this.slidePresIds = slidePresIds;
    }

    public String getAppType(){
        return appType;
    }

    public void setAppType( String appType ){
        if( appType == null || "CustomShow/Viewer/Type/".equals( appType ) ){
            appType = "csViewerWeb";
            setAppTypeWebViewer( 1 );
        }
        else if( appType.toLowerCase().indexOf( "ipad" ) > 0 ){
            setAppTypeIpadViewer( 1 );
        }
        else if( appType.toLowerCase().indexOf( "air" ) > 0 ){
            setAppTypeDesktopViewer( 1 );
        }
        this.appType = appType;
    }

    public Integer getAppTypeWebViewer(){
        return appTypeWebViewer;
    }

    public void setAppTypeWebViewer( Integer appTypeWebViewer ){
        this.appTypeWebViewer = appTypeWebViewer;
    }

    public Integer getAppTypeDesktopViewer(){
        return appTypeDesktopViewer;
    }

    public void setAppTypeDesktopViewer( Integer appTypeDesktopViewer ){
        this.appTypeDesktopViewer = appTypeDesktopViewer;
    }

    public Integer getAppTypeIpadViewer(){
        return appTypeIpadViewer;
    }

    public void setAppTypeIpadViewer( Integer appTypeIpadViewer ){
        this.appTypeIpadViewer = appTypeIpadViewer;
    }

    public String getAppVersion(){
        return appVersion;
    }

    public void setAppVersion( String appVersion ){
        this.appVersion = appVersion;
    }

    public Integer getFlRy(){
        return flRy;
    }

    public void setFlRy( Integer flRy ){
        this.flRy = flRy;
    }

    public Integer getFlRx(){
        return flRx;
    }

    public void setFlRx( Integer flRx ){
        this.flRx = flRx;
    }

    public String getFlArch(){
        return flArch;
    }

    public void setFlArch( String flArch ){
        this.flArch = flArch;
    }

    public String getFlV(){
        return flV;
    }

    public void setFlV( String flV ){
        this.flV = flV;
    }

    public String getFlL(){
        return flL;
    }

    public void setFlL( String flL ){
        this.flL = flL;
    }

    public String getQuery_flOs(){
        return query_flOs;
    }

    public void setQuery_flOs( String query_flOs ){
        this.query_flOs = query_flOs;
    }

    public String getFlOs(){
        return flOs;
    }

    public void setFlOs( String flOs ){
        this.flOs = flOs;
    }

    public String getLookUpId(){
        return lookUpId;
    }

    public void setLookUpId( String lookUpId ){
        this.lookUpId = lookUpId;
    }

    public void setFlUrl( String flUrl ){
        this.flUrl = flUrl;
    }

    public String getFlUrl(){
        return flUrl;
    }
}
