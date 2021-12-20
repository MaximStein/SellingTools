package com.salesinvoicetools.viewmodels;

import com.salesinvoicetools.models.OAuth2Token;


public class TokenSelectModel {
	public OAuth2Token token;
	public String text;
//	public boolean any = false;
	
	public TokenSelectModel(OAuth2Token token, String text, boolean any) {
		this.token = token;
		this.text = text;
//		this.any = any;
	}
	
	public String toString() {
		return text;
	}
}
