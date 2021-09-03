
package com.torvergata;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GeJiraReleases {
	private static List<Release> releases;

    public static void main(String[] args) throws Exception {
		Logger logger = Logger.getLogger(GeJiraReleases.class.getName());
        String projectName ="TAJO";

		List<Release> newReleaseNames = getReleases(projectName);
		logger.log(Level.INFO, "Releases: {0} ", newReleaseNames.size()); 
	}

	public static List<Release> getReleases(String projectName) throws JSONException, IOException {
		Logger logger = Logger.getLogger(GeJiraReleases.class.getName());
		
		//Fills the arraylist with issues dates
		//Ignores issues with missing dates
		Integer i;
		String url = buildURL(projectName);
		Integer total = 0;
		
		JSONObject json = readJsonFromUrl(url);
		if (json.has("total")) {
			total = (Integer) json.get("total");
			logger.log(Level.INFO, "Releases: {0} ", total); 
		}

		JSONArray versions = json.getJSONArray("versions");
		releases = new ArrayList<>();

		for (i = 0; i < versions.length(); i++ ) {
			String name1 = "";
			String name = "";
			String id = "";
			if(versions.getJSONObject(i).has("releaseDate")) {
				if (versions.getJSONObject(i).has("name")) {
					name1 = versions.getJSONObject(i).get("name").toString();
					name = name1.replace("-incubating", ".0");
				}
				if (versions.getJSONObject(i).has("id"))
				{
					id = versions.getJSONObject(i).get("id").toString();
				}

				addRelease(versions.getJSONObject(i).get("releaseDate").toString(),
						   name,id);
			 }
		}

		// order releases by date
		Collections.sort(releases); 

		String outname = projectName + "-versions.csv";
		try(FileWriter fileWriter = new FileWriter(outname)) {
			//Name of CSV for output
			fileWriter.append("Index,Version ID,Version Name,Date");
		    fileWriter.append("\n");

			for ( i = 0; i < releases.size(); i++) {
				Integer index = i + 1;
				fileWriter.append(index.toString());
				fileWriter.append(",");
				fileWriter.append(releases.get(i).getId());
				fileWriter.append(",");
				fileWriter.append(releases.get(i).getName());
				fileWriter.append(",");
				fileWriter.append(releases.get(i).getStartDate().toString());
				fileWriter.append("\n");
				fileWriter.flush();

				// write start and end date for this release

				var rel = releases.get(i);
				int versionNumber = index;
				rel.setVersionNumber(versionNumber);
				if (i == 0) {
					// first release
					// set start date 2 years ago
					rel.setStartDate(rel.getStartDate().minusYears(10));
					rel.setEndDate(rel.getEndDate().plusDays(1));
				} else {
					var rel2 = releases.get(i-1);
					// set start date as end date of previos version
					rel.setStartDate(rel2.getEndDate().minusDays(1));
				}
			}



		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error in csv writer.");
		}

		return releases;
	}

	private static String buildURL(String projectName) {
		return "https://issues.apache.org/jira/rest/api/2/project/" + projectName;
	}
			
	private static void addRelease(String strDate, String releaseName, String id) {
		LocalDate date = LocalDate.parse(strDate);
		Release newRelease = new Release(id, releaseName, date, date);
		if (releases != null && !releases.contains(newRelease))
		{
			releases.add(new Release(id, releaseName, date, date));
		}
	}

	private static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try(BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			String jsonText = readAll(rd);
			return new JSONObject(jsonText);
		} finally {
			is.close();
		}
	}
	
	private static String readAll(BufferedReader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}
}
