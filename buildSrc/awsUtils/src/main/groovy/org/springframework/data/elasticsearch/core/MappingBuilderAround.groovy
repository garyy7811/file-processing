package org.springframework.data.elasticsearch.core

import org.elasticsearch.common.xcontent.XContentBuilder

/**
 * User: GaryY
 * Date: 12/29/2016*/
class MappingBuilderAround extends MappingBuilder{
    public static XContentBuilder buildMapping( Class clazz, String indexType, String idFieldName, String parentType )
            throws IOException{
        return MappingBuilder.buildMapping( clazz, indexType, idFieldName, parentType )
    }
}
