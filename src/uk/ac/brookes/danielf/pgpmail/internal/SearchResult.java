package uk.ac.brookes.danielf.pgpmail.internal;


public class SearchResult {

	private String type;
	private String keyId;
	private String date;
	private String userId;
	private String keyLink;
	
	public SearchResult(String type, String keyId, String date, String userId, String keyLink) {
		this.type = type;
		this.keyId = keyId;
		this.date = date;
		this.userId = userId;
		this.keyLink = keyLink;
	}
	
	public SearchResult() {}

	public void setType(String type) {
		this.type = type;
	}

	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getType() {
		return type;
	}

	public String getKeyId() {
		return keyId;
	}

	public String getDate() {
		return date;
	}

	public String getUserId() {
		return userId;
	}

	public String getKeyLink() {
		return keyLink;
	}

	public void setKeyLink(String keyLink) {
		this.keyLink = keyLink;
	}

}
