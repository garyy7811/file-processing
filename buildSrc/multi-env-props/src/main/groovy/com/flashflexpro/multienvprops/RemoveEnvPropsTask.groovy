package com.flashflexpro.multienvprops

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskAction

/**
 * User: GaryY
 * Date: 2/21/2017*/
class RemoveEnvPropsTask extends DefaultTask{
    public static final String NAME = "removeEnvProps"


    @TaskAction
    def bbbb(){
        def propFileName = System.console().readLine( '\nENV properties file name to remove:\n' )

        if( propFileName == project.multiEnvProps_rootPropsFileName ){
            throw new Error( "Can't delete root configurations" )
        }

        List<File> delLst = new ArrayList<>()

        addToDel( project, delLst, propFileName )

        if( delLst.size() == 0 ){
            println( "No file found through projects with name:" + propFileName )
            return;
        }
        def cf = System.console().readLine( "Found property files:\n\n" + delLst.collect { it.path }.join( "\n" ) +
                "\n\nEnter y to confirm to delete:" )

        if( cf == "y" ){
            delLst.each { it.deleteOnExit() }
            println delLst.size() + " files removed."
        }
    }

    def addToDel( Project proj, List<File> delLst, String propFileName ){
        def pj = proj.file( propFileName )
        if( pj.exists() ){
            delLst.add( pj )
        }

        proj.childProjects.each { addToDel( it.value, delLst, propFileName ) }
    }
}
