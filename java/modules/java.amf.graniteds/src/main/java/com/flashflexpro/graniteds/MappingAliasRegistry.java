package com.flashflexpro.graniteds;

import org.granite.messaging.AliasRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * User: flashflexpro@gmail.com
 * Date: 4/22/2015
 * Time: 5:28 PM
 */
public class MappingAliasRegistry implements AliasRegistry{

    private Map<String, String> getAliasForType;

    public MappingAliasRegistry( Map<String, String> getTypeForAlias, Map<String, String> getAliasForType ){
        this.getTypeForAlias = getTypeForAlias;
        this.getAliasForType = getAliasForType;
    }

    private Map<String, String> getTypeForAlias;

    @Override
    public void scan( Set<String> packageNames ){

    }

    @Override
    public String getTypeForAlias( String alias ){
        if( getTypeForAlias == null ){
            return alias;
        }
        String rt = getTypeForAlias.get( alias );
        return rt == null ? alias : rt;
    }

    @Override
    public String getAliasForType( String className ){
        if( getAliasForType == null ){
            return className;
        }
        String rt = getAliasForType.get( className );
        return rt == null ? className : rt;
    }
}
