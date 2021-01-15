
var AWS = require('aws-sdk');

var DOMParser = require('xmldom').DOMParser;
var xpath = require('xpath');

var presBuilder = require('./PresentationBuilder');
var fs = require('fs')
var sanitize = require("sanitize-filename");

var db = require("./lib/dbUtils.js");
var SecurityError = require("./lib/SecurityError");


var awsRegion;

var s3;
var dynamodb;

var getSlideShowLambdName;

var loginInfoTable;
var downloadBucket;
var publishedContentTable;
var publishedUrlBase;
var pdfCrowdUsername;
var pdfCrowdApiKey;
var cdnMediaUrlBase;
var cdnFontUrlBase;
var cdnStaticUrlBase;

var pdfGeneratorHost;
var pdfGeneratorPort;


var csSessionId;
var csAuthUsername;
var slideShowUuid;
var publishedTimestamp;
var slideShowLookupId;
var slideShowXmlString;
var slideShowS3PdfKey;
var slideShowS3PdfDateTime;
var slideShowNameSanitized;
var slideShowS3XmlKey;
var slideShowS3HtmlKey;
var slideShowS3HtmlDateTime;

var parsedPresentation;



var isOptionsRequest = false;
var corsOrigin;

var rebuildPdf = false;
var noCleanup = false;

var localHtmlFileName;
var localPdfFileName;

