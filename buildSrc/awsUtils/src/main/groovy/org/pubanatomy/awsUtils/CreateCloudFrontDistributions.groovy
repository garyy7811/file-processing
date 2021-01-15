package org.pubanatomy.awsUtils

import com.amazonaws.regions.RegionUtils
import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudfront.AmazonCloudFrontClient
import com.amazonaws.services.cloudfront.model.*
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.BucketPolicy
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * User: flashflexpro@gmail.com
 * Date: 4/11/2016
 * Time: 3:23 PM*/
public class CreateCloudFrontDistributions extends DefaultTask{

    private static final Logger logger = LogManager.getLogger( CreateCloudFrontDistributions.class );

    private AmazonCloudFrontClient cloudFrontClient
    private AmazonS3Client         s3Client

    private String s3BucketName;
    private Boolean passHeadersForUpload = false;
    private String webDistributionId;

    @Input
    String getS3BucketName(){
        return s3BucketName
    }

    void setS3BucketName( String s3BucketName ){
        this.s3BucketName = s3BucketName
        if( s3BucketName == null ){
            throw new Error( "s3BucketName can't be null" )
        }
    }

    Boolean getPassHeadersForUpload(){
        return passHeadersForUpload
    }

    void setPassHeadersForUpload( Boolean passHeadersForUpload ){
        this.passHeadersForUpload = passHeadersForUpload
    }

    @Input
    String getWebDistributionId(){
        return webDistributionId
    }

    void setWebDistributionId( String webDistributionId ){
        this.webDistributionId = webDistributionId
        if( webDistributionId == null ){
            throw new Error( "webDistributionId can't be null" )
        }
    }
    @Input
    public boolean addRtmpDistribution = false;


    @Input
    public long secondsToWaitToBeActive = 10


    @Input
    public String awsRegion = Regions.US_EAST_1.name


    public String webDistributionDomain;
    public String rtmpDistributionDomain;


    private String webDistrS3AccssIDId
    private String webDistrS3CanonicalUserId

    private String rtmpDistrS3AccssIDId
    private String rtmpDistrS3CanonicalUserId

    private String  rtmpDistributionId     = null
    private boolean qualifiedWebDistrFound = false;

