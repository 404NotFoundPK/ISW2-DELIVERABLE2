package com.torvergata;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

public class GetGitHubCommits {

	private static Logger logger;
	private static String projectName ="TAJO";
	// private static String projectName ="BOOKKEEPER";

    public static void main(String[] args) throws Exception {
        String localPath = "D:\\GitCommits\\" + projectName + "-git\\.git";
		
		logger = Logger.getLogger("GetGitHubCommits");

		List<Release> releases = GeJiraReleases.getReleases(projectName);
		List<FileExtended> totalFiles = getCommits(localPath, projectName, releases);
		logger.log(Level.INFO, "total files: " + totalFiles.size());
    
    }

	public static List<FileExtended> getCommits(String localPath, String projectName, List<Release> releases) throws IOException, GitAPIException {
		logger = Logger.getLogger("GetGitHubCommits");
		List<FileExtended> totalFiles = new ArrayList<>();

		try (Git git = Git.open(new File(localPath))) {
			logger.log(Level.INFO, "Repository opened.");
			Repository repository = git.getRepository();

			List<Ref> tags = git.tagList().call();
			int conta = 0;
			int globalConta = 0;


		if(tags != null && !tags.isEmpty()) {
			for(Ref tag : tags) {
				String tagName = tag.getName();
				// get only first hlaf releases
				String[] tagVersion = tagName.split("-");
				String currentVersion = "0";
				int currentVersionNumber = 0;
				if (tagVersion.length > 0) {
					currentVersion = tagVersion[1];
				}
				currentVersionNumber = inRealeases(currentVersion, releases);
				if (currentVersionNumber == 0) {
					continue;
				}
				
				List<FileExtended> tagFiles = new ArrayList<>();
				logger.log(Level.INFO, "Tag: {0}:", tagName);
				Ref peeledRef = repository.getRefDatabase().peel(tag);
				LogCommand log = git.log();
				if(peeledRef.getPeeledObjectId() != null) {
					log.add(peeledRef.getPeeledObjectId());
					} else {
					log.add(tag.getObjectId());
					}

					Iterable<RevCommit> logs = log.call();
					conta = 0;
					for (RevCommit commit : logs) {
						conta++;
						String title = commit.getShortMessage();
						String issuesKey = "";
						if (title.contains(projectName + "-")) {
							// exract and add ticket key to file
							//string as TAJO-1234:
							String[] temp = title.split(":");
							if (temp.length > 1) {
								issuesKey = temp[0];
							}
							
							//string as [TAJO-1234]
							temp = title.split("]");
							if (temp.length > 1) {
								issuesKey = temp[0];
								// remove first carater
								issuesKey = issuesKey.substring(1);
							}

							if (issuesKey.isEmpty())
							{
								continue;
							}

							// add files only if commit has link to jira, also if has TAJO/BOOKKEEPER in title
							PersonIdent authorIdent = commit.getAuthorIdent();
							Date authorDate = authorIdent.getWhen();
							LocalDate changedDate = authorDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
							List<FileExtended> diffFiles = getFilesInCommit(repository, commit, currentVersionNumber, issuesKey, changedDate, authorIdent.getName());
							if (!diffFiles.isEmpty()) {
								tagFiles.addAll(diffFiles);
							}
						}
						
					}
					logger.info("Commit tag : " + tagName + " : " + conta);
					globalConta+=conta;
				// end tag
				// remove duplicate files in version
				// set eventuel injected and fixed version

				totalFiles.addAll(tagFiles);
			}

		}

		logger.log(Level.INFO, "Commit tag : " + globalConta);
		}

		logger.log(Level.INFO, "Fine get files and commits");

		return totalFiles;
	}

	public static List<FileExtended> getFilesInCommit(Repository repository, RevCommit commit, int version, String issuesKey, LocalDate changedDate, String author) {
		int contaFile = 0;
		List<FileExtended> files = new ArrayList<>();	
		try (RevWalk rw = new RevWalk(repository)) {
			if (commit.getParentCount() == 0) {
				TreeWalk tw = new TreeWalk(repository);
				tw.reset();
				tw.setRecursive(true);
				tw.addTree(commit.getTree());
				while (tw.next()) {
					FileExtended file = new FileExtended(tw.getPathString(), version, 0, 0, 0, 0, issuesKey, changedDate, author);
					files.add(file);
					logger.log(Level.INFO, "added-- {0}", tw.getPathString());
				}
				tw.close();
			} else {
				RevCommit parent = rw.parseCommit(commit.getParent(0).getId());
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				DiffFormatter df = new DiffFormatter(os);
				df.setRepository(repository);
				df.setDiffComparator(RawTextComparator.DEFAULT);
				df.setDetectRenames(true);
				List<DiffEntry> diffs = df.scan(parent.getTree(), commit.getTree());

				for (DiffEntry diff : diffs) {
					String path = diff.getNewPath();
					
					if (!path.startsWith(projectName.toLowerCase())) {
						continue;
					}

					int typeChanged = 0;
					int linesDeleted = 0;
					int linesAdded = 0;
					FileHeader fileHeader = df.toFileHeader(diff);
					byte[] content = getContent(commit.getTree(), path, repository);
					int linesTotal = getTotalLines(content);
					for (Edit edit : fileHeader.toEditList()) {
						linesDeleted += edit.getEndA() - edit.getBeginA();
						linesAdded += edit.getEndB() - edit.getBeginB();
					}
					
					// create the path change model
					ChangeType typeOfChange = diff.getChangeType();

					if (typeOfChange.name().equals("DELETE")) {
						path = diff.getOldPath();
						typeChanged = 3;
					}
					if (typeOfChange.name().equals("ADD")) {
						typeChanged = 1;
					}
					if (typeOfChange.name().equals("MODIFY")) {
						typeChanged = 2;
					}
					if (path.endsWith(".java")) {
						FileExtended file = new FileExtended(path, version, linesAdded, linesDeleted, linesTotal, typeChanged, issuesKey, changedDate, author);
						files.add(file);
					}
				}
				df.close();
			}
		} catch (IOException e) {
			logger.log(Level.INFO, e.toString());
		}

		int numberOfFiles = files.size();
		for (FileExtended fileExtended : files) {
			fileExtended.setNumberFiles(numberOfFiles);
		}

		if (contaFile > 0) {
			logger.log(Level.INFO, "conta file {0}", contaFile);
		}

		return files;
	}

	private static byte[] getContent(RevTree tree, String path, Repository repo) throws IOException {
		try (TreeWalk treeWalk = TreeWalk.forPath(repo, path, tree)) {
		  ObjectId blobId = treeWalk.getObjectId(0);
		  try (ObjectReader objectReader = repo.newObjectReader()) {
			ObjectLoader objectLoader = objectReader.open(blobId);
			return objectLoader.getBytes();
		  }
		}
		catch (Exception e)
		{
			return new byte[0];
		}
	}
	
	private static int getTotalLines(byte[] data) {
		if (data.length == 0) {
			return 0;
		}

		int count = 0;

		ByteArrayInputStream stream = new ByteArrayInputStream(data);
		InputStreamReader streamReader = new InputStreamReader(stream, StandardCharsets.UTF_8);
		try {
			BufferedReader bufferedReader = new BufferedReader(streamReader);
			while ((bufferedReader.readLine()) != null) {
				count++;
			}
		} catch (IOException e) {
			logger.log(Level.INFO, e.toString());	
		}

		return count;
	}

	private static int inRealeases(String version, List<Release> releases) {
		for (Release release : releases) {
			if (release.getName().equals(version)) {
				return release.getVersionNumber();
			}
		}
		return 0;
	}

}
