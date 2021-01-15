package com.flashflexpro.multienvprops

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

/**
 * User: GaryY
 * Date: 2/26/2017*/
class CloneEnvPropsTask extends DefaultTask{

    public static final String NAME = "cloneEnvProps"

    CloneEnvPropsTask(){
        group = "Help"
        description = 'Cloning multi environment properties'
        multiEnvPropsConvention = project.convention.plugins.multiEnv
    }

    MultiEnvPropsConvention multiEnvPropsConvention;

    @TaskAction
    def aaaa(){

        def rootPropsFileName = project.multiEnvProps_rootPropsFileName

        println "********************************************************************************************************"
        println "*** this task will clone ${rootPropsFileName} in dirs, please make sure it's has no unintentional changes***"
        println "********************************************************************************************************"

        Console c = System.console();
        if( c == null ){
            throw new Error(
                    "\n********************************************************************************************************\n" +
                            " Disable dameon to input from console:\n" + " --no-daemon in commandline\n" + " or \n" +
                            " org.gradle.daemon=false in gradle.properties \n" +
                            "********************************************************************************************************\n" )
        }
        String nEnvFileName = null
        if( c != null ){
            nEnvFileName = c.readLine( "\nNew environment properties file name:\n" )
        }
        if( nEnvFileName == rootPropsFileName ){
            throw new Error( "new environment properties file can't be the same as " + rootPropsFileName )
        }
        if( !nEnvFileName?.trim() ){
            nEnvFileName = rootPropsFileName + "-" + nEnvName
        }

        Map<String, String> replaceMap = new HashMap<>()
        multiEnvPropsConvention.multiEnvConfig.propValReplacements.each {
            String replVal = null
            while( !replVal?.trim() ){

                def propStr = "What do you want to replace string \"" + it.key + "\" with? nLeave empty to use:" +
                        it.value
                replVal = c.readLine( propStr )

                if( !replVal?.trim() && !it.value?.trim() ){
                    logger.warn( "Error:" + it.key + " has no default value!!! You have to provide a value!" )
                    continue
                }


                if( replVal?.trim() ){
                    replaceMap.put( it.key, replVal )
                }
                else{
                    logger.info( "Empty input, will use default value: " + it.value )
                    replaceMap.put( it.key, it.value )
                }
            }
        }

        clonePropsFilesInProjTree( project, rootPropsFileName, nEnvFileName, replaceMap )
    }

    Map<String, Project> clonePropsFilesInProjTree( Project proj, String rootPropsFileName, String nEnvFileName,
                                                    Map<String, String> replaceMap ){

        File f = proj.file( rootPropsFileName )
        if( f.exists() ){
            String nnm = nEnvFileName
            String npth = f.path.substring( 0, f.path.length() - rootPropsFileName.length() )

            proj.copy {
                from f
                into npth
                rename { nnm }
                filter { String l ->
                    replaceMap.each { l = l.replaceAll( it.key, it.value ) }
                    return l
                }
            }
            println "Properties file generated:" + npth + nnm
        }

        proj.childProjects.each {
            clonePropsFilesInProjTree( it.value, rootPropsFileName, nEnvFileName, replaceMap )
        }


    }
}