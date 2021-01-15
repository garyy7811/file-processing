// Constructor for XML object
function SlideShowXML(dbRecord) {

    this._attr = { remoteClass:"com.salesgraphics.nv.model.slideShow.SlideShow" };


    // always initialize all instance properties
    this.id = dbRecord.id;
    this.lookupId = new CDATA(dbRecord.lookup_id);
    this.description = new CDATA(dbRecord.description);
    this.displayName = new CDATA(dbRecord.displayName);
    this.hidden = dbRecord.hidden;
    this.isPrivate = dbRecord.isPrivate;
    this.name = new CDATA(dbRecord.name);
    this.notifyView = dbRecord.notifyView;
    this.parameters = new CDATA(dbRecord.parameters.toString('utf8').replace(/>\n\s*</g, "><"));
    this.security = new CDATA(dbRecord.security);

    // child objects
    this.access = new AccessXML(dbRecord);

    // date to be set from calculated value
    this.lastUpdatedDate = null;
    this.lastUpdatedTimestamp = null; // epoch value of lastUpdatedDate

    this.ownerUsername = new CDATA(dbRecord.owner_username);
    this.presentationId = dbRecord.presentation_id;
    this.presentations = "$presentationList$";

    this.summary = new SlideShowSummaryXML(dbRecord);
}

function AccessXML(dbRecord) {
    this._attr = { remoteClass:"com.salesgraphics.nv.model.access.Access" };
    this.uuid = new CDATA(dbRecord.uuid);
    this.createdTime = dbRecord.createdTime.toISOString();
    this.modifiedTime = dbRecord.modifiedTime.toISOString();
    this.ownerId = dbRecord.owner_id;
    this.ownerUsername = new CDATA(dbRecord.owner_username);
    this.inherit = dbRecord.inherit;
}

function SlideShowSummaryXML(dbRecord) {

    this._attr = { remoteClass:"com.salesgraphics.nv.model.slideShow.slideShowHelper.SlideShowSummary" };

    // properties taken from SlideShow table
    this.id = dbRecord.id;
    this.lookupId = new CDATA(dbRecord.lookup_id);
    this.description = new CDATA(dbRecord.description);
    this.displayName = new CDATA(dbRecord.displayName);
    this.isPrivate = dbRecord.isPrivate;
    this.name = new CDATA(dbRecord.name);
    this.security = new CDATA(dbRecord.security);


    // properties from Access/User/Client tables
    this.owner = new CDATA(dbRecord.owner_firstname + " " + dbRecord.owner_lastname);
    this.company = new CDATA(dbRecord.owner_company);
    this.createdDate = dbRecord.createdTime.toISOString();
    this.ownerId = dbRecord.owner_id;

    // empty values for calculated properties
    this.lastUpdatedDate = null;
    this.thumbnail = "$thumbnail$";
    this.presCt = 0;
    this.slideCt = 0;
    this.totalContentSize = 0;
}

function CDATA(strValue) {
    this._cdata = strValue;
}


// export the class
module.exports = SlideShowXML;