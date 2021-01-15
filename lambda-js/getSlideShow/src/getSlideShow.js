console.log('Loading function');

var SecurityError = require("./lib/SecurityError");

var Buffer = require('buffer').Buffer;
var zlib = require('zlib');
var data2xml = require('data2xml');

var db = require("./lib/dbUtils.js");

var DOMParser = require('xmldom').DOMParser;
var xpath = require('xpath');


var SlideShow = require("./SlideShow");
var SlideShowXML = require("./SlideShowXML");

var AWS = require('aws-sdk');

// aws clients
var dynamodb;
var s3;

// configuration values will be lazy-initialized
var awsRegion;
var ddbTableName;
var downloadBucket;
var publishedContentTable;
var secureCookieDomain;

var isProxyRequest = false;
var inputBody;
var csSessionId;
var slideShowLookupId;
var slideShowUuid;
var publishedTimestamp;
var publishedDateTime;
var slideShowS3XmlKey;
var slideShowS3XmlDateTime;
var slideShowS3DtoStartKey;
var userId;
var slideShowXMLStr;

var isOptionsRequest = false;
var corsOrigin;

function init(event) {

    // pull configuration values from environment
    awsRegion = process.env.aws_region;
    ddbTableName = process.env.awsLoginVerificationDynamoTablename;

    downloadBucket = process.env.awsS3DownloadBucket;
    publishedContentTable = process.env.awsPublishedContentDynamoTablename;

    secureCookieDomain = process.env.secureCookieDomain;

    // reset all instance variables
    isProxyRequest = false;
    inputBody = null;
    csSessionId = null;
    slideShowLookupId = null;
    slideShowUuid = null;
    publishedTimestamp = null;
    publishedDateTime = null;
    slideShowS3XmlKey = null;
    slideShowS3XmlDateTime = null;
    slideShowS3DtoStartKey = null;
    userId = null;
    slideShowXMLStr = null;

    isOptionsRequest = false;
    corsOrigin = null;
    
    
    // now obtain input parameters from event
    if (event.slideShowLookupId) {
        console.log("got slideShowLookupId directory from input event");
        slideShowLookupId = event.slideShowLookupId;
        csSessionId = event.csSessionId;
        isProxyRequest = false;
    } else {

        isProxyRequest = true;

        if (event.headers) {
            corsOrigin = event.headers["Origin"];
            if (!corsOrigin) {
                corsOrigin = event.headers["origin"];
            }
        }

        console.log("corsOrigin="+corsOrigin);

        if (event.httpMethod == "OPTIONS") {

            console.log("found OPTIONS request");
            isOptionsRequest = true;

        } else if (event.pathParameters) {

            console.log("found pathParameters=" + event.pathParameters);
            slideShowLookupId = event.pathParameters.proxy;
            console.log("found slideShowLookupId=" + slideShowLookupId);

            // look for _cssid in cookies - this is the csSessionId from the app server
            if (event.headers && event.headers["Cookie"]) {
                var cookieHeader = event.headers["Cookie"];
                var cookieList = cookieHeader.split(";");
                for (var i = 0; i < cookieList.length; i++) {
                    var tmpCookie = cookieList[i].trim();
                    if (tmpCookie.indexOf("_cssid") == 0) {
                        csSessionId = tmpCookie.split("=")[1];
                        console.log("found csSessoinId="+csSessionId);
                        break;
                    }
                }
            }

        } else {


            if (!event.body) {
                throw new Error("Missing event.body!");
            }

            if (typeof event.body === 'string') {
                console.log("parsing event.body: " + event.body);
                inputBody = JSON.parse(event.body);
            } else {
                inputBody = event.body;
            }

            console.log("got inputBody: " + inputBody);
            console.log("inputBody typeof: " + (typeof inputBody));


            if (!inputBody.slideShowLookupId) {
                throw new Error("Missing required parameter slideShowLookupId");
            }

            slideShowLookupId = inputBody.slideShowLookupId;
        }
    }

}

function prepareResponse(payload) {


    var response = payload;

    if (isProxyRequest) {

        // this indicates API gateway, so we need to return an object with status and body

        // so we need a response event with payload
        var responsePayload = payload;

        var responseEvent = {
            statusCode:200,
            headers: {
                "Content-Type": "text/xml",
                "Access-Control-Allow-Origin": corsOrigin,
                "Access-Control-Allow-Methods": "GET",
                "Access-Control-Allow-Credentials": true,
                "Access-Control-Max-Age": 3000
            },
            body:responsePayload
        }

        response = responseEvent;
    } else {
        // for non-proxy requests, we'll wrap the slideShow XML in a simple JSON object to facilitate
        // parsing on the client side, since Lambda will always attempt to serialize the response as a JSON object anyway
        response =  { "xml" : payload };
    }

    return response;
}


