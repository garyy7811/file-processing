package com.flashflexpro.multienvprops

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * User: GaryY
 * Date: 2/11/2017*/
class MultiEnvPropsPlugin implements Plugin<Project>{

    Project project;

    String rootPropsFileName = null

    String currentPropsInfo = null

    String extMapVarName = null

    String pathToPropsFileInput = null

    @Override
    void apply( Project project ){
        this.project = project
        if( project.rootProject != project ){
            throw new Error( "Can only apply on root project! projectPath:" + project.path )
        }

        if( !project.convention.plugins.multiEnv ){
            project.convention.plugins.multiEnv = new MultiEnvPropsConvention( project )
        }

        multiEnvConvention = ( MultiEnvPropsConvention )project.convention.plugins.multiEnv

        if( !project.hasProperty( "multiEnvProps_extMapVarName" ) ){
            throw new Error( "Expecting value multiEnvProps_extMapVarName in \${root}/gradle.properties" )
        }
        extMapVarName = project.multiEnvProps_extMapVarName

        if( !project.hasProperty( "multiEnvProps_rootPropsFileName" ) ){
            throw new Error( "Expecting value multiEnvProps_rootPropsFileName in \${root}/gradle.properties" )
        }
        rootPropsFileName = project.multiEnvProps_rootPropsFileName


        String overrideEnvProps = "overrideEnvProps"
        if( project.hasProperty( "multiEnvProps_overrideEnvProps" ) ){
            overrideEnvProps = project.multiEnvProps_overrideEnvProps
            project.logger.info  "overrideEnvProps name:" + overrideEnvProps
        }

        if( project.hasProperty( "multiEnvProps_currentPropsInfo" ) ){
            currentPropsInfo = project.multiEnvProps_currentPropsInfo
        }


        if( project.hasProperty( overrideEnvProps ) ){
            pathToPropsFileInput = project[ overrideEnvProps ]
            project.logger.info  "Picking up path to props file from -P" + pathToPropsFileInput
        }

        mergeProperties( project, overrideEnvProps, currentPropsInfo, extMapVarName, rootPropsFileName )

        project.tasks.create( CloneEnvPropsTask.NAME, CloneEnvPropsTask.class )
        project.tasks.create( RemoveEnvPropsTask.NAME, RemoveEnvPropsTask.class )
        project.tasks.create( ListEnvPropsTask.NAME, ListEnvPropsTask.class )
    }

    MultiEnvPropsConvention multiEnvConvention

    MultiEnvPropsConvention getMultiEnvConvention(){
        return multiEnvConvention
    }

    private void mergeProperties( Project project, String overrideEnvProps, String currentPropsInfo,
                                  String extMapVarName, String rootPropsFileName ){
        if( pathToPropsFileInput != null ){
            pathToPropsFileInput = project[ overrideEnvProps ]
            project.logger.info  "Picking up path to props file from -P" + pathToPropsFileInput
        }
        else if( currentPropsInfo != null ){
            project.logger.info "No " + extMapVarName +
                    " Found from command line, Picking up path from multiEnvProps_currentPropsInfo"
            File currentEnvConfigInfoFile = project.file( currentPropsInfo )
            if( currentEnvConfigInfoFile.exists() ){
                pathToPropsFileInput = currentEnvConfigInfoFile.text
            }
            else{
                project.logger.warn( "Current properties not found ! File Not found, fall back using root props:" +
                        pathToPropsFileInput )
            }
        }

        if( pathToPropsFileInput == null ){
            project.logger.warn( "Fall back using root props:" + rootPropsFileName )
            pathToPropsFileInput = rootPropsFileName;
        }

        String[] p2pFileArr = pathToPropsFileInput.split( ';' )
        List<PropsBranch> branches = p2pFileArr.collect { new PropsBranch( this, it ) }.collect()

        if( pathToPropsFileInput != rootPropsFileName && currentPropsInfo != null ){
            project.file( currentPropsInfo ).text = pathToPropsFileInput
        }

        final PropsBranch baseBranch = branches.removeLast()
        final Properties configMap = baseBranch.configMap

        while( branches.size() > 0 ){
            PropsBranch cbr = branches.removeLast()
            cbr.configMap.each {
                def existing = configMap[ it.key ]
                if( existing != null && existing != it.value ){
                    throw new Error( cbr.pathToPropsFile + "[" + it.key + "]'s value: " + it.value +
                            " conflict with other value:" + existing );
                }
                if( existing != it.value && it.value != null ){
                    configMap[ it.key ] = it.value
                }
            }
        }

        configMap[ overrideEnvProps ] = pathToPropsFileInput
        project.ext[ extMapVarName ] = configMap

        configMap.each { String k, v ->
            if( !k.startsWith( "multiEnvProps_" ) ){
                if( project.hasProperty( k ) && project[ k ]?.trim() ){
                    configMap[ k ] = project[ k ]
                    project.logger.
                            info( 'configMap property overriding by project:' + k + '=' + v + '->' + configMap[ k ] )
                }
                else if( System.getProperty( k ) != null ){
                    configMap[ k ] = System.getProperty( k )
                    project.logger.info( 'configMap property overriding by system properties:[' + k + ']=' + v + '->' +
                            configMap[ k ] )
                }
                else if( System.getenv( k ) != null ){
                    configMap[ k ] = System.getenv( k )
                    project.logger.
                            info( 'configMap property overriding by ENV:[' + k + ']=' + v + '->' + configMap[ k ] )
                }
            }
        }
    }

