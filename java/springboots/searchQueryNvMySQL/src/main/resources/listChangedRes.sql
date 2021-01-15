SELECT
    SR.uuid        AS uuid,
    A.modifiedTime AS listChangedResTime
FROM SlideResource SR INNER JOIN Access A
                                 USE INDEX ( Access_modifiedTime_index ) ON SR.uuid = A.uuid
    INNER JOIN User U ON A.owner_id = U.id
    INNER JOIN SlideResourceContent SRC ON SR.current_content_id = SRC.id
    INNER JOIN Folder F ON SR.folder_id = F.id
    INNER JOIN Thumbnail T ON SRC.thumbnail_id = T.id
WHERE A.deleted_by IS NULL AND A.deleted_date IS NULL AND A.modifiedTime > :listChangedResTime AND A.modifiedTime <= (
    SELECT max(SUBA.modifiedTime) AS maxModifiedTime
    FROM (
             SELECT A.modifiedTime AS modifiedTime
             FROM SlideResource SR INNER JOIN Access A
                                              USE INDEX ( Access_modifiedTime_index ) ON SR.uuid = A.uuid
                 INNER JOIN User U ON A.owner_id = U.id
                 INNER JOIN SlideResourceContent SRC ON SR.current_content_id = SRC.id
                 INNER JOIN Folder F ON SR.folder_id = F.id
                 INNER JOIN Thumbnail T ON SRC.thumbnail_id = T.id
             WHERE A.deleted_by IS NULL AND A.deleted_date IS NULL AND A.modifiedTime > :listChangedResTime
             ORDER BY modifiedTime ASC
             LIMIT 500) AS SUBA
)
ORDER BY modifiedTime ASC;