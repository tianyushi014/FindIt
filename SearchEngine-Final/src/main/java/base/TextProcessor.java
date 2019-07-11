package base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

public class TextProcessor {

	final List<String> stopWords = Arrays.asList(
			"a", "an", "and", "are", "as", "at", "be", "but", "by",
	        "for", "if", "in", "into", "is", "it",
	        "no", "not", "of", "on", "or", "such",
	        "that", "the", "their", "then", "there", "these",
	        "they", "this", "to", "was", "will", "with"
			);
	
	private boolean normalization;
	private boolean stopwordRemoval;
	private boolean stemming;
	
	public List<String> tokens;
	
	
	public TextProcessor() {
		this(true, true, true);
	}
	
	public TextProcessor(boolean normalization, boolean stopwordRemoval, boolean stemming) {
		this.normalization = normalization;		
		this.stopwordRemoval = stopwordRemoval;
		this.stemming = stemming;
		tokens = new ArrayList<String>();
	}
	
	
	public List<String> analyze(String input) {
		tokens.clear();
		input = removeSpecialCharacters(input);
		//System.out.println("After remove special characters: ["+input+"]");
		
		String[] words = input.split(" ");
		for (String word : words) {
			if (!word.equals("") && !word.contains(" ")) {
				tokens.add(word.toLowerCase());
			};
		}
		//print();
			
		if (stopwordRemoval) removeStopword();
		//print();
		
		if (stemming) stemming();
		//print();
		
		return tokens;
	}
	
	
	public List<String> analyze(List<String> words) {
		tokens.clear();
		//input = removeSpecialCharacters(input);
		//System.out.println("After remove special characters: ["+input+"]");
		
		//String[] words = input.split(" ");
		for (String word : words) {
			if (!word.equals("") && !word.contains(" ")) {
				tokens.add(word.toLowerCase());
			};
		}
		//print();
			
		if (stopwordRemoval) removeStopword();
		//print();
		
		if (stemming) stemming();
		//print();
		
		return tokens;
	}
	
	
	public List<String> analyze(List<String> words, boolean specialChar) {
		tokens.clear();
		//input = removeSpecialCharacters(input);
		//System.out.println("After remove special characters: ["+input+"]");
		
		//String[] words = input.split(" ");
		for (String word : words) {
			if (!word.equals("") && !word.contains(" ")) {
				if (specialChar)
					word = removeSpecialCharacters(word);
				tokens.add(word.toLowerCase());
			};
		}
		//print();
			
		if (stopwordRemoval) removeStopword();
		//print();
		
		if (stemming) stemming();
		//print();
		
		return tokens;
	}
	
	
	private String removeSpecialCharacters(String input) {
		if (normalization) {
			//Replace hyphens/underscores by whitespace
			input = input.replaceAll("[-_]+", "");
			//System.out.println("Normalize 1: ["+input+"]");
			
			//Replace all punctuation by whitespace except for period within words
			input = input.replaceAll("\\.\\p{Punct}", " ");
			//System.out.println("Normalize 2: ["+input+"]");			
			
			//Remove all special characters, keep only letter/digits/whitespace
			input = input.replaceAll("[^a-zA-Z0-9\\s]+", "");
			//System.out.println("Normalize 3: ["+input+"]");
		} else {
			//Replace all special characters except for period by whitespace
			input = input.replaceAll("[^a-zA-Z0-9-.]+", " ");
			//System.out.println("W/O Normalize 1: ["+input+"]");
			
			//Replace all punctuation, which follow\are followed by whitespace, by whitespace
			input = input.replaceAll("[^\\P{Punct}]+\\s", " ");
			input = input.replaceAll("\\s[^\\P{Punct}]+", " ");
			//System.out.println("W/O Normalize 2: ["+input+"]");
			
			//Replace period at end of the sentence
			input = input.replaceAll("\\.$", "");
			//System.out.println("W/O Normalize 3: ["+input+"]");
		}		
		return input;
	}
	
	
	private void removeStopword() {
		tokens.removeAll(stopWords);
	}
	
	
	private void stemming() {
		SnowballStemmer stemmer = (SnowballStemmer) new englishStemmer();		
		for (int i=0; i<tokens.size(); i++) {
			stemmer.setCurrent(tokens.get(i));
			stemmer.stem();
			tokens.set(i, stemmer.getCurrent());
		}
	}
	
	
	public void print() {
		for (String token : tokens) System.out.print("["+token+"]");
		System.out.print("\n");
	}
	
	
	/*
	public static void main(String[] args) {
		String test1 = "This- is-a-test. I like it. I hate @.234it. <If it is gonna work> I know_what U.S.A is!.";
		String test2 = "These testing sentences usually ran longer than I thought. I'm tired from running. ";
		String test3 = "Pre-processing. U.S.A country unhappy!";
		String test4 = "the";
		
		TextProcessor a = new TextProcessor(true, true, true);
		List<String> result1 = a.analyze(test1);
		List<String> result2 = a.analyze(test2);
		List<String> result3 = a.analyze(test3);
		List<String> result4 = a.analyze(test4);
	} */
}
