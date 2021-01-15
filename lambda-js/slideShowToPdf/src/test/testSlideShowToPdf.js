
var slideShowToPdf = require('../slideShowToPdf.js');

// load environment variables
require('dotenv').config({path:'./env-variables.properties'});

// load environment variables
require('dotenv').config({path:'./env-variables.properties'});

//1u2klv
//https://demo.cs.cc/slideshow/vrs34h



//define our mock input event

// var input = {
//     headers: {
//         origin: "https://demo.cs.cc",
//         Cookie: "__utmz=40063015.1518721374.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); _cssid=3E763FA2B95FB5608A77117C48B9EF4A"
//     },
//     httpMethod: "GET",
//     pathParameters: {
//         proxy: "vrs34h"
//     }
// };

var input = {
    headers: {
        origin: "test.cs.cc",
        Cookie: "__utmz=40063015.1518721374.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); _cssid=C4325880C285488AC4ECCAE6FEFB18BE"
    },
    httpMethod: "GET",
    pathParameters: {
        // proxy: "518f4da35116af70015120e3061c01d8/1535635131000"
        proxy: "latest/ds21mq"
    },
    queryStringParameters: {
        rebuildPdf: "true",
        noCleanup:"true"
    }
};

// var input = {
//     headers: {
//         origin: "dev-greg.cs.cc",
//         Cookie: "__utmz=40063015.1518721374.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); _cssid=FB1414BE675EA12DB4297794C07E8D94"
//     },
//     httpMethod: "GET",
//     pathParameters: {
//         proxy: "40288a3b61d3ca2c0161d3ce6f150001/1519658998000/7vvjnt"
//     },
//     queryStringParameters: {
//         rebuildPdf: "true",
//         noCleanup:"true"
//     }
// };

// var input = {
//     pathParameters: {
//         proxy:'list/greg@sg.com'
//     }
// };

// var input = {
//     pathParameters: {
//         proxy:'authorize/fakesessionid'
//     }
// };

function lambdaCallback(err, result) {

    if (err) {
        console.log("got error:", err);
        return;
    }

    if (result == null) {
        console.log("no result???");
        return;
    }

    // console.log("got result: \n" + JSON.stringify(result), null, 4);
    console.log("got result: \n");
    console.dir(result, {depth: null, colors: true});
}

console.log("got process.env.csDbHost: " + process.env.csDbHost)

console.log("invoking handler");
slideShowToPdf.handler(input, null, lambdaCallback);
console.log("handler inovked....");


