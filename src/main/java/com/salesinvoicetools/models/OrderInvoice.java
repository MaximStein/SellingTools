package com.salesinvoicetools.models;

import java.sql.Timestamp;
import java.time.Instant;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;



@Entity
public class OrderInvoice {
	
	public OrderInvoice() {}
	
	public OrderInvoice(long newInvoiceNumber) {
		this.invoiceNumber = newInvoiceNumber;
		invoiceTime = Timestamp.from(Instant.now());		
	}
	
	@Id
	public long invoiceNumber;
	
	
	public Timestamp invoiceTime;
	

	public long getInvoiceNumber() {
		return invoiceNumber;
	}

	public void setInvoiceNumber(long invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
	}

	public Timestamp getInvoiceTime() {
		return invoiceTime;
	}

	public void setInvoiceTime(Timestamp invoiceTime) {
		this.invoiceTime = invoiceTime;
	}

}
