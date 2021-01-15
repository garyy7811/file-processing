package org.springframework.data.elasticsearch.core;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;

import java.io.IOException;

/**
 * User: GaryY
 * Date: 12/29/2016
 */
public class MappingBA extends MappingBuilder{

    public static XContentBuilder buildMapping( Class clazz, String indexType, String idFieldName, String parentType )
            throws IOException{
        return MappingBuilder.buildMapping( clazz, indexType, idFieldName, parentType );
    }
}
