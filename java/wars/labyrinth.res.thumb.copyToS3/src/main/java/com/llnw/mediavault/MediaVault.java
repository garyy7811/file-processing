package com.llnw.mediavault;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MediaVault{
	private String	secret;

	public MediaVault(String secret) {
		this.secret = secret;
	}

	public String compute(){
		return compute(null);
	}

	public String compute(MediaVaultRequest options) throws IllegalArgumentException {
		if(options == null){
			throw new IllegalArgumentException("Invalid 'options' parameter.");
		}else if(options.getMediaURL() == null || options.getMediaURL().length() == 0){
			throw new IllegalArgumentException("options.getMediaURL() is required.");
		}else if(secret == null || secret.length() == 0){
			throw new IllegalArgumentException("MediaVault.getSecret() is null.");
		}

		StringBuilder result = new StringBuilder(options.getMediaURL());
		String urlParams = "";
		String hashParams = "";

		if(options != null){
			urlParams = options.getURLParamers();
			hashParams = options.getHashParameters();
		}

		if(!urlParams.isEmpty()){
			if(result.indexOf("?") > -1)
				result.append("&" + urlParams);
			else
				result.append("?" + urlParams);
		}

		String hash = getMD5Hash(secret + hashParams + result);

		if(result.indexOf("?") > -1)
			result.append("&h=" + hash);
		else
			result.append("?h=" + hash);

		return result.toString();
	}

	private String getMD5Hash(String hashParams) {
		try{
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(hashParams.getBytes());

			return toHexString(md.digest());
		}catch(NoSuchAlgorithmException ex){
			return "";
		}
	}

	private String toHexString(byte[] b) {
		StringBuilder sb = new StringBuilder(b.length * 2);

		for(int i=0; i< b.length; i++) {
			if(((int) b[i] & 0xff) < 0x10) {
				sb.append("0");
			}

			sb.append(Long.toString((int) b[i] & 0xff, 16));
		}
		return sb.toString();
	}

	/**
	 * @return the secret
	 */
	public String getSecret() {
		return secret;
	}

	/**
	 * @param secret
	 *            the secret to set
	 */
	public void setSecret(String secret) {
		this.secret = secret;
	}
}