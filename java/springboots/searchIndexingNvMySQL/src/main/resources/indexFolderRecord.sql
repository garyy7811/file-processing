SELECT
    Folder.uuid         AS targetUuid,
    Folder.id           AS folderId,
    Folder.is_library   AS isLib,
    User.id             AS ownerId,
    User.first_name     AS ownerFirstName,
    User.last_name      AS ownerLastName,
    User.username       AS ownerUsername,
    Access.createdTime  AS createdTime,
    Access.modifiedTime AS modifiedTime
FROM Folder
    INNER JOIN Access ON Folder.uuid = Access.uuid
    INNER JOIN User ON Access.owner_id = User.id
WHERE Folder.uuid IN (:folderUuidLst)