// Constructor for XML object
function SlideShowXML(name, description, lastModifiedDate, thumbnailUrl, ownerEmail, isOwner, url, previewUrl) {

    this._attr = {
        name: name,
        description: description,
        lastModifiedDate: lastModifiedDate,
        thumbnailUrl: thumbnailUrl,
        ownerEmail: ownerEmail,
        isOwner: isOwner,
        slideshowUrl: url,
        slideshowPreviewUrl:previewUrl
    };
}

function SlideShowListResponse (result, page_size, offset, returned_records, total_records, total_pages, slidshows) {

    this._attr = {
        result: result,
        page_size: page_size,
        offset: offset,
        returned_records: returned_records,
        total_records: total_records,
        total_pages: total_pages
    }

    this.slideshows = {slideshow:slidshows};


}



// export the classes
module.exports = {
    SlideShowXML: SlideShowXML,
    SlideShowListResponse: SlideShowListResponse
};