function prepareSecurityErrorResponse(securityError) {

    var response = securityError;

    if (isProxyRequest) {

        // this indicates API gateway, so we need to return an object with status and body

        var responseEvent = {
            statusCode:securityError.httpCode,
            headers: {
                "Content-Type": "text/plain",
                "Access-Control-Allow-Origin": corsOrigin,
                "Access-Control-Allow-Methods": "GET",
                "Access-Control-Allow-Credentials": true,
                "Access-Control-Max-Age": 3000
            },
            body:securityError.message
        }

        response = responseEvent;
    }

    return response;
}

exports.handler = function (event, context, callback) {

    console.log("handler entered");

    console.log("got input: " + JSON.stringify(event, null, 4));

    try {


        init(event);

        // configure aws sdk
        AWS.config.update({region: awsRegion});

        console.log("got slideShowLookupId: " + slideShowLookupId);

        // variables populated during processing
        var slideShowDbRecord;
        var presentationList;

        console.log("initializing db");
        db.init();

        if (isOptionsRequest) {

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

        } else if (slideShowLookupId.indexOf("authorize/") > -1) {

            var sessionId = slideShowLookupId.split("/")[1];
            console.log("found request to authorize sessionId: " + sessionId);

            var keyId = "APKAIEQLPKFFN53SW5ZQ";
            var privateKey =
                "-----BEGIN RSA PRIVATE KEY-----\n" +
                "MIIEowIBAAKCAQEArf8ijJr9bUykBZP8S2H8N1dWeA0frUVNX9xXa8vB8H5KNMGx\n" +
                "Lf9B+0OF+riLHOfqUHrvaPfeank/STXpSN8UhFG87n1kScKnS0atE+EugLs9cV+W\n" +
                "yXEAsN4Ts6efPnuZnaTGXVtBU0oetssNBxlwWVKsEO8/Hl+qMlGMQq7DX6ENfiuS\n" +
                "tfBTeaRCWrvOG76BlLQ/11Mm/tFEdJCdJXfvhsiTbQ5LcmXw7k8oaCEF//kZ1fwH\n" +
                "j8WdbP1adhabPzcgyBMvzC8yItttFBW6zrGtLCsS4Wi3izSj7KkZxMi9oyqWs1Ma\n" +
                "z0pDRmKldISwTjw2BgbO5NzlN45CF2OrGY6Y1QIDAQABAoIBAE4D7LDQubrtN0oM\n" +
                "/X2rVJRXw7yWXdKqFTRbLpN1y+KSLaGUC2aNYj9QGl29qxpt3gDx7jDJmt9wt/CS\n" +
                "jKQQ6jkeETF55aKw9aPp76bPO7OawMT50DlIQsALAxh1mxBACTPG8u5fAV5gCH97\n" +
                "cb7yKA7U47PAIjPzPvPK3rHzfER+yTW12/YVeEMmn/FsOlc1W0tfLXi743mP/HDV\n" +
                "Z8YiDMJazxJF+Bu19GyK/V37A+dYK/7Smkrbi1s8spnPmjV8GxQZaZF+pahg5DRi\n" +
                "qIutj/g9NKIFpfX8AwcrvjEl3WOOu3qwkvnRi54zNntWTz+RVED3Kl5AtngpNVy8\n" +
                "65NmeIECgYEA+DD8S0eBvUezzchiAmzL80IxMV83PxNf8OTtImjOb/8F7BDMaO0+\n" +
                "h1qXWgGks1a76k5gvdvtcKJh3AlvoAYZd84Qf8CiUDf1Dtzt1LlIDITd6c8vxQDV\n" +
                "730TXqGMuRpr/vRl8lEIFNFUTWRtfjjcbUe1RKkJEg130yNNb3R9LrUCgYEAs3iP\n" +
                "jrIIZIySCN/Zv/HSqD4fNeH5JL4RI1Mge0oylwpP91pDEPvPUy7bPQrFl1n8ZptI\n" +
                "GMFA5jEexD7//+jMQDs3CmFBouC6b/oGg+fvdpgUhD+MVFXI9JSIv0AoVTG4UeGU\n" +
                "/NfQBnjClEHqex3c/lhpVE/a/0hfg2ImoEg99aECgYEA5bu6HRhwgDs5tPaRujro\n" +
                "wh/4FwJSyiDuArZ0xhALXmHKIoweGdXYtkNlq5uvz40uXiD0rWlArKyyNpHJcG+U\n" +
                "7W+hmA9Ab06MmJhp0Sk8BtKJ8x2j0xAF9ytoXYTeFIzfgFzLbPQSrephxU17iIWr\n" +
                "i7//izGIQtySmK6pw8wo/60CgYBRNZGIGUkw7ma6O0iV0T/oP0vyHtGU7ahlmpzN\n" +
                "DRL9Q35Rx/cm/TqgQkiUQ4aLaP2MFGG4SeIrBzkLxhIi411hByptuPpxUE0slC9U\n" +
                "iqPRvxkximveUX8AJSIHoGlfu4LTEkdPbfxEoWoyme0XTrkMIkdvDj9jWoVVtxkR\n" +
                "V/8pYQKBgC+4tdzStGPR0YxRIILWWzajHHSfdFs6q6ruawuZjeiQGQgCK9XusV7s\n" +
                "BN9R9sAHyKOLdPzl6jkWCSqLofv/mMQkcig38whPQpAWdne6+EsIQ2J09WvpLkIu\n" +
                "0UTOaP3zwJv3Rqu6Gl5grccUrQ6Dp0LZ2vVKHHFHXw28eHBMpq7L\n" +
                "-----END RSA PRIVATE KEY-----\n";

            var signer = new AWS.CloudFront.Signer(keyId, privateKey);
            // var expirationEpochTime = new Date().getTime() + (1000 * 60 * 60 * 24);
            var expirationEpochTime = Math.floor(Date.now() / 1000) + (60 * 60 * 24);
            //var expirationEpochTime = 1518127528;

            var targetUrl = "https://cf-h.dev-greg.cs.cc/crossdomain.xml";

            var authorizedUrl = "https://cf-h.dev-greg.cs.cc/*";

            var customPolicy = JSON.stringify({
                Statement: [
                    {
                        Resource: authorizedUrl,
                        Condition: { DateLessThan: { 'AWS:EpochTime': expirationEpochTime } }
                    }
                ]
            });

            // var urlOptions = {
            //     url:"https://cf-h.demo.cs.cc/crossdomain.xml",
            //     expires:expirationEpochTime
            // };

            var urlOptions = {
                url: targetUrl,
                policy: customPolicy
            };

            // var cookieOptions = {
            //     url:"https://cf-h.demo.cs.cc/crossdomain.xml",
            //     expires:expirationEpochTime
            // };
            var cookieOptions = {
                policy: customPolicy,
                expires:expirationEpochTime
            };

            var signedUrl = signer.getSignedUrl(urlOptions);

            var signedCookies = signer.getSignedCookie(cookieOptions);

            // CloudFront-Expires = 1518024315912
            // CloudFront-Key-Pair-Id = "APKAIEQLPKFFN53SW5ZQ"
            // CloudFront-Signature = "mO-OAU1LdV64Z3BcCYLVlAJbi5jEoFyVRnr70oKdU0dLgpKAC7Z6FTVesODVwecBPC~pVaB8Um5mtf6d6J9~xgilNoLH77H5mB99DaALlcBwEwRDPCOEyQxexpOpefexXoDymID7UORZLONYF4T3kIWuCuICt785VON7YWxSvgcgdGb1RCVah~1piPlE~k9y~sx9-SMcBoYXcYV86UqA5-TZd0Y0QpbSypp2D8pA2SuPCLFvQnsK1KyrrRFeRL71St~Vg04td69ugpxqvKLq9FdISTE5h~YkRRFZdAAs~O~RL-yZY2jH5kTBWVoHKlEPgs9c81eYcLOfLi6DVaKRAQ__"

            const options = '; Domain=' + secureCookieDomain + '; Path=/; Secure; HttpOnly'

            var tmpBody = {
                cookieOptions: cookieOptions,
                urlOptions: urlOptions,
                signedUrl: signedUrl
            };

            // set up the response
            var responseEvent = {
                statusCode:200,
                headers: {
                    "Content-Type": "application/json",
                    "Set-Cookie" : "CloudFront-Policy="+signedCookies["CloudFront-Policy"]+options,
                    "SEt-Cookie" : "CloudFront-Key-Pair-Id="+signedCookies["CloudFront-Key-Pair-Id"]+options,
                    "SET-Cookie" : "CloudFront-Signature="+signedCookies["CloudFront-Signature"]+options,
                    "SET-COokie" : "_cssid="+sessionId+options,
                    "Access-Control-Allow-Origin": corsOrigin,
                    "Access-Control-Allow-Methods": "GET",
                    "Access-Control-Allow-Credentials": true,
                    "Access-Control-Max-Age": 3000
                },
                body:JSON.stringify(tmpBody)
            }

            // for now, just invoke callback with the cookies
            callback(null, responseEvent);

            console.log("..processing done for authorize..");
            return;
        } else if (slideShowLookupId.indexOf("list/") > -1) {

            var username = slideShowLookupId.split("/")[1];
            console.log("found request for list for username: " + username);

            db.getSlideShowsForUsername(username).then(function (slideshowListResult) {

                var slideShowObjList = [];

                slideshowListResult.forEach(function(slideshowDbRecord) {
                    var parsedSlideShow = new SlideShow(slideshowDbRecord);
                    slideShowObjList.push(parsedSlideShow);
                });


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
                    body:JSON.stringify(slideShowObjList)
                }

                // for now, just invoke callback with folder uuids
                callback(null, responseEvent);

            }).catch(function (error) {

                console.log("caught error while processing: ", error);

                callback(error);
            });


            console.log("..processing done for list..");
            return;
        }


        if (!csSessionId) {
            throw new SecurityError("Missing session token", 403);
        }


        // create AWS service objects
        dynamodb = new AWS.DynamoDB({apiVersion: '2012-08-10', region: awsRegion});
        s3 = new AWS.S3({apiVersion: '2006-03-01', region: awsRegion});

        // define parameters JSON for retrieving userId data from login table
        var ddbPullParams = {
            TableName: ddbTableName,
            Key: {'csSessionId': {S: csSessionId}},
            ProjectionExpression: 'userId'
        };


        dynamodb.getItem(ddbPullParams).promise().then(function (data) {

            if (!data.Item) {
                console.log("Could not locate login item for csSessionId="+csSessionId);
                throw new SecurityError("Invalid credentials", 403 );
            }

            userId = parseInt(data.Item.userId.S);
            console.log("got userId: " + userId);

            // query the database
            return db.getSlideShowAndPresentationsForLookupId(slideShowLookupId);

        }).then(function (slideShowResult) {

            slideShowDbRecord = slideShowResult.slideShowObj;
            console.log("got slideShowObj: " + JSON.stringify(slideShowDbRecord));


            if (slideShowDbRecord.owner_id == userId) {
                // user is owner of slidshow - always grant access
                console.log("requesting user is owner...");
            } else {

                console.log("authorizing request for security=" + slideShowDbRecord.security + " and isPrivate=" + slideShowDbRecord.isPrivate);

                //authorize the request now based upon security, isPrivate and userId
                if (slideShowDbRecord.security == 'ANONYMOUS') {
                    // no additional requirements
                    console.log("Found anonymous slideshow...");
                } else if (!slideShowDbRecord.isPrivate) {
                    // authenitcation required, but SlideShow is public, so no need to authenticate user as a recipient
                    console.log("found public slideshow...");
                } else {
                    // private slideshow requires provided userId matching an existing SlideShowRecipient.user_id
                    console.log("found private slideshow - validating userId=" + userId);
                    if (slideShowResult.recipientUserIdList.indexOf(userId) >= 0) {
                        console.log("found userId in recipientUserIdList...");
                    } else {
                        // this is our rejection case
                        console.log("User does not have access to requested SlideShow");
                        throw new SecurityError("Insufficient permissions", 403);
                    }
                }

                // user has access, but is this slideshow expired?
                if (slideShowDbRecord.expirationDate) {
                    var expiresTime = slideShowDbRecord.expirationDate.getTime();
                    var nowTime = new Date().getTime();
                    if (expiresTime < nowTime) {
                        console.log("request for expired SlideShow")
                        throw new SecurityError("SlideShow expired", 403);
                    }
                }
            }


            // parse slideshow record into SlideShow object
            var parsedSlideShow = new SlideShowXML(slideShowDbRecord);


            // we'll be updating a few properties in the summary
            var slideShowSummary = parsedSlideShow.summary

            // record number of presentations
            slideShowSummary.presCt = slideShowResult.presentationList.length;

            // while traversing presentations, we'll be gathering a few properties
            var firstPresentationThumbnailXML = null;
            var mostRecentArchiveDate = slideShowDbRecord.modifiedTime;

            // decompress all presentations and concatentate to a single string
            var presentationXmlList = [];
            slideShowResult.presentationList.forEach(function (presentationRecord) {

                // if first presentation, hang onto the thumbnail xml
                if (!firstPresentationThumbnailXML) {
                    firstPresentationThumbnailXML = presentationRecord.thumnbnail_content;
                    firstPresentationThumbnailXML = firstPresentationThumbnailXML.replace(/>\n\s*</g, "><");
                }

                // decompress presentation
                var compressedPresXmlBytes = presentationRecord.compressed_content;
                var compressedBuffer = Buffer.from(compressedPresXmlBytes);
                var decompressedPrsXmlUint8 = zlib.gunzipSync(compressedBuffer);
                var decompressedPrsXml = Buffer.from(decompressedPrsXmlUint8).toString();

                decompressedPrsXml = decompressedPrsXml.replace(/>\n\s*</g, "><");

                //console.log("got decompressedPrsXml: \n" + decompressedPrsXml);

                // parse the presentation xml to count the slides
                var domParser = new DOMParser();
                var presDom = domParser.parseFromString(decompressedPrsXml);
                var slideList = presDom.getElementsByTagName("slideReference");
                console.log("got " + slideList.length + " slides");

                // add to slideCt and total size
                slideShowSummary.slideCt += slideList.length;
                slideShowSummary.totalContentSize += presentationRecord.total_file_size;

                if (mostRecentArchiveDate == null || mostRecentArchiveDate < presentationRecord.archived_date) {
                    mostRecentArchiveDate = presentationRecord.archived_date;
                }

                // add to list
                presentationXmlList.push(decompressedPrsXml);
            });

            // set calculated lastUpdatedDate in both slideShow and summary
            parsedSlideShow.lastUpdatedDate = mostRecentArchiveDate.toISOString();
            parsedSlideShow.lastUpdatedTimestamp = mostRecentArchiveDate.getTime();
            slideShowSummary.lastUpdatedDate = mostRecentArchiveDate.toISOString();

            // generate xml document from SlideShow
            var convert = data2xml();
            slideShowXMLStr = convert('SlideShow', parsedSlideShow);


            // concatenate all presentations to a single string
            var presentationXmlListStr = presentationXmlList.join(' ');

            // replace presentationList token with actual presentationList xml
            slideShowXMLStr = slideShowXMLStr.replace("$presentationList$", presentationXmlListStr);

            // replace slideshow thumbnail with content from first archivedPresentation
            slideShowXMLStr = slideShowXMLStr.replace("<thumbnail>$thumbnail$</thumbnail>", firstPresentationThumbnailXML);

            //console.log("\n---- got slideShowXML\n: " + slideShowXML);

            // gather some metadata for future use
            slideShowUuid = parsedSlideShow.access.uuid._cdata;
            publishedTimestamp = parsedSlideShow.lastUpdatedTimestamp;
            publishedDateTime = slideShowSummary.lastUpdatedDate;

            // upload xml to S3
            slideShowS3XmlKey = "published/slideshow/" + slideShowUuid + "/" + publishedTimestamp + "/slideshow.xml";

            var params = {
                Bucket: downloadBucket,
                Key: slideShowS3XmlKey,
                ContentType: 'application/xml',
                Body: slideShowXMLStr
            };

            console.log("Uploading xml to S3 to bucket=" + downloadBucket + " with slideShowS3XmlKey=" + slideShowS3XmlKey);

            return s3.putObject(params).promise();

        }).then(function (s3Result) {

            console.log("done uploading XML to S3");

            /*******************/
            // generate dtoStart.json

            // parse the presentation xml to count the slides
            var doc = new DOMParser().parseFromString(slideShowXMLStr);

            // use xpath to select elements to build dtoStart
            var showInfo = {};
            // show info values come from SlideShow or SlideShow.access
            showInfo.ownerId = parseInt(xpath.select1("string(/SlideShow/access/ownerId)", doc));
            showInfo.ownerEmail = xpath.select1("string(/SlideShow/access/ownerUsername)", doc);
            showInfo.slideShowName = xpath.select1("string(/SlideShow/displayName)", doc);
            showInfo.slideShowId = parseInt(xpath.select1("string(/SlideShow/id)", doc));

            var presInfos = [];
            // presInfos come from Presentations - we'll need to iterate over each
            xpath.select("/SlideShow/presentations/presentation", doc).forEach( function(presentationDoc) {

                var presInfo = {};
                presInfo.ownerId = parseInt(xpath.select1("string(./access/ownerId)", presentationDoc));
                presInfo.ownerEmail = xpath.select1("string(./access/ownerUsername)", presentationDoc);
                presInfo.presentationId = parseInt(xpath.select1("string(./id)", presentationDoc));
                presInfo.presentationName = xpath.select1("string(./name)", presentationDoc);


                // slideRef data requires iterating over each slideReference node
                var slideRefIds = [];
                var slideRefNames = [];
                var slidePositions = [];
                var slideRefList = xpath.select("./slides/slideReference", presentationDoc);
                slideRefList.forEach(function(slideRefDoc) {
                    slideRefIds.push(parseInt(xpath.select1("string(./id)", slideRefDoc)));
                    var slidePosition = parseInt(xpath.select1("string(./position)", slideRefDoc));
                    slidePositions.push(slidePosition);
                    var slideName = xpath.select1("string(./slide/name)", slideRefDoc);
                    if (slideName == "") {
                        slideName = "Slide " + (slidePosition + 1);
                    }
                    slideRefNames.push(slideName);
                });
                presInfo.slideRefIds = slideRefIds;
                presInfo.slideRefNames = slideRefNames;
                presInfo.slidePositions = slidePositions;

                // push current presInfo into array
                presInfos.push(presInfo);
            })

            // combine infos into dtoStart
            var dtoStart = {
                showInfo: showInfo,
                presInfos: presInfos
            }

            var dtoStartStr = JSON.stringify(dtoStart);

            /*******************/

            // upload json to S3
            slideShowS3DtoStartKey = "published/slideshow/" + slideShowUuid + "/" + publishedTimestamp + "/dtoStart.json";

            var params = {
                Bucket: downloadBucket,
                Key: slideShowS3DtoStartKey,
                ContentType: 'application/json',
                Body: dtoStartStr
            };

            console.log("Uploading json to S3 to bucket=" + downloadBucket + " with slideShowS3DtoStartKey=" + slideShowS3DtoStartKey);

            return s3.putObject(params).promise();

        }).then(function (s3Result) {

            console.log("done uploading dtoStart.json to S3");

            slideShowS3XmlDateTime = new Date().toISOString();

            // define parameters JSON for retrieving userId data from login table
            var ddbPutParams = {
                Item: {
                    'uuid': {S: slideShowUuid},
                    'publishedTimestamp': {N: publishedTimestamp.toString()},
                    'publishedDateTime': {S: publishedDateTime},
                    'type' : {S: 'slideshow'},
                    'lookupId' : {S: slideShowLookupId},
                    's3XmlKey' : {S: slideShowS3XmlKey },
                    's3XmlDateTime' : {S: slideShowS3XmlDateTime },
                    's3DtoStartKey' : {S: slideShowS3DtoStartKey }
                },
                ExpressionAttributeNames: {
                    "#K": "uuid"
                },
                ConditionExpression : " attribute_not_exists(#K)",
                ReturnConsumedCapacity: "TOTAL",
                TableName: publishedContentTable
            };


            console.log("adding published-content record to: " + publishedContentTable);
            console.log("Pushing record: \n" + JSON.stringify(ddbPutParams));

            return dynamodb.putItem(ddbPutParams).promise();

        }).then(function (ddbPutResult) {

            console.log("done adding published-content record");

            // set up the response
            var response = prepareResponse(slideShowXMLStr);

            //console.log("sending response: " + JSON.stringify(slideShowObj));

            // for now, just invoke callback with folder uuids
            callback(null, response);

        }).catch(function (error) {

            if (error.code == "ConditionalCheckFailedException") {

                // this is ok - it just means there is already a record in dynamodb,
                // which we do not want to over-write.
                console.log("published-content record already found - returning success");

                // in this case we'll return as a success
                var response = prepareResponse(slideShowXMLStr);
                callback(null, response);

            } else if (error instanceof SecurityError) {

                console.log("caught SecurityError["+error.httpCode+"]: "+error.message, error);
                var responseErrorEvent = prepareSecurityErrorResponse(error);
                callback(null, responseErrorEvent)

            } else {
                console.log("caught error while processing: ", error);
                callback(error);
            }

        });

    } catch (error) {

        if (error instanceof SecurityError) {

            console.log("caught SecurityError["+error.httpCode+"]: "+error.message, error);
            var responseErrorEvent = prepareSecurityErrorResponse(error);
            callback(null, responseErrorEvent)

        } else {
            console.log("caught unexpected error: ", error);
            callback(error);
        }

    }

    console.log("..processing done..");

}
