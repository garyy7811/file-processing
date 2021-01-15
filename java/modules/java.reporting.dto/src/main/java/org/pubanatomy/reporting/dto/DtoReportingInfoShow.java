package org.pubanatomy.reporting.dto;


/**
 * User: flashflexpro@gmail.com
 * Date: 11/12/2014
 * Time: 3:06 PM
 */
public class DtoReportingInfoShow extends DtoReportingInfoSlideColOwner{

    private String lookupId;
    private Integer[] slidePositionOfPres;

    public DtoReportingInfoShow(){
    }

    private String slideShowName;
    private Long slideShowId;


    protected Integer[] slideIndexOfPres;
    protected Long[] slidePresIds;
    protected String[] slidePresNames;

    public String getSlideShowName(){
        return slideShowName;
    }

    public void setSlideShowName( String slideShowName ){
        this.slideShowName = slideShowName;
    }

    public Long getSlideShowId(){
        return slideShowId;
    }

    public void setSlideShowId( Long slideShowId ){
        this.slideShowId = slideShowId;
    }

    public Integer[] getSlideIndexOfPres(){
        return slideIndexOfPres;
    }

    public void setSlideIndexOfPres( Integer[] slideIndexOfPres ){
        this.slideIndexOfPres = slideIndexOfPres;
    }

    public Long[] getSlidePresIds(){
        return slidePresIds;
    }

    public void setSlidePresIds( Long[] slidePresIds ){
        this.slidePresIds = slidePresIds;
    }

    public String[] getSlidePresNames(){
        return slidePresNames;
    }

    public void setSlidePresNames( String[] slidePresNames ){
        this.slidePresNames = slidePresNames;
    }

    public void setLookupId( String lookupId ){
        this.lookupId = lookupId;
    }

    public String getLookupId(){
        return lookupId;
    }

    public void setSlidePositionOfPres( Integer[] slidePositionOfPres ){
        this.slidePositionOfPres = slidePositionOfPres;
    }

    public Integer[] getSlidePositionOfPres(){
        return slidePositionOfPres;
    }
}
