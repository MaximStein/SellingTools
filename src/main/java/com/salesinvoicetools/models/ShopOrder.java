package com.salesinvoicetools.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "DATASOURCE_ID", "ORDERNUMBER" }) })
public class ShopOrder {

	public enum Marketplace {
		EBAY_FAKE,EBAY,ETSY,OTHER
	}

	@Id
	@GeneratedValue
	public long id;

	public enum OrderStatus {
		ACTIVE, CANCELLED, CANCELPENDING, COMPLETED
	}

	@OneToOne(cascade = CascadeType.ALL)
	public ContactInfo buyer;

	public Timestamp orderTime;

	public Timestamp shippedTime;

	@OneToOne(cascade = CascadeType.ALL)
	public Address shippingAddress;

	@OneToOne(cascade = CascadeType.ALL)
	public Address billingsAddress;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval=true)
	public OrderInvoice invoice;

	public long totalGrossAmount;

	public long shippingCosts;

	public String buyerCheckoutMessage;

	public Boolean paymentComplete;

	public long refundAmount;

	public double vatPercent = 19;

	public String currencyCode = "EUR";

	@OneToMany(mappedBy = "owner", cascade = { CascadeType.ALL })
	public List<LineItem> items = new ArrayList<>();

	@Override
	public String toString() {
		var str = "[ " + getMarketplaceString() + " Order from " + shippingAddress.getName() + " (" + buyer.getUserName()
				+ ")]";
		for (var item : items) {
			str += "\r\n	" + item.getSingleItemAmount().doubleValue() + " " + currencyCode + "	"
					+ item.getProduct().getDescription() + "	x" + item.getQuantity() + "\r\n";
		}
		str += "	-----------------\r\n";
		return str;
	}

	public long getGrandTotalAmount() {
		return getItemsTotalAmount() + shippingCosts;
	}

	public long getItemsTotalAmount() {
		return items.stream().map(i -> i.getTotalPriceGross()).reduce(0l, Long::sum);
	}

	public long getVatAmount() {
		return Math.round(getGrandTotalAmount() - getGrandTotalAmount() / (1 + 1d * vatPercent / 100));
	}

	public String getMarketplaceString() {
		return dataSource == null ? "-sonstige-" : dataSource.getToken().getOwner().getPlatform().toString();
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public ContactInfo getBuyer() {
		return buyer;
	}

	public void setBuyer(ContactInfo buyer) {
		this.buyer = buyer;
	}

	public Timestamp getOrderTime() {
		return orderTime;
	}

	public void setOrderTime(Timestamp orderTime) {
		this.orderTime = orderTime;
	}

	public Timestamp getShippedTime() {
		return shippedTime;
	}

	public void setShippedTime(Timestamp shippedTime) {
		this.shippedTime = shippedTime;
	}

	public Address getShippingAddress() {
		return shippingAddress;
	}

	public void setShippingAddress(Address shippingAddress) {
		this.shippingAddress = shippingAddress;
	}

	public Address getBillingsAddress() {
		return billingsAddress;
	}

	public void setBillingsAddress(Address billingsAddress) {
		this.billingsAddress = billingsAddress;
	}

	public OrderInvoice getInvoice() {
		return invoice;
	}

	public void setInvoice(OrderInvoice invoice) {
		this.invoice = invoice;
	}

	public long getTotalGrossAmount() {
		return totalGrossAmount;
	}

	public void setTotalGrossAmount(long totalGrossAmount) {
		this.totalGrossAmount = totalGrossAmount;
	}

	public long getShippingCosts() {
		return shippingCosts;
	}

	public void setShippingCosts(long shippingCosts) {
		this.shippingCosts = shippingCosts;
	}

	public String getBuyerCheckoutMessage() {
		return buyerCheckoutMessage;
	}

	public void setBuyerCheckoutMessage(String buyerCheckoutMessage) {
		this.buyerCheckoutMessage = buyerCheckoutMessage;
	}

	public Boolean getPaymentComplete() {
		return paymentComplete;
	}

	public void setPaymentComplete(Boolean paymentComplete) {
		this.paymentComplete = paymentComplete;
	}

	public long getRefundAmount() {
		return refundAmount;
	}

	public void setRefundAmount(long refundAmount) {
		this.refundAmount = refundAmount;
	}

	public double getVatPercent() {
		return vatPercent;
	}

	public void setVatPercent(double vatPercent) {
		this.vatPercent = vatPercent;
	}

	public String getCurrencyCode() {
		return currencyCode;
	}

	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}

	public List<LineItem> getItems() {
		return items;
	}

	public void setItems(List<LineItem> items) {
		this.items = items;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	public DataSource dataSource;

	public String orderNumber;

	public String getOrderNumber() {
		return orderNumber;
	}

	public void setOrderNumber(String orderNumber) {
		this.orderNumber = orderNumber;
	}

}
