package org.pubanatomy.reporting.dto;


import java.io.Serializable;
import java.util.List;

/**
 * User: flashflexpro@gmail.com
 * Date: 11/12/2014
 * Time: 3:06 PM
 */
public abstract class DtoReportingInfoSlideColOwner implements Serializable{

    public DtoReportingInfoSlideColOwner(){
    }

    protected Long ownerId;
    protected String ownerEmail;
    protected String ownerFirstName;
    protected String ownerLastName;

    protected Long ownerClientId;
    protected String ownerClientName;

    protected Long ownerGroupId;
    protected String ownerGroupName;

    protected String ownerGroupIdPath;
    protected String[] ownerGroupNamePath;

    protected Long[] slideRefIds;
    protected String[] slideRefNames;

    protected Boolean[] slideIsLibs;
    protected String[] slideOwnerEmails;
    protected String[] slideOwnerFirstNames;
    protected String[] slideOwnerLastNames;
    protected Long[] slideIds;
    protected String[] slideNames;

    protected Long[] slideOwnerIds;
    protected Long[] slideCreateTimes;
    protected Long[] slideModifiedTimes;

    protected Long[] slideOwnerGroupIds;
    protected String[] slideOwnerGroupNames;
    protected String[] slideOwnerGroupIdPaths;
    protected List<String[]> slideOwnerGroupNamePaths;
    protected Long[] slideOwnerClientIds;
    protected String[] slideOwnerClientNames;

    protected Long lastUpdatedTime;
    protected Long createdTime;


    public Long getOwnerId(){
        return ownerId;
    }

    public void setOwnerId( Long ownerId ){
        this.ownerId = ownerId;
    }

    public String getOwnerEmail(){
        return ownerEmail;
    }

    public void setOwnerEmail( String ownerEmail ){
        this.ownerEmail = ownerEmail;
    }

    public String getOwnerFirstName(){
        return ownerFirstName;
    }

    public void setOwnerFirstName( String ownerFirstName ){
        this.ownerFirstName = ownerFirstName;
    }

    public String getOwnerLastName(){
        return ownerLastName;
    }

    public void setOwnerLastName( String ownerLastName ){
        this.ownerLastName = ownerLastName;
    }

    public Long getOwnerClientId(){
        return ownerClientId;
    }

    public void setOwnerClientId( Long ownerClientId ){
        this.ownerClientId = ownerClientId;
    }

    public String getOwnerClientName(){
        return ownerClientName;
    }

    public void setOwnerClientName( String ownerClientName ){
        this.ownerClientName = ownerClientName;
    }

    public Long getOwnerGroupId(){
        return ownerGroupId;
    }

    public void setOwnerGroupId( Long ownerGroupId ){
        this.ownerGroupId = ownerGroupId;
    }

    public String getOwnerGroupName(){
        return ownerGroupName;
    }

    public void setOwnerGroupName( String ownerGroupName ){
        this.ownerGroupName = ownerGroupName;
    }

    public String getOwnerGroupIdPath(){
        return ownerGroupIdPath;
    }

    public void setOwnerGroupIdPath( String ownerGroupIdPath ){
        this.ownerGroupIdPath = ownerGroupIdPath;
    }

    public String[] getOwnerGroupNamePath(){
        return ownerGroupNamePath;
    }

    public void setOwnerGroupNamePath( String[] ownerGroupNamePath ){
        this.ownerGroupNamePath = ownerGroupNamePath;
    }

    public Long[] getSlideRefIds(){
        return slideRefIds;
    }

    public void setSlideRefIds( Long[] slideRefIds ){
        this.slideRefIds = slideRefIds;
    }

    public String[] getSlideRefNames(){
        return slideRefNames;
    }

    public void setSlideRefNames( String[] slideRefNames ){
        this.slideRefNames = slideRefNames;
    }

    public Boolean[] getSlideIsLibs(){
        return slideIsLibs;
    }

