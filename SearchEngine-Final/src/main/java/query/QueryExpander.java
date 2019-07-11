package query;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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

import util.Synonym;
import util.Utility;


/**
 * (for VSM only)
 * Expansion: implicit
 * Combination of scores: sum of similarities
 * Results limitation: query is expanded with 2 additional tokens
 * 
 * Process:
 * 1. Read thesaurus JSON file from given directory path
 * 2. Find synonyms of given tokens
 * 3. Rank similarities and select the most similar 2
 * 5. Output reformulated query
 * */
public class QueryExpander {

	private Utility util;
	
	private static String selection;
	private static String outPath;
	private static String inFile;
	
	//Map: key=word, value=SimilarityPair
	private Map<String, List<Synonym>> thesaurusMap;
	
	//Map: key=synonym, value=sum of similarities
	private Map<String, Double> sumOfSim;
	private List<String> resultTokens;
	
	//Map: key=synonym, value=sum of similarities
	private Map<String, Double> resultsMap;
	private List<String> results;
		
	private final int size = 2;
	
	
	public QueryExpander(String collection, Map<String, List<Synonym>> thesaurusMap) {
		util = new Utility();
		
		sumOfSim = new HashMap<String, Double>();
		resultTokens = new ArrayList<String>();
		
		resultsMap = new HashMap<String, Double>();
		results = new ArrayList<String>();
		
		this.thesaurusMap = thesaurusMap;
	}
	
	
	public QueryExpander(String collection) {
		util = new Utility();
		
		sumOfSim = new HashMap<String, Double>();
		resultTokens = new ArrayList<String>();
		
		resultsMap = new HashMap<String, Double>();
		results = new ArrayList<String>();
		
		thesaurusMap = new HashMap<String, List<Synonym>>();
		
		selection = collection;
		outPath = util.outPath+selection;
		inFile = outPath+util.outThesaurus;
		
	}

	
	/**
	 * Read JSON file
	 * @throws IOException 
	 * */
	private void readJsonStream() throws IOException {
		//System.out.println("ThesaurusIndex.readJsonStream() Start");
		thesaurusMap.clear();
		
		Gson gson = new GsonBuilder().create();

		InputStream stream = new FileInputStream(inFile);
		JsonReader reader = new JsonReader(new InputStreamReader(stream, "UTF-8"));       
        
		reader.beginArray();
        while (reader.hasNext()) {
            Synonym synonym = gson.fromJson(reader, Synonym.class);
            
            /**/
            for (int i=0; i<2; i++) {
            	List<Synonym> value = new ArrayList<Synonym>();
        		
            	//Update map by key token
            	if (thesaurusMap.containsKey(synonym.getPair()[i])) {
            		value = thesaurusMap.get(synonym.getPair()[i]);
            		value.add(synonym);
            	} else {
            		value.add(synonym);
            	}  
            	
            	thesaurusMap.put(synonym.getPair()[i], value);
            }
        }
        reader.close();
        
        System.out.println("QueryExpander.readJsonStream() [Thesaurus] Output-Size: "+thesaurusMap.size()+"\n");
	}
	
	
	public String getInFile() {
		return inFile;
	}
	
	
	
