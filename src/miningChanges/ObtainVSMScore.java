package miningChanges;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import main.Main;
import utils.ReadBugsFromXML;
import utils.WriteLinesToFile;
import generics.Bug;
import utils.FileToLines;

public class ObtainVSMScore {
	public String loc = main.Main.settings.get("workingLoc");
	public HashMap<Integer, List<String>> bugTermList;
	public List<Bug> bugs;
	public HashMap<Integer,List<Integer>> bugCLTIndex;
	public HashMap<String,List<Integer>> logCLTs;
	public HashMap<String,List<Integer>> hunkCLTs;
	public HashMap<String,HashSet<Integer>> bugRelatedHunks;
	public List<List<String>> hunkTermList;
	public List<String> hunkIndex;	
	public HashMap<String,Integer> cltIndex;
	public HashMap<Integer, String> hunkChangeMap;
	public HashMap<Integer, String> hunkSourceMap;
	private static HashMap<Integer,HashSet<String>> potentialChanges;
	
	public void loadCLTIndex() {
		logCLTs = new HashMap<String,List<Integer>>();
		hunkCLTs = new HashMap<String,List<Integer>>();
		bugCLTIndex = new HashMap<Integer,List<Integer>>();
		
		String cltIndexName = loc + File.separator + "codeLikeTerms.txt";
		List<String> lines = FileToLines.fileToLines(cltIndexName);
		cltIndex = new HashMap<String,Integer>();
		for (String line : lines) {
			cltIndex.put(line.split("\t")[0], Integer.parseInt(line.split("\t")[1]));
		}
		String filename = loc + File.separator + "commitCLTIndex.txt";
		lines = FileToLines.fileToLines(filename);
		for (String line : lines) {
			String[] tmp = line.split("\t");
			logCLTs.put(tmp[0], new ArrayList<Integer>());
//			System.out.println(line);
			for (int i = 1; i < tmp.length; i++)
				logCLTs.get(tmp[0]).add(Integer.parseInt(tmp[i]));
		}
		
		filename = loc + File.separator + "hunkCLTIndex.txt";
		lines = FileToLines.fileToLines(filename);
		for (String line : lines) {
			String[] tmp = line.split("\t");
			hunkCLTs.put(tmp[0], new ArrayList<Integer>());
//			System.out.println(line);
			for (int i = 1; i < tmp.length; i++)
				hunkCLTs.get(tmp[0]).add(Integer.parseInt(tmp[i]));
		}
		
		filename = loc + File.separator + "bugCLTIndex.txt";
		lines = FileToLines.fileToLines(filename);
		for (String line : lines) {
			String[] tmp = line.split("\t");
			int bid = Integer.parseInt(tmp[0]);
			bugCLTIndex.put(bid, new ArrayList<Integer>());
			for (int i = 1; i < tmp.length; i++)
				bugCLTIndex.get(bid).add(Integer.parseInt(tmp[i]));
		}
	}

	
	public void loadBugFiles() {
		bugs = ReadBugsFromXML.getFixedBugsFromXML(main.Main.settings.get("bugReport"));
		bugTermList = new HashMap<Integer, List<String>>();
		String bugDir = loc + File.separator + "bugText";
	
		for (Bug bug : bugs) {
			int bugId = bug.id;
			List<String> lines = FileToLines.fileToLines(bugDir + File.separator + bugId + ".txt");
			bugTermList.put(bugId, lines);
		}

		String filename = loc + File.separator + "concernedCommits.txt";
		List<String> lines = FileToLines.fileToLines(filename);
		potentialChanges = new HashMap<>();
		for (String line : lines) {
			String[] splits = line.split("\t");
			int bid = Integer.parseInt(splits[0]);
			String[] changes = splits[1].substring(1, splits[1].length() - 1).split(",");
			potentialChanges.put(bid, new HashSet<>());
			for (String change : changes) {
				potentialChanges.get(bid).add(change.trim());
			}
		}
	}
	
	public void loadHunkFiles() {
		hunkTermList = new ArrayList<List<String>>();
		String hunkIndexName = loc + File.separator + "hunkIndex.txt";
		List<String> lines = FileToLines.fileToLines(hunkIndexName);
		hunkIndex = new ArrayList<String>();
		int index = 0;
		hunkChangeMap = new HashMap<>();
		for (String line : lines) {
			hunkIndex.add(line.split("\t")[0]);
			hunkChangeMap.put(index, line.split("\t")[0].split("@")[0]);
			index++;
		}
//		hunkIndex = FileToLines.fileToLines(hunkIndexName);
		hunkSourceMap = new HashMap<>();
		String filename = loc + File.separator + "sourceHunkLink.txt";
		lines = FileToLines.fileToLines(filename);
		for (String line : lines) {
			String[] split = line.split("\t");
			for (int i = 1; i < split.length; i++)
				hunkSourceMap.put(Integer.parseInt(split[i]), split[0]);
		}

		for (int i = 0; i < hunkIndex.size(); i++) {
			String line = hunkIndex.get(i);
			List<String> terms = new ArrayList<String>();
			filename = loc + File.separator + "hunkLog" + File.separator + line;
			terms.addAll(FileToLines.fileToLines(filename));
			filename = loc + File.separator + "hunkCode" + File.separator + line;
			terms.addAll(FileToLines.fileToLines(filename));
			hunkTermList.add(terms);
		}



	}
	
