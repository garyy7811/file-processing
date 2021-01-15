package org.pubanatomy.reporting.dto;


/**
 * User: flashflexpro@gmail.com
 * Date: 11/12/2014
 * Time: 3:06 PM
 */
public class DtoReportingShowEnd extends DtoReportingActivity{

    public static final String SESSION_TERMINATER = "-terminate";

    public DtoReportingShowEnd(){
    }

    private Boolean termination;

    public Boolean getTermination(){
        return termination;
    }

    public void setTermination( Boolean termination ){
        this.termination = termination;
    }
}
