
SELECT SR.uuid
FROM SlideResource AS SR
    INNER JOIN Folder ON SR.folder_id = Folder.id
WHERE Folder.uuid in (:folderUuidLst)