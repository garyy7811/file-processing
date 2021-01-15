package org.pubanatomy.awsUtils

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.ecs.AmazonECSClient
import com.amazonaws.services.ecs.model.*
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

public class StartEcsService extends DefaultTask{

    @Input
    public String awsRegion = "us-east-1";

    @Input
    String taskDefName = "!init me!"

    @Input
    String taskDefinitionArn = "!init me!"

    @Input
    String ecsClusterName

    @Input
    int count = 1


    @TaskAction
    public void doTheTask() throws IOException{

        AmazonECSClient ecsClient = new AmazonECSClient( new DefaultAWSCredentialsProviderChain() )
        ecsClient.setRegion( RegionUtils.getRegion( awsRegion ) )

        ecsClient.createCluster( new CreateClusterRequest().withClusterName( ecsClusterName ) )


        DescribeServicesResult describeServicesResult = ecsClient.
                describeServices( new DescribeServicesRequest().withCluster( ecsClusterName ).
                        withServices( taskDefName ) )

        println 'ECS service described: ' + describeServicesResult

        if( describeServicesResult.services.size() == 0 || describeServicesResult.services.get( 0 ).status ==
                "INACTIVE" ){
            CreateServiceResult createServiceResult = ecsClient.createService(
                    new CreateServiceRequest().withCluster( ecsClusterName ).withTaskDefinition( taskDefinitionArn ).
                            withServiceName( taskDefName ).withDesiredCount( count ).withPlacementConstraints(
                            new PlacementConstraint().withType( PlacementConstraintType.DistinctInstance ) ).
                            withPlacementStrategy( new PlacementStrategy().withType( PlacementStrategyType.Spread ).
                                    withField( "attribute:ecs.availability-zone" ) ) )

            println 'ECS Service created:' + createServiceResult
        }
        else{
            Service service = describeServicesResult.services.get( 0 );
            if( service.getTaskDefinition() != taskDefinitionArn || service.desiredCount != count ){
                ecsClient.updateService(
                        new UpdateServiceRequest().withCluster( ecsClusterName ).withService( taskDefName ).
                                withTaskDefinition( taskDefinitionArn ).
                                withDesiredCount( 0 ) )

                while( true ){
                    List<String> tsks = ecsClient.
                            listTasks( new ListTasksRequest().withCluster( ecsClusterName ).
                                    withServiceName( taskDefName ) ).taskArns
                    if( tsks.size() == 0 ){
                        break
                    }
                    println ' waiting tasks to stop ..' + tsks.join( "; " )
                    Thread.currentThread().sleep( 2222 )
                }

                def updateServiceResult = ecsClient.updateService(
                        new UpdateServiceRequest().withCluster( ecsClusterName ).withService( taskDefName ).
                                withDesiredCount( count ) )

                println 'ECS service updated: ' + updateServiceResult
            }
            else{
                println 'ECS service up to date ...'
            }
        }
    }

}
