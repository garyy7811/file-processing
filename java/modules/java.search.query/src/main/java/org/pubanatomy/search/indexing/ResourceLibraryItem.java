package org.pubanatomy.search.indexing;

import java.util.Date;

import lombok.Data;

/**
 * User: GaryY
 * Date: 7/27/2017
 */
@Data
public class ResourceLibraryItem{
    private String  targetUuid;
    private Integer targetId;
    private Integer folderId;

    private String  targetName;
    private Boolean isLib;
    private String  targetType;
    private Integer ownerId;
    private String  ownerName;
    private String  ownerFirstName;
    private String  ownerLastName;
    private String  ownerUsername;
    private Date    createdTime;
    private Date    modifiedTime;
    private Integer resourceWidth;
    private Integer resourceHeight;
    private Long    filesize;
    private String  orgFilename;

    private Integer resourceDuration;
    private String  uuidPath;
    private String  namePath;

    private Integer thumbnailWidth;
    private Integer thumbnailHeight;
    private String  thumbnailFileName;

    private Date indexedTime;
}
