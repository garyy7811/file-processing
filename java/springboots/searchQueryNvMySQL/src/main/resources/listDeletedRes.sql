SELECT
    SR.uuid        AS uuid,
    A.deleted_date AS listDeletedResTime
FROM SlideResource SR INNER JOIN Access A ON SR.uuid = A.uuid
WHERE deleted_date > :listDeletedResTime
ORDER BY deleted_date ASC