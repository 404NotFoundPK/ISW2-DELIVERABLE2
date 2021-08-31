package com.torvergata;

import java.time.LocalDate;

public class Release implements Comparable<Release> {
	private String id;
	private LocalDate startDate;
	private LocalDate endDate;
	private String name;
	private int versionNumber;  // N progressivo

	public int getVersionNumber() {
		return this.versionNumber;
	}

	public void setVersionNumber(int versionNumber) {
		this.versionNumber = versionNumber;
	}

	public Release(String id, String name, LocalDate startDate, LocalDate endDate) {

		this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
		this.name = name;
	}

	public String getId() {
		return this.id;
	}

	public void setID(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

    public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	@Override
	/*
	* This is where we write the logic to sort. This method sort 
	* automatically by the first name in case that the last name is 
	* the same.
	*/
	public int compareTo(Release other){
		/* 
		* Sorting by id. compareTo should return < 0 if this(keyword) 
		* is supposed to be less than other, > 0 if this is supposed to be 
		* greater than object other and 0 if they are supposed to be equal.
		*/
		return this.startDate.compareTo(other.startDate);
	}

	@Override
    public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
        return ((Release)other).id.equals(this.id); 
    }

	@Override
    public int hashCode() {
        return this.id.hashCode();
    }
}