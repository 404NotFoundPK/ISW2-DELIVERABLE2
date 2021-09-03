package com.torvergata;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.*;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.json.JSONException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GetMetrics {

    static Logger logger;

    public static void main(String[] args) throws Exception {
		logger = Logger.getLogger(GetMetrics.class.getName());

        String projectName ="TAJO";
        // String projectName ="BOOKKEEPER";

        String localPath = "D:\\GitCommits\\" + projectName + "-git\\.git";
        // String repositoryURL = "https://github.com/apache/"+ projectName + ".git";

        // if git open error the clone
        // try {
        //     Git git = Git.open(new File(localPath));
        //     git.close();
        // } catch (Exception e) {
		// 	logger.log(Level.INFO, "Starting Cloning Repository");
		// 	Git git = Git.cloneRepository().setURI(repositoryURL).setDirectory(new File(localPath)).call();
		// 	logger.log(Level.INFO, "Repository cloned Succesfully");
		//     git.close();
        // }
       

		List<Release> releases = GeJiraReleases.getReleases(projectName);
        int halfIndex = (int)((double)releases.size()/2 + 0.5);
        var halfVersions = releases.subList(0, halfIndex);

        List<Issue> issues = GetJiraIssues.getIssues(projectName, releases);
       
        List<FileExtended> files = GetGitHubCommits.getCommits(localPath, projectName, releases);

        // set up opened version
        List<FileExtended> newFiles = setOpenedVersion(files, issues);


		List<FileMetrics> filesWithMetrics = getMetricsForAllFiles(newFiles, halfVersions, projectName);

        writeCSV(filesWithMetrics, projectName);
        writeArff(filesWithMetrics, projectName);

        // write arff for every verion
        for (Release release : halfVersions) {
            writeCSVWeka(filesWithMetrics, projectName, release.getVersionNumber());
        }

        logger.log(Level.INFO, "Wrote classes: {0}", Integer.toString(newFiles.size()));
	}

    public static void startGetMetrics(String projectName) throws JSONException, IOException, ParseException, GitAPIException {
        logger = Logger.getLogger(GetMetrics.class.getName());

        String localPath = "D:\\GitCommits\\" + projectName + "-git\\.git";
        // String repositoryURL = "https://github.com/apache/"+ projectName + ".git";

        // if git open error the clone
        // try {
        //     Git git = Git.open(new File(localPath));
        //     git.close();
        // } catch (Exception e) {
		// 	logger.log(Level.INFO, "Starting Cloning Repository");
		// 	Git git = Git.cloneRepository().setURI(repositoryURL).setDirectory(new File(localPath)).call();
		// 	logger.log(Level.INFO, "Repository cloned Succesfully");
		//     git.close();
        // }
       

		List<Release> releases = GeJiraReleases.getReleases(projectName);
        int halfIndex = (int)((double)releases.size()/2 + 0.5);
        var halfVersions = releases.subList(0, halfIndex);

        List<Issue> issues = GetJiraIssues.getIssues(projectName, releases);
       
        List<FileExtended> files = GetGitHubCommits.getCommits(localPath, projectName, halfVersions);

        // set up opened version
        List<FileExtended> newFiles = setOpenedVersion(files, issues);


		List<FileMetrics> filesWithMetrics = getMetricsForAllFiles(newFiles, releases, projectName);

        writeCSV(filesWithMetrics, projectName);
        writeArff(filesWithMetrics, projectName);

        // write arff for every verion
        for (Release release : releases) {
            writeCSVWeka(filesWithMetrics, projectName, release.getVersionNumber());
        }

		logger.log(Level.INFO, "Wrote classes: {0}", Integer.toString(newFiles.size()));
    }

    private static LocalDate getAddedDate(String path, String projectName) {
        String localPath = "D:\\GitCommits\\" + projectName + "-git\\.git";
        try (Git git = Git.open(new File(localPath))) {

			Iterable<RevCommit> logs = git.log().addPath(path).call();
            LocalDate addedDate = LocalDate.now();

            for (RevCommit commit : logs) {
                PersonIdent authorIdent = commit.getAuthorIdent();
                Date authorDate = authorIdent.getWhen();
                LocalDate temp = authorDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                if (temp.isBefore(addedDate)) {
                    addedDate = temp;
                }                
            }

            return addedDate;
        }
        catch (Exception e)
        {
            logger.info(e.toString());
            return LocalDate.now();
        }
    }

	public static List<FileMetrics> getMetricsForAllFiles(List<FileExtended> files, List<Release> releases, String projectName) {
        List<FileMetrics> newFiles = new ArrayList<>();
        // remove duplicate
        // calculate metrics as avg N files in the same commit, max N files in the same commit, N of authors

        Map<Integer, List<FileExtended>> filesGroupedbyVersion =
        files.stream().collect(Collectors.groupingBy(f -> f.getCurrentVersion()));

        List<Entry<Integer, List<FileExtended>>> versionsList = new ArrayList<>(filesGroupedbyVersion.entrySet());
        versionsList.sort(Entry.comparingByKey());

        for (Entry<Integer,List<FileExtended>> pair : versionsList) {
            Integer version = pair.getKey();
            Release release = getRelease(version, releases);
            
            Map<String, List<FileExtended>> filesGroupedByPath =
            files.stream().collect(Collectors.groupingBy(f -> f.getPath()));

            for (Entry<String,List<FileExtended>> pairByPath : filesGroupedByPath.entrySet()) {
                String path = pairByPath.getKey();
                List<FileExtended> duplicates = pairByPath.getValue();

                Map<String, List<FileExtended>> filesGroupedByAuthor =
                    duplicates.stream().collect(Collectors.groupingBy(f -> f.getAuthor()));

                Map<String, List<FileExtended>> filesGroupedByIssue =
                    duplicates.stream().collect(Collectors.groupingBy(f -> f.getIssuesKey()));

                newFiles.add(getMetricsFileProVersion(path, release, duplicates, filesGroupedByAuthor.size(),
                filesGroupedByIssue.size(), projectName));
            }
        }
       
        return newFiles;
    }

    public static FileMetrics getMetricsFileProVersion(String path, Release release, List<FileExtended> duplicates,
                                int numberOfAuthors, int numberOfBugsFixed, String projectName) {
        FileMetrics newFile = new FileMetrics(path);
        newFile.setVersion(release.getVersionNumber());

        int numbersOfDuplicates = duplicates.size();
        int numberOfLines = 0; // AVG?
        long age = 0;
        long weightedAge = 0;  // somma(age*added lines)/somma(added lines) ?
        int changeSetSize = 0;
        int avgChangeSetSize = 0;
        int maxChangeSetSize = 0;
        int addedLines = 0;
        int avgAddedLines = 0;
        int maxAddedLines = 0;
        int changedLines = 0;
        int avgChangedLines = 0;
        int maxChangedLines = 0;
        int churnLines = 0;
        int avgChurnLines = 0;
        int maxChurnLines = 0;
        String buggy = "NO";

        LocalDate addedDate = getAddedDate(path, projectName);

        // age = date of release - date of create/added file in weeks
        age = getWeeksBetweenDates(addedDate, release.getEndDate());
        if (age < 0) {
            age = 1;
        }

        for (FileExtended fileToCalculate : duplicates) {
            numberOfLines += fileToCalculate.getTotalLines();

            weightedAge += age*fileToCalculate.getAddedLines();
            
            changeSetSize+= fileToCalculate.getNumberFiles();
            if (maxChangeSetSize < fileToCalculate.getNumberFiles()) {
                maxChangeSetSize = fileToCalculate.getNumberFiles();
            }

            addedLines += fileToCalculate.getAddedLines();
            if (maxAddedLines < fileToCalculate.getAddedLines()) {
                maxAddedLines = fileToCalculate.getAddedLines();
            }

            changedLines += (fileToCalculate.getAddedLines() + fileToCalculate.getDeletedLines());
            if (maxChangedLines < (fileToCalculate.getAddedLines() + fileToCalculate.getDeletedLines())) {
                maxChangedLines = (fileToCalculate.getAddedLines() + fileToCalculate.getDeletedLines());
            }

            churnLines += (fileToCalculate.getAddedLines() - fileToCalculate.getDeletedLines());
            if (maxChurnLines < (fileToCalculate.getAddedLines() - fileToCalculate.getDeletedLines())) {
                maxChurnLines = (fileToCalculate.getAddedLines() - fileToCalculate.getDeletedLines());
            }

            // if in AV then bug
            if (fileToCalculate.getAffectedVersion().contains(release.getName())) {
                buggy = "YES";
            }
        }

        newFile.setNumberOfAuthors(numberOfAuthors);

        // avg
        if (numbersOfDuplicates > 0) {
            numberOfLines = numberOfLines / numbersOfDuplicates;
        }
        newFile.setNumberOfLines(numberOfLines);
        newFile.setAge(age);
        if (addedLines > 0 && weightedAge > 0) {
            weightedAge = weightedAge / addedLines;
        }

        newFile.seWeightAge(weightedAge);
        newFile.setNumberOfCommits(numbersOfDuplicates);
        newFile.setNumberOfBugs(numberOfBugsFixed);  
       
        newFile.setChangeSetSize(changeSetSize);
        if (numbersOfDuplicates > 0) {
            avgChangeSetSize = changeSetSize / numbersOfDuplicates;
        }

        newFile.setAvgChangeSetSize(avgChangeSetSize);
        newFile.setMaxChangeSetSize(maxChangeSetSize);
        newFile.setAddedLines(addedLines);
        if (numbersOfDuplicates > 0) {
            avgAddedLines = addedLines / numbersOfDuplicates;
        }

        newFile.setAvgAddedLines(avgAddedLines);
        newFile.setMaxAddedLines(maxAddedLines);
        newFile.setChangedLines(changedLines);
        if (numbersOfDuplicates > 0) {
            avgChangedLines = changedLines / numbersOfDuplicates;
        }

        newFile.setAvgChangedLines(avgChangedLines);
        newFile.setMaxChangedLines(maxChangedLines);
        newFile.setChurn(churnLines);
        if (numbersOfDuplicates > 0) {
            avgChurnLines = churnLines / numbersOfDuplicates;
        }

        newFile.setAvgChurn(avgChurnLines);
        newFile.setMaxChurn(maxChurnLines);
        newFile.setBuggy(buggy);
        return newFile;
    }

    private static long getWeeksBetweenDates(LocalDate addedDate, LocalDate releaseDate) {
        return ChronoUnit.WEEKS.between(addedDate, releaseDate);
    }

    private static Release getRelease(int version, List<Release> releases) {
        for (Release release : releases) {
          if (version == release.getVersionNumber())  {
            return release;
          }
        }

        return null;
    }

    private static List<FileExtended> setOpenedVersion (List<FileExtended> files, List<Issue> issues) 
    {
        List<FileExtended> newFiles = new ArrayList<>();
        for (FileExtended file : files) {
            String keyIssue = file.getIssuesKey();
            for (Issue issue : issues) {
                if (keyIssue.equals(issue.getId())) {
                    file.setOpenedVersion(issue.getOpenVers());
                    file.setInjectVersion(issue.getInjectVers());
                    file.setFixedVersion(issue.getFixedVers());
                    file.setAffectedVersion(issue.getAffectVers());
                    newFiles.add(file);
                    break;
                }
            }
        }
        return newFiles;
    }

    private static void writeCSVWeka(List<FileMetrics> filesWithMetrics, String projectName, int version) {
        String wekaFile = projectName + "-dataset-" + version + ".arff";
        int i = 0;

        try(FileWriter fileWriter = new FileWriter(wekaFile)) {
            fileWriter.append("@relation '" + projectName + "'\n"
                + "\n"
                + "@attribute numberOfLines numeric\n"
                + "@attribute numberOfAuthors numeric\n"
                + "@attribute age numeric\n"
                + "@attribute weightAge numeric\n"
                + "@attribute numberOfCommits numeric\n"
                + "@attribute changeSetSize numeric\n"
                + "@attribute avgChangeSetSize numeric\n"
                + "@attribute maxChangeSetSize numeric\n"
                + "@attribute addedLines numeric\n"
                + "@attribute avgAddedLines numeric\n"
                + "@attribute maxAddedLines numeric\n"
                + "@attribute changedLines numeric\n"
                + "@attribute avgChangedLines numeric\n"
                + "@attribute maxChangedLines numeric\n"
                + "@attribute churn numeric\n"
                + "@attribute avgChurn numeric\n"
                + "@attribute maxChurn numeric\n"
                + "@attribute numberOfBugs numeric\n"
                + "@attribute buggy {NO,YES}\n"
                + "\n"
                + "@data\n");

			// fileWriter.append("version,fileName,numberOfLines,age,weightAge,numberOfAuthors,numberOfCommits,"
            // +"changeSetSize,avgChangeSetSize,maxChangeSetSize,addedLines,avgAddedLines,maxAddedLines,"
            // +"changedLines,avgChangedLines,maxChangedLines,churn,avgChurn,maxChurn,numberOfBugs, buggy,");
			// fileWriter.append("\n");

			for (i = 0; i < filesWithMetrics.size(); i++) {
                if (filesWithMetrics.get(i).getVersion() > version) {
                    break;
                }
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getNumberOfLines()));
				fileWriter.append(",");
				fileWriter.append(Long.toString(filesWithMetrics.get(i).getAge()));
				fileWriter.append(",");
				fileWriter.append(Long.toString(filesWithMetrics.get(i).getWeightAge()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getNumberOfAuthors()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getNumberOfCommits()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getChangeSetSize()));
				fileWriter.append(",");
                fileWriter.append(Integer.toString(filesWithMetrics.get(i).getAvgChangeSetSize()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getMaxChangeSetSize()));
				fileWriter.append(",");
                fileWriter.append(Integer.toString(filesWithMetrics.get(i).getAddedLines()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getAvgAddedLines()));
				fileWriter.append(",");
                fileWriter.append(Integer.toString(filesWithMetrics.get(i).getMaxAddedLines()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getChangedLines()));
				fileWriter.append(",");
                fileWriter.append(Integer.toString(filesWithMetrics.get(i).getAvgChangedLines()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getMaxChangedLines()));
				fileWriter.append(",");
                fileWriter.append(Integer.toString(filesWithMetrics.get(i).getChurn()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getAvgChurn()));
				fileWriter.append(",");
                fileWriter.append(Integer.toString(filesWithMetrics.get(i).getMaxChurn()));
				fileWriter.append(",");
                fileWriter.append(Integer.toString(filesWithMetrics.get(i).getNumberOfBugs()));
				fileWriter.append(",");
				fileWriter.append(filesWithMetrics.get(i).getBuggy());
				fileWriter.append("\n");
			}

			fileWriter.flush();

		} catch (Exception e) {
			logger.log(java.util.logging.Level.SEVERE, "Error in csv writer.");
		}
    }

    private static void writeCSV(List<FileMetrics> filesWithMetrics, String projName) {
        String outname = projName + "-dataset.csv";
		try(FileWriter fileWriter = new FileWriter(outname)) {
			//Name of CSV for output
			fileWriter.append("version,fileName,numberOfLines,age,weightAge,numberOfAuthors,numberOfCommits,"
            +"changeSetSize,avgChangeSetSize,maxChangeSetSize,addedLines,avgAddedLines,maxAddedLines,"
            +"changedLines,avgChangedLines,maxChangedLines,churn,avgChurn,maxChurn,numberOfBugs,buggy,");
			fileWriter.append("\n");

			for (int i = 0; i < filesWithMetrics.size(); i++) {
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getVersion()));
				fileWriter.append(",");
                fileWriter.append(filesWithMetrics.get(i).getFileName());
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getNumberOfLines()));
				fileWriter.append(",");
				fileWriter.append(Long.toString(filesWithMetrics.get(i).getAge()));
				fileWriter.append(",");
				fileWriter.append(Long.toString(filesWithMetrics.get(i).getWeightAge()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getNumberOfAuthors()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getNumberOfCommits()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getChangeSetSize()));
				fileWriter.append(",");
                fileWriter.append(Integer.toString(filesWithMetrics.get(i).getAvgChangeSetSize()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getMaxChangeSetSize()));
				fileWriter.append(",");
                fileWriter.append(Integer.toString(filesWithMetrics.get(i).getAddedLines()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getAvgAddedLines()));
				fileWriter.append(",");
                fileWriter.append(Integer.toString(filesWithMetrics.get(i).getMaxAddedLines()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getChangedLines()));
				fileWriter.append(",");
                fileWriter.append(Integer.toString(filesWithMetrics.get(i).getAvgChangedLines()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getMaxChangedLines()));
				fileWriter.append(",");
                fileWriter.append(Integer.toString(filesWithMetrics.get(i).getChurn()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getAvgChurn()));
				fileWriter.append(",");
                fileWriter.append(Integer.toString(filesWithMetrics.get(i).getMaxChurn()));
				fileWriter.append(",");
                fileWriter.append(Integer.toString(filesWithMetrics.get(i).getNumberOfBugs()));
				fileWriter.append(",");
				fileWriter.append(filesWithMetrics.get(i).getBuggy());
				fileWriter.append(",");
				fileWriter.append("\n");
			}

			fileWriter.flush();

		} catch (Exception e) {
			logger.log(java.util.logging.Level.SEVERE, "Error in csv metrics writer.");
		}
    }

    private static void writeArff(List<FileMetrics> filesWithMetrics, String projectName) {
        String wekaFile = projectName + "-dataset.arff";
        int i = 0;

        try(FileWriter fileWriter = new FileWriter(wekaFile)) {
            fileWriter.append("@relation '" + projectName + "'\n"
                + "\n"
                + "@attribute release numeric\n"
                + "@attribute fileName string\n"
                + "@attribute numberOfLines numeric\n"
                + "@attribute numberOfAuthors numeric\n"
                + "@attribute age numeric\n"
                + "@attribute weightAge numeric\n"
                + "@attribute numberOfCommits numeric\n"
                + "@attribute changeSetSize numeric\n"
                + "@attribute avgChangeSetSize numeric\n"
                + "@attribute maxChangeSetSize numeric\n"
                + "@attribute addedLines numeric\n"
                + "@attribute avgAddedLines numeric\n"
                + "@attribute maxAddedLines numeric\n"
                + "@attribute changedLines numeric\n"
                + "@attribute avgChangedLines numeric\n"
                + "@attribute maxChangedLines numeric\n"
                + "@attribute churn numeric\n"
                + "@attribute avgChurn numeric\n"
                + "@attribute maxChurn numeric\n"
                + "@attribute numberOfBugs numeric\n"
                + "@attribute buggy {NO,YES}\n"
                + "\n"
                + "@data\n");

			// fileWriter.append("version,fileName,numberOfLines,age,weightAge,numberOfAuthors,numberOfCommits,"
            // +"changeSetSize,avgChangeSetSize,maxChangeSetSize,addedLines,avgAddedLines,maxAddedLines,"
            // +"changedLines,avgChangedLines,maxChangedLines,churn,avgChurn,maxChurn,numberOfBugs, buggy,");
			// fileWriter.append("\n");

			for (i = 0; i < filesWithMetrics.size(); i++) {
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getVersion()));
				fileWriter.append(",");
                fileWriter.append(filesWithMetrics.get(i).getFileName());
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getNumberOfLines()));
				fileWriter.append(",");
				fileWriter.append(Long.toString(filesWithMetrics.get(i).getAge()));
				fileWriter.append(",");
				fileWriter.append(Long.toString(filesWithMetrics.get(i).getWeightAge()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getNumberOfAuthors()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getNumberOfCommits()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getChangeSetSize()));
				fileWriter.append(",");
                fileWriter.append(Integer.toString(filesWithMetrics.get(i).getAvgChangeSetSize()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getMaxChangeSetSize()));
				fileWriter.append(",");
                fileWriter.append(Integer.toString(filesWithMetrics.get(i).getAddedLines()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getAvgAddedLines()));
				fileWriter.append(",");
                fileWriter.append(Integer.toString(filesWithMetrics.get(i).getMaxAddedLines()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getChangedLines()));
				fileWriter.append(",");
                fileWriter.append(Integer.toString(filesWithMetrics.get(i).getAvgChangedLines()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getMaxChangedLines()));
				fileWriter.append(",");
                fileWriter.append(Integer.toString(filesWithMetrics.get(i).getChurn()));
				fileWriter.append(",");
				fileWriter.append(Integer.toString(filesWithMetrics.get(i).getAvgChurn()));
				fileWriter.append(",");
                fileWriter.append(Integer.toString(filesWithMetrics.get(i).getMaxChurn()));
				fileWriter.append(",");
                fileWriter.append(Integer.toString(filesWithMetrics.get(i).getNumberOfBugs()));
				fileWriter.append(",");
				fileWriter.append(filesWithMetrics.get(i).getBuggy());
				fileWriter.append("\n");
			}

			fileWriter.flush();

		} catch (Exception e) {
			logger.log(java.util.logging.Level.SEVERE, "Error in arff writer.");
		}
    }

}
