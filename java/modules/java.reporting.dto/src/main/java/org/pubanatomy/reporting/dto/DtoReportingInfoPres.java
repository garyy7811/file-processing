package org.pubanatomy.reporting.dto;


/**
 * User: flashflexpro@gmail.com
 * Date: 11/12/2014
 * Time: 3:06 PM
 */
public class DtoReportingInfoPres extends DtoReportingInfoSlideColOwner{

    public DtoReportingInfoPres(){
    }

    private String presentationName;
    private Long presentationId;
    private Integer[] slidePositions;

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


    public Integer[] getSlidePositions(){
        return slidePositions;
    }

    public void setSlidePositions( Integer[] slidePositions ){
        this.slidePositions = slidePositions;
    }
}
