package com.flashflexpro.multienvprops

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskAction

/**
 *
 * User: GaryY
 * Date: 2/26/2017*/
class AnalyzeEnvPropsForCommonTask extends DefaultTask{

    public static final String NAME = "analyzeEnvProps"

    AnalyzeEnvPropsForCommonTask(){
        group = "Help"
        description = 'Analyze multi environment properties'
    }

    @TaskAction
    def aaaa(){
        //todo:
    }

}
