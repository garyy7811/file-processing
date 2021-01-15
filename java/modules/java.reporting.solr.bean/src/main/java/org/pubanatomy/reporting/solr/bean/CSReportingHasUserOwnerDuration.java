package org.pubanatomy.reporting.solr.bean;

import org.apache.solr.client.solrj.beans.Field;

/**
 * User: flashflexpro@gmail.com
 * Date: 11/25/2014
 * Time: 6:20 PM
 */
public abstract class CSReportingHasUserOwnerDuration extends CSReportingSolrAbstract{


    @Field( "query_slideShowName__sw_t_f_f" )
    protected String query_slideShowName;


    @Field( "slideShowName__si_t_t_f" )
    protected String slideShowName;


    @Field( "slideShowId__l_t_t_f" )
    protected Long slideShowId;


    @Field( "query_userClientName__sw_t_f_f" )
    protected String query_userClientName;


    @Field( "userClientName__si_t_t_f" )
    protected String userClientName;


    @Field( "userClientId__l_t_t_f" )
    protected Long userClientId;


    @Field( "query_userGroupName__sw_t_f_f" )
    protected String query_userGroupName;


    @Field( "userGroupName__si_t_t_f" )
    protected String userGroupName;


    @Field( "userGroupId__l_t_t_f" )
    protected Long userGroupId;

    @Field( "userGroupIdPath__si_t_t_f" )
    protected String userGroupIdPath;

    @Field( "userGroupNamePath__si_t_t_t" )
    protected String[] userGroupNamePath;

    @Field( "query_userEmail__sw_t_f_f" )
    protected String query_userEmail;


    @Field( "userEmail__si_t_t_f" )
    protected String userEmail;


    @Field( "userEmailDomain__si_t_t_f" )
    protected String userEmailDomain;


    @Field( "userId__l_t_t_f" )
    protected Long userId;


    @Field( "query_ownerClientName__sw_t_f_f" )
    protected String query_ownerClientName;

    @Field( "ownerClientName__si_t_t_f" )
    protected String ownerClientName;

    @Field( "ownerClientId__l_t_t_f" )
    protected Long ownerClientId;


    @Field( "query_ownerGroupName__sw_t_f_f" )
    protected String query_ownerGroupName;

    @Field( "ownerGroupName__si_t_t_f" )
    protected String ownerGroupName;

    @Field( "ownerGroupId__l_t_t_f" )
    protected Long ownerGroupId;

    @Field( "ownerGroupIdPath__si_t_t_f" )
    protected String ownerGroupIdPath;

    @Field( "ownerGroupNamePath__si_t_t_t" )
    protected String[] ownerGroupNamePath;

    @Field( "query_ownerEmail__sw_t_f_f" )
    protected String query_ownerEmail;

    @Field( "ownerEmail__si_t_t_f" )
    protected String ownerEmail;


    @Field( "ownerEmailDomain__si_t_t_f" )
    protected String ownerEmailDomain;


    @Field( "query_ownerFirstName__sw_t_f_f" )
    protected String query_ownerFirstName;

    @Field( "ownerFirstName__si_t_t_f" )
    protected String ownerFirstName;


    @Field( "query_ownerLastName__sw_t_f_f" )
    protected String query_ownerLastName;

    @Field( "ownerLastName__si_t_t_f" )
    protected String ownerLastName;


    @Field( "ownerId__l_t_t_f" )
    protected Long ownerId;


    @Field( "durationNet__l_t_t_f" )
    protected Long durationNet;
    @Field( "duration__l_t_t_f" )
    protected Long duration;


    @Field( "createTime__l_t_t_f" )
    protected Long createTime;

    @Field( "lastUpdatedTime__l_t_t_f" )
    protected Long lastUpdatedTime;

    @Field( "endTime__l_t_t_f" )
    protected Long endTime;
    @Field( "startTime__l_t_t_f" )
    protected Long startTime;


