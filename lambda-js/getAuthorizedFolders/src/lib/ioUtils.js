/**
 * serialization routines for working with base64-encodings of amf-3 serialized payloads
 */

var Duplex = require('stream').Duplex;
var amfjs = require('amfjs');
var streamBuffers = require('stream-buffers');

/**
 * Takes a base-64 string as input, then attempts to decode result as a binary amf3 payload.
 *
 * @param base64Str
 * @returns {*}
 */
function base64StrToObject(base64Str) {

    console.log("converting to Buffer...");
    var bodyBuffer = Buffer.from(base64Str, 'base64');

    // create a readable stream from Buffer
    // from: http://derpturkey.com/buffer-to-stream-in-node/
    var stream = new Duplex();
    stream.push(bodyBuffer);
    stream.push(null);

    // pass stream to new decoder
    var decoder = new amfjs.AMFDecoder(stream);

    // decode to AMF3 object
    console.log("decoding amf3 bytes to object");
    return decoder.decode(amfjs.AMF3);
}

/**
 * Takes a javascript object, serializes to amf3, then converts to base64 string
 *
 * @param obj
 * @returns {string}
 */
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
    console.log("outStream.Buffer.length= " + outputBuffer.length);
    console.log("outStream.Buffer[0-3] = " + (outputBuffer[0]) + ","+ (outputBuffer[1]) + ","+ (outputBuffer[2]) + ","+ (outputBuffer[3]) )

    // encode the bytes to a base64 string
    var outputStringB64 = outputBuffer.toString('base64');
    return outputStringB64;

}

// export the functions to define this module
exports.base64StrToObject = base64StrToObject;
exports.objectToBase64Str = objectToBase64Str;