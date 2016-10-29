package miningChanges;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import utils.ReadBugsFromXML;
import utils.WriteLinesToFile;
import generics.Bug;
import utils.FileToLines;

public class ObtainVSMScore {
	public String loc = main.Main.settings.get("workingLoc");
	public HashMap<Integer, List<String>> bugTermList;
	public List<Bug> bugs;
	public HashMap<Integer,Integer> hunkSourceMap;
	public HashMap<Integer,List<Integer>> bugCLTIndex;
	public HashMap<String,List<Integer>> logCLTs;
	public HashMap<String,List<Integer>> hunkCLTs;
	public HashMap<String,HashSet<Integer>> bugRelatedHunks;
	public List<List<String>> hunkTermList;
	public List<String> hunkIndex;	
	public HashMap<String,Integer> cltIndex;
	public HashMap<Integer, String> hunkChangeMap;
	private static HashMap<Integer,HashSet<String>> inducingRevisions;
	private static HashMap<Integer,List<HashSet<String>>> potentialRevisions;
	
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
	
	public void loadOracles() {
		String filename = main.Main.changeOracle;
		List<String> lines = FileToLines.fileToLines(filename);
		int index = 0;
		int depth = 3;
		
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
		
		hunkChangeMap = new HashMap<Integer,String>();
		filename = loc + File.separator + "hunkIndex.txt";
		lines = FileToLines.fileToLines(filename);
		for (int i = 0; i < lines.size(); i++) {
			String[] split = lines.get(i).split("@");
			hunkChangeMap.put(i, split[0]);
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
	}
	
	public void loadHunkFiles() {
		hunkTermList = new ArrayList<List<String>>();
		String hunkIndexName = loc + File.separator + "hunkIndex.txt";
		hunkIndex = FileToLines.fileToLines(hunkIndexName);
		for (int i = 0; i < hunkIndex.size(); i++) {
			String line = hunkIndex.get(i);
			List<String> terms = new ArrayList<String>();
			String filename = loc + File.separator + "hunkLog" + File.separator + line;
			terms.addAll(FileToLines.fileToLines(filename));
			filename = loc + File.separator + "hunkCode" + File.separator + line;
			terms.addAll(FileToLines.fileToLines(filename));
			hunkTermList.add(terms);
		}
	}
	
	public HashMap<Integer,Double> getVSMScoreNL(Bug bug, List<Integer> hunkId) {
		HashMap<Integer,Double> results = new HashMap<Integer,Double>();
		HashSet<String> corpus = new HashSet<String>();
		int bid = bug.id;
		List<String> bugTerm = bugTermList.get(bid);
		corpus.addAll(bugTerm);
		for (int id : hunkId) {
			List<String> hunkTerm = hunkTermList.get(id);
			corpus.addAll(hunkTerm);
		}
		
		HashMap<String,Integer> corpusInverseIndex = new HashMap<String,Integer>();
		List<String> corpusIndex = new ArrayList<String>();
		List<HashMap<Integer,Integer>> hunkTermCount = new  ArrayList<HashMap<Integer,Integer>>();
		List<HashMap<Integer,Double>> hunkTermFreq = new ArrayList<HashMap<Integer,Double>>();
		HashMap<Integer, HashSet<Integer>> termHunkAppears = new HashMap<Integer,HashSet<Integer>>();
		HashMap<Integer,Double> termHunkFreq = new HashMap<Integer,Double>();
		
		
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
				if (!termHunkAppears.containsKey(index)) 
					termHunkAppears.put(index, new HashSet<Integer>());
				termHunkAppears.get(index).add(hunkSourceMap.get(index));
//				if (!termHunkCount.containsKey(index)) termHunkCount.put(index, 1);
//				else termHunkCount.put(index, termHunkCount.get(index) + 1);
			}
			hunkTermFreq.add(tmp1);
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
	
	public HashMap<Integer,Double> getVSMScoreCLT(Bug bug, List<Integer> hunkId) {
		HashMap<Integer,Double> results = new HashMap<Integer,Double>();
		int bugId = bug.id;
		List<Integer> bugIndex = bugCLTIndex.get(bugId);
		HashMap<Integer,Integer> bugCLTCount = getIndexCount(bugIndex);
		HashMap<Integer,Double> bugCLTFreq = new HashMap<Integer,Double>();
		for (int clt : bugCLTCount.keySet()) {
			bugCLTFreq.put(clt, Math.log(bugCLTCount.get(clt)) + 1);
		}
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
			}
		}
		
		int[] cltCount = new int[cltIndex.size()];
		for (int hunk : hunkId) {
			HashMap<Integer,Double> cltFreq = hunkCLTFreq.get(hunk);
			for (int index : cltFreq.keySet()) {
				cltCount[index]++;
			}
		}
		double[] inverseFreq = new double[cltIndex.size()];
		for (int i = 0; i < cltIndex.size(); i++)
			inverseFreq[i] = (cltCount[i] == 0) ? 0 : Math.log(hunkId.size() * 1.0 / cltCount[i]);
		
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
	
	public void localization() {
		List<String> linesNL = new ArrayList<String>();
		List<String> linesCLT = new ArrayList<String>();
		List<String> combineResults = new ArrayList<String>();
		
		String resultNLFile = loc + "resultsNL" + ".txt";
		String resultCLTFile = loc + "resultsCLT" + ".txt";
		String resultFile = loc + "results" + ".txt";
		for (Bug bug : bugs) {
			int bid = bug.id;
			System.out.print(bid);
			List<Integer> hunks = new ArrayList<Integer>();
			for (int i = 0; i < hunkIndex.size(); i++) {
				if (potentialRevisions.get(bid).get(0).contains(hunkChangeMap.get(i)))
					hunks.add(i);
			}
			
			System.out.println(hunks.toString());
			List<Integer> NLHunksList = new ArrayList<Integer>(hunks);
			List<Integer> CLTHunksList = new ArrayList<Integer>(hunks);
			HashMap<Integer,Double> resultNL = getVSMScoreNL(bug,NLHunksList);
			HashMap<Integer,Double> resultCLT = getVSMScoreCLT(bug,CLTHunksList);
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
				String change = hunkChangeMap.get(hid);
				if (!result.containsKey(change))
					result.put(change, value);
				else if (result.get(change) < value)
					result.put(change, value);
			}
			
			line = "" + bug.id;
			for (String change : result.keySet()) {
				line += "\t" + change + ":" + result.get(change);
			}
			
			combineResults.add(line);
			
		}
		WriteLinesToFile.writeLinesToFile(linesCLT, resultCLTFile);
		WriteLinesToFile.writeLinesToFile(linesNL, resultNLFile);
		WriteLinesToFile.writeLinesToFile(combineResults, resultFile);
	}
	
	public void obtainSimilarity() {
		loadBugFiles();
		loadCLTIndex();
		loadHunkFiles();
		loadOracles();
		localization();
	}
	
}
