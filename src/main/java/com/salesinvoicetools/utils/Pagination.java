package com.salesinvoicetools.utils;

import java.util.List;

/**
 *  helper class to define a page in a set of items
 * @author Maxim Stein
 *
 */
public class Pagination {
	public int pageSize = 10;
	public int currentPage = 1;	
	public int totalEntries;
	
	
	public Pagination() {
		
	}
	
	public Pagination(int pageSize) {
		this.pageSize = pageSize;
	}
	
	public Pagination(int pageSize, int totalEntries) {
		this.totalEntries = totalEntries;	
		this.pageSize = pageSize;
	}
	
	
	public int getOffset() {
		return (currentPage -1 ) * pageSize;
	}
	
	public boolean hasMorePages() {
		return pageSize * currentPage < totalEntries; 
	}
	
	public int getTotalPageCount() {		
		return (int)(1.0 * totalEntries / pageSize) + 1; 
	}
	
	public <T> List<T> getCurrentListPage(List<T> list) {
		var start = (currentPage - 1) * pageSize;
		int end = Math.min(list.size(), start + pageSize);						
		return list.subList(start, end);
	}
}
