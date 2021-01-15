
var doSearchResources = require('../searchResources.js');

// load environment variables
require('dotenv').config({path:'./env-variables.properties'});

// define our mock input event
var input = {
    fromOffset: 0,
    pageSize: 50,
    csSessionId:'TEST_GREG_ID',
    folderUuidPathList:'518f4da339e5dd7f0139e98b5f010087/518f4da339e5dd7f0139e98e3866009f',
    // folderUuidPathList:"518f4da339e5dd7f0139e98b5efb0084,518f4da339e5dd7f0139e98b5efb0084,4028817b405e372801405f5fa6050002,4028817b405e372801405f5fbe350005,4028817b405e372801405f5fd5f30008,4028817b405e372801405f5fee33000b,40288ae4557410e8015574203b790003",
    includeVideo:true,
    includeImage:true,
    includeFlash:true,
    searchString:'skate',
    // sortField:'targetName',
    sortOrder:''
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
// doSearchResources.handler(input, null, lambdaCallback);
var proxyInput = {
    body: JSON.stringify(input)
}
doSearchResources.handler(proxyInput, null, lambdaCallback);
console.log("handler inovked....");