    @TaskAction
    public void doTheTask() throws IOException{
        ObjectMapper om = new ObjectMapper()

        cloudFrontClient = new AmazonCloudFrontClient()
        cloudFrontClient.setRegion( RegionUtils.getRegion( awsRegion ) )
        s3Client = new AmazonS3Client()
        s3Client.setRegion( RegionUtils.getRegion( awsRegion ) )

        GetDistributionResult getDistributionResult = cloudFrontClient.
                getDistribution( new GetDistributionRequest( webDistributionId ) )
        Distribution webDistribution = getDistributionResult.distribution;


        if( webDistribution == null ){
            throw new Error( "Cloudfront web distribution NOT FOUND for id:{}" + webDistributionId )
        }

        logger.info( "s3BucketName:{}", s3BucketName )
        logger.info( "passHeadersForUpload :{}", passHeadersForUpload )
        logger.info( "webDistributionId:{}", webDistributionId )

        webDistributionDomain = webDistribution.domainName
        DistributionConfig webDistrConfig = webDistribution.distributionConfig;


        if( webDistrConfig.origins.items.size() == 1 ){
            Origin existWebOrg = webDistrConfig.origins.items.get( 0 )
            if( existWebOrg.domainName == ( s3BucketName + ".s3.amazonaws.com" ) && existWebOrg.originPath == "" ){
                String existWebOrgAccIDId = existWebOrg.s3OriginConfig.originAccessIdentity.split( "/" ).last()

                BucketPolicy s3BucketPolicy = s3Client.getBucketPolicy( s3BucketName )
                if( s3BucketPolicy.policyText != null ){
                    logger.info( "s3 bucket {} policy:{}", s3BucketName, s3BucketPolicy.policyText )
                    HashMap p = om.readValue( s3BucketPolicy.policyText, HashMap.class )

                    List<HashMap> s3plcsttm = p.get( "Statement" )
                    def existing = s3plcsttm.find { isExistingStatement( it, existWebOrgAccIDId ) }
                    if( existing != null ){
                        qualifiedWebDistrFound = true
                        logger.warn(
                                "Found existing s3Origin:{}, with existing S3 accessID:{}\n Please check {}'s configurations are good manually:\n 1) Behaviour -- Allowed methods;\n 2) Behaviour -- TrustedSigners;\n 3) Distribution -- PriceClass",
                                existWebOrg.toString(), existWebOrgAccIDId, webDistributionId )
                    }
                }
                if( !qualifiedWebDistrFound ){
                    CloudFrontOriginAccessIdentitySummary webS3Id = cloudFrontClient.
                            listCloudFrontOriginAccessIdentities(
                                    new ListCloudFrontOriginAccessIdentitiesRequest() ).cloudFrontOriginAccessIdentityList.items.
                            find { it.id == existWebOrgAccIDId }

                    if( webS3Id != null ){
                        webDistrS3AccssIDId = webS3Id.id
                        webDistrS3CanonicalUserId = webS3Id.s3CanonicalUserId
                        logger.info( "Found Qulified Origin with S3 accessID:{}", webS3Id.toString() )
                    }
                }

            }
            else{
                logger.info( " existing Origin not qualified :{}\n will clear origin and add a new one",
                        existWebOrg.toString() )
            }
        }
        else{
            logger.info( "more than one origin found:{} \n will clear origin and add a new one",
                    webDistrConfig.origins.items.collect { it.toString() }.join( "\n" ) )
        }

        if( !qualifiedWebDistrFound && webDistrS3AccssIDId == null ){
            CreateCloudFrontOriginAccessIdentityResult s3AccessIdCreateRslt
            s3AccessIdCreateRslt = cloudFrontClient.createCloudFrontOriginAccessIdentity(
                    new CreateCloudFrontOriginAccessIdentityRequest(
                            new CloudFrontOriginAccessIdentityConfig( webDistributionId ).
                                    withCallerReference( System.currentTimeMillis() + " byGradle" ).
                                    withComment( "WebCF[" + webDistributionId + "] --> S3[" + s3BucketName +
                                            "] By Gradle" ) ) )


            webDistrS3AccssIDId = s3AccessIdCreateRslt.cloudFrontOriginAccessIdentity.id
            webDistrS3CanonicalUserId = s3AccessIdCreateRslt.cloudFrontOriginAccessIdentity.s3CanonicalUserId

            Origin theNewWebOrigin = new Origin()
                    .withId( s3BucketName )
                    .withDomainName( s3BucketName + ".s3.amazonaws.com" )
                    .withCustomHeaders( new CustomHeaders().withQuantity( 0 ) )
                    .withOriginPath( "" )
                    .withS3OriginConfig( new S3OriginConfig().
                    withOriginAccessIdentity( "origin-access-identity/cloudfront/" + webDistrS3AccssIDId ) )

            webDistrConfig.origins.items.each {
                logger.warn( "removing existing origin:\n{}", it.toString() )
            }

            webDistrConfig.origins.setItems( [theNewWebOrigin] )
            webDistrConfig.origins.setQuantity( 1 )
            DefaultCacheBehavior defBehv = webDistrConfig.getDefaultCacheBehavior()
            if( defBehv == null ){
                defBehv = new DefaultCacheBehavior()
            }

            AllowedMethods methds = defBehv.getAllowedMethods()
            if( methds == null ){
                methds = new AllowedMethods()
            }

            methds.withItems( Method.GET, Method.HEAD )
            if( passHeadersForUpload ){
                methds.withItems( Method.DELETE, Method.POST, Method.OPTIONS, Method.PUT, Method.PATCH )
            }
            methds.withQuantity( methds.items.size() )
            defBehv.setAllowedMethods( methds )
            defBehv.setTargetOriginId( theNewWebOrigin.id )
            defBehv.setViewerProtocolPolicy( ViewerProtocolPolicy.HttpsOnly )
            if( !passHeadersForUpload && addRtmpDistribution ){
                defBehv.setTrustedSigners( new TrustedSigners( ["self"] ).withQuantity( 1 ).withEnabled( true ) )
            }

            webDistrConfig.setDefaultCacheBehavior( defBehv )
            webDistrConfig.setPriceClass( PriceClass.PriceClass_100 )

            UpdateDistributionResult webDistrUpdateRslt = cloudFrontClient.updateDistribution(
                    new UpdateDistributionRequest( webDistrConfig, webDistributionId, getDistributionResult.ETag ) )
            logger.info( "Added new origin: {} to Cloudfront web distribution rslt:{}", theNewWebOrigin,
                    webDistrUpdateRslt )
        }

        logger.info( "addingRtmpDistribution:{}", addRtmpDistribution )
        if( addRtmpDistribution ){
            StreamingDistributionSummary existRtmpOrg = cloudFrontClient.listStreamingDistributions(
                    new ListStreamingDistributionsRequest() ).streamingDistributionList.items.find {
                it.s3Origin.domainName == s3BucketName + ".s3.amazonaws.com"
            }

            if( existRtmpOrg != null ){
                rtmpDistributionDomain = existRtmpOrg.domainName
                rtmpDistributionId = existRtmpOrg.id

                rtmpDistrS3AccssIDId = existRtmpOrg.s3Origin.originAccessIdentity.split( "/" ).last()

                def tmpRtmpDistrS3AccssIDId = rtmpDistrS3AccssIDId
                def s3OriginAccessIDSum = cloudFrontClient.listCloudFrontOriginAccessIdentities(
                        new ListCloudFrontOriginAccessIdentitiesRequest() ).cloudFrontOriginAccessIdentityList.items.
                        find { it.id == tmpRtmpDistrS3AccssIDId }
                rtmpDistrS3CanonicalUserId = s3OriginAccessIDSum.s3CanonicalUserId

                logger.info( "existing rtmp distribution found for bucket:{}, origin:{}", s3BucketName,
                        existRtmpOrg.toString() )
            }
            else{

                S3Origin theNewStreamS3Origin = new S3Origin( s3BucketName + ".s3.amazonaws.com" ).
                        withOriginAccessIdentity( "origin-access-identity/cloudfront/" + webDistrS3AccssIDId )

                CreateStreamingDistributionResult createdRtmpRslt = cloudFrontClient.createStreamingDistribution(
                        new CreateStreamingDistributionRequest( new StreamingDistributionConfig(
                                System.currentTimeMillis() + "Stream from S3[" + s3BucketName + "] byGradle",
                                theNewStreamS3Origin,
                                true ).withAliases( new Aliases().withQuantity( 0 ) ).
                                withTrustedSigners( new TrustedSigners( ["self"] ).withQuantity( 1 ).
                                        withEnabled( true ) ).
                                withEnabled( true ).
                                withPriceClass( PriceClass.PriceClass_100 ).withEnabled( true ).
                                withComment( "Stream from S3[" + s3BucketName + "] by Gradle" ) ) )
                logger.info( "creating new one streaming distribution:{} with origin:{} for bucket:{}",
                        createdRtmpRslt.toString(), theNewStreamS3Origin.toString(), s3BucketName )
                rtmpDistributionDomain = createdRtmpRslt.streamingDistribution.domainName
                rtmpDistributionId = createdRtmpRslt.streamingDistribution.id

                rtmpDistrS3AccssIDId = webDistrS3AccssIDId
                rtmpDistrS3CanonicalUserId = webDistrS3CanonicalUserId
            }
        }

        String plTxt = s3Client.getBucketPolicy( s3BucketName ).policyText
        if( plTxt == null ){
            plTxt = "{\n    \"Version\": \"2012-10-17\",\n    \"Statement\": [\n        \n    ]\n}"
            logger.info( "New S3 policy :{} for bucket:{}", plTxt, s3BucketName )
        }
        else{
            logger.info( "Existing S3 policy :{} for bucket:{}", plTxt, s3BucketName )
        }

        HashMap pmap = om.readValue( plTxt, HashMap.class )
        List<HashMap> sttmtLst = pmap.get( "Statement" )

        int prevSttmtSize = sttmtLst.size()
        if( !qualifiedWebDistrFound && webDistrS3CanonicalUserId != null ){
            appendStatement( sttmtLst, webDistributionId, webDistrS3AccssIDId, webDistrS3CanonicalUserId )
        }
        if( addRtmpDistribution && rtmpDistrS3CanonicalUserId != null && rtmpDistrS3CanonicalUserId !=
                webDistrS3CanonicalUserId ){
            appendStatement( sttmtLst, rtmpDistributionId, rtmpDistrS3AccssIDId, rtmpDistrS3CanonicalUserId )
        }
        if( prevSttmtSize < sttmtLst.size() ){
            plTxt = om.writeValueAsString( pmap )
            logger.info( "Cloudfront access  policy: {}\n If Failed check the existing policy manually!", plTxt )
            s3Client.setBucketPolicy( s3BucketName, plTxt )
            logger.info( "Cloudfront access  policy added!" )
        }
        else{
            logger.info( "No policy change needed ..." )
        }


        long start = System.currentTimeMillis()
        boolean webDeployed = false
        boolean rtmpDeployed = false
        while( System.currentTimeMillis() - start < secondsToWaitToBeActive * 1000 ){
            if( !webDeployed ){
                def webDeployStatus = cloudFrontClient.
                        getDistribution( new GetDistributionRequest( webDistributionId ) ).distribution.status
                webDeployed = ( webDeployStatus == "Deployed" )
                logger.info( "WebDistr:{}, status:{}", webDistributionId, webDeployStatus )
            }

            if( addRtmpDistribution && !rtmpDeployed ){
                def rtmpDeployStatus = cloudFrontClient.getStreamingDistribution(
                        new GetStreamingDistributionRequest( rtmpDistributionId ) ).streamingDistribution.status
                rtmpDeployed = ( rtmpDeployStatus == "Deployed" )
                logger.info( "RtmpDistr:{}, status={}", rtmpDistributionId, rtmpDeployStatus )
            }

            if( ( webDeployed && !addRtmpDistribution ) || ( addRtmpDistribution && rtmpDeployed ) ){
                logger.info( "Both distribution deployed" )
                return;
            }
            Thread.currentThread().sleep( 2222 )
        }
        logger.warn( "Time out ... ... secondsToWaitToBeActive={}", secondsToWaitToBeActive )
    }

