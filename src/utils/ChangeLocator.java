package utils;

import java.util.HashMap;
import java.util.List;

public class ChangeLocator {
	public static HashMap<String,String> shortChangeMap = null;
	
	public static HashMap<String,String> getShortChangeMap() {
		if (shortChangeMap == null) {
			shortChangeMap = readShortChangeMap();
		} 
		
		return shortChangeMap;
	}
	
	public static HashMap<String,String> readShortChangeMap() {
		HashMap<String,String> changeMap = new HashMap<String,String>();
		List<String> lines = FileToLines.fileToLines("/home1/shared-resources/changelocator/subject/AnalyzeDataset/dataset/revision.log");
		for (String line : lines) {
			String[] split = line.split("\t");
			changeMap.put(split[0].substring(0, 12), split[0]);
		}
		return changeMap;
	}
}