exports.handler = function (event, context, callback) {

    console.log("handler entered");

    // pull configuration values from environment
    awsRegion = process.env.aws_region;
    AWS.config.region = awsRegion;

    getSlideShowLambdName = process.env.getSlideShowLambdName;

    loginInfoTable = process.env.awsLoginVerificationDynamoTablename;
    downloadBucket = process.env.awsS3DownloadBucket;
    publishedContentTable = process.env.awsPublishedContentDynamoTablename;
    publishedUrlBase = process.env.publishedUrlBase;
    pdfCrowdUsername = process.env.pdfCrowdUsername;
    pdfCrowdApiKey = process.env.pdfCrowdApiKey;
    cdnMediaUrlBase = process.env.cdnMediaUrlBase;
    cdnFontUrlBase = process.env.cdnFontUrlBase;
    cdnStaticUrlBase = process.env.cdnStaticUrlBase;
    pdfGeneratorHost = process.env.pdfGeneratorHost;
    pdfGeneratorPort = process.env.pdfGeneratorPort;

    if (event.headers) {

        // always look for Origin
        corsOrigin = event.headers["Origin"];
        if (!corsOrigin) {
            corsOrigin = event.headers["origin"];
        }


        // look for _cssid in cookies - this is the csSessionId from the app server
        if (event.headers["Cookie"]) {
            var cookieHeader = event.headers["Cookie"];
            var cookieList = cookieHeader.split(";");
            for (var i = 0; i < cookieList.length; i++) {
                var tmpCookie = cookieList[i].trim();
                if (tmpCookie.indexOf("_cssid") == 0) {
                    csSessionId = tmpCookie.split("=")[1];
                    console.log("found csSessoinId=" + csSessionId);
                    break;
                }
            }
        }
    }

    // first check for CORS processing
    if (event.httpMethod == "OPTIONS") {

        console.log("found OPTIONS request");
        isOptionsRequest = true;

        // set up the response
        var responseEvent = {
            statusCode:200,
            headers: {
                "Content-Type": "application/json",
                "Access-Control-Allow-Origin": corsOrigin,
                "Access-Control-Allow-Methods": "GET",
                "Access-Control-Allow-Credentials": true,
                "Access-Control-Max-Age": 3000
            },
            body:JSON.stringify(tmpBody)
        }

        // for now, just invoke callback with the cookies
        callback(null, responseEvent);

        console.log("..processing done for options request..");
        return;
    }

    // we expect to have path parameters:
    // "proxy": "slideShowUuid/publishedTimestamp"

    console.log("found pathParameters=" + event.pathParameters);
    var pathParamsArray = event.pathParameters.proxy.split("/");

    // we now support specifying the "latest" version of a SlideShow for pdf generation,
    // by passing 'latest' as the first path parameter, and the slideshow lookupId as the second
    if (pathParamsArray[0] == "latest") {
        slideShowLookupId = pathParamsArray[1];
        console.log("found request for latest version of lookupId="+slideShowLookupId);
        slideShowUuid = null;
        publishedTimestamp = null;
    } else {
        // this is the original behavior, used by the HTML Viewer, of specifying the uuid/timestamp of a slideshow
        slideShowUuid = pathParamsArray[0];
        console.log("found slideShowUuid=" + slideShowUuid);
        publishedTimestamp = pathParamsArray[1];
        console.log("found publishedTimestamp=" + publishedTimestamp);
        slideShowLookupId = null;
    }

    // re-initialize instance variables
    slideShowXmlString = null;


    // look for options in query string
    if (event.queryStringParameters !== null && event.queryStringParameters !== undefined) {
        if (event.queryStringParameters.rebuildPdf !== undefined &&
            event.queryStringParameters.rebuildPdf !== null &&
            event.queryStringParameters.rebuildPdf !== "") {
            console.log("Received rebuildPdf= " + event.queryStringParameters.rebuildPdf);
            rebuildPdf = JSON.parse(event.queryStringParameters.rebuildPdf.toLowerCase());
        }
        if (event.queryStringParameters.noCleanup !== undefined &&
            event.queryStringParameters.noCleanup !== null &&
            event.queryStringParameters.noCleanup !== "") {
            console.log("Received noCleanup= " + event.queryStringParameters.noCleanup);
            noCleanup = JSON.parse(event.queryStringParameters.noCleanup.toLowerCase());
        }
    }



    // create S3 client
    s3 = new AWS.S3({apiVersion: '2006-03-01', region: awsRegion});

    // create DynamoDB service object
    dynamodb = new AWS.DynamoDB({apiVersion: '2012-08-10', region: awsRegion});

    // TODO: add code to look for _cssid and validate login table record, log username making request

    // parameters for retrieving userId data from login table
    var ddbGetLoginInfoParams = {
        TableName: loginInfoTable,
        Key: {'csSessionId': {S: csSessionId}}
    };


    dynamodb.getItem(ddbGetLoginInfoParams).promise().then(function (data) {

        if (!data.Item) {
            console.log("Could not locate login item for csSessionId="+csSessionId);
            throw new SecurityError("Invalid credentials", 403 );
        }

        userId = parseInt(data.Item.userId.S);
        console.log("got userId: " + userId);


        console.log("initializing db");
        db.init();

        console.log("fetching username");
        return db.getUsernameForUserId(userId)

    }).then(function (username) {

        csAuthUsername = username;
        console.log("got csAuthUsername=" + csAuthUsername);

        return getXmlIfNecessary();

    }).then(function (checkXmlResult) {

        console.log("checkXmlResult="+checkXmlResult);

        // parameters for retrieving slideshow recored from publishedContent table
        var ddbGetPublishedContentParams = {
            TableName: publishedContentTable,
            Key: {
                'uuid': {S: slideShowUuid},
                'publishedTimestamp': {N: publishedTimestamp.toString()}
            }
        };

        console.log("fetching publishedContentRecord...");

        return dynamodb.getItem(ddbGetPublishedContentParams).promise();

    }).then(function (publishedContentRecord) {

        console.log("got publishedContentRecord: " + JSON.stringify(publishedContentRecord));

        if (publishedContentRecord.Item == undefined) {
            console.log("could not locate publishedContentRecord - aborting!")
            throw new Error("publishedContentRecord not found!");
        }

        if (publishedContentRecord.Item.s3XmlKey == undefined) {
            console.log("could not locate publishedContentRecord.s3XmlKey - aborting!")
            throw new Error("publishedContentRecord.xml not found!");
        }

        var s3XmlKey = publishedContentRecord.Item.s3XmlKey.S;

        if (publishedContentRecord.Item.s3PdfKey) {

            if (rebuildPdf) {
                console.log("found existing pdf key");
                console.log("rebuildPdf="+rebuildPdf+", rebuilding now");
                return generateNewPdf(s3XmlKey);

            } else {

                console.log("found existing pdf key - returning now");
                return publishedContentRecord.Item.s3PdfKey.S;

            }
        } else {

            console.log("did not find s3PdfKey - creating pdf now");
            return generateNewPdf(s3XmlKey);
        }

    }).then( function (s3PdfKey) {

        console.log("got s3PdfKey="+s3PdfKey);

        // clean up local files
        doCleanup();

        var publishedPdfUrl = publishedUrlBase + s3PdfKey;

        var responseBody = {
            "response": "Success",
            "file": publishedPdfUrl
        };

        var responseEvent = {
            statusCode:200,
            headers: {
                "Content-Type": "application/json",
                "Access-Control-Allow-Origin": corsOrigin,
                "Access-Control-Allow-Methods": "GET",
                "Access-Control-Allow-Credentials": true,
                "Access-Control-Max-Age": 3000
            },
            body:JSON.stringify(responseBody)
        }


        callback(null, responseEvent);

    }).catch(function (error) {


        console.log("got error while processing: ", error);

        // clean up local files
        doCleanup();

        var responseCode = 500;
        var responseMsg = error.message;

        if (error instanceof SecurityError) {
            responseCode = error.httpCode;
        } else {
            console.log("caught unexpected error: ", error);
            callback(error);
            responseMsg = "Unexpected error generating pdf";
        }

        var errorResponse = {
            statusCode:responseCode,
            headers: {
                "Content-Type": "text/plain",
                "Access-Control-Allow-Origin": corsOrigin,
                "Access-Control-Allow-Methods": "GET",
                "Access-Control-Allow-Credentials": true,
                "Access-Control-Max-Age": 3000
            },
            body:responseMsg
        }

        callback(null, errorResponse)


    });

}


