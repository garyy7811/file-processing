package com.llnw.mediavault;

public class MediaVaultRequest {
	private long startTime = -1;
	private long endTime = -1;
	private String ipAddress;
	private String referrer;
	private String pageURL;
	private String mediaURL;

	public MediaVaultRequest(String mediaURL) {
		this.mediaURL = mediaURL;
	}

	public String getURLParamers() {
		StringBuilder urlParams = new StringBuilder();
		if (referrer != null) {
			urlParams.append("&ru=" + referrer.length());
		}
		if (pageURL != null && pageURL.length() > 0) {
			urlParams.append("&pu=" + pageURL.length());
		}
		if (ipAddress != null && ipAddress.length() > 0) {
			urlParams.append("&ip=" + ipAddress);
		}
		if (startTime != -1) {
			urlParams.append("&s=" + startTime);
		}
		if (endTime != -1) {
			urlParams.append("&e=" + endTime);
		}
		if (urlParams.length() > 0) {
			return urlParams.substring(1); // clear the first &
		} else {
			return urlParams.toString();
		}
	}

	public String getHashParameters() {
		StringBuilder hash = new StringBuilder();
		if (referrer != null)
			hash.append(referrer);
		if (pageURL != null)
			hash.append(pageURL);

		return hash.toString();
	}

	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * @param startTime
	 *            the startTime to set
	 */
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	/**
	 * @return the endTime
	 */
	public long getEndTime() {
		return endTime;
	}

	/**
	 * @param endTime
	 *            the endTime to set
	 */
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	/**
	 * @return the ipAddress
	 */
	public String getIPAddress() {
		return ipAddress;
	}

	/**
	 * @param ipAddress
	 *            the ipAddress to set
	 */
	public void setIPAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	/**
	 * @return the referrer
	 */
	public String getReferrer() {
		return referrer;
	}

	/**
	 * @param referrer
	 *            the referrer to set
	 */
	public void setReferrer(String referrer) {
		this.referrer = referrer;
	}

	/**
	 * @return the pageURL
	 */
	public String getPageURL() {
		return pageURL;
	}

	/**
	 * @param pageURL
	 *            the pageURL to set
	 */
	public void setPageURL(String pageURL) {
		this.pageURL = pageURL;
	}

	/**
	 * @return the mediaURL
	 */
	public String getMediaURL() {
		return mediaURL;
	}

	/**
	 * @param mediaURL
	 *            the mediaURL to set
	 */
	public void setMediaURL(String mediaURL) {
		this.mediaURL = mediaURL;
	}
}