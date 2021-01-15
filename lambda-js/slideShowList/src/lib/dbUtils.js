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
    "   OwnerUser.username as owner_username, SSP.presentation_id as presentation_id,\n" +
    "   AP.thumnbnail_content, AP.archived_date, A.modifiedTime\n" +
    "FROM SlideShow SS\n" +
    "   INNER JOIN SlideShowPresentation SSP ON SS.id = SSP.slide_show_id\n" +
    "   INNER JOIN Presentation P on SSP.presentation_id = P.id\n" +
    "   INNER JOIN ArchivedPresentation AP on P.current_archive_id = AP.id\n" +
    "   INNER JOIN Access A ON SS.uuid = A.uuid\n" +
    "   INNER JOIN User OwnerUser ON A.owner_id = OwnerUser.id\n" +
    "   LEFT OUTER JOIN SlideShowRecipient SR ON SS.id = SR.slide_show_id\n" +
    "   LEFT OUTER JOIN User SSRUser ON SR.user_id = SSRUser.id\n" +
    "WHERE  (OwnerUser.username = ? OR (SSRUser.username = ? AND (SS.expirationDate IS NULL OR SS.expirationDate < NOW()))) $SEARCH_PHRASE$ \n" +
    "GROUP BY SS.id \n" +
    "ORDER BY $SORT_FIELD$ $SORT_DIRECTION$ \n" +
    "LIMIT ?, ?";

const getNumSlideShowsForUsernameSql =
    "SELECT COUNT(DISTINCT SS.id) as numSlideShows\n" +
    "FROM SlideShow SS\n" +
    "INNER JOIN SlideShowPresentation SSP ON SS.id = SSP.slide_show_id\n" +
    "INNER JOIN Access A ON SS.uuid = A.uuid\n" +
    "INNER JOIN User OwnerUser ON A.owner_id = OwnerUser.id\n" +
    "LEFT OUTER JOIN SlideShowRecipient SR ON SS.id = SR.slide_show_id\n" +
    "LEFT OUTER JOIN User SSRUser ON SR.user_id = SSRUser.id\n" +
    "WHERE (OwnerUser.username = ? OR (SSRUser.username = ? AND (SS.expirationDate IS NULL OR SS.expirationDate < NOW()))) $SEARCH_PHRASE$";



function init() {

    // set configuration variables from environment
    dbHost = process.env.csDbHost;
    console.log("got dbHost: " + dbHost);
    dbUser = process.env.csDbUsername;
    dbPassword = process.env.csDbPassword;
}

async function getSlideShowsForUsername(ownerUserMatch, sharedUserMatch, searchPhrase, sortField, sortDirection, limit, offset) {

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


        let searchClauses = "";
        if (searchPhrase && searchPhrase != "") {

            // add a wildcard search for each whitespace-separated term
            searchPhrase.split(" ").forEach(function(term) {
                if (term != "") {
                    // add 'OR' if we've already added a clause
                    searchClauses += (searchClauses == "" ? "" : " OR ") + " (SS.displayName LIKE '%"+term+"%')";
                }
            })

            // prefix all clauses with 'AND'
            searchClauses = " AND " + searchClauses;
        }

        let sql = getSlideShowsForUsernameSql.replace("$SEARCH_PHRASE$", searchClauses);

        sql = sql.replace("$SORT_FIELD$", sortField);
        sql = sql.replace("$SORT_DIRECTION$", sortDirection);


        // query for slideshows
        var getSlideShowsForUsernameSqlFormatted = mysql.format(sql, [ownerUserMatch, sharedUserMatch,  offset, limit]);
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

async function getNumSlideShowsForUsername(ownerUserMatch, sharedUserMatch, searchPhrase) {

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

        let searchClauses = "";
        if (searchPhrase && searchPhrase != "") {

            // add a wildcard search for each whitespace-separated term
            searchPhrase.split(" ").forEach(function(term) {
                if (term != "") {
                    // add 'OR' if we've already added a clause
                    searchClauses += (searchClauses == "" ? "" : " OR ") + " (SS.displayName LIKE '%"+term+"%')";
                }
            })

            // prefix all clauses with 'AND'
            searchClauses = " AND " + searchClauses;
        }

        let sql = getNumSlideShowsForUsernameSql.replace("$SEARCH_PHRASE$", searchClauses);

        // query for userRoles (permissions)
        var getNumSlideShowsForUsernameFormatted = mysql.format(sql, [ownerUserMatch, sharedUserMatch]);
        console.log(getNumSlideShowsForUsernameFormatted);
        return connection.query(getNumSlideShowsForUsernameFormatted);

    }).then(function (rows) {

        console.log('got slideshows: ', rows[0].numSlideShows);

        if (connection && connection.end) {
            connection.end();
        }

        console.log("got all items, returning as object");

        return rows[0].numSlideShows;

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
exports.getNumSlideShowsForUsername = getNumSlideShowsForUsername;

