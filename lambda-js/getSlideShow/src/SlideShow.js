
// Constructor
function SlideShow(dbRecord) {
    // always initialize all instance properties
    this.id = dbRecord.id;
    this.lookupId = dbRecord.lookup_id;
    this.name = dbRecord.name;
    this.description = dbRecord.description;
    this.displayName = dbRecord.displayName;
    this.ownerUsername = dbRecord.owner_username;
    this.presentationId = dbRecord.presentation_id;
    this.presentations = "$presentationList$";
}

// export the class
module.exports = SlideShow;