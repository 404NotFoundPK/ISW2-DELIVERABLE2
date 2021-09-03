package com.torvergata;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.time.ZoneId;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GetJiraIssues {

	static Logger logger;

    public static void main(String[] args) throws Exception {
		logger = Logger.getLogger(GetJiraIssues.class.getName());
        String projectName ="TAJO";


		List<Release> releases = GeJiraReleases.getReleases(projectName);
		
		List<Issue> issues = getIssues(projectName, releases);
		logger.log(Level.INFO, "Count issues: {0}", Integer.toString(issues.size()));
	}

	public static List<Issue> getIssues(String projName, List<Release> releases) throws JSONException, IOException, ParseException {
		logger = Logger.getLogger(GetJiraIssues.class.getName());
		List<Issue> issues = new ArrayList<>();
		
		//Fills the arraylist with issues dates
		//Ignores issues with missing dates
		int i;
		int total = 100;
		int startAt = 0;
		String url = buildURL(projName, startAt, total);
		
		JSONObject json = readJsonFromUrl(url);
		if (json.has("total")) {
			total = (Integer) json.get("total");
			logger.log(Level.INFO, "Count total issues in JIRA: {0}", total);
		}
		if (total < 1000) {
			// non dobbiamo iterare (xche sono meno di 1000)
			// prendiamo tutti i record
			url = buildURL(projName, startAt, total);
			json = readJsonFromUrl(url);
		}

		JSONArray issuesFromJson = json.getJSONArray("issues");
		for (i = 0; i < issuesFromJson.length(); i++ ) {
			String key = "";
			if(issuesFromJson.getJSONObject(i).has("key")) {
				key = issuesFromJson.getJSONObject(i).get("key").toString();
				JSONObject fields = (JSONObject) issuesFromJson.getJSONObject(i).get("fields");
				LocalDate releaseDate = getParsedDate(fields.get("resolutiondate").toString());
                LocalDate createdDate = getParsedDate(fields.get("created").toString());
                JSONArray versions = fields.getJSONArray("versions");
				JSONArray fixVersions = fields.getJSONArray("fixVersions");
				String openVersion = getVersionFromDate(releases, createdDate);

				Issue newIssue = addIssue(key, createdDate, releaseDate, openVersion, fixVersions, versions);

				if (!issues.contains(newIssue)) {
					if (newIssue.getFixedVers().isEmpty()) {
						newIssue.setFixedVers(getVersionFromDate(releases, releaseDate));
					}
					issues.add(newIssue);
				}
				
			}
		}

		// order issues by date
		Collections.sort(issues); 

		 // calculate proportion
		 int proportion = incremental(issues, releases);
		 rimakeIssuesVersions(issues, proportion, releases);

		String outname = projName + "-issues.csv";
		try(FileWriter fileWriter = new FileWriter(outname)) {
			//Name of CSV for output
			fileWriter.append("Index;Issue;IVersion;OVersion;AVersion;FixVersion");
			fileWriter.append("\n");

			for ( i = 0; i < issues.size(); i++) {
				fileWriter.append(Integer.toString(i));
				fileWriter.append(";");
				fileWriter.append(issues.get(i).getId());
				fileWriter.append(";");
				fileWriter.append(issues.get(i).getInjectVers());
				fileWriter.append(";");
				fileWriter.append(issues.get(i).getOpenVers());
				fileWriter.append(";");
				String listString = String.join(", ", issues.get(i).getAffectVers());
				fileWriter.append(listString);
				fileWriter.append(";");
				fileWriter.append(issues.get(i).getFixedVers());
				fileWriter.append(";");
				fileWriter.append("\n");
			}

			fileWriter.flush();

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error in csv writer.");
		}

		return issues;
	}

	private static String getVersionFromDate(List<Release> releases, LocalDate dateToSource) {
		
		for (var release : releases) {
			if (dateToSource.isBefore(release.getEndDate()) && dateToSource.isAfter(release.getStartDate())) {
				return release.getName();
			}
		}	
		return "";
	}

	private static LocalDate getParsedDate(String dateToParse) throws ParseException
	{
		// parse date from json format
		dateToParse = dateToParse.replace("T", " ");
		dateToParse = dateToParse.replace(".000+0000", "");
		DateFormat fmtForParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		var date = fmtForParser.parse(dateToParse);
		
		return LocalDate.ofInstant(date.toInstant(),
											ZoneId.systemDefault());
	}

	private static String buildURL(String projName, int startAt, int maxResults) {
		String base = "https://issues.apache.org/jira/rest/api/2/search?jql=project=";

		return base + projName +
		"%20AND%20issuetype=bug%20AND%20(status=resolved%20OR%20status=closed)%20AND%20resolution=fixed&"
		+ "fields=key,fixVersions,versions,created,resolutiondate&startAt="+ startAt + "&maxResults=" + maxResults + "";
	}
			
	private static Issue addIssue(String id, LocalDate creationDate,LocalDate resolutionDate, String openVers,
				JSONArray fixedVersions, JSONArray affectedVersions) {
		// injection version is first from affected version
		// affected version == versions from json
		// fixed == fixed versions from json
		// openening version is version from date of open ticket
	
		List<String> aVersions = new ArrayList<>();
		String[] affVersions = new String[affectedVersions.length()];
		for (int i = 0; i < affectedVersions.length(); i++ ) {
			String aversion = affectedVersions.getJSONObject(i).get("name").toString().replace("-incubating", ".0");
			affVersions[i] = aversion;
		}

		// remove all strange version: index, support, ecc
		for (int i = 0; i < affectedVersions.length(); i++ ) {
			String aversion = affectedVersions.getJSONObject(i).get("name").toString();
			if (isNumeric(aversion)) {
				aVersions.add(aversion);
			}
		}

		String[] fixVersions = new String[fixedVersions.length()];
		for (int i = 0; i < fixedVersions.length(); i++ ) {
			String fversion = fixedVersions.getJSONObject(i).get("name").toString().replace("-incubating", ".0");
			fixVersions[i] = fversion;
		}

		return new Issue(id, creationDate, resolutionDate, openVers, aVersions, fixVersions);
	}

	private static boolean isNumeric(String str){
        return str != null && str.matches("[0-9.]+");
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

	private static void rimakeIssuesVersions(List<Issue> issues, int proportion, List<Release> releases)
   {
       for (Issue issue : issues) {
            // set IV from proportion if IV is null
            if (issue.getInjectVers().isEmpty()) {
                int versionF = getNumberVersion(issue.getFixedVers(), releases);
                int versionO = getNumberVersion(issue.getOpenVers(), releases);
                int predictedIV = versionF - proportion * (versionF - versionO + 1);
                // set previos version of predicted IV
                predictedIV =  predictedIV-1;
				String newIV = getNearVersion(predictedIV, releases);
                issue.setInjectVers(newIV);
            }

            // calculate Affected version: between IV and FV incluso IV ma escluso FV
            List<String> newAffVersions = new ArrayList<>();
            for (Release rel : releases) {
				int iV = getNumberVersion(issue.getInjectVers(), releases);
				int fX = getNumberVersion(issue.getFixedVers(), releases);
                if (rel.getVersionNumber() >= iV && rel.getVersionNumber() < fX) {
                    newAffVersions.add(rel.getName());
                }
            }
            issue.setAffectVers(newAffVersions);
       }
   }

  	private static int getNumberVersion(String version, List<Release> releases) {
		for (Release release : releases) {
			if (version.equals(release.getName()))
			{
				return release.getVersionNumber();
			}
		}
		return 0;
	}

   	private static String getNearVersion(int version, List<Release> releases) {
		for (Release release : releases) {
			if (version <= release.getVersionNumber())
			{
				return release.getName();
			}
		}
		return "";
  	}

	private static int incremental(List<Issue> issues, List<Release> releases) {
		double sum = 0;
		int i = 0;
	
		for (Issue issue : issues) {
			String iVersion = issue.getInjectVers();
			String oVersion = issue.getOpenVers();
			String fVersion = issue.getFixedVers();
			if (!iVersion.isEmpty()) {
				int iv = getNumberVersion(iVersion, releases);
				int ov = getNumberVersion(oVersion, releases);
				int fv = getNumberVersion(fVersion, releases);
				if ((fv - ov + 1) == 0) {
					continue;
				}
				sum += (double) (fv - iv) / (fv - ov + 1);
				i++;
			}
		}
		if (i > 0) {
			return (int) Math.round(sum / i);
		}

		return 0;
	}
}
