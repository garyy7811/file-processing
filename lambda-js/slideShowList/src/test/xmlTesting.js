
var fs = require('fs');

var xpath = require('xpath');
var DOMParser = require('xmldom').DOMParser;

var srcXmlFileName = "request.xml";
var dstJsonFile = "response.xml";

var srcXml = fs.readFileSync(srcXmlFileName).toString();

//console.log("read decompressedPrsXml:\n" + decompressedPrsXml);


// parse the presentation xml to count the slides
var doc = new DOMParser().parseFromString(srcXml);

var opcode = xpath.select1("/request/operation/@opcode", doc).value;
console.log("got opcode: " + opcode);



const xml2js = require('xml2js');

let parser = new xml2js.Parser({explicitArray:false, mergeAttrs:true})

var reqObj = null;
parser.parseString(srcXml, function (err, result) {
    reqObj = result;
});

console.log("reqObj: " + JSON.stringify(reqObj));

var operation = reqObj.request.operation;
console.log("operation: "+ operation);

var params = operation.params;
console.log("params: " + params);

console.log("tenant_id: " + params.tenant_id);
console.log("username: " + params.username);
console.log("include_own_slideshows: " + params.include_own_slideshows);
console.log("include_shared_slideshows: " + params.include_shared_slideshows);
console.log("sort_by_field: " + params.sort_by_field);

var myString = "something format_abc";
var arr = myString.match(/\bformat_(.*?)\b/);
console.log(arr[0] + " " + arr[1]);


var myString = "something <foo>boo</foo> format_abc";
var arr = myString.match(/<foo>(.*?)<\/foo>/);
console.log(arr[0] + " " + arr[1]);
