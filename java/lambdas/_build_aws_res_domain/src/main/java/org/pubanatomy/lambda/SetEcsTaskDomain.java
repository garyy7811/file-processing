package org.pubanatomy.lambda;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesRequest;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.route53.model.*;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * User: GaryY
 * Date: 8/24/2017
 */
public class SetEcsTaskDomain implements RequestHandler<Map<String, Object>, Integer>{
    private static AWSStaticCredentialsProvider staticCredentialsProvider;

    @Override
    public Integer handleRequest( Map<String, Object> input, Context context ){
        staticCredentialsProvider =
                new AWSStaticCredentialsProvider( new BasicAWSCredentials( System.getenv( "runtimeAwsAccessKeyId" ), System.getenv( "runtimeAwsSecretAccessKey" ) ) );

        final String s3ConfigBucket = System.getenv( "awsS3ConfigBucket" );
        final String awsRegion = System.getenv( "aws_region" );
        final String hostedZoneId = System.getenv( "awsHostedZoneId" );
        final String ecsCluster = System.getenv( "ecsClusterName" );

        try{
            final Map<String, Object> detail = ( Map<String, Object> )input.get( "detail" );
            if( detail == null ){
                return - 1;
            }
            final String taskDefinitionArn = ( String )detail.get( "taskDefinitionArn" );

            final String nameClVer = taskDefinitionArn.split( ":task-definition/" )[ 1 ];

            AmazonS3Client s3Client = new AmazonS3Client( staticCredentialsProvider );
            s3Client.setRegion( RegionUtils.getRegion( awsRegion ) );

            final String[] nameAndVer = nameClVer.split( ":" );
            final String taskName = nameAndVer[ 0 ];
            final Integer finalVersion = Integer.parseInt( nameAndVer[ 1 ] ) * 1000 + ( Integer )detail.get( "version" );
            final String s3Key = "ecs-task-domain/" + taskName + ".domain_conf";
            S3Object s3Object;

            try{
                s3Object = s3Client.getObject( s3ConfigBucket, s3Key );
            }
            catch( com.amazonaws.services.s3.model.AmazonS3Exception e ){
                context.getLogger().log( " info S3Key:" + s3Key + " NOT FOUND! " );
                return 0;
            }

            final String s3ObjStr = IOUtils.toString( s3Object.getObjectContent() );
            context.getLogger().log( ">>s3ObjStr:" + s3ObjStr );
            String[] domainAndVersionFromS3 = s3ObjStr.split( "@v@" );

            final String domainName = domainAndVersionFromS3[ 0 ];
            final String versionFromS3 = domainAndVersionFromS3[ 1 ];
            final String lastStatus = ( String )detail.get( "lastStatus" );
            String updatedS3Str = domainName + "@v@" + finalVersion + "@v@" + lastStatus;
            context.getLogger().log( ">>>>versionFromS3:" + versionFromS3 + "; finalVersion:" + finalVersion + "; lastStatus:" + lastStatus );
            if( Integer.parseInt( versionFromS3 ) < finalVersion ){
                PutObjectResult putS3Rslt = s3Client.putObject( s3ConfigBucket, s3Key, updatedS3Str );
                if( "RUNNING".equals( lastStatus ) ){
                    final String containerInstanceArn = ( String )detail.get( "containerInstanceArn" );


                    AmazonECSClient ecsClient = new AmazonECSClient( staticCredentialsProvider );
                    ecsClient.setRegion( RegionUtils.getRegion( awsRegion ) );
                    DescribeContainerInstancesResult contDesRslt = ecsClient
                            .describeContainerInstances( new DescribeContainerInstancesRequest().withCluster( ecsCluster ).withContainerInstances( containerInstanceArn ) );
                    if( contDesRslt.getFailures().size() > 0 ){
                        final ObjectMapper objectMapper = new ObjectMapper();
                        context.getLogger().log( objectMapper.writeValueAsString( contDesRslt.getFailures() ) );
                        context.getLogger().log( objectMapper.writeValueAsString( input ) );

                    }


                    AmazonEC2Client ec2Client = new AmazonEC2Client( staticCredentialsProvider );
                    ec2Client.setRegion( RegionUtils.getRegion( awsRegion ) );

                    DescribeInstancesResult descEc2InstsRslt = ec2Client.describeInstances( new DescribeInstancesRequest().
                            withInstanceIds( contDesRslt.getContainerInstances().stream().map( ContainerInstance::getEc2InstanceId ).collect( Collectors.toSet() ) ) );

                    final List<String> ec2IPAddresses = descEc2InstsRslt.getReservations().stream().flatMap( it -> it.getInstances().stream() ).map( Instance::getPrivateIpAddress )
                            .collect( Collectors.toList() );

                    AmazonRoute53Client route53Client = new AmazonRoute53Client( staticCredentialsProvider );
                    route53Client.setRegion( RegionUtils.getRegion( awsRegion ) );

                    final String theIP = ec2IPAddresses.remove( 0 );
                    route53Client.changeResourceRecordSets( new ChangeResourceRecordSetsRequest( hostedZoneId, new ChangeBatch( Collections.singletonList(
                            new Change( ChangeAction.UPSERT,
                                    new ResourceRecordSet( domainName, RRType.A ).withResourceRecords( new ResourceRecord( theIP ) ).withTTL( 1L ) ) ) ) ) );

                    context.getLogger().log( "DNS name updated >>>>> ipAddress:" + theIP + ", domainName:" + domainName + ", hostedZonId:" + hostedZoneId + "Running PutS3Rslt->" +
                            putS3Rslt.getContentMd5() );

                }
                else if( "STOPPED".equals( lastStatus ) ){
                    context.getLogger().log( "Stopped PutS3Rslt->" + putS3Rslt.getContentMd5() );
                }

            }
            context.getLogger()
                    .log( "STATUS:" + lastStatus + "; S3 str:" + s3ObjStr + "; UPDATED:" + updatedS3Str + "; INPUT:::::>>>>>" + new ObjectMapper().writeValueAsString( input ) );

            return 0;
        }
        catch( Throwable e ){
            final ObjectMapper objectMapper = new ObjectMapper();
            try{
                context.getLogger().log( objectMapper.writeValueAsString( input ) );
            }
            catch( JsonProcessingException e1 ){
                e1.printStackTrace();
            }
            e.printStackTrace();
        }

        return 1;
    }


    public static void main( String[] args ){
        staticCredentialsProvider = new AWSStaticCredentialsProvider( new BasicAWSCredentials( "aaabbbccc", "AgbmMJ3v6ceIykddkt+4Re5plGskDAwQNTXRxkd3" ) );
        //        >>>10.2.2.222>>>dev-gary--mysql.cs.cc>>>Z3PAW63STDB4LD
        AmazonRoute53Client route53Client = new AmazonRoute53Client( staticCredentialsProvider );
        route53Client.setRegion( RegionUtils.getRegion( "us-east-1" ) );
        ChangeResourceRecordSetsResult chRslt = route53Client.changeResourceRecordSets( new ChangeResourceRecordSetsRequest( "Z3PAW63STDB4LD", new ChangeBatch( Collections
                .singletonList( new Change( ChangeAction.UPSERT,
                        new ResourceRecordSet( "dev-gary--mysql.cs.cc", RRType.A ).withResourceRecords( new ResourceRecord( "10.2.2.222" ) ).withTTL( 60L ) ) ) ) ) );

        System.out.println( chRslt );
    }


}
