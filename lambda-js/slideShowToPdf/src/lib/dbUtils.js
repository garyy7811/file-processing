/**
 * This represents a 'module' that provides two functions for querying a mysql database:
 * - getUserIdForUsername(username)
 * - getFoldersForUserId(userId)
 *
 * It is implemented with the 'promise-mysql' library, which wraps the mysql library with the 'bluebird' Promise
 * library for improved code organization.
 */
var mysql      = require('promise-mysql');

// configuration
var dbHost = "10.0.3.18";
var dbUser = "root";
var dbPassword = "sgcustom";
var dbSchema = "Magnet";

// variables populated while processing
var connection;
var userRoleIds = [];
var personalFolderUuids = [];
var libraryFolderUuids = [];
var userResourceFolderUuid = 0;

// query definitions
var getUsernameSql = "SELECT username FROM User WHERE id=?;";


function init() {

    // set configuration variables from environment
    dbHost = process.env.csDbHost;
    console.log("got dbHost: " + dbHost);
    dbUser = process.env.csDbUsername;
    dbPassword = process.env.csDbPassword;
}

/**
 * takes a username (email address), and queries the db to fetch the user.id
 *
 * If no user is found, throws an exception.
 *
 * @param username
 * @returns a Promise that will resolve to userId
 */
function getUsernameForUserId(userId) {

    /*
     * This function is implemented as a chain of Promises, which is achieved by using the 'promise-mysql'
     * library, which itself uses the 'bluebird' Promise library.
     */


    // create connection and connect
    return mysql.createConnection({
        host: dbHost,
        user: dbUser,
        password: dbPassword,
        database: dbSchema

    }).then(function (conn) {

        // hold reference to connection
        connection = conn;

        // query for userId from username
        return connection.query(mysql.format(getUsernameSql, [userId]));

    }).then(function (rows) {

        // if we don't get a record, we're done - just throw an exception
        if (rows.length == 0) {
            throw new Error("No user found for userId '"+userId+"'");
        }

        // we found a user record, so we can close the connection now.
        connection.end();

        // return the username
        return rows[0].username;

    }).catch(function(error){

        // error - just close the connection, log the error, and re-throw to allow caller to handle

        if (connection && connection.end) {
            connection.end();
        }

        console.log("Error locating user for username.", error);
        throw error;
    });
}

// export the functions to define this module
exports.init = init;
exports.getUsernameForUserId= getUsernameForUserId;

