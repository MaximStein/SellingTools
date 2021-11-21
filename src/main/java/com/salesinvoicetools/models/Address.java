package com.salesinvoicetools.models;

import javax.persistence.*;

@Entity
public class Address {
	
	@Id
	@GeneratedValue
	public long id;
	
	public String name;
	
	public String company;
	
	public String phoneNumber;
	
	public String additionalInfo;
	
	public String street;
	
	public String postalCode;
	
	public String city;

	@Enumerated(EnumType.STRING)
	public Country country;

	public String customInfo;
	
	public String toString() {
		return name+"\r\n"
			+ (company == null || company.isBlank() ? "" : company+"\r\n")
    		+ street+"\r\n"
    		+ (additionalInfo == null || additionalInfo.isBlank() ? "" : additionalInfo+"\r\n")
    		+ postalCode+" "+city;
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getAdditionalInfo() {
		return additionalInfo;
	}

	public void setAdditionalInfo(String additionalInfo) {
		this.additionalInfo = additionalInfo;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}


	public enum Country {
		DE, UK, AT, FR, IT, RM, PL, NL, USA, CND, OTHER;
	}
}
