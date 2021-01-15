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
var getUserIdSql = "SELECT id FROM User WHERE username=?;";

const getUserRolesSql =
    "SELECT DISTINCT AuthRoles.id\n" +
    "FROM (\n" +
    " SELECT R.id\n" +
    " FROM Magnet.Role R\n" +
    "   INNER JOIN Magnet.Group G ON R.id = G.role_id\n" +
    "   INNER JOIN Magnet.OrganizationTree OT ON G.id = OT.ancestor_id\n" +
    "   INNER JOIN Magnet.User U ON OT.group_id = U.group_id\n" +
    " WHERE U.id = ?\n" +
    " UNION\n" +
    " SELECT R.id\n" +
    " FROM Magnet.Role R\n" +
    "   INNER JOIN Magnet.GroupRoles GR ON R.id = GR.role_id\n" +
    "   INNER JOIN Magnet.OrganizationTree OT ON GR.group_id = OT.ancestor_id\n" +
    "   INNER JOIN Magnet.User U ON OT.group_id = U.group_id\n" +
    " WHERE U.id = ?\n" +
    " UNION\n" +
    " SELECT R.id\n" +
    " FROM Magnet.Role R\n" +
    "   INNER JOIN Magnet.UserRoles UR ON R.id = UR.role_id\n" +
    "   INNER JOIN Magnet.User U ON UR.user_id = U.id\n" +
    " WHERE U.id = ?\n" +
    " UNION\n" +
    " SELECT R.id\n" +
    " FROM Magnet.Role R\n" +
    "   INNER JOIN Magnet.User U ON R.id = U.role_id\n" +
    " WHERE U.id = ?\n" +
    ") AS AuthRoles;";

const getPersonalFoldersSql =
    "SELECT DISTINCT F.uuid\n" +
    "FROM Access A\n" +
    "  INNER JOIN Rule RU ON A.uuid = RU.access_uuid\n" +
    "  INNER JOIN Folder F ON F.uuid = A.uuid\n" +
    "  INNER JOIN ResourceFolder RF ON F.id = RF.id\n" +
    "WHERE RU.Role_id IN (?);";

const getLibraryFoldersSql =
    "SELECT F.uuid\n" +
    "FROM Access A\n" +
    "  INNER JOIN Rule RU ON A.uuid = RU.access_uuid\n" +
    "  INNER JOIN Library L ON L.uuid = A.uuid\n" +
    "  INNER JOIN Folder F ON F.id = L.resource_folder_id\n" +
    "  INNER JOIN ResourceFolder RF ON F.id = RF.id\n" +
    "WHERE RU.Role_id IN (?);";

const getUserResourceFolderSql =
    "SELECT F.*\n" +
    "FROM User U\n" +
    "  INNER JOIN UserConfiguration UC ON U.user_config_id = UC.id\n" +
    "  INNER JOIN Folder F ON F.id = UC.resource_folder_id\n" +
    "  INNER JOIN ResourceFolder RF ON F.id = RF.id\n" +
    "WHERE U.id = ?;"


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
function getUserIdForUsername(username) {

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
        return connection.query(mysql.format(getUserIdSql, [username]));

    }).then(function (rows) {

        // if we don't get a record, we're done - just throw an exception
        if (rows.length == 0) {
            throw new Error("No user found for username '"+username+"'");
        }

        // we found a user record, so we can close the connection now.
        connection.end();

        // return the userId
        return rows[0].id;

    }).catch(function(error){

        // error - just close the connection, log the error, and re-throw to allow caller to handle

        if (connection && connection.end) {
            connection.end();
        }

        console.log("Error locating user for username.", error);
        throw error;
    });
}

/**
 * take a userId as input, and query database for all top-level folders this user has permissions on:
 * - user's own My Resources folder
 * - Personal Folders
 * - Library Folders
 *
 * @param userId
 * @returns a Promise that will resolve to an Array of Folder.uuid values
 */
function getFoldersForUserId(userId) {

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

        // query for userRoles (permissions)
        var getUserRolesSqlFormatted = mysql.format(getUserRolesSql, [userId, userId, userId, userId]);
        console.log(getUserRolesSqlFormatted);
        return connection.query(getUserRolesSqlFormatted);

    }).then(function (rows) {

        console.log('got roles: ', rows.length);

        // convert result to array of roleId values
        userRoleIds = [];
        rows.forEach(function (row) {
            userRoleIds.push(row.id);
        });

        console.log("userRolesIds="+JSON.stringify(userRoleIds));

        // query for user's visible-personal-folders
        var getPersonalFoldersSqlFormatted = mysql.format(getPersonalFoldersSql, [userRoleIds]);
        console.log("executing getPersonalFoldersSql...")
        console.log(getPersonalFoldersSqlFormatted);
        return connection.query(getPersonalFoldersSqlFormatted);

    }).then(function (rows) {

        console.log("got personalFolders.length= ", rows.length);

        // convert result to array of uuids
        personalFolderUuids = [];
        rows.forEach(function (row) {
            personalFolderUuids.push(row.uuid);
        });

        console.log("got personalFolders: ", personalFolderUuids.join(","))

        // query for user's visible-library-folders
        var getLibraryFoldersSqlFormatted = mysql.format(getLibraryFoldersSql, [userRoleIds]);
        console.log(getLibraryFoldersSqlFormatted);
        return connection.query(getLibraryFoldersSqlFormatted);

    }).then(function (rows) {

        console.log("got libraryFolders.length= ", rows.length);

        // convert result to array of uuids
        libraryFolderUuids = [];
        rows.forEach(function (row) {
            libraryFolderUuids.push(row.uuid);
        });

        console.log("got libraryFolders: ", libraryFolderUuids.join(","))


        // query for user's own ResourceFolder
        var getUserResourceFolderSqlFormatted = mysql.format(getUserResourceFolderSql, [userId]);
        console.log(getUserResourceFolderSqlFormatted);
        return connection.query(getUserResourceFolderSqlFormatted);

    }).then(function (rows) {


        userResourceFolderUuid = rows[0].uuid;

        console.log("got userResourceFolderUuid= ", rows[0].uuid);

        // this is the last db call - we can close the connection now

        connection.end();

        console.log("got all folders, returning as object");

        // at this point we are done, return the results as an object with each set of uuids
        return {
            userResourceFolderUuid: userResourceFolderUuid,
            libraryFolderUuids: libraryFolderUuids,
            personalFolderUuids: personalFolderUuids
        };

    }).catch(function (error) {

        if (connection && connection.end) {
            connection.end();
        }

        console.log(error);

        throw error;
    });

}

// export the functions to define this module
exports.init = init;
exports.getUserIdForUsername = getUserIdForUsername;
exports.getFoldersForUserId = getFoldersForUserId;

