package preprocess;

import generics.Bug;
import generics.Commit;
import generics.Hunk;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import miningChanges.CorpusCreation;
import utils.ChangeLocator;
import utils.FileToLines;
import utils.GitHelp;
import utils.HgHelp;
import utils.WriteLinesToFile;

public class ExtractCommits {
	public static List<Bug> bugs;
	public static HashSet<String> validCommits;
	public static String loc = main.Main.settings.get("workingLoc");
	public static String repo = main.Main.settings.get("repoDir");
	public static HashSet<String> concernedCommits;
	public static HashMap<String,String> changeMap;
	
	public static void indexHunks() throws Exception {
		getCommitsOneLine();
		loadCommits();
		extractHunks();
	}
	
	public static void getCommitsOneLine() throws Exception{
		String logFile = loc + File.separator + "logOneline.txt";
		File file = new File(logFile);
		if (!file.exists()) {
			String content = GitHelp.getAllCommitOneLine(logFile);
			WriteLinesToFile.writeToFiles(content, logFile);
		}
	}

	public static void loadCommits() {
		String commitFile = "";
		if (main.Main.settings.containsKey("concernedCommit"))
			commitFile = main.Main.settings.get("concernedCommit");
		List<String> lines = null;
		if (commitFile.equals("")) {
			lines = FileToLines.fileToLines(loc + File.separator + "logOneline.txt");
		} else lines = FileToLines.fileToLines(commitFile);
		System.out.println(commitFile);
		concernedCommits = new HashSet<String>();
		for (String line : lines) {
            System.out.println(line);
			concernedCommits.add(line.split("\t")[0].trim());
		}
		
		changeMap = ChangeLocator.getShortChangeMap();

	}
		
	private static boolean isValid(Hunk hunk) {
		
		if (hunk == null) return false;
		if (hunk.sourceFile == null || hunk.preChangeSet == null || hunk.postChangeSet == null || hunk.codes == null) 
			return false;
		else if (hunk.codes.size() == 0) return false;
		else return true;
	}
	
	
	public static void extractHunks() throws Exception {
		System.out.println("Extracting Hunks");
		String revisionLoc = loc + File.separator + "revisions";
		if (main.Main.settings.containsKey("revisionsLoc"))
			revisionLoc = main.Main.settings.get("revisionsLoc");
		File file = new File(revisionLoc);
		if (!file.exists())
			file.mkdir();
		
		File f = new File(loc + File.separator + "hunkCode");
		if (!f.exists()) f.mkdir();
		f = new File(loc + File.separator + "hunkLog");
		if (!f.exists()) f.mkdir();
		List<String> hunkIndex = new ArrayList<String>();

		for (String hash : concernedCommits) {
			
			if (!changeMap.containsKey(hash)) continue;
			
			// adaptpr for project ChangeLocator
			
			String fullHash = changeMap.get(hash);
			file = new File(revisionLoc + File.separator + fullHash);
			if (!file.exists())
				file.mkdir();
			String commitFile = revisionLoc + File.separator + fullHash + File.separator + fullHash + ".txt";
			file = new File(commitFile);
			if (!file.exists()) {
				String content = HgHelp.getCommitByRevision(hash, repo);
				WriteLinesToFile.writeToFiles(content, commitFile);
			}
			
			Commit commit = utils.ReadHunksFromLog.readOneCommitWithHunkGit(commitFile);
			List<Hunk> hunks = commit.getAllHunks();
			if (hunks == null) continue;
			System.out.println(hash + "\t" + hunks.size());
			for (Hunk hunk : hunks) {
				boolean flag = isValid(hunk);
//				System.out.println(flag);
				if (!flag) continue;
				String sourceFile = hunk.sourceFile;
				
				if (!sourceFile.endsWith(".java")) continue;
				
				sourceFile = sourceFile.substring(2,sourceFile.length()).replace("/", ".");
				
				String savePath = hash + "@" + hunk.preChangeSet + "@" + hunk.postChangeSet + "@" + file + "@" + hunk.bs + "@" + hunk.bl + "@" + hunk.as + "@" + hunk.al + ".txt";
				String content = commit.description + " " + commit.userName;

				List<String> words = CorpusCreation.getProcessedWords(content);
				WriteLinesToFile.writeLinesToFile(words, loc + File.separator + "hunkLog" + File.separator + savePath);
				content = file + " ";
				List<String> codes = hunk.codes;
				for (String code : codes) {
					content += code + " ";
				}
				words = CorpusCreation.getProcessedWords(content);
				WriteLinesToFile.writeLinesToFile(words, loc + File.separator + "hunkCode" + File.separator + savePath);				
				hunkIndex.add(savePath + "\t" + hunk.isSemantic());
			}
		}
		String saveFile = loc + File.separator + "hunkIndex.txt";
		WriteLinesToFile.writeLinesToFile(hunkIndex, saveFile);
	}

}