    public void setSlideIsLibs( Boolean[] slideIsLibs ){
        this.slideIsLibs = slideIsLibs;
    }

    public String[] getSlideOwnerEmails(){
        return slideOwnerEmails;
    }

    public void setSlideOwnerEmails( String[] slideOwnerEmails ){
        this.slideOwnerEmails = slideOwnerEmails;
    }

    public String[] getSlideOwnerFirstNames(){
        return slideOwnerFirstNames;
    }

    public void setSlideOwnerFirstNames( String[] slideOwnerFirstNames ){
        this.slideOwnerFirstNames = slideOwnerFirstNames;
    }

    public String[] getSlideOwnerLastNames(){
        return slideOwnerLastNames;
    }

    public void setSlideOwnerLastNames( String[] slideOwnerLastNames ){
        this.slideOwnerLastNames = slideOwnerLastNames;
    }

    public Long[] getSlideIds(){
        return slideIds;
    }

    public void setSlideIds( Long[] slideIds ){
        this.slideIds = slideIds;
    }

    public String[] getSlideNames(){
        return slideNames;
    }

    public void setSlideNames( String[] slideNames ){
        this.slideNames = slideNames;
    }

    public Long[] getSlideOwnerIds(){
        return slideOwnerIds;
    }

    public void setSlideOwnerIds( Long[] slideOwnerIds ){
        this.slideOwnerIds = slideOwnerIds;
    }

    public Long[] getSlideCreateTimes(){
        return slideCreateTimes;
    }

    public void setSlideCreateTimes( Long[] slideCreateTimes ){
        this.slideCreateTimes = slideCreateTimes;
    }

    public Long[] getSlideModifiedTimes(){
        return slideModifiedTimes;
    }

    public void setSlideModifiedTimes( Long[] slideModifiedTimes ){
        this.slideModifiedTimes = slideModifiedTimes;
    }

    public Long[] getSlideOwnerGroupIds(){
        return slideOwnerGroupIds;
    }

    public void setSlideOwnerGroupIds( Long[] slideOwnerGroupIds ){
        this.slideOwnerGroupIds = slideOwnerGroupIds;
    }

    public String[] getSlideOwnerGroupNames(){
        return slideOwnerGroupNames;
    }

    public void setSlideOwnerGroupNames( String[] slideOwnerGroupNames ){
        this.slideOwnerGroupNames = slideOwnerGroupNames;
    }

    public String[] getSlideOwnerGroupIdPaths(){
        return slideOwnerGroupIdPaths;
    }

    public void setSlideOwnerGroupIdPaths( String[] slideOwnerGroupIdPaths ){
        this.slideOwnerGroupIdPaths = slideOwnerGroupIdPaths;
    }

    public List<String[]> getSlideOwnerGroupNamePaths(){
        return slideOwnerGroupNamePaths;
    }

    public void setSlideOwnerGroupNamePaths( List<String[]> slideOwnerGroupNamePaths ){
        this.slideOwnerGroupNamePaths = slideOwnerGroupNamePaths;
    }

    public Long[] getSlideOwnerClientIds(){
        return slideOwnerClientIds;
    }

    public void setSlideOwnerClientIds( Long[] slideOwnerClientIds ){
        this.slideOwnerClientIds = slideOwnerClientIds;
    }

    public String[] getSlideOwnerClientNames(){
        return slideOwnerClientNames;
    }

    public void setSlideOwnerClientNames( String[] slideOwnerClientNames ){
        this.slideOwnerClientNames = slideOwnerClientNames;
    }

    public Long getLastUpdatedTime(){
        return lastUpdatedTime;
    }

    public void setLastUpdatedTime( Long lastUpdatedTime ){
        this.lastUpdatedTime = lastUpdatedTime;
    }

    public Long getCreatedTime(){
        return createdTime;
    }

    public void setCreatedTime( Long createdTime ){
        this.createdTime = createdTime;
    }
}
