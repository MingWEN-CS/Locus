package main;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import miningChanges.CorpusCreation;
import miningChanges.ExtractCodeLikeTerms;
import miningChanges.ProduceChangeLevelResults;
import miningChanges.ObtainVSMScore;
import preprocess.ExtractCommits;
import utils.FileToLines;

public class Main {
	
	public static HashMap<String,String> settings;
	public static String task = "";
	public static String repoDir = "";
	public static String workingLoc = "";
	public static String bugReport = "";
	public static String changeOracle = "";
	
	public static void startTask() throws Exception {
		if (task.equals("indexHunks")) {
			ExtractCommits.indexHunks();
		}
		else if (task.equals("corpusCreation")) {
			// Create the corpus change logs, hunks, and create the code like term corpus
			CorpusCreation.createCorpus();
		}
		else if (task.equals("extractCodeLikeTerms"))
			ExtractCodeLikeTerms.entry();
		else if (task.equals("obtainSimilarity")) {
			ObtainVSMScore os = new ObtainVSMScore();
			os.obtainSimilarity();
		} else if (task.equals("produceFinalResults")) {
			ProduceChangeLevelResults rank = new ProduceChangeLevelResults();
			rank.getFinalResults();
		} else if (task.equals("all")) {
			CorpusCreation.createCorpus();
			System.out.println("Finish Creating Corpus");
			ExtractCodeLikeTerms.entry();
			System.out.println("Finish Extracting Code Like Terms");
			ObtainVSMScore os = new ObtainVSMScore();
			os.obtainSimilarity();
			System.out.println("Finish Obtaing VSM Similarities");
			ProduceChangeLevelResults rank = new ProduceChangeLevelResults();
			rank.getFinalResults();
			System.out.println("Get Final Results");
		}
	}
	
	public static void loadConfigure(String filename) throws Exception {
		File file = new File(filename);
		settings = new HashMap<String,String>();
        if (!file.exists()) {
			System.out.println("Configuration File Not Found!");
			return;
		} 
		else {
			List<String> lines = FileToLines.fileToLines(filename);
			for (String line : lines) {
				if (!line.startsWith("#") && line.contains("=")) {
					settings.put(line.split("=")[0].trim(), line.split("=")[1].trim());
				}
			}
		}
		
		if (settings.containsKey("task"))
			task = settings.get("task");
		if (settings.containsKey("repoDir"))
			repoDir = settings.get("repoDir");
		if (settings.containsKey("workingLoc"))
			workingLoc = settings.get("workingLoc");
		if (settings.containsKey("bugReport"))
			bugReport = settings.get("bugReport");
		if (settings.containsKey("oracle"))
			changeOracle = settings.get("oracle");
		
		if (task.equals("") || repoDir.equals("") || workingLoc.equals("") || bugReport.equals("") || changeOracle.equals("")) {
			System.out.println("Missing Required Configuration");
			return;
		} else {
			startTask();
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		if (args.length > 0) {
			loadConfigure(args[0]);
		} else {
			System.out.println("Using default configuration file");
			loadConfigure("./config.txt");
		}
		
	}
}
