SELECT count(*) AS c
FROM SlideResource SR INNER JOIN Access A ON SR.uuid = A.uuid
    INNER JOIN User U ON A.owner_id = U.id
    INNER JOIN SlideResourceContent SRC ON SR.current_content_id = SRC.id
    INNER JOIN Folder F ON SR.folder_id = F.id
    INNER JOIN Thumbnail T ON SRC.thumbnail_id = T.id
WHERE deleted_by IS NULL AND SR.id >= ? AND SR.id < ?