function doCleanup() {

    if (noCleanup) {
        console.log("noCleanup=true - not cleaning up");
        return;
    }

    console.log("attempting to clean up local files");

    if (fs.existsSync(localHtmlFileName)) {
        console.log("deleting localHtmlFileName=" + localHtmlFileName);
        fs.unlinkSync(localHtmlFileName);
        console.log("deleted localHtmlFileName=" + localHtmlFileName);
    } else {
        console.log("could not find localHtmlFileName=" + localHtmlFileName);
    }

    if (fs.existsSync(localPdfFileName)) {
        console.log("deleting localPdfFileName=" + localPdfFileName);
        fs.unlinkSync(localPdfFileName);
        console.log("deleted localPdfFileName=" + localPdfFileName);
    } else {
        console.log("could not find localPdfFileName=" + localPdfFileName);
    }

}



function generateNewPdf(s3XmlKey) {

    // pull xml from S3
    var params = {
        Bucket: downloadBucket,
        Key: s3XmlKey
    };

    console.log("downloading xml from S3 at: " + s3XmlKey);

    return s3.getObject(params).promise().then( function (s3Result) {


        console.log("got S3 object: " + s3Result);

        var srcXml = s3Result.Body.toString();

        // form filenames for srcHtml and dstPdf
        localHtmlFileName = "/tmp/"+ slideShowUuid + ".html"
        localPdfFileName =  "/tmp/" +slideShowUuid + ".pdf";

        // convert xml to html
        presBuilder.init(cdnStaticUrlBase, cdnMediaUrlBase, cdnFontUrlBase);
        parsedPresentation =  presBuilder.buildPresentation(srcXml);
        console.log("built presentation html");

        slideShowNameSanitized =  sanitize(parsedPresentation["deck"].name.replace(/\s/g, "_"));

        var slideShowLookupId = parsedPresentation["deck"].slideshow.lookupId;

        console.log("generating pdf for slideshow with lookupId="+slideShowLookupId+" for username="+csAuthUsername+", csSessionId="+csSessionId);

        // save html to local file system
        fs.writeFileSync(localHtmlFileName, parsedPresentation["html"]);
        console.log("wrote html to: " + localHtmlFileName);

        // derive s3 keys for output files
        slideShowS3XmlKey = s3XmlKey;
        slideShowS3PdfKey = "published/slideshow/" + slideShowUuid + "/" + publishedTimestamp + "/"+slideShowNameSanitized+".pdf";
        slideShowS3HtmlKey = "published/slideshow/" + slideShowUuid + "/" + publishedTimestamp + "/pdf.html";

        console.log("invoking pdfGenerator");

        return invokePdfGenerator(parsedPresentation);

    }).then( function (result) {

        console.log("got result: " +result);

        slideShowS3PdfDateTime = new Date().toISOString();

        // define parameters JSON for retrieving userId data from login table
        var ddbUpdateParams = {
            Key: {
                'uuid': {S: slideShowUuid},
                'publishedTimestamp': {N: publishedTimestamp.toString()}
            },
            ExpressionAttributeNames: {
                "#K": "s3PdfKey",
                "#D": "s3PdfDateTime"
            },
            ExpressionAttributeValues: {
                ":k": {
                    S: slideShowS3PdfKey
                },
                ":d": {
                    S: slideShowS3PdfDateTime
                }
            },
            ReturnValues: "ALL_NEW",
            TableName: publishedContentTable,
            UpdateExpression: "SET #K = :k, #D = :d"
        };


        console.log("adding published-content record");

        return dynamodb.updateItem(ddbUpdateParams).promise();

    }).then(function (ddbPutResult) {

        console.log("done adding published-content record");
        return slideShowS3PdfKey;
    }).catch(function (error) {

        console.log("caught error: " + error);
        throw error;
    });

}


