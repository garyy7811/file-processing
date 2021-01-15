package org.pubanatomy.reporting.dto;

/**
 * User: flashflexpro@gmail.com
 * Date: 11/12/2014
 * Time: 3:06 PM
 */
public class DtoReportingActivity extends DtoReportingAbstract{

    public DtoReportingActivity(){
    }

    private Integer fromSlideIndexOfPres;
    private Integer toSlideIndexOfPres;
    private Integer pauseBeginOrEnd = 0;
    private Long fromSlideRefId;
    private Long clientTime;
    private Long toSlideRefId;
    private Long recTimeStamp;
    private String fromSlideName;
    private String toSlideName;
    private String pauseReason;

    public Integer getFromSlideIndexOfPres(){
        return fromSlideIndexOfPres;
    }

    public void setFromSlideIndexOfPres( Integer fromSlideIndexOfPres ){
        this.fromSlideIndexOfPres = fromSlideIndexOfPres;
    }

    public Integer getToSlideIndexOfPres(){
        return toSlideIndexOfPres;
    }

    public void setToSlideIndexOfPres( Integer toSlideIndexOfPres ){
        this.toSlideIndexOfPres = toSlideIndexOfPres;
    }

    public Integer getPauseBeginOrEnd(){
        return pauseBeginOrEnd;
    }

    public void setPauseBeginOrEnd( Integer pauseBeginOrEnd ){
        this.pauseBeginOrEnd = pauseBeginOrEnd;
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

    public Long getToSlideRefId(){
        return toSlideRefId;
    }

    public void setToSlideRefId( Long toSlideRefId ){
        this.toSlideRefId = toSlideRefId;
    }

    public Long getRecTimeStamp(){
        return recTimeStamp;
    }

    public void setRecTimeStamp( Long recTimeStamp ){
        this.recTimeStamp = recTimeStamp;
    }

    public String getFromSlideName(){
        return fromSlideName;
    }

    public void setFromSlideName( String fromSlideName ){
        this.fromSlideName = fromSlideName;
    }

    public String getToSlideName(){
        return toSlideName;
    }

    public void setToSlideName( String toSlideName ){
        this.toSlideName = toSlideName;
    }

    public String getPauseReason(){
        return pauseReason;
    }

    public void setPauseReason( String pauseReason ){
        this.pauseReason = pauseReason;
    }
}
