package miningChanges;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import utils.ReadBugsFromXML;
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
	public List<Integer> sourceIndex;
	public HashMap<String,Integer> cltIndex;
	public HashMap<String,Integer> sourceFileIndex;
	public List<String> sourceFiles;
	
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
		String bugDir = loc + "bugText";
		File file = new File(bugDir);
		File[] bugs = file.listFiles();
		for (File bug : bugs) {
			int bugId = Integer.parseInt(bug.getName().substring(0, bug.getName().indexOf(".")));
			List<String> lines = FileToLines.fileToLines(bug.getAbsolutePath());
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
	
	
	
}
