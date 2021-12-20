package com.salesinvoicetools.dataaccess.models;

import java.time.LocalDate;

import com.salesinvoicetools.viewmodels.TokenSelectModel;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn.SortType;


/**
 * this class is used to filter and sort the orders overview 
 * @author Maxim Stein
 *
 */
public class ShopOrdersFilterModel {
	public int page = 0;
	public int entriesPerPage = 20;
	
	public String searchText;
	public String sortField = "";
	public SortType sortType;
	
	public boolean onlyWithoutInvoice = false;
	public ObservableList<TokenSelectModel> filterByToken;
	
	public LocalDate orderStartDate = null;
	
	public ShopOrdersFilterModel() {}
	
	public ShopOrdersFilterModel(int page, int entriesPerPage) {
		this.page = page;
		this.entriesPerPage = entriesPerPage;
	}
	
	public ShopOrdersFilterModel(String text, String sortField, SortType sortType) {
		this.searchText = text;
		this.sortField = sortField;
		this.sortType = sortType;
	}
}
