package org.pubanatomy.awsUtils

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.RegionUtils
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import com.amazonaws.services.dynamodbv2.model.*
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import java.util.jar.JarEntry
import java.util.jar.JarInputStream

/**
 * User: flashflexpro@gmail.com
 * Date: 4/11/2016
 * Time: 3:23 PM*/
public class CreateDynamoTableTask extends DefaultTask{

    private static final Logger logger = LogManager.getLogger( CreateDynamoTableTask.class );

    private boolean exitOnError = true;

    private Project[] javaSourceProjects = [];

    private Map<String, String> classToTableName4Capacities = new HashMap<>();

    private Long secondsToWaitForActive = 20L;

    public String dynamoEndpoint = null

    @Input
    public String awsRegion = Regions.US_EAST_1.name

    @Input
    boolean getExitOnError(){
        return exitOnError
    }

    void setExitOnError( boolean exitOnError ){
        this.exitOnError = exitOnError
    }

    @Input
    Project[] getJavaSourceProjects(){
        return javaSourceProjects
    }

    void setJavaSourceProjects( Project[] javaSourceProjects ){
        this.javaSourceProjects = javaSourceProjects

        javaSourceProjects.each { j -> this.dependsOn( j.tasks[ 'jar' ] )
        }
    }

    @Input
    Map<String, String> getClassToTableName4Capacities(){
        return classToTableName4Capacities
    }

    void setClassToTableName4Capacities( Map<String, String> classToTableName4Capacities ){
        this.classToTableName4Capacities = classToTableName4Capacities
    }

    @Input
    Long getSecondsToWaitForActive(){
        return secondsToWaitForActive
    }

    void setSecondsToWaitForActive( Long secondsToWaitForActive ){
        this.secondsToWaitForActive = secondsToWaitForActive
    }


    private AmazonDynamoDBClient dynamoDB;

    AmazonDynamoDBClient getDynamoDB(){
        return dynamoDB
    }
    Map<String, File> classNameToJarFile = new HashMap<>();
    Map<String, Class> classNameToClass = new HashMap<>();

    @TaskAction
    public void doTheTask() throws IOException{
        dynamoDB = new AmazonDynamoDBClient( new DefaultAWSCredentialsProviderChain() );
        dynamoDB.setRegion( RegionUtils.getRegion( awsRegion ) )

        if( dynamoEndpoint?.trim() ){
            logger.info( "setting local dynamoEndpoint:{}", dynamoEndpoint )
            dynamoDB.setEndpoint( dynamoEndpoint )
        }

        logger.info( "setting AWS dynamoEndpoint:{}, Region:{}", awsRegion, dynamoEndpoint )

        Set<String> jarPaths = new HashSet<>()
        javaSourceProjects.each { p ->
            p.configurations.runtime.files.each {
                jarPaths.add( it.absolutePath )
            }

            File pJar = p.file( p.jar.archivePath )
            jarPaths.add( pJar.absolutePath )
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

                if( classToTableName4Capacities.containsKey( classInJarClassName ) ){
                    this.classNameToJarFile.put( classInJarClassName, pJar )
                }

            }
            logger.info( "project:{}, jar:{} ", p.path, p.jar.archivePath )
        }

        if( classNameToJarFile.size() == 0 ){
            logger.info( "No class found " )
            return;
        }

        List<URL> classpathsUrls = new ArrayList<URL>();
        for( String path : jarPaths ){
            URL url = new File( path ).toURI().toURL();
            classpathsUrls.add( url );
        }


        def loader = Thread.currentThread().getContextClassLoader()
        URLClassLoader clsLdr = new URLClassLoader( classpathsUrls.toArray( new URL[classpathsUrls.size()] ), loader );

        classNameToJarFile.keySet().each { className ->
            Class dynamoMapperClass = clsLdr.loadClass( className );
            DynamoDBTable dynaTableAnnot = dynamoMapperClass.getDeclaredAnnotation( DynamoDBTable.class )
            if( dynaTableAnnot != null ){
                logger.info( "DynamoTable Class:{}", className )
                classNameToClass.put( className, dynamoMapperClass );
            }
        }

        logger.info( "{} DynamoDBTables Mapper Classes found!", classNameToClass.size() )

