SELECT
    Ancestor.uuid,
    Ancestor.id,
    Ancestor.name,
    DT.depth
FROM Folder Ancestor INNER JOIN DirectoryTree DT ON Ancestor.id = DT.ancestor_id
    INNER JOIN Folder TargetFolder ON DT.folder_id = TargetFolder.id
WHERE TargetFolder.uuid = :folderUuid
ORDER BY DT.depth DESC;