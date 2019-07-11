package query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import util.BigramTerm;


/**
 * Process:
 * 1. Read bigram index file from given directory path
 * 2. Generate bigrams of wildcard query
 * 3. Search words that match the AND version of the wildcard query
 * 4. Filter search results
 * */
public class WildcardHandler {

	private Map<String, BigramTerm> bigramMap;	
	private List<String> results;
	
	
	public WildcardHandler(Map<String, BigramTerm> index) {
		bigramMap = index;
		results = new ArrayList<String>();
	} 
	
	
	public List<String> getResults() {
		return results;
	}	
	
	
	/**
	 * Handle the wildcard query and find words that match the query's AND version.
	 * 
	 * @param a single word which contains only one wildcard
	 * @return a list of string contains all operands in the query's AND version
	 * */
	public List<String> handle(String query) {
		results.clear();
		
		//Generate bigrams of wildcard query
		String[] queryBigrams = generateBigrams(query);
		
		//Search words that match the AND version of the wildcard query
		List<String> words = searchWords(queryBigrams);	
		results = words;
		
		//Filter results by wildcard query
		if (words.size()>0) {
			List<String> toBeRemoved = new ArrayList<String>();
			
			String queryBefore = "";
			String queryAfter = "";
					
			for (String word : words) {
				int pos = query.indexOf("*");
				if (pos==0) {
					queryAfter = query.substring(1);
				} else {
					queryBefore = query.substring(0, pos);
					if (pos!=query.length()-1) 
						queryAfter = query.substring(pos+1);
				}
				
				String wordBefore = word.substring(0, queryBefore.length());
				String wordAfter = word.substring(word.length()-queryAfter.length(), word.length());
				
				if (!wordBefore.equals(queryBefore) || !wordAfter.equals(queryAfter)) {
					//results.remove(word);
					toBeRemoved.add(word);
				}	
			}
			
			results.removeAll(toBeRemoved);
		}
		
		//System.out.println("WildcardHandler.handle() Input: "+query);
		System.out.println("WildcardHandler.handle() Output: "+results);
		return results;
	}
	
	
	/**
	 * Generate bigrams for a word.
	 * */
	private String[] generateBigrams(String word) {
		//Count of two letters grams (ab, bc)  -> str.length()-2+1
		//Count of single letter gram ($a, c$ )-> 2
		int numOfGrams = (word.length()-2+1)+2;		
		String[] grams = new String[numOfGrams];

		grams[0] = "$"+word.charAt(0);
		for (int i=0; i<numOfGrams-2; i++) {
			grams[i+1] = word.substring(i, i+2);
		}
		grams[numOfGrams-1] = word.substring(numOfGrams-2)+"$";
		
		return grams;
	}
	
	
	/**
	 * Use a bigram-words index to search common words among a list of bigrams
	 * 
	 * @param an array of bigrams
	 * @return a list of words after AND operations between associated words
	 * */
	private List<String> searchWords(String[] queryBigrams) {
		System.out.print("WildcardHandler.searchWords() Input: ");
		for (String bigram : queryBigrams) 
			System.out.print("["+bigram+"] ");
		System.out.println();
			
		List<String> results = new ArrayList<String>();
		
		//Check if the first element of the query is *
		if (queryBigrams[0].contains("*")) {
			BigramTerm term = bigramMap.get(queryBigrams[2]);
			List<String> words = term.getWords();
			results.addAll(words);

			//System.out.println("WildcardHandler.searchWords() Initial-Size ["+term.getToken()+"]: "+results.size());
		} else {
			BigramTerm term = bigramMap.get(queryBigrams[0]);
			List<String> words = term.getWords();
			results.addAll(words);
			
			//System.out.println("WildcardHandler.searchWords() Initial-Size ["+term.getToken()+"]: "+results.size());
		}		
		//System.out.println("WildcardHandler.searchWords() Initial: "+results);
		
		//Perform AND operations
		for (int i=1; i<queryBigrams.length; i++) {
			if (!queryBigrams[i].contains("*")) {
				List<String> words = bigramMap.get(queryBigrams[i]).getWords();
				//System.out.println("WildcardHandler.searchWords() Iterate-Size ["+queryBigrams[i]+"]: "+words.size());		
				results.retainAll(words);
			}
		}
		
		System.out.println("WildcardHandler.searchWords() Output: "+results);
		return results;
	}
	
	
	
	
	/*
	public static void main(String[] args) throws IOException {
		Utility util = new Utility();
		
		String test1 = "comput*";
		String test2 = "graph*";
		String test3 = "crypto*";
		String test4 = "se*ty";
		
		String test5 = "*stem";
		String test6 = "syst*";
		String test7 = "sy*em";
		String test8 = "*sis";
		
		String query = test5;
		String word = "system";
		
		//WildcardHandler handler = new WildcardHandler(util.COURSES);
		//List<String> res = handler.handle(test8);
		//System.out.println(res);
	} */
}
