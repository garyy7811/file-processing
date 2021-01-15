/**
 * this class demonstrates a full round trip from a base64-encoded string, decoded to a binary Buffer,
 * decoded to an AMF3 Object, then re-encoded to an AMF3 binary Buffer, and re-encded to a base64 string.
 *
 * NOTE: we use the simple 'Duplex' class from the built-in Streams module for the readable stream, and a
 * WritableStream from an open-source stream-buffers module, which provides a very basic synchronous implementation of a
 * writiable stream.
 *
 */

// declare our dependencies
const assert = require('assert');
var stream = require('stream');
var streamBuffers = require('stream-buffers');
var amfjs = require('amfjs');

// define our input:
// this is the object we are encoding/decoding (an array containing a string):
const inputObject = ["{\n  \n  \"query\": {\n    \"match_all\": {}\n    \n  },\n  \"size\": 20\n}"];
// this is a base-64 string representing an AMF3-encoded object.
const inputStringB64 = "CQMBBn17CiAgCiAgInF1ZXJ5IjogewogICAgIm1hdGNoX2FsbCI6IHt9CiAgICAKICB9LAogICJzaXplIjogMjAKfQ==";

console.log("inputObject: " + inputObject);
console.log("inputStringB64: " + inputStringB64);

// use Buffer to convert base64 string to bytes
console.log("converting to Buffer...");
var inputBuffer = Buffer.from(inputStringB64, 'base64');

// push Buffer into a Duplex instance which functions as a readable stream
var inStream = new stream.Duplex();
inStream.push(inputBuffer);
inStream.push(null);

// pass stream to decoder
var decoder = new amfjs.AMFDecoder(inStream);

// decode to AMF3 object
console.log("converting Buffer to AMF3...");
var amfObj = decoder.decode(amfjs.AMF3);

console.log("amfObj typeof: " + (typeof amfObj));
console.log("got amfObj: " + amfObj);

// validate the decoded object is correct
assert.deepEqual(inputObject, amfObj);
console.log("decoded object equals inputObject!");

// create a writable stream using stream-buffers module
var outStream = new streamBuffers.WritableStreamBuffer();

// pass stream to encoder
console.log("creating encoder");
var encoder = new amfjs.AMFEncoder(outStream);

// encode object back to bytes
console.log("encoding object to stream");
encoder.writeObject(amfObj, amfjs.AMF3);
console.log("encoder returned...");

// get the Buffer that the stream wrote to
var outputBuffer = outStream.getContents();

console.log("outStream.Buffer typeof:" + (typeof outputBuffer));
console.log("outStream.Buffer: " + outputBuffer);

// validate inputBuffer matches outputBuffer
assert.deepEqual(inputBuffer, outputBuffer);
console.log("inputBuffer matches outputBuffer!");

// encode the bytes to a base64 string
var outputStringB64 = outputBuffer.toString('base64');

console.log("outputStringB64: " + outputStringB64);
console.log("inputStringB64: " + inputStringB64);

// validate input string matches output
assert.ok(inputStringB64 == outputStringB64, "input does not match output!");

console.log("inputString matches outputString!");