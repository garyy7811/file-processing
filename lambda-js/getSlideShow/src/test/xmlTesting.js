
var fs = require('fs');

var xpath = require('xpath');
var DOMParser = require('xmldom').DOMParser;

var srcXmlFileName = "hlqe13-test-2-pres.xml";
var dstJsonFile = "hlqe13-startDto.json";

var decompressedPrsXml = fs.readFileSync(srcXmlFileName).toString();

//console.log("read decompressedPrsXml:\n" + decompressedPrsXml);


// parse the presentation xml to count the slides
var doc = new DOMParser().parseFromString(decompressedPrsXml);

// use xpath to select elements
var slideList = xpath.select("//slideReference", doc);
console.log("got " + slideList.length + " slides");

var allAccessList = xpath.select("//access", doc);
console.log("got "+ allAccessList.length + " total access objects");

var slideAccessList = xpath.select("//slide/access", doc);
console.log("got "+ slideAccessList.length + " slide.access objects");

var showInfo = {};

var slideShowAccess = xpath.select("/SlideShow/access", doc, true);

var ownerId = parseInt(xpath.select1("string(/SlideShow/access/ownerId)", doc));
showInfo.ownerId = ownerId;
console.log("ownerId: " + ownerId);
var ownerEmail = xpath.select1("string(/SlideShow/access/ownerUsername)", doc);
showInfo.ownerEmail = ownerEmail;
console.log("ownerEmail: " + ownerEmail);

var slideShowName = xpath.select1("string(/SlideShow/displayName)", doc);
showInfo.slideShowName = slideShowName;
console.log("slideShowName: " + slideShowName);

var slideShowId = parseInt(xpath.select1("string(/SlideShow/id)", doc));
showInfo.slideShowId = slideShowId;
console.log("slideShowId: " + slideShowId);

var presInfos = [];
var presentationList = xpath.select("/SlideShow/presentations/presentation", doc);
console.log("got " + presentationList.length + " presentations");

presentationList.forEach( function(presentationDoc) {

    var presInfo = {};
    var ownerId = parseInt(xpath.select1("string(./access/ownerId)", presentationDoc));
    presInfo.ownerId = ownerId;
    console.log("ownerId: " + ownerId);
    var ownerEmail = xpath.select1("string(./access/ownerUsername)", presentationDoc);
    presInfo.ownerEmail = ownerEmail;
    console.log("ownerEmail: " + ownerEmail);

    var presentationId = parseInt(xpath.select1("string(./id)", presentationDoc));
    presInfo.presentationId = presentationId;
    console.log("presentationId: " + presentationId);

    var presentationName = xpath.select1("string(./name)", presentationDoc);
    presInfo.presentationName = presentationName;
    console.log("presentationName: " + presentationName);


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
    console.log("slideRefIds: " + slideRefIds);
    console.log("slideRefNames: " + slideRefNames);
    console.log("slidePositions: " + slidePositions);

    presInfos.push(presInfo);
})

var dtoStart = {
    showInfo: showInfo,
    presInfos: presInfos
}

console.log("dtoStart: \n" + JSON.stringify(dtoStart,null, 2));

/*
{
  "showInfo": {
    "ownerId": long,
    "ownerEmail": "string",
    "slideShowName": "string",
    "slideShowId": long,
  },
  "presInfos": [ // array of presInfo objects of the following form:
    {
      "ownerId": long,
      "ownerEmail": "",
      "slideRefIds": [array of long],
      "slideRefNames": [array of string],
      "presentationName": "",
      "presentationId": long,
      "slidePositions": [array of long]
    }
  ]
}
 */