	public HashMap<Integer,Double> getVSMScoreNL(Bug bug, List<Integer> hunkId, boolean isChangeLevel) {
		HashMap<Integer,Double> results = new HashMap<Integer,Double>();
		HashSet<String> corpus = new HashSet<String>();
		int bid = bug.id;
		List<String> bugTerm = bugTermList.get(bid);

		corpus.addAll(bugTerm);
		HashSet<String> relatedEntities = new HashSet<String>();
		for (int id : hunkId) {
			List<String> hunkTerm = hunkTermList.get(id);
			corpus.addAll(hunkTerm);
			if (isChangeLevel)
				relatedEntities.add(hunkChangeMap.get(id));
			else 
				relatedEntities.add(hunkSourceMap.get(id));
		}

//		System.out.println(relatedEntities.toString());

		HashMap<String,Integer> corpusInverseIndex = new HashMap<String,Integer>();
		List<String> corpusIndex = new ArrayList<String>();
		List<HashMap<Integer,Integer>> hunkTermCount = new  ArrayList<HashMap<Integer,Integer>>();
		List<HashMap<Integer,Double>> hunkTermFreq = new ArrayList<HashMap<Integer,Double>>();
		HashMap<Integer,Double> termHunkFreq = new HashMap<Integer,Double>();
		HashMap<Integer, HashSet<String>> termEntityCount = new HashMap<Integer,HashSet<String>>();
		
		for (String term : corpus) {
			corpusInverseIndex.put(term, corpusIndex.size());
			corpusIndex.add(term);
		}
		
		HashMap<Integer,Integer> bugTermCount = new HashMap<Integer,Integer>();
		HashMap<Integer,Double> bugTermFreq = new HashMap<Integer,Double>();
		for (String term : bugTerm) {
			int index = corpusInverseIndex.get(term);
			if (!bugTermCount.containsKey(index)) bugTermCount.put(index, 1);
			else bugTermCount.put(index, bugTermCount.get(index) + 1);
		}
		
		for (int index : bugTermCount.keySet()) {
			bugTermFreq.put(index, Math.log(bugTermCount.get(index)) + 1);
		}
	
		for (int i = 0; i < hunkId.size(); i++) {
			int hid = hunkId.get(i);
			HashMap<Integer,Integer> tmp = new HashMap<Integer,Integer>();
			List<String> hunkTerm = hunkTermList.get(hid);
			for (String term : hunkTerm) {
				int index = corpusInverseIndex.get(term);
				if (!tmp.containsKey(index)) tmp.put(index, 1);
				else tmp.put(index, tmp.get(index) + 1);	
			}
			hunkTermCount.add(tmp);
			HashMap<Integer,Double> tmp1 = new HashMap<Integer,Double>();
			for (int index : tmp.keySet()) {
				tmp1.put(index, Math.log(tmp.get(index)) + 1);
				
				if (!termEntityCount.containsKey(index))
					termEntityCount.put(index, new HashSet<String>());
				if (isChangeLevel)
					termEntityCount.get(index).add(hunkChangeMap.get(hid));
				else 
					termEntityCount.get(index).add(hunkSourceMap.get(hid));
			}
			
			hunkTermFreq.add(tmp1);
		}
//		System.out.println(termEntityCount.size());
		for (int index : termEntityCount.keySet()) {
			termHunkFreq.put(index, Math.log(relatedEntities.size() * 1.0 / termEntityCount.get(index).size()));
//		    System.out.println(index + "\t" + termHunkFreq.get(index));
        }
		
		double bugNorm = 0;
		for (int k : bugTermFreq.keySet()) {
			if (termHunkFreq.containsKey(k))
				bugNorm += bugTermFreq.get(k) * bugTermFreq.get(k) * termHunkFreq.get(k) * termHunkFreq.get(k);
		}
		for (int index = 0; index < hunkId.size(); index++) {
			int hid = hunkId.get(index);
			HashMap<Integer,Double> termFreq = hunkTermFreq.get(index);
			double hunkNorm = 0;
			HashSet<Integer> intersect = new HashSet<Integer>();
			for (int k : termFreq.keySet()) {
				hunkNorm += termFreq.get(k) * termFreq.get(k) * termHunkFreq.get(k) * termHunkFreq.get(k);
			}
			intersect.addAll(termFreq.keySet());
			intersect.retainAll(bugTermFreq.keySet());
			double consine = 0;
			for (int k : intersect) {
				consine += bugTermFreq.get(k) * termFreq.get(k) * termHunkFreq.get(k) * termHunkFreq.get(k);
				
			}
//            System.out.println(consine + "\t" + bugNorm + "\t" + hunkNorm);
			double similarity = consine / (Math.sqrt(bugNorm) * Math.sqrt(hunkNorm));
//			similarity *= hunkLength.get(index);
			results.put(hid, similarity);
		}
		return results;
		
	}
	
