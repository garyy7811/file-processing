SELECT f.uuid
FROM Folder AS f INNER JOIN Folder AS p ON f.parent_id = p.id
    INNER JOIN ResourceFolder RF ON f.id = RF.id
WHERE p.uuid IN (:folderUuidLst)