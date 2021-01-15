console.log('Loading function');

var amfjs = require('amfjs');

var Duplex = require('stream').Duplex;

var streamBuffers = require('stream-buffers');

function base64StrToObject(base64Str) {

    console.log("converting to Buffer...");
    var bodyBuffer = Buffer.from(base64Str, 'base64');

    // create a readable stream from Buffer
    // from: http://derpturkey.com/buffer-to-stream-in-node/
    var stream = new Duplex();
    stream.push(bodyBuffer);
    stream.push(null);

    // pass stream to decoder
    var decoder = new amfjs.AMFDecoder(stream);

    // decode to AMF3 object
    return decoder.decode(amfjs.AMF3);
}

function objectToBase64Str(obj) {

// create a writable stream using stream-buffers module
    var outStream = new streamBuffers.WritableStreamBuffer();

// pass stream to encoder
    console.log("creating encoder");
    var encoder = new amfjs.AMFEncoder(outStream);

// encode object back to bytes
    console.log("encoding object to stream");
    encoder.writeObject(obj, amfjs.AMF3);
    console.log("encoder returned...");

// get the Buffer that the stream wrote to
    var outputBuffer = outStream.getContents();

    console.log("outStream.Buffer typeof:" + (typeof outputBuffer));
    console.log("outStream.Buffer: " + outputBuffer);

// validate inputBuffer matches outputBuffer
    console.log("inputBuffer matches outputBuffer!");

// encode the bytes to a base64 string
    var outputStringB64 = outputBuffer.toString('base64');
    return outputStringB64;

}

exports.handler = function (event, context, callback) {

    if (event.hasOwnProperty("body")) {
        var args = base64StrToObject(event.body);

        return callback(null, {statusCode: 200, body: objectToBase64Str(event)});
    }
    else {
        console.log("...")
    }

}
