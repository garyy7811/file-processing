SELECT
    Folder.uuid         AS uuid,
    Access.deleted_date AS listDeletedFolderTime
FROM Folder
    INNER JOIN Access ON Access.uuid = Folder.uuid
WHERE Access.deleted_date > :listDeletedFolderTime
ORDER BY modifiedTime ASC