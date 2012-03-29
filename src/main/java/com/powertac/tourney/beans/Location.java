package com.powertac.tourney.beans;

import java.util.Calendar;

import javax.faces.bean.ManagedBean;

@ManagedBean
public class Location{
	private String name;
	private Calendar fromDate;
	private Calendar toDate;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Calendar getFromDate() {
		return fromDate;
	}
	public void setFromDate(Calendar fromDate) {
		this.fromDate = fromDate;
	}
	public Calendar getToDate() {
		return toDate;
	}
	public void setToDate(Calendar toDate) {
		this.toDate = toDate;
	}
}
