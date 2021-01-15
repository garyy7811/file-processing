package org.pubanatomy.awsUtils

import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement
import com.amazonaws.services.dynamodbv2.model.ScanRequest
import com.amazonaws.services.dynamodbv2.model.ScanResult
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.StringUtils
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.springframework.beans.BeanUtils
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate
import org.springframework.data.elasticsearch.core.MappingBuilderAround
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity
import org.springframework.data.elasticsearch.core.query.IndexQuery
import org.springframework.data.mapping.PersistentProperty

import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.stream.Collectors

class ReplicDynamoToESIndexTypesTask extends DefaultTask{

    private static final Logger logger = LogManager.getLogger( ReplicDynamoToESIndexTypesTask.class );


    private Project[] javaSourceProjects = [];

    private Map<String, String> classNameToTableName = new HashMap<>()

    @Input
    Map<String, String> getClassNameToTableName(){
        return classNameToTableName
    }

    void setClassNameToTableName( Map<String, String> classNameToTableName ){
        this.classNameToTableName = classNameToTableName
    }


    @Input
    public String elasticsearchClusterName = "elasticsearch"

    @Input
    public String elasticsearchIndexName

    @Input
    public Boolean deleteExistingIndex

    @Input
    public String elasticsearchClusterNodes

    @Input
    public Boolean elasticsearchClientTransportSniff = false

    @Input
    Project[] getJavaSourceProjects(){
        return javaSourceProjects
    }

    void setJavaSourceProjects( Project[] javaSourceProjects ){
        this.javaSourceProjects = javaSourceProjects

        javaSourceProjects.each { j -> this.dependsOn( j.tasks[ 'jar' ] ) }
    }


    @Input
    public String awsRegion = Regions.US_EAST_1.name


    Map<String, File> classNameToJarFile = new HashMap<>();
    Map<String, Class> classNameToClass = new HashMap<>();

