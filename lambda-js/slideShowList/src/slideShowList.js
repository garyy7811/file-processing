console.log('Loading function');

// 3rd-party modules
const xml2js = require('xml2js');
let parser = new xml2js.Parser({explicitArray:false, mergeAttrs:true})
const data2xml = require('data2xml');

// internal modules
const db = require("./lib/dbUtils.js");
const xmlDocs = require("./lib/xmlDocs");

// env variables
let cdnMediaUrlBase = "";
let csAppServerBaseUrl = "";

function init(event) {

    // pull configuration values from environment
    cdnMediaUrlBase = process.env.cdnMediaUrlBase;
    csAppServerBaseUrl = process.env.csAppServerBaseUrl;
}

/**
 * lambda entry-point, takes event with full proxy object, invokes processGetSlideShows, handles errors,
 * prepares proxy response.
 *
 * This is an async method that implicitly returns a Promise that resovles to a proxy response.
 *
 * @param event
 * @param context
 * @returns {Promise<*>}
 */
exports.handler = async function (event, context) {

    console.log("handler entered");

    console.log("got input: " + JSON.stringify(event, null, 4));

    try {

        init(event);

        // async call to process request, sending in the proxy body
        let slideShowObjList = await processGetSlideShows(event.body);

        // set up the response
        var responseEvent = {
            statusCode:200,
            headers: {
                "Content-Type": "text/xml"
            },
            body:slideShowObjList
        }

        return responseEvent;

    } catch (error) {

        console.log("caught unexpected error: ", error);

        let errorResponse = '<response result="failure"><error message="'+error.message+'"/></response>';
        // set up the response
        var responseEvent = {
            statusCode:500,
            headers: {
                "Content-Type": "application/json"
            },
            body:errorResponse
        }

        return responseEvent;
    }

    console.log("..processing done..");
}

/**
 * Expects an xml document formatted as follows:
 * <request>
 *   <operation opcode="GET_CUSTOMSHOW_SLIDESHOWS">
 *       <params
 *               tenant_id="12"
 *               username="foo@foo.com"
 *               include_own_slideshows="true"
 *               include_shared_slideshows="true"
 *               sort_by_field="name"
 *               sort_direction="asc"
 *               search_phrase=""
 *               page_size="10"
 *               offset="0"
 *       />
 *   </operation>
 * </request>
 *
 * @param getSlideShowsRequest
 * @returns {Promise<void>}
 */
async function processGetSlideShows(getSlideShowsRequestXml) {

    var reqObj = null;

    try {
        // attempt to parse input
        parser.parseString(getSlideShowsRequestXml, function (err, result) {
            reqObj = result;
        });
    } catch (e) {
        console.log("failed to parse input: " + getSlideShowsRequestXml);
        console.log("Unexpected error parsing input", e);
        throw new Error("Invalid input", e);
    }


    console.log("reqObj: " + JSON.stringify(reqObj));

    var operation = reqObj.request.operation;
    console.log("operation: "+ operation);

    // validate operation
    if (operation.opcode != "GET_CUSTOMSHOW_SLIDESHOWS") {
        throw new Error("Unknown operation!");
    }

    if (!operation.params) {
        throw new Error("Missing params!");
    }

    var params = operation.params;

    console.log("tenant_id: " + params.tenant_id);
    console.log("username: " + params.username);
    console.log("include_own_slideshows: " + params.include_own_slideshows);
    console.log("include_shared_slideshows: " + params.include_shared_slideshows);
    console.log("search_phrase: " + params.search_phrase);
    console.log("sort_by_field: " + params.sort_by_field);
    console.log("sort_direction: " + params.sort_direction);
    console.log("page_size: " + params.page_size);
    console.log("offset: " + params.offset);


    console.log("initializing db");
    db.init();


    let pageSize = Number.parseInt(params.page_size);
    let offset = Number.parseInt(params.offset);


    let ownerUserMatch = (params.include_own_slideshows == "true") ? params.username : "nomatch";
    let sharedUserMatch = (params.include_shared_slideshows == "true") ? params.username : "nomatch";
    let searchPhrase = params.search_phrase;

    let sortField =  "SS.displayName";
    if (params.sort_by_field == "name") {
        sortField = "SS.displayName";
    } else if (params.sort_by_field == "lastModifiedDate") {
        sortField = "AP.archived_date";
    } else if (params.sort_by_field) {
        throw new Error("Invalid sort_by_field");
    }

    let sortDirection = "ASC";
    if (params.sort_direction == "asc") {
        sortDirection = "ASC";
    } else if (params.sort_direction == "desc") {
        sortDirection = "DESC";
    } else {
        throw new Error("Invalid sort_direction");
    }

    var numSlideShows = await db.getNumSlideShowsForUsername(ownerUserMatch, sharedUserMatch, searchPhrase);


    var slideshowListResult = await db.getSlideShowsForUsername(ownerUserMatch, sharedUserMatch, searchPhrase, sortField, sortDirection, pageSize, offset);

    var slideShowObjList = [];
    slideshowListResult.forEach(function(slideshowDbRecord) {

        // parse out the thumbnail filename from the archived xml
        let thumbFileName = slideshowDbRecord.thumnbnail_content.match(/<fileName>(.*?)<\/fileName>/)[1];

        // now check if thubmnail file is wrapped in CDATA, in which case we'll strip it out
        if (thumbFileName.match(/<!\[CDATA\[.*?\]\]>/)) {
            thumbFileName = thumbFileName.match(/<!\[CDATA\[(.*?)\]\]>/)[1];
        }

        let name = slideshowDbRecord.displayName;
        let description = ""; // slideshowDbRecord.description;

        // set lastModifiedDate to presentation archived_date
        let lastModifiedDate = slideshowDbRecord.archived_date;

        let thumbnailUrl = cdnMediaUrlBase + "thumbnail/"+thumbFileName;
        let ownerEmail = slideshowDbRecord.owner_username;
        let isOwner = (ownerEmail == params.username) ? "true" : "false";
        let slideshowUrl = csAppServerBaseUrl + "slideshow/"+slideshowDbRecord.lookup_id;
        let slideshowPreviewUrl = csAppServerBaseUrl + "viewer?s=" + slideshowDbRecord.lookup_id;
        // add additional query string params to force auto-loading the html viewer when necessary
        if (!slideshowDbRecord.html_only) {
            slideshowPreviewUrl += "&noflash=true";
        }
        if (!slideshowDbRecord.auto_load) {
            slideshowPreviewUrl += "&autoload=true";
        }
        if (slideshowDbRecord.security == 'IDENTITY_REQUIRED') {
            // this will auto-login the zoomifier user if they are not already authenticated on CustomShow
            slideshowPreviewUrl += "&csu="+params.username;
        }

        var parsedSlideShow = new xmlDocs.SlideShowXML(name, description, lastModifiedDate,
            thumbnailUrl, ownerEmail, isOwner, slideshowUrl, slideshowPreviewUrl);

        slideShowObjList.push(parsedSlideShow);
    });


    // populate our response object to be serialized to xml
    var responseXmlObj = new xmlDocs.SlideShowListResponse('success', pageSize, offset, slideShowObjList.length, numSlideShows, Math.ceil(numSlideShows/pageSize), slideShowObjList);

    // generate xml document from SlideShow
    var convert = data2xml({ xmlDecl : false });

    let responseXml = convert('response', responseXmlObj);

    console.log("responseXml: \n" + responseXml);

    return responseXml;

}


