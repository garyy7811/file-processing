
var getSlideShow = require('../getSlideShow.js');

// load environment variables
require('dotenv').config({path:'./env-variables.properties'});

//1u2klv
//https://demo.cs.cc/slideshow/vrs34h



//define our mock input event
var input = {
    // slideShowLookupId:'lmvmv0'
    //slideShowLookupId:'ju7i4j',
    slideShowLookupId:'ds21mq', // TEST
    csSessionId:'C4325880C285488AC4ECCAE6FEFB18BE' // TEST
};

// var input = {
//     headers: {
//         origin: "https://demo.cs.cc",
//         Cookie: "__utmz=40063015.1518721374.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); _cssid=8B82364C20B9CC536691C083BD2E4194"
//     },
//     httpMethod: "GET",
//     pathParameters: {
//         proxy: "vrs34h"
//     }
// };

// var input = {
//     headers: {
//         origin: "test.cs.cc",
//         Cookie: "__utmz=40063015.1518721374.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); _cssid=8ED153F881D409C8F59C317FA6F79823"
//     },
//     httpMethod: "GET",
//     pathParameters: {
//         proxy: "hlqe13"
//     }
// };

// var input = {
//     headers: {
//         origin: "dev-greg.cs.cc",
//         Cookie: "__utmz=40063015.1518721374.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); _cssid=2AB15595710461A09B95BF119CCF980F"
//     },
//     httpMethod: "GET",
//     pathParameters: {
//         proxy: "7vvjnt"
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

    console.log("got result: \n" + JSON.stringify(result), null, 4);
    // console.log("got result: \n" + result);
    // console.dir(result, {depth: null, colors: true});
}

console.log("got process.env.csDbHost: " + process.env.csDbHost)

console.log("invoking handler");
getSlideShow.handler(input, null, lambdaCallback);
console.log("handler inovked....");


