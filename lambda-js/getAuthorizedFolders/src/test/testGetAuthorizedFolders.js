
var getAuthorizedFolders = require('../getAuthorizedFolders.js');

// load environment variables
require('dotenv').config({path:'./env-variables.properties'});

// define our mock input event
var input = {
    body: {
        csSessionId:'TEST_GREG_ID'
    }
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

    // console.log("got result: \n" + JSON.stringify(result), null, 4);
    console.log("got result: \n");
    console.dir(result, {depth: null, colors: true});
}

console.log("got process.env.awsLoginVerificationDynamoTablename: " + process.env.awsLoginVerificationDynamoTablename)

console.log("invoking handler");
getAuthorizedFolders.handler(input, null, lambdaCallback);
console.log("handler inovked....");


