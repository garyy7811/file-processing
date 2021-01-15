package com.flashflexpro.multienvprops

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * User: GaryY
 * Date: 2/21/2017*/
class ListEnvPropsTask extends DefaultTask{
    public static final String NAME = "listEnvProps"

    ListEnvPropsTask(){

        group = "Help"
        description = 'Cloning multi environment properties'
        multiEnvPropsConvention = project.convention.plugins.multiEnv
    }

    MultiEnvPropsConvention multiEnvPropsConvention;

    @TaskAction
    def action(){
        Map<String, String> configMap = project.ext[ project.multiEnvProps_extMapVarName ]

        configMap.each {
            println( "[" + it.key + "]->" + it.value )
        }


    }


}
