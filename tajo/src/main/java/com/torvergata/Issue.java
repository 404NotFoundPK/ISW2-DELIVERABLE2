package com.torvergata;

import java.time.LocalDate;
import java.util.List;

public class Issue implements Comparable<Issue> {
	private String id;
	private LocalDate resolutionDate;
	private LocalDate creationDate;
	private String fixedVers;
	private String openVers;
	private String injectVers;
	private List<String> affectVers;

	public Issue(String id, LocalDate creationDate, LocalDate resolutionDate, String openVers,
				List<String> affectedVersions, String[] fixVersions) {

		// injection version is first (min) from affected version
		// affected version == versions from json
		// fixed is max version from fixed versions from json
		this.id = id;
		this.affectVers = affectedVersions;
		this.fixedVers = getMaxFromStringArray(fixVersions);
		this.injectVers = getMinFromStringArray(affectedVersions);
		this.creationDate = creationDate;
		this.openVers = openVers;
		this.resolutionDate = resolutionDate;
	}

	public String getId() {
		return this.id;
	}

	public void setID(String id) {
		this.id = id;
	}

	public String getInjectVers() {
		return injectVers;
	}

	public void setInjectVers(String injectVers) {
		this.injectVers = injectVers;
	}

	public String getOpenVers() {
		return openVers;
	}

	public void setOpenVers(String openVers) {
		this.openVers = openVers;
	}

	public List<String> getAffectVers() {
		return affectVers;
	}

	public void setAffectVers(List<String> affectVers) {
		this.affectVers = affectVers;
	}

	public String getFixedVers() {
		return fixedVers;
	}

	public void setFixedVers(String fixedVers) {
		this.fixedVers = fixedVers;
	}

	public LocalDate getResolutionDate() {
		return this.resolutionDate;
	}

	public void setResolutionDate(LocalDate resolutionDate) {
		this.resolutionDate = resolutionDate;
	}

	public LocalDate getCreationDate() {
		return this.creationDate;
	}

	public void setCreationDate(LocalDate creationDate) {
		this.creationDate = creationDate;
	}

	private String getMaxFromStringArray(String[] arrayToMax)
	{
		String maxVersion = "0";
		if (arrayToMax.length == 0) {
			return "";
		}

		if (arrayToMax.length == 1) {
			if (arrayToMax[0].contains("index") || arrayToMax[0].contains("block_iteration")) {
				return "";
			}
			return arrayToMax[0];
		}

		for (String version : arrayToMax) {
			if (version.contains("index") || version.contains("block_iteration")) {
				continue;
			}
			if (version.contains("-incubating")) {
				version = version.replace("-incubating", ".0");
			}

			final int versionA = Integer.parseInt( maxVersion.replace( ".", "" ) );
			final int versionB = Integer.parseInt( version.replace( ".", "" ) );

			if (versionB > versionA) {
				maxVersion = version;
			}
		}
		
		if (maxVersion.equals("0")) {
			return "";
		}
		return maxVersion;
	}

	private String getMinFromStringArray(List<String> arrayToMin)
	{
		String minVersion = "1000000";
		if (arrayToMin.isEmpty()) {
			return "";
		}

		if (arrayToMin.size() == 1) {
			return arrayToMin.get(0);
		}

		for (String version : arrayToMin) {

			final int versionA = Integer.parseInt( minVersion.replace( ".", "" ) );
			final int versionB = Integer.parseInt( version.replace( ".", "" ) );

			if (versionB < versionA) {
				minVersion = version;
			}
		}
		
		if (minVersion.equals("100")) {
			return "";
		}
		return minVersion;
	}

	@Override
	/*
	* This is where we write the logic to sort. This method sort 
	* automatically by the first name in case that the last name is 
	* the same.
	*/
	public int compareTo(Issue other){
		/* 
		* Sorting by id. compareTo should return < 0 if this(keyword) 
		* is supposed to be less than other, > 0 if this is supposed to be 
		* greater than object other and 0 if they are supposed to be equal.
		*/
		return this.id.compareTo(other.id);
	}

	@Override
    public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
        return ((Issue)other).id.equals(this.id); 
    }

	@Override
    public int hashCode() {
        return this.id.hashCode();
    }

}
