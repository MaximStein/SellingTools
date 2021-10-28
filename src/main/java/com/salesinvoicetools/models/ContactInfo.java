package com.salesinvoicetools.models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class ContactInfo {
	@Id
	@GeneratedValue
	public long id;
	
	public String phone;
	public String fax;
	public String website;

	public String userName;


	public String email;

	public ContactInfo() {}
	
	public ContactInfo(String userName) {
		this.userName = userName;
	}
	
	public ContactInfo(String phone, String fax, String email, String website) {
		this.phone = fax;
		this.fax = fax;
		this.email = email;
		this.website = website;
	}
	
	public String toString() {
		var str = "";
		if(phone != null && !phone.isBlank())
			str += "Tel.: "+phone+"\r\n";
		if(fax != null && !fax.isBlank())
			str += "Fax.: "+fax+"\r\n";
		if(email != null && !email.isBlank())
			str += "E-Mail: "+email+"\r\n";
		if(website != null && !website.isBlank())
			str += "Web: "+website;
		return str;		
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getFax() {
		return fax;
	}

	public void setFax(String fax) {
		this.fax = fax;
	}

	public String getWebsite() {
		return website;
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
