package miningChanges;


import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import generics.Bug;
import generics.Pair;
import utils.FileToLines;
import utils.ReadBugsFromXML;
import utils.WriteLinesToFile;

public class ProduceChangeLevelResults {
	public String loc = main.Main.settings.get("workingLoc");
	private HashMap<Integer,HashSet<String>> inducingRevisions;
	private HashMap<Integer,List<HashSet<String>>> potentialRevisions;
	private HashMap<String,Long> revisionTime;
	public List<Bug> bugs;
	HashMap<Integer, HashMap<String,Double>> hunkResults;
	
	public boolean loadOracles() {
		String filename = main.Main.changeOracle;
		
		File file = new File(filename);
		if (!file.exists()) {
			System.err.println("cound not find change level oracles");
			return false;
		}
		
		List<String> lines = FileToLines.fileToLines(filename);
		int index = 0;
		int depth = 3;
		
		bugs = ReadBugsFromXML.getFixedBugsFromXML(main.Main.settings.get("bugReport"));
		inducingRevisions = new HashMap<Integer,HashSet<String>>();
		potentialRevisions = new HashMap<Integer,List<HashSet<String>>>();
		while (index < lines.size()) {
//			System.out.println(lines.get(index));
			String[] splits = lines.get(index).split("\t");
			int bucketId = Integer.parseInt(splits[0].trim());
			String inducings = splits[1].substring(1, splits[1].length() - 1);
			
			splits = inducings.split(",");
			inducingRevisions.put(bucketId, new HashSet<String>());
			for (int i = 0; i < splits.length; i++)
				inducingRevisions.get(bucketId).add(splits[i].trim());
			index++;
			index++;
			
			potentialRevisions.put(bucketId, new ArrayList<HashSet<String>>());
			for (int i = 0; i <= depth; i++) {
				index++;
				potentialRevisions.get(bucketId).add(new HashSet<String>());
				String line = lines.get(index);
				line = line.substring(1, line.length() - 1);
				splits = line.split(",");
	//			System.out.println(bucketId + "\t" + splits.length);	
				for (int j = 0; j < splits.length; j++)
					potentialRevisions.get(bucketId).get(i).add(splits[j].trim());
			}
			index++;
		}
		
		return true;
	}
	
	public void loadResults() {
		String filename = loc + File.separator + "results.txt";
		List<String> lines = FileToLines.fileToLines(filename);
		hunkResults = new HashMap<Integer, HashMap<String,Double>>();
		System.out.println(filename);
		for (String line : lines) {
			String[] split = line.split("\t");
			int bid = Integer.parseInt(split[0]);
			hunkResults.put(bid, new HashMap<String,Double>());
			for (int i = 1; i < split.length; i++) {
				hunkResults.get(bid).put(split[i].split(":")[0], Double.parseDouble(split[i].split(":")[1]));
			}
		}
	}
	
	public void loadRevisionTime() throws ParseException {
		List<String> lines = FileToLines.fileToLines(main.Main.settings.get("workingLoc") + File.separator + "logOneline.txt");
		revisionTime = new HashMap<String, Long>();
		for (String line : lines) {
//			System.out.println(line);
			String[] splits = line.split("\t");
			String revisionNO = splits[0];
			Date date = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z").parse(splits[4]);
			revisionTime.put(revisionNO.substring(0,12), date.getTime());
		}
	}
	
	public void integrateResults() {
		List<List<Integer>> ranks = new ArrayList<List<Integer>>();
		List<String> resultsLines = new ArrayList<String>();
		for (Bug bug : bugs) {
			int bid = bug.id;
		
			HashSet<String> potentialChanges = potentialRevisions.get(bid).get(0);
			List<Pair<String, Long>> changeRanks = new ArrayList<Pair<String,Long>>();
			for (String change : potentialChanges) {
				if (revisionTime.containsKey(change))
					changeRanks.add(new Pair<String,Long>(change, revisionTime.get(change)));
				else changeRanks.add(new Pair<String, Long>(change, Long.MAX_VALUE));
			}
			Collections.sort(changeRanks);
			HashMap<String, Double> timeScore = new HashMap<String,Double>();
			for (int i = 0; i < changeRanks.size(); i++) {
				int index = changeRanks.size() - i - 1;
				timeScore.put(changeRanks.get(index).getKey(), 1.0 / (i + 1));
			}
				
			HashMap<String,Double> results = hunkResults.get(bid);
			
			for (String change : results.keySet()) {
//				System.out.println(change + "\t" + revisionTime.get(change));
				if (revisionTime.containsKey(change) && revisionTime.get(change) > bug.reportTime) {
					continue;
				}
				if (timeScore.containsKey(change))
					results.put(change, results.get(change) + 0.1 * timeScore.get(change));
			}
			
			List<Pair<String, Double>> finalRanks = new ArrayList<Pair<String,Double>>();
			for (String change : results.keySet()) {
				finalRanks.add(new Pair<String,Double>(change, results.get(change)));
			}
			
			Collections.sort(finalRanks);
			List<Integer> rank = new ArrayList<Integer>();
			
			for (int i = 0; i < finalRanks.size(); i++) {
				int index = finalRanks.size() - i - 1;
				if (inducingRevisions.get(bid).contains(finalRanks.get(index).getKey()))
					rank.add(i);
			}
			ranks.add(rank);
//			System.out.println(potentialRevisions.get(bid).get(0).size() + "\t" + rank.toString() + "\t" + finalRanks.toString());
		}
		
		int N = 10;
		double[] topN = EvaluationMetric.topN(ranks, N);
		double map = EvaluationMetric.MAP(ranks);
		double mrr = EvaluationMetric.MRR(ranks);
		resultsLines.add("map:\t" + map);
		resultsLines.add("mrr:\t" + mrr);
		
		System.out.println(map + "\t" + mrr);
		for (int i = 0; i < N; i++) {
			System.out.print(topN[i] + "\t");
			resultsLines.add("top@" + (i + 1) + "\t" + topN[i]);
		}
		System.out.println();
		String filename = main.Main.settings.get("workingLoc") + File.separator + "fileLevelResults.txt";
		WriteLinesToFile.writeLinesToFile(resultsLines, filename);
	}
	
	public void getFinalResults() throws ParseException {
		if (loadOracles()) {
			loadResults();
			loadRevisionTime();
			integrateResults();
		}
	}
}
