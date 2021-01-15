package com.flashflexpro.multienvprops

import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

/**
 * User: GaryY
 * Date: 3/8/2017*/
class MultiEnvPropsConvention{

    private Project project

    MultiEnvPropsConfigConvention multiEnvConfig;

    def multiEnvConfig( Closure closure ){
        ConfigureUtil.configure( closure, multiEnvConfig )
    }

    MultiEnvPropsConvention( Project project ){
        this.project = project
        this.multiEnvConfig = new MultiEnvPropsConfigConvention()
    }
}
