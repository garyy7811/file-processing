-- this query is used to locate all fonts to be compiled for the iPad app
SELECT C.name, CCF.id as ccf_id, CF.font_display_name, CF.font_face_name, CF.swf_file_name, 
	count(FID.id) as numFaces, sum(FID.regular) as has_reg, sum(FID.bold) as has_bold, sum(FID.italic) as has_italic, sum(FID.bold_italic) AS has_bi
FROM ClientCustomFont CCF
	INNER JOIN Client C ON C.id  = CCF.client_id
	INNER JOIN CustomFont CF ON CCF.custom_font_id = CF.id
	INNER JOIN FontFamilyDefinition FAD ON FAD.custom_font_id = CF.id
	INNER JOIN (
		SELECT id, font_family_definition_id, 
			(if (STRCMP(style_name,'regular'), 0,  1)) AS regular, 
			(if (STRCMP(style_name,'bold'), 0,  1)) AS bold, 
			(if (STRCMP(style_name,'italic'), 0,  1)) AS italic, 
			(if (STRCMP(style_name,'boldItalic'), 0,  1)) AS bold_italic
		FROM FontFileDefinition
	) AS FID ON FID.font_family_definition_id = FAD.id
WHERE C.status = "ACTIVE"
GROUP BY CCF.id