	private HashMap<Integer,Integer> getIndexCount(List<Integer> list) {
		HashMap<Integer,Integer> count = new HashMap<Integer,Integer>();
		for (int clt : list) {
			if (!count.containsKey(clt))
				count.put(clt, 0);
			count.put(clt, count.get(clt) + 1);
		}
		return count;
	}
	
	public HashMap<Integer,Double> getVSMScoreCLT(Bug bug, List<Integer> hunkId, boolean isChangeLevel) {
		HashMap<Integer,Double> results = new HashMap<Integer,Double>();
		int bugId = bug.id;
		List<Integer> bugIndex = bugCLTIndex.get(bugId);
		HashMap<Integer,Integer> bugCLTCount = getIndexCount(bugIndex);
		HashMap<Integer,Double> bugCLTFreq = new HashMap<Integer,Double>();
		for (int clt : bugCLTCount.keySet()) {
			bugCLTFreq.put(clt, Math.log(bugCLTCount.get(clt)) + 1);
		}
		
		HashSet<String> relatedEntities = new HashSet<String>();
		
		/**
		 * Calculate the CLT frequencies of hunks
		 */
		HashMap<Integer,HashMap<Integer,Double>> hunkCLTFreq = new HashMap<Integer,HashMap<Integer,Double>>();
		for (int hunk : hunkId) {
			if (!hunkCLTFreq.containsKey(hunk)) {
				List<Integer> clts = new ArrayList<Integer>();
				
				String[] tmp = hunkIndex.get(hunk).split("@");
				if (logCLTs.containsKey(tmp[0])) 
					clts.addAll(logCLTs.get(tmp[0]));
				
//				for (int term : fileTerm) {
//					if (term != 411) clts.add(term);
//				}
				if (hunkCLTs.containsKey(hunk + ":f")) {
					List<Integer> fileTerm = hunkCLTs.get(hunk + ":f");
					clts.addAll(fileTerm);
					clts.addAll(hunkCLTs.get(hunk + ":-1"));
					clts.addAll(hunkCLTs.get(hunk + ":0"));
					clts.addAll(hunkCLTs.get(hunk + ":1"));
				}
				HashMap<Integer,Integer> cltCount = getIndexCount(clts);
				HashMap<Integer,Double> cltFreq = new HashMap<Integer,Double>();
				for (int clt : cltCount.keySet()) {
					cltFreq.put(clt, Math.log(cltCount.get(clt)) + 1);
				}
				hunkCLTFreq.put(hunk, cltFreq);
				
				if (isChangeLevel)
					relatedEntities.add(hunkChangeMap.get(hunk));
				else 
					relatedEntities.add(hunkSourceMap.get(hunk));
				
			}
		}
//		System.out.println(relatedEntities.toString());
//		int[] cltCount = new int[cltIndex.size()];
		HashMap<Integer, HashSet<String>> termEntityCount = new HashMap<Integer,HashSet<String>>();
		for (int hunk : hunkId) {
			HashMap<Integer,Double> cltFreq = hunkCLTFreq.get(hunk);
			for (int index : cltFreq.keySet()) {
				if (!termEntityCount.containsKey(index))
					termEntityCount.put(index, new HashSet<String>());

				if (isChangeLevel)
					termEntityCount.get(index).add(hunkChangeMap.get(hunk));
				else 
					termEntityCount.get(index).add(hunkSourceMap.get(hunk));
			}
		}
		
		double[] inverseFreq = new double[cltIndex.size()];
		for (int i = 0; i < cltIndex.size(); i++) {

			if (termEntityCount.containsKey(i)) {
				inverseFreq[i] = (termEntityCount.get(i).size() == 0) ? 0 : Math.log(relatedEntities.size() * 1.0 / termEntityCount.get(i).size());
//				System.out.println("contains:" + i + "\t" + termEntityCount.get(i));
			} else inverseFreq[i] = 0;
		}
		double bugNorm = 0;
		for (int index : bugCLTFreq.keySet()) {
			bugNorm += bugCLTFreq.get(index) * bugCLTFreq.get(index) * inverseFreq[index] * inverseFreq[index];
		}	
		for (int hunk : hunkId) {
			HashMap<Integer,Double> cltFreq = hunkCLTFreq.get(hunk);
			double hunkNorm = 0;
			for (int index : cltFreq.keySet()) {
				hunkNorm += cltFreq.get(index) * cltFreq.get(index) * inverseFreq[index] * inverseFreq[index];
			}
			HashSet<Integer> intersect = new HashSet<Integer>();
			intersect.addAll(cltFreq.keySet());
			intersect.retainAll(bugCLTFreq.keySet());
//			System.out.println(cltFreq.toString());
//			System.out.println(bugCLTFreq.toString());
			double cosine = 0;
			for (int index : intersect) {
				cosine += bugCLTFreq.get(index) * cltFreq.get(index) * inverseFreq[index] * inverseFreq[index];
			}
			double similarity = 0;
			if (bugNorm > 0 && hunkNorm > 0) 
				similarity = cosine / (Math.sqrt(bugNorm) * Math.sqrt(hunkNorm));
			results.put(hunk, similarity);
			
		}		
		return results;
	}
	