    @TaskAction
    public void doTheTask() throws IOException{

        if( elasticsearchClusterNodes == null || elasticsearchClusterNodes?.trim() == "" ){
            throw new Error( "elasticsearchClusterNodes can't be null" )
        }
        if( elasticsearchIndexName == null || elasticsearchIndexName?.trim() == "" ){
            throw new Error( "elasticsearchIndexName can't be null" )
        }

        TransportClient client = TransportClient.builder().settings( Settings.builder()
                .put( "cluster.name", elasticsearchClusterName )
                .put( "client.transport.sniff", elasticsearchClientTransportSniff )
                .put( "client.transport.ignore_cluster_name", true )
                .put( "client.transport.ping_timeout", "5s" )
                .put( "client.transport.nodes_sampler_interval", "8s" )
                .build() ).build();
        for( String clusterNode : StringUtils.split( elasticsearchClusterNodes, "," ) ){
            String hostName = StringUtils.substringBeforeLast( clusterNode, ":" );
            String port = StringUtils.substringAfterLast( clusterNode, ":" );
            client.addTransportAddress(
                    new InetSocketTransportAddress( InetAddress.getByName( hostName ), Integer.valueOf( port ) ) );
        }
        client.connectedNodes();

        ElasticsearchTemplate esTemplate = new ElasticsearchTemplate( client )

        try{
            if( esTemplate.indexExists( elasticsearchIndexName ) ){
                if( !deleteExistingIndex ){
                    throw new Error( "Existing index:" + elasticsearchIndexName );
                }
                logger.info( "deleting exising index:" + elasticsearchIndexName )
                esTemplate.deleteIndex( elasticsearchIndexName )
            }
            logger.info( "creating index:" + elasticsearchIndexName )
            esTemplate.createIndex( elasticsearchIndexName )
        }
        catch( e ){
            logger.warn( "Ignore error when deleting/creating index:\n {}", e )
        }

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
                if( classNameToTableName.keySet().contains( classInJarClassName ) ){
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
        URLClassLoader clsLdr =
                new URLClassLoader( classpathsUrls.toArray( new URL[classpathsUrls.size()] ), loader );

        List clzzLst = classNameToTableName.keySet().collect()
        List orgClzzLst = classNameToTableName.keySet().collect()

        classNameToJarFile.keySet().each { className ->
            Class esTypeClass = clsLdr.loadClass( className );
            DynamoDBTable dynaTable = esTypeClass.getDeclaredAnnotation( DynamoDBTable.class )
            Document esDoc = esTypeClass.getDeclaredAnnotation( Document.class )
            if( esDoc != null && dynaTable != null ){
                if( orgClzzLst.indexOf( className ) >= 0 ){
                    logger.info( "EsType/DynaTable Class:{}", className )
                    classNameToClass.put( className, esTypeClass );
                    clzzLst.remove( className )
                }
                else{
                    logger.warn( "Ignore class {}, because it's not in List", className )
                }
            }
        }

        logger.info( "{} EsTypes Mapper Classes found!\n {} classes Not found :{}", classNameToClass.size(),
                clzzLst.size(),
                clzzLst.join( ", " ) )

        ArrayList<String> successLst = new ArrayList<>();

        classNameToTableName.each {
            Class clzz = classNameToClass.get( it.key )
            String typeName = null;
            try{
                ElasticsearchPersistentEntity persisEntity = esTemplate.getPersistentEntityFor( clzz )
                typeName = persisEntity.getIndexType()

                def xContentBuilder = MappingBuilderAround.
                        buildMapping( clzz, typeName, persisEntity.getIdProperty().getField().getName(),
                                persisEntity.getParentType() )

                esTemplate.putMapping( elasticsearchIndexName, typeName, xContentBuilder )
                successLst.add( typeName )
            }
            catch( e1 ){
                logger.warn( "Error when putting class:{} into ES type:{},\n{}", it.key, typeName, e1 )
                throw e1;
            }
        }

        logger.info( "{} types created/confirmed:\n{}", successLst.size(), successLst.join( "\r\n" ) )

        esTemplate.refresh( elasticsearchIndexName )

        def dynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion( awsRegion ).build()

        List<Thread> workerLst = new ArrayList<>()
        classNameToTableName.each {
            Class clzz = classNameToClass.get( it.key )

            def worker = new Dyna2EsWorker()
            worker.project = project
            worker.elasticsearchIndexName = elasticsearchIndexName
            worker.dynaEsClass = clzz
            worker.dynamoTableName = it.value
            worker.dynamoClient = dynamoDB
            worker.esTemplate = esTemplate
            worker.start()
            workerLst.add( worker )
        }

        while( true ){
            Dyna2EsWorker found = workerLst.find { it.alive }
            if( found == null ){
                break
            }
            Thread.currentThread().sleep( 999 )
            print( " ... " )
        }
    }

    static class Dyna2EsWorker extends Thread{

        static volatile ObjectMapper objectMapper = new ObjectMapper()

        Project project
        String  elasticsearchIndexName

        Class          dynaEsClass
        String         dynamoTableName
        AmazonDynamoDB dynamoClient

        ElasticsearchTemplate esTemplate

        @Override
        void run(){

            final DynamoDBMapperConfig mapperConfig = DynamoDBMapperConfig.builder()
                    .withTableNameOverride(
                    DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement( dynamoTableName ) )
                    .build();

            DynamoDBMapper dynamoMapper = new DynamoDBMapper( dynamoClient, mapperConfig );

            List<KeySchemaElement> keySchema = dynamoClient.describeTable( dynamoTableName ).getTable().
                    getKeySchema()
            if( keySchema.size() > 1 ){
                keySchema = keySchema.stream().
                        sorted( new Comparator<KeySchemaElement>(){
                            @Override
                            int compare( KeySchemaElement o1, KeySchemaElement o2 ){
                                o1.getKeyType() == "HASH" ? 1 : -1
                            }
                        } ).collect( Collectors.toList() )
            }



            ElasticsearchPersistentEntity persistentEntity = esTemplate.getPersistentEntityFor( dynaEsClass )

            Map<String, AttributeValue> lastKeyEvaluated = null;
            def lastEvlFilePath = "Dyna_to_ES_last_evl__" + dynamoTableName + ".json"
            if( project.file( lastEvlFilePath ).exists() ){
                lastKeyEvaluated = objectMapper.
                        readValue( project.file( lastEvlFilePath ), new TypeReference<Map<String, AttributeValue>>(){} )
            }
            while( true ){

                ScanRequest scanRequest = new ScanRequest()
                        .withTableName( dynamoTableName )
                        .withExclusiveStartKey( lastKeyEvaluated )

                ScanResult result = dynamoClient.scan( scanRequest )
                if( result.items.size() == 0 ){
                    logger.info( "table {} has no record at all", dynamoTableName )
                    break
                }
                def indexQueries = new ArrayList<IndexQuery>( result.items.size() )
                result.getItems().collect {

                    String esId
                    if( keySchema.size() == 1 ){
                        esId = it.get( keySchema.get( 0 ).getAttributeName() ).getS()
                    }
                    else{
                        esId = it.get( keySchema.get( 0 ).getAttributeName() ).getS() + "∈@∋" +
                                it.get( keySchema.get( 1 ).getAttributeName() ).getS();
                    }
                    final Object newImgInst = dynamoMapper.marshallIntoObject( dynaEsClass, it );

                    BeanUtils.getPropertyDescriptor( dynaEsClass, persistentEntity.getIdProperty().getName() ).
                            getWriteMethod().invoke( newImgInst, esId );
                    PersistentProperty parentIdProperty = persistentEntity.getParentIdProperty();

                    def idx = new IndexQuery()
                    idx.setId( esId )
                    idx.setObject( newImgInst )
                    idx.setIndexName( elasticsearchIndexName )
                    if( parentIdProperty != null ){
                        idx.setParentId(
                                ( String )BeanUtils.getPropertyDescriptor( dynaEsClass, parentIdProperty.getName() )
                                        .getReadMethod().invoke( newImgInst ) );
                    }
                    indexQueries.add( idx )

                }
                esTemplate.bulkIndex( indexQueries )

                logger.info( "{} items, {} -> {}, last-eval:{}", result.items.size(), dynamoTableName,
                        persistentEntity.getIndexType(), result.getLastEvaluatedKey() )
                if( result.getLastEvaluatedKey() == null ){
                    break;
                }
                lastKeyEvaluated = result.getLastEvaluatedKey()

                project.file( lastEvlFilePath ).text = objectMapper.writeValueAsString( lastKeyEvaluated )
            }
        }
    }

}
