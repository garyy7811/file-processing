/**
 * This is the implementation of a full lambda function that takes as input an http sessionid,
 * validates the session by retrieving the matching dynamodb record in the login verification table,
 * loads the top-level folder uuids this user has access to, then queries elasticsearch to retrieve all of the
 * documents whose targetUuid matches the results of the mysql queries.
 *
 * The 'exports.handler' line declares the module function that shall be invoked by lambda.
 */

console.log('Loading function');

var bodybuilder = require('bodybuilder');
var elasticsearch = require('elasticsearch');

// Configuring the AWS SDK
// NOTE: you must have the aws sdk installed, and a credentials file in ~/.aws/credentials
var AWS = require('aws-sdk');

// environment configuration
var awsRegion;
var ddbTableName;
var elasticsearchHost;
var esIndexName;

// request parameters
var isProxyRequest = false;
var inputParameters;

function init(event) {

    // pull configuration values from environment
    awsRegion = process.env.aws_region;
    ddbTableName = process.env.awsLoginVerificationDynamoTablename;
    elasticsearchHost = process.env.awsResourceIndexEsHost;
    esIndexName = process.env.awsResourceIndexName;

    // configure aws sdk
    AWS.config.update({region: awsRegion});

    if (event.body) {
        isProxyRequest = true;

        if (typeof event.body === 'string') {
            console.log("parsing event.body: " + event.body);
            inputParameters = JSON.parse(event.body);
        } else {
            inputParameters = event.body;
        }

    } else {
        console.log("NOT a proxy request - using direct input");
        isBinaryRequest = false;
        isProxyRequest = false;
        inputParameters = event;
    }
}

function prepareResponse(payload) {


    var response = payload;

    if (isProxyRequest) {

        // this indicates API gateway, so we need to return an object with status and body

        // so we need a response event with payload serialized with amf-base64
        var responsePayload = JSON.stringify(payload);

        var responseEvent = {
            statusCode:200,
            body:responsePayload
        }

        return responseEvent;
    }

    return payload;
}

exports.handler = function(event, context, callback) {

    console.log("handler entered");

    init(event);

    var sessionId =  inputParameters.csSessionId;

    console.log("got csSessionId: " + sessionId);

    // create DynamoDB service object
    var request = new AWS.DynamoDB({region:awsRegion, apiVersion: '2012-08-10'});

    // define parameters JSON for retrieving userId data from login table
    var ddbPullParams = {
        TableName: ddbTableName,
        Key: {'csSessionId': {S: sessionId}},
        ProjectionExpression: 'userId'
    };


    console.log("locating session record using params: " + JSON.stringify(ddbPullParams));

    request.getItem(ddbPullParams).promise().then(function (data) {

        if (!data.Item) {
            throw new Error("Could not locate login item for sessionId '" + sessionId + "'");
        }

        var folderUuidPathList = inputParameters.folderUuidPathList;

        console.log("\n\n---> got folderUuidList: " + folderUuidPathList);

        var esclient = new elasticsearch.Client({
            host: elasticsearchHost,
            log: 'info'
        });


        // start query with size, from, and uuid filter
        var body = bodybuilder()
            .from(inputParameters.fromOffset)
            .size(inputParameters.pageSize)
            .filter('terms', 'uuidPath', folderUuidPathList.split(","));

        // optionally add filters for resource type
        if (inputParameters.includeVideo) {
            body = body.orFilter('match', 'targetType', 'video');
        }

        if (inputParameters.includeImage) {
            body = body.orFilter('match', 'targetType', 'image');
        }

        if (inputParameters.includeFlash) {
            body = body.orFilter('match', 'targetType', 'flash');
        }

        // add query arugments for searchString
        body = body.orQuery('term', 'targetName.raw', { value: inputParameters.searchString, boost: 100 })
            .orQuery('match_phrase_prefix', 'targetName', { query: inputParameters.searchString, slop: 10 })
            .orQuery('match', 'targetName', inputParameters.searchString)
            .orQuery('term', 'orgFilename.raw', { value: inputParameters.searchString, boost: 10 })
            .orQuery('match_phrase_prefix', 'orgFilename', { query: inputParameters.searchString, slop: 100 })
            .orQuery('match', 'orgFilename', inputParameters.searchString)
            .queryMinimumShouldMatch(1);

        var splitWords = inputParameters.searchString.split(" ");
        splitWords.forEach(function(searchWord) {
            if (searchWord != null && searchWord.length > 1) {
                console.log("adding wildcard search for term='" + searchWord+"'");
                var searchPhrase = "*" + searchWord.toLowerCase() + "*";
                body.orQuery("wildcard", "targetName", searchPhrase);
            } else {
                console.log("skipping wildcard search for short term='" + searchWord+"'");
            }
        })

        if (inputParameters.sortField) {
            var sortOrder = (inputParameters.sortOrder ? inputParameters.sortOrder : 'asc');
            console.log("applying sort=" + inputParameters.sortField + "--" + sortOrder);
            body = body.sort(inputParameters.sortField, sortOrder);
        }

        var bodyObj = body.build();

        console.log("executing search:");
        console.log(JSON.stringify(bodyObj));
        // console.dir(bodyObj, {depth: null, colors: true});

        return esclient.search({
            index: esIndexName,
            type: 'resource_library_item',
            body: bodyObj
        });

    }).then(function (resp) {

        console.log("got total hits: " + resp.hits.total);


        var currentHits = resp.hits.hits;
        //console.log("full hits: " + JSON.stringify(currentHits));

        var resourceRecords = [];
        currentHits.forEach(function(item) {
            resourceRecords.push(item._source);
        });

        var result = {
            totalHits:resp.hits.total,
            currentHits:currentHits.length,
            currentOffset: 0,
            resourceRecords: resourceRecords
        }

        var response = prepareResponse(result);
        callback(null, response);

    }).catch(function (error) {

        console.log("got error: ", error);

        callback("got error: " + error);
    });

    console.log("..processing done..");
}