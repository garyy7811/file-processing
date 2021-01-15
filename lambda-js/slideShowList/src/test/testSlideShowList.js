
var slideShowList = require('../slideShowList.js');

// load environment variables
require('dotenv').config({path:'./env-variables.properties'});

//1u2klv
//https://demo.cs.cc/slideshow/vrs34h





var input = {
    httpMethod: "POST",
    body: "<request> " +
    "    <operation opcode=\"GET_CUSTOMSHOW_SLIDESHOWS\"> " +
    "        <params " +
    "                tenant_id=\"12\" " +
    "                username=\"viktor@sg.com\" " +
    "                include_own_slideshows=\"true\" " +
    "                include_shared_slideshows=\"true\" " +
    "                sort_by_field=\"lastModifiedDate\" " +
    "                sort_direction=\"asc\" " +
    "                search_phrase=\"Test\" " +
    "                page_size=\"10\" " +
    "                offset=\"0\" " +
    "        /> " +
    "    </operation> " +
    "</request>"
};

function lambdaCallback(err, result) {

    if (err) {
        console.log("got error:", err);
        return;
    }

    if (result == null) {
        console.log("no result???");
        return;
    }

    // console.log("got result:  " + JSON.stringify(result), null, 4);
    console.log("got result:  ");
    console.dir(result, {depth: null, colors: true});
}

console.log("got process.env.csDbHost: " + process.env.csDbHost)

console.log("invoking handler");
// slideShowList.handler(input, null, lambdaCallback);
slideShowList.handler(input, null).then((result) => {
    console.log("slideShowList.handler returned: " + result);
    lambdaCallback(null, result);
});
console.log("handler inovked....");


