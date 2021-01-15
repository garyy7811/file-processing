-- this query is used to locate all fonts missing the 'Regular' font face, which currently cannot
-- be used on the iPad.
SELECT *
FROM 
(
SELECT C.name, CCF.id as ccf_id, CF.font_display_name, CF.font_face_name, CF.swf_file_name, 
	count(FID.id) as numFaces, sum(FID.regular) as has_reg, GROUP_CONCAT(FID.bold) as has_bold, GROUP_CONCAT(FID.italic) as has_italic, GROUP_CONCAT(FID.bold_italic) AS has_bi
FROM ClientCustomFont CCF
	INNER JOIN Client C ON C.id  = CCF.client_id
	INNER JOIN CustomFont CF ON CCF.custom_font_id = CF.id
	INNER JOIN FontFamilyDefinition FAD ON FAD.custom_font_id = CF.id
	INNER JOIN (
		SELECT id, font_family_definition_id, 
			(if (STRCMP(style_name,'regular'), 0,  1)) AS regular,
			(if (STRCMP(style_name,'bold'), "",  font_file_name)) AS bold, 
			(if (STRCMP(style_name,'italic'), "",  font_file_name)) AS italic, 
			(if (STRCMP(style_name,'boldItalic'), "",  font_file_name)) AS bold_italic
		FROM FontFileDefinition
	) AS FID ON FID.font_family_definition_id = FAD.id
WHERE C.status = "ACTIVE"
GROUP BY CCF.id
) AS AllFonts
WHERE AllFonts.has_reg = 0;