function invokePdfGenerator(presentation) {

    // return new pending promise
    return new Promise((resolve, reject) => {

        // for now we are using http only
        const lib = require('http');

        var slideShowWidthPixels = presentation["deck"].width;
        var slideShowHeightPixels = presentation["deck"].height;

        var srcHtmlUrl = publishedUrlBase + slideShowS3HtmlKey;

        var post_data = {

            "cdnStaticUrlBase": cdnStaticUrlBase,
            "cdnMediaUrlBase": cdnMediaUrlBase,
            "cdnFontUrlBase": cdnFontUrlBase,

            "width": slideShowWidthPixels,
            "height": slideShowHeightPixels,

            // "url": srcHtmlUrl,

            "pdfname": slideShowNameSanitized,

            "s3Bucket" : downloadBucket,
            "s3XmlKey" : slideShowS3XmlKey,
            "s3HtmlKey" : slideShowS3HtmlKey,
            "s3PdfKey": slideShowS3PdfKey
        };

        // An object of options to indicate where to post to
        var post_options = {
            host: pdfGeneratorHost,
            port: pdfGeneratorPort,
            path: '/',
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        };

        console.log("Preparing request for pdfGenerator="+pdfGeneratorHost+":"+pdfGeneratorPort);

        const request = lib.request(post_options, (response) => {
            // handle http errors
            if (response.statusCode < 200 || response.statusCode > 299) {
                reject(new Error('Failed to load page, status code: ' + response.statusCode));
            }
            // temporary data holder
            const data = [];

            // on every content chunk, push it to the data array
            response.on('data', (chunk) => data.push(chunk));

            // we are done, resolve promise with those joined chunks
            response.on('end', () => {

                var buffer = Buffer.concat(data);

                console.log("received " + buffer.length + " bytes");


                var resultObj = JSON.parse(buffer.toString());
                if (resultObj != null && resultObj.response == "Success") {
                    resolve(buffer.toString());
                } else {
                    console.log("got error response from pdfGenerator!")
                    reject(buffer.toString());
                }


            } );

        });

        // handle connection errors of the request
        request.on('error', (err) => reject(err))

        // post the data
        console.log("writing post data: " + JSON.stringify(post_data));
        request.write(JSON.stringify(post_data));
        console.log("closing post request");
        request.end();

    })
};

function getXmlIfNecessary() {


    if (!slideShowLookupId) {
        console.log("found no slideShowLookupId - returning resolved Promise!");
        return new Promise(resolve => {
            resolve("slideshowXml-not-fetched");
        })
    } else {
        console.log("found slideShowLookupId, invoking getSlideShow");
        return invokeGetSlideShow(slideShowLookupId, csSessionId)
    }
}

/**
 * returns a Promise that will resolve to the SlideShow XML string for the given slideShowLookupId
 *
 * @param slideShowLookupId
 * @param csSessionId
 * @returns {Promise<any>}
 */
function invokeGetSlideShow(slideShowLookupId, csSessionId) {

    var payload = { slideShowLookupId: slideShowLookupId, csSessionId: csSessionId};

    var params = {
        FunctionName: getSlideShowLambdName, // the lambda function we are going to invoke
        InvocationType: 'RequestResponse',
        LogType: 'Tail',
        Payload: JSON.stringify(payload)
    };

    return new Promise((resolve, reject) => {

        console.log("invoking lambda: " + getSlideShowLambdName);

        var lambda = new AWS.Lambda();
        lambda.invoke(params, function(err, data) {
            if (err) {
                console.error("got error: " + err);
                reject(err);
            } else {

                console.log('getSlideShow returned '+ data.Payload.length + ' characters');


                var resultJson = JSON.parse(data.Payload);

                slideShowXmlString = resultJson.xml;

                // parse the presentation xml to obtain uuid/publishedTimestamp
                var doc = new DOMParser().parseFromString(slideShowXmlString);

                slideShowUuid = xpath.select1("string(/SlideShow/access/uuid)", doc)
                console.log("found slideShowUuid: " + slideShowUuid);

                publishedTimestamp = xpath.select1("string(/SlideShow/lastUpdatedTimestamp)", doc)
                console.log("found publishedTimestamp: " + publishedTimestamp);

                resolve("slideshowXml-fetched");
            }
        })

        console.log("invoked lambda...");

    });

}

