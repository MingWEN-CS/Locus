package miningChanges;

import java.util.ArrayList;
import java.util.List;

import utils.Splitter;
import utils.Stem;
import utils.Stopword;

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
}