	public HashMap<Integer, HashMap<String, Double>> getResults(boolean isChangeLevel) {
		List<String> linesNL = new ArrayList<String>();
		List<String> linesCLT = new ArrayList<String>();
		List<String> combineResults = new ArrayList<String>();
		HashMap<Integer, HashMap<String, Double>> bugChangeResults = new HashMap<Integer, HashMap<String,Double>>();
		String resultNLFile = loc + File.separator + "resultsNL_" + (isChangeLevel ? "change":"file") + ".txt";
		String resultCLTFile = loc + File.separator + "resultsCLT_" + (isChangeLevel ? "change":"file") + ".txt";
		String resultFile = loc + File.separator + "results_" + (isChangeLevel ? "change":"file") + ".txt";
		for (Bug bug : bugs) {
			int bid = bug.id;
			System.out.println("processing bug:" + bid);
			List<Integer> hunks = new ArrayList<Integer>();
			for (int i = 0; i < hunkIndex.size(); i++) {

				if (potentialChanges.get(bid).contains(hunkChangeMap.get(i)))
					hunks.add(i);
			}
			
			List<Integer> NLHunksList = new ArrayList<Integer>(hunks);
			List<Integer> CLTHunksList = new ArrayList<Integer>(hunks);
			HashMap<Integer,Double> resultNL = getVSMScoreNL(bug,NLHunksList,isChangeLevel);
			HashMap<Integer,Double> resultCLT = getVSMScoreCLT(bug,CLTHunksList,isChangeLevel);
			HashMap<String, Double> result = new HashMap<String,Double>();
			String line = "" + bug.id;
			for (int sid : resultNL.keySet()) {
				line += "\t" + sid + ":" + resultNL.get(sid);
			}
			linesNL.add(line);
			line = "" + bug.id;
			for (int sid : resultCLT.keySet()) {
				line += "\t" + sid + ":" + resultCLT.get(sid);
			}
			linesCLT.add(line);
			
			double bugCLTWeight = 5 * bugCLTIndex.get(bid).size() * 1.0 / bugTermList.get(bid).size();
			if (bugCLTWeight > 1) bugCLTWeight = 1;
			for (int hid : resultNL.keySet()) {
				double value = resultNL.get(hid) + bugCLTWeight * resultCLT.get(hid);
				String entity = "";
				if (isChangeLevel)
					entity = hunkChangeMap.get(hid);
				else entity = hunkSourceMap.get(hid);
				if (!result.containsKey(entity))
					result.put(entity, value);
				else if (result.get(entity) < value)
					result.put(entity, value);
			}
			
			line = "" + bug.id;
			for (String change : result.keySet()) {
				line += "\t" + change + ":" + result.get(change);
			}
			combineResults.add(line);
			bugChangeResults.put(bid, result);
			
		}
		WriteLinesToFile.writeLinesToFile(linesCLT, resultCLTFile);
		WriteLinesToFile.writeLinesToFile(linesNL, resultNLFile);
		WriteLinesToFile.writeLinesToFile(combineResults, resultFile);
		return bugChangeResults;
	}
	
	public HashMap<Integer, HashMap<String, Double>> obtainSimilarity(boolean isChangeLevel) {
		loadBugFiles();
		loadCLTIndex();
		loadHunkFiles();
		return getResults(isChangeLevel);
	}
	
}