    @Field( "query_ipAddress__sw_t_f_f" )
    protected String query_ipAddress;
    @Field( "ipAddress__si_t_t_f" )
    protected String ipAddress;


    @Field( "query_location__sw_t_f_f" )
    protected String query_location;
    @Field( "location__si_t_t_f" )
    protected String location;


    @Field( "query_coordinate__sw_t_f_t" )
    protected String[] query_coordinate;
    @Field( "coordinate__si_t_t_t" )
    protected String[] coordinate;


    public String getQuery_slideShowName(){
        return query_slideShowName;
    }

    public void setQuery_slideShowName( String query_slideShowName ){
        this.query_slideShowName = query_slideShowName;
    }

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

    public String getQuery_userClientName(){
        return query_userClientName;
    }

    public void setQuery_userClientName( String query_userClientName ){
        this.query_userClientName = query_userClientName;
    }

    public String getUserClientName(){
        return userClientName;
    }

    public void setUserClientName( String userClientName ){
        this.userClientName = userClientName;
    }

    public Long getUserClientId(){
        return userClientId;
    }

    public void setUserClientId( Long userClientId ){
        this.userClientId = userClientId;
    }

    public String getQuery_userGroupName(){
        return query_userGroupName;
    }

    public void setQuery_userGroupName( String query_userGroupName ){
        this.query_userGroupName = query_userGroupName;
    }

    public String getUserGroupName(){
        return userGroupName;
    }

    public void setUserGroupName( String userGroupName ){
        this.userGroupName = userGroupName;
    }

    public Long getUserGroupId(){
        return userGroupId;
    }

    public void setUserGroupId( Long userGroupId ){
        this.userGroupId = userGroupId;
    }

    public String getQuery_userEmail(){
        return query_userEmail;
    }

    public void setQuery_userEmail( String query_userEmail ){
        this.query_userEmail = query_userEmail;
    }

    public String getUserEmail(){
        return userEmail;
    }

    public void setUserEmail( String userEmail ){
        this.userEmail = userEmail;
    }

    public String getUserEmailDomain(){
        return userEmailDomain;
    }

    public void setUserEmailDomain( String userEmailDomain ){
        this.userEmailDomain = userEmailDomain;
    }

    public Long getUserId(){
        return userId;
    }

    public void setUserId( Long userId ){
        this.userId = userId;
    }

    public String getQuery_ownerClientName(){
        return query_ownerClientName;
    }

    public void setQuery_ownerClientName( String query_ownerClientName ){
        this.query_ownerClientName = query_ownerClientName;
    }

    public String getOwnerClientName(){
        return ownerClientName;
    }

    public void setOwnerClientName( String ownerClientName ){
        this.ownerClientName = ownerClientName;
    }

    public Long getOwnerClientId(){
        return ownerClientId;
    }

    public void setOwnerClientId( Long ownerClientId ){
        this.ownerClientId = ownerClientId;
    }

    public String getQuery_ownerGroupName(){
        return query_ownerGroupName;
    }

    public void setQuery_ownerGroupName( String query_ownerGroupName ){
        this.query_ownerGroupName = query_ownerGroupName;
    }

    public String getOwnerGroupName(){
        return ownerGroupName;
    }

    public void setOwnerGroupName( String ownerGroupName ){
        this.ownerGroupName = ownerGroupName;
    }

    public Long getOwnerGroupId(){
        return ownerGroupId;
    }

    public void setOwnerGroupId( Long ownerGroupId ){
        this.ownerGroupId = ownerGroupId;
    }

    public String getQuery_ownerEmail(){
        return query_ownerEmail;
    }

    public void setQuery_ownerEmail( String query_ownerEmail ){
        this.query_ownerEmail = query_ownerEmail;
    }

    public String getOwnerEmail(){
        return ownerEmail;
    }

    public void setOwnerEmail( String ownerEmail ){
        this.ownerEmail = ownerEmail;
    }