        ArrayList<String> successLst = new ArrayList<>();
        classNameToClass.each {
            String tcStr = classToTableName4Capacities.get( it.key )
            String tableName = null;
            Long tableRead = 9L;
            Long tableWrite = 9L;
            Long indexRead = 9L;
            Long indexWrite = 9L;
            if( tcStr != null && tcStr.length() > 1 ){
                logger.info( "customizing {} : {}", it.key, tcStr )
                String[] tableNameCapacities = tcStr.split( "\\|" )

                tableName = tableNameCapacities[ 0 ];
                if( tableNameCapacities.length > 0 ){
                    tableRead = Long.parseLong( tableNameCapacities[ 1 ] )
                    tableWrite = Long.parseLong( tableNameCapacities[ 2 ] )
                    indexRead = Long.parseLong( tableNameCapacities[ 3 ] )
                    indexWrite = Long.parseLong( tableNameCapacities[ 4 ] )
                }
            }

            if( createDynamoTablesNow( tableName, it.value, tableRead, tableWrite, indexRead, indexWrite ) ){
                successLst.add( tableName )
            }
        }

        logger.info( "{} tables created/confirmed in {}:\n{}", successLst.size(), dynamoEndpoint,
                successLst.join( "\r\n" ) )


        long retryBegin = System.currentTimeMillis()
        while( true ){
            List<String> waitForLst = []
            def secsHaveTried = ( System.currentTimeMillis() - retryBegin ) / 1000;
            successLst.each {
                DescribeTableResult descRslt = dynamoDB.describeTable( it );
                if( descRslt.table.tableStatus != TableStatus.ACTIVE.toString() ){
                    waitForLst.add( it )
                }
            }
            if( waitForLst.size() == 0 ){
                logger.info( "ALL ARE ACTIVE !!!" )
                return;
            }
            else{
                logger.info( "{} secs retried for {}'s status to turn active ", secsHaveTried, waitForLst.join( "," ) )
            }

            if( secondsToWaitForActive < secsHaveTried ){
                logger.info( "time out after retried {} seconds", secsHaveTried )
                return;
            }
            Thread.currentThread().sleep( 3000 )
        }

    }


    boolean createDynamoTablesNow( String tableName, Class tableClass, Long tableReadingCapacity,
                                   Long tableWritingCapacity, Long indexWritingCapacity,
                                   Long indexReadingCapacity ){
        DynamoDBMapperConfig mapperConfig = DynamoDBMapperConfig.DEFAULT;
        if( tableName != null ){
            if( tableName.toLowerCase() == "null" ){
                logger.warn( "class {} can't have a tablename: {}", tableClass, tableName )
                return false;
            }
            mapperConfig = DynamoDBMapperConfig.builder()
                    .withTableNameOverride( new DynamoDBMapperConfig.TableNameOverride( tableName ) ).build();
        }

        DynamoDBMapper dynamoDBMapper = new DynamoDBMapper( dynamoDB, mapperConfig );
        CreateTableRequest createTableRequest = dynamoDBMapper.generateCreateTableRequest( tableClass );
        try{
            if( createTableRequest.getLocalSecondaryIndexes() != null ){
                createTableRequest.getLocalSecondaryIndexes()
                        .forEach { it.setProjection( new Projection().withProjectionType( ProjectionType.ALL ) ) };
            }

            if( createTableRequest.getGlobalSecondaryIndexes() != null ){
                createTableRequest.getGlobalSecondaryIndexes().forEach {
                    it.setProvisionedThroughput(
                            new ProvisionedThroughput( indexReadingCapacity, indexWritingCapacity ) );
                    it.setProjection( new Projection().withProjectionType( ProjectionType.ALL ) );
                };
            }

            createTableRequest.
                    setProvisionedThroughput( new ProvisionedThroughput( tableReadingCapacity, tableWritingCapacity ) );

            dynamoDB.createTable( createTableRequest );
        }
        catch( Exception e ){
            if( !( e instanceof ResourceInUseException && "ResourceInUseException" ==
                    ( ( ResourceInUseException )e ).getErrorCode() ) ){
                logger.info( "Error creating table:{}, class:{}, e:{}", createTableRequest.tableName, tableClass,
                        ExceptionUtils.getStackTrace( e ) )
                if( !exitOnError ){
                    logger.info( "continue because existOnError : false" )
                    return false;
                }
                throw e;
            }
        }

        for( int i = 0; i < 5; i++ ){
            try{
                DescribeTableResult descRslt = dynamoDB.describeTable( createTableRequest.getTableName() );
                logger.info( "table found:{}", descRslt.toString() );
                return true;
            }
            catch( Exception e ){
                if( !( e instanceof ResourceNotFoundException ) ){
                    throw e;
                }
                logger.info( "Check on table:{}, for the {} time", createTableRequest.getTableName(), i )
            }
        }
        throw new Exception( "failed in confirming table:" + createTableRequest.getTableName() )
    }

}
