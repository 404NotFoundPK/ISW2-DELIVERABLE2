package com.torvergata;

import java.time.LocalDate;
import java.util.List;

public class FileExtended {
    private String path;
	private String author;
	private int addedLines;
	private int deletedLines;
	private int totalLines;
	private int typeOfChange;  // 0 - none, 1 -add, 2 - change, 3 -delete
	private String issuesKey;
	private LocalDate dateOfChange;
	private int numberFiles;
	private String fixedVersion;
	private List<String> affectedVersion;
	private String openedVersion;
	private String injectVersion;
	private int currentVersion;
 
    public FileExtended(String path, int version, int addedLines, int deletedLines, int totalLines, int typeOfChange, 
	String issuesKey, LocalDate dateOfChange, String author) {

		this.path = path;
		this.currentVersion = version;
		this.addedLines = addedLines;
		this.deletedLines = deletedLines;
        this.totalLines = totalLines;
		this.typeOfChange = typeOfChange;
		this.issuesKey = issuesKey;
		this.dateOfChange = dateOfChange;
		this.author = author;
	}

    public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public int getAddedLines() {
		return this.addedLines;
	}

	public void setAddedLines(int addedLines) {
		this.addedLines = addedLines;
	}

    public int getDeletedLines() {
		return this.deletedLines;
	}

	public void setDeletedLines(int deletedLines) {
		this.deletedLines = deletedLines;
	}

    public int getTotalLines() {
		return this.totalLines;
	}

	public void setTotalLines(int totalLines) {
		this.totalLines = totalLines;
	}

	public int getTypeOfChange() {
		return this.typeOfChange;
	}

	public void setTypeOfChange(int typeOfChange) {
		this.typeOfChange = typeOfChange;
	}

	public String getIssuesKey() {
		return this.issuesKey;
	}

	public void setIssuesKey(String issuesKey) {
		this.issuesKey = issuesKey;
	}

	public void setNumberFiles(int numberFiles) {
		this.numberFiles = numberFiles;
	}

	public int getNumberFiles() {
		return this.numberFiles;
	}

	public String getAuthor() {
		return this.author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	
	public LocalDate getDateOfChange() {
		return this.dateOfChange;
	}

	public void setDateOfChange(LocalDate dateOfChange) {
		this.dateOfChange = dateOfChange;
	}

	
	public String getFixedVersion() {
		return this.fixedVersion;
	}

	public void setFixedVersion(String fixedVersion) {
		this.fixedVersion = fixedVersion;
	}

	public String getOpenedVersion() {
		return this.openedVersion;
	}

	public void setOpenedVersion(String openedVersion) {
		this.openedVersion = openedVersion;
	}

	public String getInjectVersion() {
		return this.injectVersion;
	}

	public void setInjectVersion(String injectVersion) {
		this.injectVersion = injectVersion;
	}

	public int getCurrentVersion() {
		return this.currentVersion;
	}

	public void setCurrentVersion(int currentVersion) {
		this.currentVersion = currentVersion;
	}
	
	public List<String> getAffectedVersion() {
		return this.affectedVersion;
	}

	public void setAffectedVersion(List<String> affectedVersion) {
		this.affectedVersion = affectedVersion;
	}
}
