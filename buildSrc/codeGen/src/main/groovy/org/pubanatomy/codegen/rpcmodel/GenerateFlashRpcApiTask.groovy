package com.customshow.codegen.rpcmodel

import org.apache.commons.lang3.StringUtils
import org.apache.tools.ant.types.selectors.SelectorUtils
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

import java.beans.IntrospectionException
import java.util.jar.JarEntry
import java.util.jar.JarInputStream

class GenerateFlashRpcApiTask extends DefaultTask{

    private Project[] javaSourceProjects;

    Project[] getJavaSourceProjects(){
        return javaSourceProjects
    }

    void setJavaSourceProjects( Project[] javaSourceProjects ){
        this.javaSourceProjects = javaSourceProjects
        javaSourceProjects.each { j -> this.dependsOn( j.tasks[ 'jar' ] )
        }
    }
    String[] includeDtoClasses;
    String[] excludeDtoClasses;

    String[] includeServiceClasses;
    String[] excludeServiceClasses;

    File outputDirectory;


    @TaskAction
    def doitnow(){

        if( includeDtoClasses == null || includeDtoClasses.length == 0 ){
            throw new IllegalArgumentException( "no solr bean classes included, check configure!" );
        }

        if( !outputDirectory.exists() ){
            outputDirectory.mkdirs();
        }

        Map<String, File> dtoClassToJarFile = new HashMap<>();
        Map<String, File> serviceClassToJarFile = new HashMap<>();

        List<String> jarPaths = new ArrayList<>();
        javaSourceProjects.each { p ->
            p.configurations.runtime.files.each {
                jarPaths.add( it.absolutePath )
            }



            File pJar = p.file( p.jar.archivePath )
            jarPaths.add( pJar.getAbsolutePath() )
            JarInputStream pJarInStrm = new JarInputStream( new FileInputStream( pJar ) );

            JarEntry jarEntry;
            while( true ){
                jarEntry = pJarInStrm.getNextJarEntry();

                if( jarEntry == null ){
                    break;
                }

                if( jarEntry.isDirectory() || !jarEntry.getName().endsWith( ".class" ) ){
                    continue;
                }
                String classInJarClassName = jarEntry.getName();


                classInJarClassName = classInJarClassName.replace( '/', '.' );
                classInJarClassName = classInJarClassName.substring( 0, classInJarClassName.length() - 6 );

                if( matchWildCard( classInJarClassName, includeDtoClasses ) &&
                        !matchWildCard( classInJarClassName, excludeDtoClasses ) ){
                    dtoClassToJarFile.put( classInJarClassName, pJar );
                }
                else if( matchWildCard( classInJarClassName, includeServiceClasses ) &&
                        !matchWildCard( classInJarClassName, excludeServiceClasses ) ){
                    serviceClassToJarFile.put( classInJarClassName, pJar )
                }
            }
        }


        if( dtoClassToJarFile.size() == 0 && serviceClassToJarFile.size() == 0 ){
            throw new RuntimeException( "No Classes found to generate: jar paths:" + StringUtils.join( jarPaths, ";" ) +
                    "; includeDtoClasses:" + includeDtoClasses + "; excludeDtoClasses:" + excludeDtoClasses +
                    "; includeServiceClasses:" + includeServiceClasses + "; excludeServiceClasses:" +
                    excludeServiceClasses );
        }

        logger.info( dtoClassToJarFile.size() + " dtoclasses found to generate " )
        logger.info( serviceClassToJarFile.size() + " serviceclasses found to generate " )


        try{
            List<URL> classpathsUrls = new ArrayList<URL>();

            // add all the jars to the new child realm
            for( String path : jarPaths ){
                URL url = new File( path ).toURI().toURL();
                classpathsUrls.add( url );
            }


            def loader = Thread.currentThread().getContextClassLoader()
            URLClassLoader clsLdr = new URLClassLoader( classpathsUrls.toArray( new URL[classpathsUrls.size()] ),
                    loader );

            for( String clsKey : dtoClassToJarFile.keySet() ){
                Class<? extends Serializable> tmpCls = ( Class<? extends Serializable> )clsLdr.loadClass( clsKey );

                PoaoClassGenerating classGenerating = new PoaoClassGenerating( tmpCls );
                String code = classGenerating.generateCode();
                String clzName = classGenerating.getClassName();
                String[] packagez = classGenerating.getPackage()

                writeFile( packagez, clzName, code, ".as" );
            }


            for( String clsKey : serviceClassToJarFile.keySet() ){
                Class tmpCls = ( Class )clsLdr.loadClass( clsKey );
                if( tmpCls.anonymousClass || !tmpCls.getCanonicalName()?.trim() ){
                    continue;
                }

                ServiceClassGenerating classGenerating = new ServiceClassGenerating( tmpCls );
                String code = classGenerating.generateCode();
                String clzName = classGenerating.getClassName();
                String[] packagez = classGenerating.getPackage()

                writeFile( packagez, clzName, code, ".as" );
            }

        }
        catch( ClassNotFoundException e ){
            e.printStackTrace();
        }
        catch( IntrospectionException e ){
            e.printStackTrace();
        }
    }

    def void writeFile( String[] packagz, String fileName, String code, String fileExtWithDot ){
        File dir = new File(
                outputDirectory.getAbsolutePath() + File.separator + StringUtils.join( packagz, File.separator ) );
        dir.mkdirs();
        FileWriter fr = null;
        try{
            fr = new FileWriter( new File( dir.getAbsolutePath() + File.separator + fileName + fileExtWithDot ) );
            fr.write( code );
            fr.flush();
        }
        catch( IOException e ){
            e.printStackTrace();
        }
        finally{
            if( fr != null ){
                try{
                    fr.close();
                }
                catch( IOException e ){
                }
            }
        }
    }

    def boolean matchWildCard( String className, String[] wildCards ){
        if( wildCards == null ){
            return false;
        }

        for( String wildCard : wildCards ){
            if( className.equals( wildCard ) ){
                return true;
            }

            if( SelectorUtils.matchPath( wildCard, className ) ){
                return true;
            }
        }

        return false;
    }


}
