package miningChanges;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import utils.ChangeLocator;
import utils.FileToLines;
import utils.GitHelp;
import utils.HgHelp;
import utils.ReadBugsFromXML;
import generics.Bug;
import generics.Commit;
import generics.Hunk;
import utils.Splitter;
import utils.Stem;
import utils.Stopword;
import utils.WriteLinesToFile;

public class CorpusCreation {
	
	public static List<Bug> bugs;
	public static HashSet<String> validCommits;
	public static String loc = main.Main.settings.get("workingLoc");
	public static String repo = main.Main.settings.get("repoDir");
	public static HashSet<String> concernedCommits;
	public static HashMap<String,String> changeMap;
	
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
	
	public static List<String> getProcessedWords(String content) {
		String[] words = Splitter.splitSourceCode(content);
		List<String> processedWords = new ArrayList<String>();
		for (String word : words) {
			word = Stem.stem(word.toLowerCase());
			if (Stopword.isEnglishStopword(word) || Stopword.isKeyword(word)) continue;
			if (word.trim().equals("")) continue;
			processedWords.add(word);
		}
		return processedWords;
	}
	
	public static void processBugReports() {
		String proDir = main.Main.settings.get("workingLoc");

		
		List<Bug> bugs = ReadBugsFromXML.getFixedBugsFromXML(main.Main.settings.get("bugReport"));
		
		File file = new File(proDir + File.separator + "bugText");
		if (!file.exists()) file.mkdir();
		System.out.println("The number of bugs:" + bugs.size());
		ExtractCodeLikeTerms eclt = new ExtractCodeLikeTerms();
		HashMap<String,Integer> cltMaps = eclt.extractCodeLikeTerms();
		List<String> lines = new ArrayList<String>();
		for (Bug bug : bugs) {
			String bugContent = bug.toString();
			List<String> processedWords = getProcessedWords(bugContent);
			String fileName = file.getAbsolutePath() + File.separator + bug.id + ".txt";
			WriteLinesToFile.writeLinesToFile(processedWords, fileName);
			String content = bug.summary + " " + bug.description;
			List<String> clts = eclt.extractCLTFromNaturalLanguage(content);
			String line = "" + bug.id;
			for (String clt : clts) {
				if (cltMaps.containsKey(clt))
				    line += "\t" + cltMaps.get(clt);
			}
			lines.add(line);
		}
		String filename = main.Main.settings.get("workingLoc") + File.separator + "bugCLTIndex.txt";
		WriteLinesToFile.writeLinesToFile(lines, filename);
	}
	
	public static void processHunks() throws Exception {
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
		int count = 0;
		List<String> hashCLTIndex = new ArrayList<String>();
		List<String> hunkCLTIndex = new ArrayList<String>();
		ExtractCodeLikeTerms eclt = new ExtractCodeLikeTerms();
		HashMap<String,Integer> cltMaps = eclt.extractCodeLikeTerms();
		for (String hash : concernedCommits) {
			count++;
			System.out.println(count + ":" + concernedCommits.size());
			if (!changeMap.containsKey(hash)) continue;
			//System.out.println(fullHash);
			// adaptpr for project ChangeLocator
			
			String fullHash = changeMap.get(hash);
//			System.out.println(fullHash);
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
//			System.out.println(hash + "\t" + hunks.size());
			
			String content = commit.description;
			for (String tmp : commit.files) {
				content += " " + tmp;
			}
			List<String> clts = eclt.extractCLTFromNaturalLanguage(content);
			String line = "" + hash;
			for (String clt : clts) {
//				if (cltMaps.get(clt) == null) System.out.println(clts.toString());
                if (cltMaps.containsKey(clt))
				    line += "\t" + cltMaps.get(clt);
			}
			hashCLTIndex.add(line);
			
			for (Hunk hunk : hunks) {
				boolean flag = isValid(hunk);
//				System.out.println(flag);
				if (!flag) continue;
				String sourceFile = hunk.sourceFile;
				
				if (!sourceFile.endsWith(".java")) continue;
				
				sourceFile = sourceFile.substring(2,sourceFile.length()).replace("/", ".");
				
				String savePath = hash + "@" + hunk.preChangeSet + "@" + hunk.postChangeSet + "@" + sourceFile + "@" + hunk.bs + "@" + hunk.bl + "@" + hunk.as + "@" + hunk.al + ".txt";
				content = commit.description + " " + commit.userName;

				List<String> words = CorpusCreation.getProcessedWords(content);
				WriteLinesToFile.writeLinesToFile(words, loc + File.separator + "hunkLog" + File.separator + savePath);
				content = file + " ";
				List<String> codes = hunk.codes;
				for (String code : codes) {
					content += code + " ";
				}
				words = CorpusCreation.getProcessedWords(content);
				WriteLinesToFile.writeLinesToFile(words, loc + File.separator + "hunkCode" + File.separator + savePath);				
				int index = hunkIndex.size();
				hunkIndex.add(savePath + "\t" + hunk.isSemantic());
				
				String unchangedCode = "";
				String deleteCode = "";
				String addCode = "";
				
				List<Integer> mark = hunk.mark;
				for (int j = 0; j < codes.size(); j++) {
					String code = codes.get(j);
					if (mark.get(j) == -1) deleteCode += " " + code;
					else if (mark.get(j) == 0) unchangedCode += " " + code;
					else if (mark.get(j) == 1) addCode += " " + code;
				}
				
				clts = eclt.extractCLTFromCodeSnippet(sourceFile);
				line = index + ":f";
				for (String clt : clts) {
					if (cltMaps.containsKey(clt))
					    line += "\t" + cltMaps.get(clt);
				}
				hunkCLTIndex.add(line);
				clts = eclt.extractCLTFromCodeSnippet(unchangedCode);
				line = index + ":0";
				for (String clt : clts) {
					if (cltMaps.containsKey(clt))
					    line += "\t" + cltMaps.get(clt);
				}
				hunkCLTIndex.add(line);
				clts = eclt.extractCLTFromCodeSnippet(deleteCode);
				line = index + ":-1";
				for (String clt : clts) {
					if (cltMaps.containsKey(clt))
					    line += "\t" + cltMaps.get(clt);
				}
				hunkCLTIndex.add(line);
				clts = eclt.extractCLTFromCodeSnippet(addCode);
				line = index + ":1";
				for (String clt : clts) {
					if (cltMaps.containsKey(clt))
                        line += "\t" + cltMaps.get(clt);
				}
				hunkCLTIndex.add(line);
			}
		}
		String saveFile = loc + File.separator + "hunkIndex.txt";
		WriteLinesToFile.writeLinesToFile(hunkIndex, saveFile);
		String filename = loc + File.separator + "commitCLTIndex.txt";
		WriteLinesToFile.writeLinesToFile(hashCLTIndex, filename);
		filename = loc + File.separator + "hunkCLTIndex.txt";
		WriteLinesToFile.writeLinesToFile(hunkCLTIndex, filename);
	}
	
	private static boolean isValid(Hunk hunk) {
		
		if (hunk == null) return false;
		if (hunk.sourceFile == null || hunk.preChangeSet == null || hunk.postChangeSet == null || hunk.codes == null) 
			return false;
		else if (hunk.codes.size() == 0) return false;
		else return true;
	}
	
	public static void createCorpus() throws Exception {
		loadCommits();
        processBugReports();
		processHunks();
	}
}
