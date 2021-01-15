console.log('Loading function');


var db = require("./lib/dbUtils.js");
var io = require("./lib/ioUtils.js");
var bodybuilder = require('bodybuilder');
var elasticsearch = require('elasticsearch');

var AWS = require('aws-sdk');

// configuration values will be lazy-initialized
var awsRegion;
var ddbTableName;
var elasticsearchHost;
var esIndexName;

var isBinaryRequest = false;
var isProxyRequest = false;
var inputBody;
var csSessionId;

function init(event) {

    // pull configuration values from environment
    awsRegion = process.env.aws_region;
    ddbTableName = process.env.awsLoginVerificationDynamoTablename;
    elasticsearchHost = process.env.awsResourceIndexEsHost;
    esIndexName = process.env.awsResourceIndexName;

    // now obtain input parameters from event
    if (event.csSessionId) {
        console.log("got csSessionId directory from input event");
        csSessionId = event.csSessionId;
        isProxyRequest = false;
        isBinaryRequest = false;
    } else {

        if (!event.body) {
            throw new Error("Missing event.body!");
        }

        isProxyRequest = true;
        isBinaryRequest = event.isBase64Encoded;

        if (isBinaryRequest) {
            inputBody = io.base64StrToObject(event.body);
        } else {
            if (typeof event.body === 'string') {
                console.log("parsing event.body: " + event.body);
                inputBody = JSON.parse(event.body);
            } else {
                inputBody = event.body;
            }
        }

        console.log("isBinaryRequest="+isBinaryRequest);

        console.log("got inputBody: " + inputBody);
        console.log("inputBody typeof: " + (typeof inputBody));
    }

    if (!inputBody.csSessionId) {
        throw new Error("Missing required parameter csSessionId");
    }

    csSessionId = inputBody.csSessionId;
}

function prepareResponse(payload) {


    var response = payload;

    if (isProxyRequest) {

        // this indicates API gateway, so we need to return an object with status and body

        // so we need a response event with payload serialized with amf-base64
        var responsePayload = payload;

        if (isBinaryRequest) {
            responsePayload = io.objectToBase64Str(payload);
        } else {
            responsePayload = JSON.stringify(payload);
        }

        var responseEvent = {
            statusCode:200,
            body:responsePayload,
            isBase64Encoded:isBinaryRequest
        }

        return responseEvent;
    }

    return payload;
}

exports.handler = function (event, context, callback) {

    console.log("handler entered");

    console.log("got input: " + JSON.stringify(event, null, 4));

    try {


        init(event);

        // configure aws sdk
        AWS.config.update({region: awsRegion});

        console.log("got csSessionId: " + csSessionId);

        // variables populated during processing
        var userId = 0;
        var userFolderUuidDTO;

        // create DynamoDB service object
        var request = new AWS.DynamoDB({region: awsRegion, apiVersion: '2012-08-10'});

        // define parameters JSON for retrieving userId data from login table
        var ddbPullParams = {
            TableName: ddbTableName,
            Key: {'csSessionId': {S: csSessionId}},
            ProjectionExpression: 'userId'
        };


        request.getItem(ddbPullParams).promise().then(function (data) {

            if (!data.Item) {
                throw new Error("Could not locate login item for sessionId '" + csSessionId + "'");
            }

            userId = data.Item.userId.S;
            console.log("got userId: " + userId);

            console.log("initializing db");
            db.init();

            return db.getFoldersForUserId(userId)

        }).then(function (folderResult) {

            userFolderUuidDTO = folderResult;
            console.log("got userFolderDTO: " + JSON.stringify(userFolderUuidDTO));

            folderUuidList = folderResult.personalFolderUuids
                .concat(folderResult.libraryFolderUuids)
                .concat(folderResult.userResourceFolderUuid);

            var esclient = new elasticsearch.Client({
                host: elasticsearchHost,
                log: 'info'
            });

            var body = bodybuilder()
                .size(1000)
                .filter('terms', '_id', folderUuidList);

            var bodyObj = body.build();

            console.log("executing search:");
            console.log(JSON.stringify(bodyObj));

            return esclient.search({
                index: esIndexName,
                type: 'resource_library_item',
                body: bodyObj
            });

        }).then(function (resp) {

            console.log("got total hits: " + resp.hits.total);

            // map id to uuidPath
            var uuidToUuidPathMap = {};
            resp.hits.hits.forEach(function(item) {
                uuidToUuidPathMap[item._id] = item._source.uuidPath;
            });

            // now generate userFolderUuidPathDTO

            var userFolderUuidPathDTO = {};
            userFolderUuidPathDTO.userResourceFolderUuidPath = uuidToUuidPathMap[userFolderUuidDTO.userResourceFolderUuid];

            userFolderUuidPathDTO.personalFolderUuidPaths = [];
            userFolderUuidDTO.personalFolderUuids.forEach(function(personalFolderUuid) {
                userFolderUuidPathDTO.personalFolderUuidPaths.push(uuidToUuidPathMap[personalFolderUuid]);
            })

            userFolderUuidPathDTO.libraryFolderUuidPaths = [];
            userFolderUuidDTO.libraryFolderUuids.forEach(function(libraryFolderUuid) {
                userFolderUuidPathDTO.libraryFolderUuidPaths.push(uuidToUuidPathMap[libraryFolderUuid]);
            })


            // set up the response
            var response = prepareResponse(userFolderUuidPathDTO);

            console.log("sending response: " + JSON.stringify(response));

            // for now, just invoke callback with folder uuids
            callback(null, response);

        }).catch(function (error) {

            console.log("got error while processing: ", error);

            callback(error);
        });

    } catch (error) {

        console.log("got unexpected error: ", error);

        callback(error);
    }

    console.log("..processing done..");

}