    private static class PropsBranch{
        private String pathToPropsFile;

        String propConfigName;

        final Properties configMap = new Properties()

        PropsBranch( MultiEnvPropsPlugin plugin, String p ){
            pathToPropsFile = p
            Project project = plugin.project
            String rootPropsFileName = plugin.rootPropsFileName

            List<String> pathToPropsSplittedArr = pathToPropsFile.split( "/" ) as List;
            List<File> propLst = new ArrayList<>()
            List<File> basePropsLst = new ArrayList<>()
            if( pathToPropsSplittedArr.size() > 0 ){
                propConfigName = pathToPropsSplittedArr.removeLast();
                while( true ){
                    String tmpDirPath = pathToPropsSplittedArr.size() > 0 ?
                            ( pathToPropsSplittedArr.join( "/" ) + "/" ) : ""
                    File tmpPropFile = project.file( tmpDirPath + propConfigName )

                    if( tmpPropFile.exists() ){
                        propLst.add( tmpPropFile )
                        if( propConfigName != rootPropsFileName ){
                            basePropsLst.add( project.file( tmpDirPath + rootPropsFileName ) )
                        }
                        project.logger.info  'adding properties:' + tmpPropFile.absolutePath
                    }
                    else if( propConfigName != rootPropsFileName ){
                        File ccfgtmp = project.file( tmpDirPath + rootPropsFileName )
                        if( ccfgtmp.exists() ){
                            throw new Error( ccfgtmp.path + " found but according " + propConfigName +
                                    " NOT FOUND did you missing it???" )
                        }
                    }

                    if( pathToPropsSplittedArr.size() == 0 ){
                        break
                    }
                    pathToPropsSplittedArr.removeLast()
                }
            }


            Set<Object> bsKeys = new HashSet<>()

            while( propLst.size() > 0 ){
                File cuPropF = propLst.removeLast(  )
                File cubsPrF = null
                if( basePropsLst.size() > 0 ){
                    cubsPrF = basePropsLst.removeLast()
                }
                Properties curProps = new Properties()
                curProps.load( new FileInputStream( cuPropF ) )
                project.logger.info( 'loaded ' + curProps.size() + " properties from " + cuPropF.path )

                if( cubsPrF != null ){
                    Properties cubsPrps = new Properties()
                    cubsPrps.load( new FileInputStream( cubsPrF ) )
                    bsKeys = cubsPrps.keySet()
                }

                curProps.each {
                    bsKeys.remove( it.key )

                    project.logger.
                            info( cuPropF.path + "[" + it.key + "]:" + it.value + " overriding:" + configMap[ it.key ] )

                    configMap[ it.key ] = it.value
                }
                if( bsKeys.size() > 0 ){
                    plugin.multiEnvConvention.multiEnvConfig.propNameCheckExempts.
                            each { exProp -> bsKeys.remove( exProp ) }
                    if( bsKeys.size() > 0 ){
                        throw new Error( "Found properties defined in base config:\n" + cubsPrF.absolutePath +
                                "\nbut undefined for current environment:\n" + cuPropF.absolutePath + "\n" +
                                bsKeys.join( "\n" ) + "\n!!!" )
                    }
                }
            }

        }
    }
}