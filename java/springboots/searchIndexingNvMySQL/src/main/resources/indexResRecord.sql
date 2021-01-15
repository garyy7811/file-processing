SELECT
    SR.uuid               AS targetUuid,
    SR.id                 AS targetId,
    SR.folder_id          AS folderId,
    SR.is_library         AS isLib,
    SR.resource_type      AS targetType,
    SR.name               AS targetName,
    U.id                  AS ownerId,
    U.first_name          AS ownerFirstName,
    U.last_name           AS ownerLastName,
    U.username            AS ownerUsername,
    A.createdTime         AS createdTime,
    A.modifiedTime        AS modifiedTime,
    SRC.width             AS resourceWidth,
    SRC.height            AS resourceHeight,
    SRC.filesize          AS filesize,
    SRC.original_filename AS orgFilename,
    SRC.metadata          AS metadata,
    T.fileName            AS thumbnailFileName,
    T.width               AS thumbnailWidth,
    T.height              AS thumbnailHeight
FROM SlideResource SR INNER JOIN Access A ON SR.uuid = A.uuid
    INNER JOIN User U ON A.owner_id = U.id
    INNER JOIN SlideResourceContent SRC ON SR.current_content_id = SRC.id
    INNER JOIN Thumbnail T ON SRC.thumbnail_id = T.id
WHERE SR.uuid IN (:resUuidLst)