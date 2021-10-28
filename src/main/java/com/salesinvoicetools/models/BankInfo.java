package com.salesinvoicetools.models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class BankInfo {
	
	@Id
	@GeneratedValue
	public  long id;
	
	public  String bankName;
	
	public  String iban;
	
	public  String bic;

	public BankInfo() {}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getBankName() {
		return bankName;
	}

	public void setBankName(String bankName) {
		this.bankName = bankName;
	}

	public String getIban() {
		return iban;
	}

	public void setIban(String iban) {
		this.iban = iban;
	}

	public String getBic() {
		return bic;
	}

	public void setBic(String bic) {
		this.bic = bic;
	}

	public BankInfo(String bankName, String iban, String bic) {
		this.bankName = bankName;
		this.iban = iban;
		this.bic = bic;
	}
	
	@Override
	public String toString() {
		return bankName+"\r\n"
			+"IBAN: "+iban+"\r\n"
			+"BIC: "+bic;
	}
}
