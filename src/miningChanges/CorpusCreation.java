package miningChanges;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import utils.ReadBugsFromXML;
import generics.Bug;
import utils.Splitter;
import utils.Stem;
import utils.Stopword;
import utils.WriteLinesToFile;

public class CorpusCreation {
	
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
		for (Bug bug : bugs) {
			String bugContent = bug.toString();
			List<String> processedWords = getProcessedWords(bugContent);
			String fileName = file.getAbsolutePath() + File.separator + bug.id + ".txt";
			WriteLinesToFile.writeLinesToFile(processedWords, fileName);
		}
	}
	
	public static void createCorpus() {
		processBugReports();
	}
}
