package org.pubanatomy.reporting.dto;


/**
 * User: flashflexpro@gmail.com
 * Date: 11/12/2014
 * Time: 3:06 PM
 */
public class DtoReportingShowStart extends DtoReportingActivity{

    public DtoReportingShowStart(){
    }


    private Long userId;
    private String userEmail;

    private Long userClientId;
    private String userClientName;

    private Long userGroupId;
    private String userGroupName;
    protected String userGroupIdPath;
    protected String[] userGroupNamePath;

    private String flArch;
    private String flL;
    private String flOs;
    private Integer flRx;
    private Integer flRy;
    private String flV;
    private String flUrl;

    private String ipAddress;
    private String location;
    private String[] coordinate;

    private String appType;
    private String appVersion;

    private int processingSource = 0;


    private DtoReportingInfoShow showInfo;

    private DtoReportingInfoPres[] presInfos;

    public Long getUserId(){
        return userId;
    }

    public void setUserId( Long userId ){
        this.userId = userId;
    }

    public String getUserEmail(){
        return userEmail;
    }

    public void setUserEmail( String userEmail ){
        this.userEmail = userEmail;
    }

    public Long getUserClientId(){
        return userClientId;
    }

    public void setUserClientId( Long userClientId ){
        this.userClientId = userClientId;
    }

    public String getUserClientName(){
        return userClientName;
    }

    public void setUserClientName( String userClientName ){
        this.userClientName = userClientName;
    }

    public Long getUserGroupId(){
        return userGroupId;
    }

    public void setUserGroupId( Long userGroupId ){
        this.userGroupId = userGroupId;
    }

    public String getUserGroupName(){
        return userGroupName;
    }

    public void setUserGroupName( String userGroupName ){
        this.userGroupName = userGroupName;
    }

    public String getUserGroupIdPath(){
        return userGroupIdPath;
    }

    public void setUserGroupIdPath( String userGroupIdPath ){
        this.userGroupIdPath = userGroupIdPath;
    }

    public String[] getUserGroupNamePath(){
        return userGroupNamePath;
    }

    public void setUserGroupNamePath( String[] userGroupNamePath ){
        this.userGroupNamePath = userGroupNamePath;
    }

    public String getFlArch(){
        return flArch;
    }

    public void setFlArch( String flArch ){
        this.flArch = flArch;
    }

    public String getFlL(){
        return flL;
    }

    public void setFlL( String flL ){
        this.flL = flL;
    }

    public String getFlOs(){
        return flOs;
    }

    public void setFlOs( String flOs ){
        this.flOs = flOs;
    }

    public Integer getFlRx(){
        return flRx;
    }

    public void setFlRx( Integer flRx ){
        this.flRx = flRx;
    }

    public Integer getFlRy(){
        return flRy;
    }

    public void setFlRy( Integer flRy ){
        this.flRy = flRy;
    }

    public String getFlV(){
        return flV;
    }

    public void setFlV( String flV ){
        this.flV = flV;
    }

    public String getFlUrl(){
        return flUrl;
    }

    public void setFlUrl( String flUrl ){
        this.flUrl = flUrl;
    }

    public String getIpAddress(){
        return ipAddress;
    }

    public void setIpAddress( String ipAddress ){
        this.ipAddress = ipAddress;
    }

    public String getLocation(){
        return location;
    }

    public void setLocation( String location ){
        this.location = location;
    }

    public String[] getCoordinate(){
        return coordinate;
    }

    public void setCoordinate( String[] coordinate ){
        this.coordinate = coordinate;
    }

    public String getAppType(){
        return appType;
    }

    public void setAppType( String appType ){
        this.appType = appType;
    }

    public String getAppVersion(){
        return appVersion;
    }

    public void setAppVersion( String appVersion ){
        this.appVersion = appVersion;
    }

    public DtoReportingInfoShow getShowInfo(){
        return showInfo;
    }

    public void setShowInfo( DtoReportingInfoShow showInfo ){
        this.showInfo = showInfo;
    }

    public DtoReportingInfoPres[] getPresInfos(){
        return presInfos;
    }

    public void setPresInfos( DtoReportingInfoPres[] presInfos ){
        this.presInfos = presInfos;
    }

    public int getProcessingSource(){
        return processingSource;
    }

    public void setProcessingSource( int processingSource ){
        this.processingSource = processingSource;
    }
}
