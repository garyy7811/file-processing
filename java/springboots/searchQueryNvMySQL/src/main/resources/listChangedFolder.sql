SELECT
    F.uuid         AS uuid,
    A.modifiedTime AS listChangedFolderTime
FROM Folder F INNER JOIN ResourceFolder RF ON F.id = RF.id
    INNER JOIN Access A
               USE INDEX ( Access_modifiedTime_index ) ON A.uuid = F.uuid
    INNER JOIN User ON A.owner_id = User.id
WHERE
    A.deleted_by IS NULL AND A.deleted_date IS NULL AND A.modifiedTime > :listChangedFolderTime AND A.modifiedTime <= (
        SELECT max(SUBA.modifiedTime) AS maxModifiedTime
        FROM (
                 SELECT A.modifiedTime AS modifiedTime
                 FROM Folder F INNER JOIN ResourceFolder RF ON F.id = RF.id
                     INNER JOIN Access A
                                USE INDEX ( Access_modifiedTime_index ) ON F.uuid = A.uuid
                     INNER JOIN User ON A.owner_id = User.id
                 WHERE A.deleted_by IS NULL AND A.deleted_date IS NULL AND A.modifiedTime > :listChangedFolderTime
                 ORDER BY modifiedTime ASC
                 LIMIT 500) AS SUBA
    )
ORDER BY modifiedTime ASC;