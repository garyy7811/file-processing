package org.pubanatomy.awsUtils

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.ecs.AmazonECSClient
import com.amazonaws.services.ecs.model.*
import com.amazonaws.services.logs.AWSLogsClient
import com.amazonaws.services.logs.model.CreateLogGroupRequest
import com.amazonaws.services.logs.model.ResourceAlreadyExistsException
import com.amazonaws.services.s3.AmazonS3Client
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import java.util.regex.Pattern

public class CreateEcsTaskDefinition extends DefaultTask{

    private static final Logger logger = LogManager.getLogger( CreateEcsTaskDefinition.class );

    @Input
    public String awsRegion = "us-east-1";

    @Input
    String taskDefName


    @Input
    Boolean increaseRevisionOnExist = false

    @Input
    String networkBridge = "bridge"


    @Input
    String awsLogsGroup

    @Input
    String awsLogsStreamPrefix

    @Input
    String awsLogsDriver = "awslogs"

    String taskDefinitionArn

    @Input
    String awsS3ConfigBucket

    @Input
    String domainName = ""

    @Input
    List<ContainerModel> containerList


    @TaskAction
    public void doTheTask() throws IOException{

        AmazonECSClient ecsClient = new AmazonECSClient( new DefaultAWSCredentialsProviderChain() )
        ecsClient.setRegion( RegionUtils.getRegion( awsRegion ) )
        if( !increaseRevisionOnExist ){

            try{
                DescribeTaskDefinitionResult descTskDefRslt = ecsClient.
                        describeTaskDefinition( new DescribeTaskDefinitionRequest().withTaskDefinition( taskDefName ) )

                taskDefinitionArn = descTskDefRslt.taskDefinition.taskDefinitionArn

                logger.info( "Task found: " + taskDefinitionArn )
                return;
            }
            catch( e ){
                logger.debug( e )
            }
        }
        Collection<Volume> volumes = new LinkedList<>()
        List<ContainerDefinition> cLst = containerList.collect { c ->

            ContainerDefinition containerDef = new ContainerDefinition()
            containerDef.setImage( c.image )
            def imgName = c.image.split( "/" ).last().replaceAll( "\\:", "-" ).replaceAll( "\\.", "_" )
            containerDef.setName( taskDefName + imgName )
            containerDef.setMemory( c.memoryLimit )
            containerDef.setMemoryReservation( c.memoryReserve )

            def cmdStr = c.command.replaceAll( Pattern.compile( ' +' ), ' ' )
            if( cmdStr.length() > 1 ){
                containerDef.setCommand( Arrays.asList( cmdStr.split( ' ' ) ) )
            }

            containerDef.setUlimits( c.ulimits.collect {
                String[] nameEpair = it.split( "=" )
                String[] limits = nameEpair[ 1 ].split( ":" )
                new Ulimit().withName( nameEpair[ 0 ] ).withSoftLimit( Integer.parseInt( limits[ 0 ] ) ).
                        withHardLimit( Integer.parseInt( limits[ 1 ] ) )
            } )

            if( c.envVarMappings.size() > 0 ){
                containerDef.setEnvironment( c.envVarMappings.entrySet().
                        collect { new KeyValuePair().withName( it.key ).withValue( it.value ) } )
            }

            Collection<PortMapping> portMappingLst = new LinkedList<>()

            if( c.portMappings.size() > 0 ){
                portMappingLst.addAll( c.portMappings.collect {
                    new PortMapping().
                            withContainerPort( it.key ).
                            withHostPort( it.value ).
                            withProtocol( TransportProtocol.Tcp )
                } )
            }

            if( c.udpPortMappings.size() > 0 ){

                portMappingLst.addAll( c.udpPortMappings.collect {
                    new PortMapping().
                            withContainerPort( it.key ).
                            withHostPort( it.value ).
                            withProtocol( TransportProtocol.Udp )
                } )
            }

            if( portMappingLst.size() > 0 ){
                containerDef.setPortMappings( portMappingLst )
            }

            Collection<MountPoint> mountPoints = new LinkedList<>()

            if( c.mountMappings.size() > 0 ){

                volumes.addAll( c.mountMappings.entrySet().collect {
                    new Volume().withHost( new HostVolumeProperties().withSourcePath( it.key ) ).
                            withName( it.key.replaceAll( "/", "-" ) )
                } )

                mountPoints.addAll( c.mountMappings.entrySet().collect {
                    new MountPoint().withSourceVolume( it.key.replaceAll( "/", "-" ) ).
                            withContainerPath( it.value )
                } )
            }

            if( c.readOnlyMountMappings.size() > 0 ){

                volumes.addAll( c.readOnlyMountMappings.entrySet().collect {
                    new Volume().
                            withHost( new HostVolumeProperties().withSourcePath( it.key ) ).
                            withName( it.key.replaceAll( "/", "-" ) )
                } )


                mountPoints.addAll( c.readOnlyMountMappings.entrySet().collect {
                    new MountPoint().
                            withSourceVolume( it.key.replaceAll( "/", "-" ) ).
                            withContainerPath( it.value ).
                            withReadOnly( true )
                } )
            }

            if( mountPoints.size() > 0 ){
                containerDef.setMountPoints( mountPoints )
            }


            // only set privileged value when true
            if( c.privileged ){
                containerDef.setPrivileged( c.privileged )
            }


            Map<String, String> loggingOptions = ["awslogs-group"        : awsLogsGroup,
                                                  "awslogs-region"       : awsRegion,
                                                  "awslogs-stream-prefix": awsLogsStreamPrefix + imgName]
            containerDef.withLogConfiguration(
                    new LogConfiguration().withLogDriver( awsLogsDriver ).withOptions( loggingOptions ) )

            containerDef.setEssential( c.essential )

            containerDef

        }.collect()


        def regDefReq = new RegisterTaskDefinitionRequest().withFamily( taskDefName ).
                withContainerDefinitions( cLst ).withNetworkMode( networkBridge )

        if( volumes.size() > 0 ){
            regDefReq.setVolumes( volumes )
        }

        RegisterTaskDefinitionResult tskRegRslt = ecsClient.registerTaskDefinition( regDefReq )

        taskDefinitionArn = tskRegRslt.taskDefinition.taskDefinitionArn

        logger.info( "Task created: " + tskRegRslt.taskDefinition.taskDefinitionArn )

        logger.debug( tskRegRslt.taskDefinition.toString() )

        if( domainName?.trim() ){
            AmazonS3Client s3Client = new AmazonS3Client( new DefaultAWSCredentialsProviderChain() )

            def s3key = "ecs-task-domain/" + taskDefName + ".domain_conf"
            s3Client.putObject( awsS3ConfigBucket, s3key, domainName + "@v@0" )
            println ">>" + awsS3ConfigBucket + ":" + s3key + "->" + domainName

        }
        AWSLogsClient logsClient = new AWSLogsClient( new DefaultAWSCredentialsProviderChain() )
        logsClient.setRegion( RegionUtils.getRegion( awsRegion ) )
        try{
            logsClient.createLogGroup( new CreateLogGroupRequest( awsLogsGroup ) )
        }
        catch( e ){
            if( e instanceof ResourceAlreadyExistsException ){
                logger.info( "exist logs group:" + awsLogsGroup )
            }
            else{
                throw e
            }
        }

    }

    static class ContainerModel{

        String image = "! INIT ME !"


        int memoryLimit = 3


        int memoryReserve = 4


        Map<Integer, Integer> portMappings = [] as Map


        Map<Integer, Integer> udpPortMappings = [] as Map


        Map<String, String> envVarMappings = [] as Map


        Map<String, String> mountMappings = [] as Map


        Map<String, String> readOnlyMountMappings = [] as Map


        Boolean privileged = false


        String command = ""


        List<String> ulimits = [] as List<String>

        boolean essential = true;

    }
}