	/**
	 * Find synonyms in the form of Lists of String (which contains the two token) and Double (the similarity)
	 * */
	private List<Synonym> iterateThesaurus(List<String> targets) throws IOException {
		//System.out.println("ThesaurusIndex.iterateThesaurus() Start");
		
		//thesaurusMap.clear();
		List<Synonym> syns = new ArrayList<Synonym>();
		
		Gson gson = new GsonBuilder().create();

		InputStream stream = new FileInputStream(inFile);
		JsonReader reader = new JsonReader(new InputStreamReader(stream, "UTF-8"));       
		
		reader.beginArray();
		while (reader.hasNext()) {
			Synonym synonym = gson.fromJson(reader, Synonym.class);
            
    		if (targets.contains(synonym.getPair()[0]) ||
    				targets.contains(synonym.getPair()[1])) {
    			syns.add(synonym);
			}
        }
        reader.endArray();
        reader.close();
        
        System.out.println("QueryExpander.iterateThesaurus() [Thesaurus] Output-Size: "+syns.size()+"\n");
	
        return syns;
	}
	
	
	public List<String> getSynonyms(List<String> tokens) throws IOException {
		readJsonStream();
		
		sumOfSim.clear();
		resultTokens.clear();
		
		resultsMap.clear();
		results.clear();
		
		//Find all synonyms
		for (String token : tokens) {
			resultsMap.put(token, (double) 1);
			findSynonymsOfToken(token);
		}
		
		//Rank results
		if (sumOfSim.size()>1)
			sortResults();
		
		//for the final results:
		for (int i=0; i<size; i++) {
			String token = resultTokens.get(i);
			
			if (!tokens.contains(token)) {
				resultsMap.put(token, sumOfSim.get(token));
			}
		}
		
		results.addAll(resultsMap.keySet());
				
		System.out.println("QueryExpander.expand() Output: "+resultsMap);
		System.out.println("QueryExpander.expand() Output: "+results+"\n");
		
		return results;
	}
	
	
	
	
	/**
	 * Expand query
	 * @throws IOException 
	 * */
	public List<String> expand(List<String> tokens) throws IOException {
		sumOfSim.clear();
		resultTokens.clear();
		
		resultsMap.clear();
		results.clear();
		
		//Include the original tokens in the reformulated query
		for (String token : tokens) {
			resultsMap.put(token, (double) 1);
			findSynonymsOfToken(token);
		}
				
		//Find all synonyms
		List<Synonym> allSyns = iterateThesaurus(tokens);
		
		//Update sum of similarity of each token
    	for (Synonym syn : allSyns) {
			Double sim = syn.getSimilarity();
			
			for (int i=0; i<2; i++) {
        		if (sumOfSim.containsKey(syn.getPair()[i])) {
        			Double oldSim = sumOfSim.get(syn.getPair()[i]);
        			sumOfSim.put(syn.getPair()[i], oldSim+sim);
            	} else {
            		sumOfSim.put(syn.getPair()[i], sim);
            	}
            }
		}
    	
    	//Sort by similarity
    	if (sumOfSim.size()>1)
			sortResults();
    	
    	//Select 2 most similar token
    	int count=0;
    	int i=0;
    	while (count<size) {
    		String token = resultTokens.get(i);
    		i++;
    		
			if (!tokens.contains(token)) {
				resultsMap.put(token, sumOfSim.get(token));
				count++;
			}
    	}
    	
    	results.addAll(resultsMap.keySet());
    	
    	System.out.println("QueryExpander.expand() Input: "+tokens);
		System.out.println("QueryExpander.expand() SynsMap: "+resultsMap);
    	System.out.println("QueryExpander.expand() Output: "+results+"\n");
		return results;
	}
	
	
	/**
	 * Find synonyms of a token
	 * */
	private void findSynonymsOfToken(String token) {
		System.out.println("QueryExpander.updateSimilarity() Input: "+token);
		
		if (!thesaurusMap.containsKey(token))
			return;
		
		
		List<Synonym> synonyms = thesaurusMap.get(token);
		
		for (Synonym syn : synonyms) {
			//System.out.println("QueryExpander.updateSimilarity() Synonym: "+syn);
			
			String word = syn.getPair()[0];
			Double value;
			
			if (token.equals(word))
				word = syn.getPair()[1];
			
			if (sumOfSim.containsKey(word))
				value = sumOfSim.get(word)+syn.getSimilarity();
			else
				value = syn.getSimilarity();
			
			sumOfSim.put(word, value);
		}
		
		System.out.println("QueryExpander.updateSimilarity() Output: "+sumOfSim);
		System.out.println("QueryExpander.updateSimilarity() Output-Size: "+sumOfSim.size()+"\n");
		
		//sortResults();
	}
	
	
	/**
	 * Sort search results by scores
	 * */
	private void sortResults() {
		List<Entry<String, Double> > list = new LinkedList<Entry<String, Double>>(sumOfSim.entrySet());
        
        //Sort list (results are in ascending order)
        Collections.sort(list, new Comparator<Entry<String, Double>>() { 
            public int compare(Entry<String, Double> o1, Entry<String, Double> o2) { 
                return (o1.getValue()).compareTo(o2.getValue()); 
            } 
        }); 
        
        //Revise scores map and results list
        Map<String, Double> sortedScores = new LinkedHashMap<String, Double>();
        ArrayList<String> sortedResults = new ArrayList<String>();
        
        for (int i=list.size(); i>0; i--) {
        	Entry<String, Double> tmp = list.get(i-1);
        	sortedScores.put(tmp.getKey(), tmp.getValue());
        	sortedResults.add(tmp.getKey());
        }
        
        sumOfSim = sortedScores;
        resultTokens = sortedResults;
        System.out.println("QueryExpander.sortResults() Output: "+sumOfSim+"\n");
	}
	
	
	
	
	
	
	
	
	public static void main(String[] args) throws IOException {
		Utility util = new Utility();
		
		List<String> tokens = new ArrayList<String>();
		tokens.add("comput");
		tokens.add("system");
		//tokens.add("oil");
		
		long start = System.nanoTime();
		
		QueryExpander qe1 = new QueryExpander(util.COURSES);
		qe1.expand(tokens);
		//qe1.getSynonyms(tokens);
		//QueryExpander qe2 = new QueryExpander(util.REUTERS);
		//qe2.expand(tokens);
		//qe2.getSynonyms(tokens);
		
		System.out.println("QueryExpander - Elapsed Time(ms): "+(System.nanoTime()-start)/1000000);

	}

}