    public String getOwnerEmailDomain(){
        return ownerEmailDomain;
    }

    public void setOwnerEmailDomain( String ownerEmailDomain ){
        this.ownerEmailDomain = ownerEmailDomain;
    }

    public String getQuery_ownerFirstName(){
        return query_ownerFirstName;
    }

    public void setQuery_ownerFirstName( String query_ownerFirstName ){
        this.query_ownerFirstName = query_ownerFirstName;
    }

    public String getOwnerFirstName(){
        return ownerFirstName;
    }

    public void setOwnerFirstName( String ownerFirstName ){
        this.ownerFirstName = ownerFirstName;
    }

    public String getQuery_ownerLastName(){
        return query_ownerLastName;
    }

    public void setQuery_ownerLastName( String query_ownerLastName ){
        this.query_ownerLastName = query_ownerLastName;
    }

    public String getOwnerLastName(){
        return ownerLastName;
    }

    public void setOwnerLastName( String ownerLastName ){
        this.ownerLastName = ownerLastName;
    }

    public Long getOwnerId(){
        return ownerId;
    }

    public void setOwnerId( Long ownerId ){
        this.ownerId = ownerId;
    }

    public Long getDurationNet(){
        return durationNet;
    }

    public void setDurationNet( Long durationNet ){
        this.durationNet = durationNet;
    }

    public Long getDuration(){
        return duration;
    }

    public void setDuration( Long duration ){
        this.duration = duration;
    }

    public Long getCreateTime(){
        return createTime;
    }

    public void setCreateTime( Long createTime ){
        this.createTime = createTime;
    }

    public Long getLastUpdatedTime(){
        return lastUpdatedTime;
    }

    public void setLastUpdatedTime( Long lastUpdatedTime ){
        this.lastUpdatedTime = lastUpdatedTime;
    }

    public Long getEndTime(){
        return endTime;
    }

    public void setEndTime( Long endTime ){
        this.endTime = endTime;
    }

    public Long getStartTime(){
        return startTime;
    }

    public void setStartTime( Long startTime ){
        this.startTime = startTime;
    }

    public String getQuery_ipAddress(){
        return query_ipAddress;
    }

    public void setQuery_ipAddress( String query_ipAddress ){
        this.query_ipAddress = query_ipAddress;
    }

    public String getIpAddress(){
        return ipAddress;
    }

    public void setIpAddress( String ipAddress ){
        this.ipAddress = ipAddress;
    }

    public String getQuery_location(){
        return query_location;
    }

    public void setQuery_location( String query_location ){
        this.query_location = query_location;
    }

    public String getLocation(){
        return location;
    }

    public void setLocation( String location ){
        this.location = location;
    }

    public String[] getQuery_coordinate(){
        return query_coordinate;
    }

    public void setQuery_coordinate( String[] query_coordinate ){
        this.query_coordinate = query_coordinate;
    }

    public String[] getCoordinate(){
        return coordinate;
    }

    public void setCoordinate( String[] coordinate ){
        this.coordinate = coordinate;
    }

    public String getOwnerGroupIdPath() {
        return ownerGroupIdPath;
    }

    public void setOwnerGroupIdPath(String ownerGroupIdPath) {
        this.ownerGroupIdPath = ownerGroupIdPath;
    }

    public String[] getOwnerGroupNamePath() {
        return ownerGroupNamePath;
    }

    public void setOwnerGroupNamePath(String[] ownerGroupNamePath) {
        this.ownerGroupNamePath = ownerGroupNamePath;
    }

    public String getUserGroupIdPath() {
        return userGroupIdPath;
    }

    public void setUserGroupIdPath(String userGroupIdPath) {
        this.userGroupIdPath = userGroupIdPath;
    }

    public String[] getUserGroupNamePath() {
        return userGroupNamePath;
    }

    public void setUserGroupNamePath(String[] userGroupNamePath) {
        this.userGroupNamePath = userGroupNamePath;
    }
}