    private boolean appendStatement( List<HashMap> sttmtLst, String distriId, String s3AccsId,
                                     String s3CanonicalUserId ){
        logger.info( "distriId:{}, s3AccsId:{}, s3CanonicalUserId:{}", distriId, s3AccsId, s3CanonicalUserId )
        ObjectMapper om = new ObjectMapper()
        Map existSttmnt = sttmtLst.find { isExistingStatement( it, s3AccsId ) }
        if( existSttmnt != null ){
            logger.info( "Existing Found :\n{} \nin S3 bucket policy, no need to add statement",
                    om.writeValueAsString( existSttmnt ) )
            return false;
        }
        HashMap adding = om.readValue( getTheStatement( distriId, s3CanonicalUserId, s3AccsId ), HashMap.class )
        sttmtLst.add( adding )
        logger.info(
                "Statement:\n{}\n added to S3 bucket policy,\n We try not to update the distribution because it takes time, so we only look for the distribution's principle in S3 bucket instead of reuse S3 bucket's principle!",
                adding )
        return true;
    }

    protected boolean isExistingStatement( HashMap sttmnt, String s3AccsId ){
        def sttmntP = sttmnt.get( "Principal" )
        if( !( sttmntP instanceof Map ) ){
            logger.warn( "Ignore bucket " + s3BucketName + " 's policy because of unexpected Principle !! statment:" +
                    new ObjectMapper().writeValueAsString( sttmnt ) )
            return false
        }
        def B1 = sttmntP.get( "AWS" ) == "arn:aws:iam::cloudfront:user/CloudFront Origin Access Identity " + s3AccsId
        def B2 = sttmnt.get( "Resource" ) == "arn:aws:s3:::" + s3BucketName +
                ( passHeadersForUpload ? "/crossdomain.xml" : "/*" )
        def B3 = sttmnt.get( "Action" ) == "s3:GetObject"
        logger.debug( ">>>>" )
        logger.debug( new ObjectMapper().writeValueAsString( sttmnt ) )
        logger.debug( "<<<<|" + B1 + "|" + B2 + "|" + B3 )
        B1 && B2 && B3
    }


    private static final String cfStatement = "{\n" + "            \"Sid\": \"&{sid}&\",\n" +
            "            \"Effect\": \"Allow\",\n" + "            \"Principal\": {\n" +
            "                \"CanonicalUser\": \"&{s3CanonicalUserId}&\"\n" + "            },\n" +
            "            \"Action\": \"s3:GetObject\",\n" +
            "            \"Resource\": \"arn:aws:s3:::&{bucketPath}&\"\n" + "        }"

    private String getTheStatement( String distriId, String s3CanonicalUserId, String s3AccssId ){
        String rt = cfStatement.replace( "&{sid}&", "distribution:" + distriId + " with accessId:" + s3AccssId )
        rt = rt.replace( "&{bucketPath}&", s3BucketName + ( passHeadersForUpload ? "/crossdomain.xml" : "/*" ) )
        return rt.replace( "&{s3CanonicalUserId}&", s3CanonicalUserId )

    }

}
