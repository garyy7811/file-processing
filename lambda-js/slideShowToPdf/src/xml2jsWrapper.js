var xml2js = require('xml2js');
var xml2jsParser = new xml2js.Parser({explicitArray:false, mergeAttrs:true, emptyTag:{}, charkey:"$t"});

exports.toJson = function(xml, options) {

    var xml2jsObj = null;
    xml2jsParser.parseString(xml, function(err, result) {
        xml2jsObj = result;
    });

    var xml2jsStr = JSON.stringify(xml2jsObj);
    return xml2jsStr;
}


// test code to verify output of xml2json == xml2js
//
// var xml2jsonObj = parser.toJson(srcXml,{sanitize:false});
// var xml2jsonStr = xml2jsonObj;
// fs.writeFileSync("xml2jsonStr.json", xml2jsonStr );
//
// var xml2jsParser = new xml2js.Parser({explicitArray:false, mergeAttrs:true, emptyTag:{}});
// var xml2jsObj = null;
// xml2jsParser.parseString(srcXml, function(err, result) {
//     xml2jsObj = result;
// });
//
// var xml2jsStr = JSON.stringify(xml2jsObj);
// fs.writeFileSync("xml2jsStr.json", xml2jsStr );
//
