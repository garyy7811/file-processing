package com.flashflexpro.multienvprops

/**
 * User: GaryY
 * Date: 3/8/2017*/
class MultiEnvPropsConfigConvention{


    Map<String,String> propValReplacements = new HashMap<>()

    /**
     * example when user want to deploy to different servers for different environments
     */
    List<String> propNameCheckExempts = []

}
