/**
 * This represents a 'module' that provides two functions for querying a mysql database:
 * - getSlideShowsForUser(username)
 * - getSlideShowAndPresentationsForLookupId(lookupId)
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

// ------------
var slideShowObj;
var presentationList = [];
var recipientUserIdList = [];

// query definitions
var getUserIdSql = "SELECT id FROM User WHERE username=?;";

const getSlideShowsForUsernameSql =
    "SELECT DISTINCT SS.*, " +
    "   OwnerUser.username as owner_username, SSP.presentation_id as presentation_id\n" +
    "FROM SlideShow SS\n" +
    "INNER JOIN SlideShowPresentation SSP ON SS.id = SSP.slide_show_id\n" +
    "INNER JOIN Access A ON SS.uuid = A.uuid\n" +
    "INNER JOIN User OwnerUser ON A.owner_id = OwnerUser.id\n" +
    "LEFT OUTER JOIN SlideShowRecipient SR ON SS.id = SR.slide_show_id\n" +
    "LEFT OUTER JOIN User SSRUser ON SR.user_id = SSRUser.id\n" +
    "WHERE SSRUser.username = ? OR OwnerUser.username = ?\n" +
    "ORDER BY SS.ID DESC;";

const getSlideShowSummarySql =
    "SELECT SS.*, A.*, OwnerUser.username as owner_username, " +
    "   OwnerUser.first_name as owner_firstname, OwnerUser.last_name as owner_lastname, " +
    "   C.name as owner_company\n" +
    "FROM SlideShow SS\n" +
    "INNER JOIN Access A ON SS.uuid = A.uuid\n" +
    "INNER JOIN User OwnerUser ON A.owner_id = OwnerUser.id\n" +
    "INNER JOIN OrganizationTree OT ON OwnerUser.group_id = OT.group_id\n" +
    "INNER JOIN Magnet.Group CG ON OT.ancestor_id = CG.id\n" +
    "INNER JOIN Magnet.Client C ON CG.client_id = C.id\n" +
    "WHERE SS.lookup_id = ?;"

const getArchivedPresentationsForSlideShowSql =
    "SELECT AP.*\n" +
    "FROM SlideShow SS\n" +
    "  INNER JOIN SlideShowPresentation SSP on SS.id = SSP.slide_show_id\n" +
    "  INNER JOIN Presentation P ON SSP.presentation_id = P.id\n" +
    "  INNER JOIN ArchivedPresentation AP ON P.current_archive_id = AP.id\n" +
    "WHERE SS.lookup_id = ?;";

const getSlideShowRecipientUserIdsSql =
    "SELECT SSR.user_id \n" +
    "FROM SlideShow SS \n" +
    "  INNER JOIN SlideShowRecipient SSR ON SS.id = SSR.slide_show_id \n" +
    "WHERE SS.lookup_id = ?";

function init() {

    // set configuration variables from environment
    dbHost = process.env.csDbHost;
    console.log("got dbHost: " + dbHost);
    dbUser = process.env.csDbUsername;
    dbPassword = process.env.csDbPassword;
}

function getSlideShowsForUsername(username) {

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
        var getSlideShowsForUsernameSqlFormatted = mysql.format(getSlideShowsForUsernameSql, [username, username]);
        console.log(getSlideShowsForUsernameSqlFormatted);
        return connection.query(getSlideShowsForUsernameSqlFormatted);

    }).then(function (rows) {

        console.log('got slideshows: ', rows.length);

        var slideshowList = [];
        rows.forEach(function (row) {
           slideshowList.push(row);
        });


        if (connection && connection.end) {
            connection.end();
        }

        console.log("got all items, returning as object");

        return slideshowList;

    }).catch(function (error) {

        if (connection && connection.end) {
            connection.end();
        }

        console.log(error);

        throw error;
    });

}


/**
 * queries the database for the SlideShow record and associated ArchivedPresentation records for a given
 * SlideShow, using the currentArchive for each Presentation.
 *
 * @param slideShowLookupId
 * @returns {*}
 */
function getSlideShowAndPresentationsForLookupId(slideShowLookupId) {

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
        var getSlideShowSummarySqlFormatted = mysql.format(getSlideShowSummarySql, [slideShowLookupId]);
        console.log(getSlideShowSummarySqlFormatted);
        return connection.query(getSlideShowSummarySqlFormatted);

    }).then(function (rows) {

        console.log('got slideshows: ', rows.length);

        slideShowObj = rows[0];

        console.log("located slideshow.id="+slideShowObj.id);


        // query for slideshow recipient user_id list
        var getSlideShowRecipientUserIdsSqlFormatted = mysql.format(getSlideShowRecipientUserIdsSql, [slideShowLookupId]);
        console.log("executing getSlideShowRecipientUserIdsSql...")
        console.log(getSlideShowRecipientUserIdsSqlFormatted);
        return connection.query(getSlideShowRecipientUserIdsSqlFormatted);

    }).then(function (rows) {

        // could be empty...
        recipientUserIdList = [];


        console.log("got recipientUserIdList.length= ", rows.length);

        rows.forEach(function (row) {
            recipientUserIdList.push(row.user_id);
        });


        // query for user's visible-personal-folders
        var getArchivedPresentationsForSlideShowSqlFormatted = mysql.format(getArchivedPresentationsForSlideShowSql, [slideShowLookupId]);
        console.log("executing getArchivedPresentationsForSlideShowSql...")
        console.log(getArchivedPresentationsForSlideShowSqlFormatted);
        return connection.query(getArchivedPresentationsForSlideShowSqlFormatted);

    }).then(function (rows) {

        console.log("got archivedPresentations.length= ", rows.length);

        // copy items into our response array
        presentationList = [];
        rows.forEach(function (row) {
            presentationList.push(row);
        });

        if (connection && connection.end) {
            connection.end();
        }

        console.log("got all items, returning as object");

        // at this point we are done, return the results as an object
        return {
            slideShowObj: slideShowObj,
            presentationList: presentationList,
            recipientUserIdList: recipientUserIdList
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
exports.getSlideShowsForUsername = getSlideShowsForUsername;
exports.getSlideShowAndPresentationsForLookupId = getSlideShowAndPresentationsForLookupId;

