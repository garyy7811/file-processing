SELECT count(*) AS c
FROM Folder F INNER JOIN ResourceFolder RF ON F.id = RF.id
    INNER JOIN Access A ON A.uuid = F.uuid
    INNER JOIN User ON A.owner_id = User.id
WHERE deleted_by IS NULL AND F.id >= ? AND F.id < ?