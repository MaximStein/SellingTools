package com.salesinvoicetools.models;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

@Entity
public class AppConfiguration {
	
	@Id
	@GeneratedValue
	public long id;

	public String vatNumber;
	@OneToOne(orphanRemoval = true, cascade = {CascadeType.ALL})
	@JoinColumn(name="ADDRESS_ID")
	public Address address;
	
	
	@OneToOne(orphanRemoval = true, cascade = {CascadeType.ALL})
	@JoinColumn(name="BANK_INFO_ID")
	public BankInfo bankInfo;
		
	@OneToOne(orphanRemoval = true, cascade = {CascadeType.ALL})
	@JoinColumn(name="CONTACT_INFO_ID")
	public ContactInfo contactInfo;
	
	
	public double defaultTax;
	
	public boolean bankInfoOnInvoice = true;
	public boolean emailOnInvoice = true;
	public boolean phoneOnInvoice = true;
	
	public String kleinunternehmerInfoText;
	public String additionalText;
	public boolean isKleinunternehmer = false;
	
	public int currentInvoiceNumber = 0;
	
	public String invoiceDirectory;
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getVatNumber() {
		return vatNumber;
	}

	public void setVatNumber(String vatNumber) {
		this.vatNumber = vatNumber;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public BankInfo getBankInfo() {
		return bankInfo;
	}

	public void setBankInfo(BankInfo bankInfo) {
		this.bankInfo = bankInfo;
	}

	public ContactInfo getContactInfo() {
		return contactInfo;
	}

	public void setContactInfo(ContactInfo contactInfo) {
		this.contactInfo = contactInfo;
	}

	public double getDefaultTax() {
		return defaultTax;
	}

	public void setDefaultTax(double defaultTax) {
		this.defaultTax = defaultTax;
	}

	public boolean isBankInfoOnInvoice() {
		return bankInfoOnInvoice;
	}

	public void setBankInfoOnInvoice(boolean bankInfoOnInvoice) {
		this.bankInfoOnInvoice = bankInfoOnInvoice;
	}

	public boolean isEmailOnInvoice() {
		return emailOnInvoice;
	}

	public void setEmailOnInvoice(boolean emailOnInvoice) {
		this.emailOnInvoice = emailOnInvoice;
	}

	public boolean isPhoneOnInvoice() {
		return phoneOnInvoice;
	}

	public void setPhoneOnInvoice(boolean phoneOnInvoice) {
		this.phoneOnInvoice = phoneOnInvoice;
	}

	public String getKleinunternehmerInfoText() {
		return kleinunternehmerInfoText;
	}

	public void setKleinunternehmerInfoText(String kleinunternehmerInfoText) {
		this.kleinunternehmerInfoText = kleinunternehmerInfoText;
	}

	public String getAdditionalText() {
		return additionalText;
	}

	public void setAdditionalText(String additionalText) {
		this.additionalText = additionalText;
	}

	public boolean isKleinunternehmer() {
		return isKleinunternehmer;
	}

	public void setKleinunternehmer(boolean isKleinunternehmer) {
		this.isKleinunternehmer = isKleinunternehmer;
	}

	public int getCurrentInvoiceNumber() {
		return currentInvoiceNumber;
	}
	
	public int getIncrementedInvoiceNumber() {
		return ++currentInvoiceNumber;
	}

	public void setCurrentInvoiceNumber(int currentInvoiceNumber) {
		this.currentInvoiceNumber = currentInvoiceNumber;
	}

	public String getInvoiceDirectory() {
		return invoiceDirectory;
	}

	public void setInvoiceDirectory(String invoiceDirectory) {
		this.invoiceDirectory = invoiceDirectory;
	}

	public AppConfiguration() {
		this.bankInfo = new BankInfo();
		this.address = new Address();
		this.contactInfo = new ContactInfo();
	}
	
}
