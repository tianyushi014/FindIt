package query;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import base.TextProcessor;
import util.Term;
import util.Utility;


/**
 * Process:
 * 1. Read weighted index JSON file
 * 2. Handle query, extract the last token
 * 3. Find all next tokens of input token
 * 4. Output at most 10 suggested tokens based on ranking of probability P(suggestion|input)
 * */
public class QueryCompleter {

	private Utility util;	
	
	private static String selection;
	private static String outPath;
	
	private static String inFile;
	
	private QueryProcessor processor;
	private TextProcessor analyzer;
	
	private Map<String, Term> termMap;
	private Map<String, Integer> nextTokensMap;
	
	private List<String> allKeyTokens;
	private List<String> allNextTokens;
	
	//final suggestions (max 10)
	private String[] results;
	private final int size = 10;
	
	private final List<String> removedElements = Arrays.asList("AND", "OR", "NOT", "\\(", "\\)");	
	
	
	public QueryCompleter(String collection) {
		util = new Utility();
		analyzer = new TextProcessor(util.normalization, util.stopwordRemoval, util.stemming);
		
		termMap = new HashMap<String, Term>();
		nextTokensMap = new HashMap<String, Integer>();
		
		allKeyTokens = new ArrayList<String>();		
		allNextTokens = new ArrayList<String>();
		results = new String[size];
		
		selection = collection;
		outPath = util.outPath+selection;	
		inFile = outPath+util.outWeightedIndex;
		
		processor = new QueryProcessor(selection);
		
		try {
			readJsonStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public QueryCompleter(String collection, QueryProcessor processor) {
		util = new Utility();		
		
		this.processor = processor;
		analyzer = new TextProcessor(util.normalization, util.stopwordRemoval, util.stemming);
		
		termMap = new HashMap<String, Term>();
		nextTokensMap = new HashMap<String, Integer>();
		
		allKeyTokens = new ArrayList<String>();		
		allNextTokens = new ArrayList<String>();
		
		selection = collection;
		outPath = util.outPath+selection;	
		inFile = outPath+util.outWeightedIndex;
		
		try {
			readJsonStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public QueryCompleter(Map<String, Term> termMap, QueryProcessor processor) {
		util = new Utility();		
		
		this.processor = processor;
		analyzer = new TextProcessor(util.normalization, util.stopwordRemoval, util.stemming);
		
		this.termMap = termMap;
		nextTokensMap = new HashMap<String, Integer>();
		
		allKeyTokens = new ArrayList<String>();		
		allNextTokens = new ArrayList<String>();
	}
	
	
	/**
	 * Read JSON file
	 * @throws IOException 
	 * */
	private void readJsonStream() throws IOException {
		termMap.clear();
		
		//Read JSON file in stream mode
		Gson gson = new GsonBuilder().create();

		InputStream stream = new FileInputStream(inFile);
		JsonReader reader = new JsonReader(new InputStreamReader(stream, "UTF-8"));
        
        reader.beginArray();
        while (reader.hasNext()) {
            Term term = gson.fromJson(reader, Term.class);
            termMap.put(term.getToken(), term);
        }
        reader.close();
        
        System.out.println("QueryCompleter.readJsonStream() [WeightedIndexMap] Output-Size: "+termMap.size()+"\n");
	}
	
	
	/**
	 * Suggest next token 
	 * */
	public String[] suggest(String inputQuery) {
		nextTokensMap.clear();
		allNextTokens.clear();
		
		//Text-process query since documents are also processed
		allKeyTokens = handleQuery(inputQuery);
		
		//Find all nextTokens and build the token-freq map
		findNextTokens();
			
		//Sort by freqs
		sortResults();
		
		//Choose at most 5 as suggestions
		if (allNextTokens.size()>size) {
			results = new String[size];
			
			for (int i=0; i<size; i++)
				results[i] = allNextTokens.get(i);
		} else {
			results = new String[allNextTokens.size()];
			
			for (int i=0; i<allNextTokens.size(); i++)
				results[i] = allNextTokens.get(i);
		}
		
		System.out.println("QueryCompleter.suggest() Input: "+inputQuery);
		System.out.println("QueryCompleter.suggest() All Results: "+allNextTokens);
		System.out.print("QueryCompleter.suggest() Output: ");
		for (String str : results)
			System.out.print("["+str+"]");
		System.out.println("\n");
		
		return results;
	}
	
	
	/**
	 * Get the last word, do query processing and handle wildcard (if any) in this word
	 * Examples:
	 * 		1. (crypto* OR security) => [security]
	 * 		2. (comput* AND graph*) => [graph, graphs, graphics, graphical, graphtheoretical]
	 * */
	private List<String> handleQuery(String query) {
		System.out.println("QueryCompleter.handleQuery() Input: "+query);
		
		List<String> tokens = new ArrayList<String>();
		
		for (String element : removedElements) {
			query = query.replaceAll(element, "");
		}
		//System.out.println("Query ["+query+"]");
		
		List<String> words = Arrays.asList(query.split(" "));
		String word = words.get(words.size()-1);
		//System.out.println("Word ["+word+"]");
		
		List<String> tmp = processor.booleanQuery(word);
		//System.out.println("tmp before: "+tmp);
		
		tmp = analyzer.analyze(tmp);
		//System.out.println("tmp after: "+tmp);
		
		for (String s : tmp) {
			if (!removedElements.contains(s)) {
				if (!tokens.contains(s))
					tokens.add(s);
			}
		}
		
		System.out.println("QueryCompleter.handleQuery() Output: "+tokens+"\n");
		return tokens;
	}
	
	
	/**
	 * Find all nextTokens
	 * */
	private void findNextTokens() {
		//Check bigram model for each token
		for (String token : allKeyTokens) {
			Map<String, Integer> nextTokens = termMap.get(token).getNextTokens();
			//System.out.println("["+token+"] : "+nextTokens);
			
			for (Entry<String, Integer> entry : nextTokens.entrySet()) {
				String key = entry.getKey();
				int value = entry.getValue();
				
				if (nextTokensMap.containsKey(key)) {
					int freq = nextTokensMap.get(key)+value;
					nextTokensMap.replace(key, freq);
				} else 
					nextTokensMap.put(key, value);
			}
		}
		
		System.out.println("QueryCompleter.findNextTokens() Output: "+nextTokensMap+"\n");
	}
	
	
	/**
	 * Sort tokens by descending freqs
	 * */
	private void sortResults() {
		List<Entry<String, Integer> > list = new LinkedList<Entry<String, Integer>>(nextTokensMap.entrySet());
        
        //Sort list (results are in ascending order)
        Collections.sort(list, new Comparator<Entry<String, Integer>>() { 
            public int compare(Entry<String, Integer> o1, Map.Entry<String, Integer> o2) { 
                return (o1.getValue()).compareTo(o2.getValue()); 
            } 
        }); 
        
        //Reverse map
        Map<String, Integer> sortedTokens = new LinkedHashMap<String, Integer>();
        List<String> sortedResults = new ArrayList<String>();
        
        for (int i=list.size(); i>0; i--) {
        	Entry<String, Integer> tmp = list.get(i-1);
        	sortedTokens.put(tmp.getKey(), tmp.getValue());
        	sortedResults.add(tmp.getKey());
        }
        
        nextTokensMap = sortedTokens;
        allNextTokens = sortedResults;
	}
	
	
	/*
	public static void main(String[] args) {
		Utility util = new Utility();
		
		String test1 = "(operating AND system)";
		String test2 = "(comput* AND graph*)";
		String test3 = "(crypto* OR security)";
		
		String test4 = "(function AND logic) OR principle";
		String test5 = "(NOT function) AND (logic OR principle)";
		
		String test7 = "(farm* AND cocoa)";
		
		long start = System.nanoTime();
		
		QueryCompleter processor1 = new QueryCompleter(util.COURSES);
		processor1.suggest(test2);
		
		//QueryProcessor processor2 = new QueryProcessor(util.REUTERS);
		//processor2.booleanQuery(test7);
		
		System.out.println("QueryCompleter - Elapsed Time(ms): "+(System.nanoTime()-start)/1000000);

	} */

}
