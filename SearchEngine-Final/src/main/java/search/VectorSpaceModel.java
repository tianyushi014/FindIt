package search;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import base.TextProcessor;
import util.Term;
import util.Utility;


/**
 * Process:
 * 1. Read weighted index file from given directory path
 * 2. Iterate all tokens
 * 		3.1 Get docIDs
 * 		3.2 Update document scores (sum of weights of each token)
 * 3. Sort all search results by scores (weights)
 * */
public class VectorSpaceModel {

	private Utility util;
	
	private static String selection;
	private static String outPath;
	
	private static String inFile;
		
	private TextProcessor analyzer;
	
	private Map<String, Term> indexMap;
	
	//Map: key = docID, value = score
	private Map<Integer, Double> results;
	
	//docIDs
	private List<Integer> resultIDs;	
	
	
	public VectorSpaceModel(Map<String, Term> index) {
		util = new Utility();	
		analyzer = new TextProcessor(util.normalization, util.stopwordRemoval, util.stemming);	
		
		indexMap = index;
		results = new HashMap<Integer, Double>();
		resultIDs = new ArrayList<Integer>();
	}
	
	
	public VectorSpaceModel(String collection) {
		util = new Utility();		
		analyzer = new TextProcessor(util.normalization, util.stopwordRemoval, util.stemming);	
		
		indexMap = new HashMap<String, Term>();
		results = new HashMap<Integer, Double>();
		resultIDs = new ArrayList<Integer>();	
		
		selection = collection;
		outPath = util.outPath+selection;
		inFile = outPath+util.outWeightedIndex;
		
		try {
			readJsonStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
	
	/**
	 * Initial index
	 * @throws IOException
	 * */
	private void readJsonStream() throws IOException {
		indexMap.clear();
		
		Gson gson = new GsonBuilder().create();

		InputStream stream = new FileInputStream(inFile);
		JsonReader reader = new JsonReader(new InputStreamReader(stream, "UTF-8"));
        
        reader.beginArray();
        while (reader.hasNext()) {
            Term term = gson.fromJson(reader, Term.class);
            indexMap.put(term.getToken(), term);
            //break;
        }
        reader.close();
        
        System.out.println("VectorSpaceModel.readJsonStream() [WeightedIndexMap] Output-Size: "+indexMap.size()+"\n");
	}
	
	
	public String getInFile() {
		return inFile;
	}
	
	
	/**
	 * Search in vector space model
	 * @throws IOException 
	 * */
	public Map<Integer, Double> search(String query) throws IOException {
		results.clear();
		resultIDs.clear();
		
		//Tokenize the query
		List<String> tokens = analyzer.analyze(query);
		//System.out.println("\nVSM.search() Tokens: "+tokens);
		
		//Search each token and combine the results
		iterateTokens(tokens);
		
		//Sort results by descending order of scores
		sortResults();
		
		System.out.println("VSM.search() Input: "+query);
		System.out.println("VSM.search() Output: "+results);
		System.out.println("VSM.search() Output-Size: "+results.size());
		
		return results;
	}
	
	
	/**
	 * Search in vector space model
	 * @throws IOException 
	 * */
	public Map<Integer, Double> search(List<String> tokens) throws IOException {
		results.clear();
		resultIDs.clear();
		
		//Search each token and combine the results
		iterateTokens(tokens);
		
		//Sort results by descending order of scores
		sortResults();
		
		System.out.println("VSM.search() Input: "+tokens);
		System.out.println("VSM.search() Output: "+results);
		System.out.println("VSM.search() Output-Size: "+results.size());
		
		return results;
	}
	
	
	/**
	 * Update document score
	 * */
	private void updateScores(Term term) {
		if (term!=null) {
			List<Integer> docIDs = term.getDocIDs();
        	List<Double> weights = term.getWeights();

			//System.out.println("VSM.updateScores() Input: "+term);		        	
        	for (int i=0; i<docIDs.size(); i++) {
				int docID = docIDs.get(i);
				double weight = weights.get(i);
				//System.out.println("current weight: "+weight);
				
				if (results.containsKey(docID)) {
					double score = results.get(docID);
					//System.out.println("current score: "+score);
					
					double newScore = score+weight;
					newScore = Math.round(newScore*100000.0)/100000.0;
	        		results.put(docID, newScore);
					//System.out.println("new score: "+newScore);
				} else {
					results.put(docID, weight);
				}
			}
			//System.out.println("VSM.updateScores() Output: "+results+"\n");
		}
	}
	
	
	/**
	 * Search for a term in weighted index by token value
	 * @throws IOException 
	 * */
	private Term searchTerm(String token) throws IOException {
		readJsonStream();
		//System.out.println("\nVSM.searchTerm() Input: "+token);
		if (indexMap.containsKey(token)) {
			Term term = indexMap.get(token);
			//System.out.println("VSM.searchTerm() Output: "+term);
			return term;
		}
		return null;
	}
	
	
	/**
	 * Iterate tokens
	 * @throws IOException 
	 * */
	private void iterateTokens(List<String> tokens) throws IOException {
		if (!tokens.isEmpty()) {			
			for (int i=0; i<tokens.size(); i++) {
				Term term = searchTerm(tokens.get(i));	
				
				if (term!=null) {
					//System.out.println("\nVSM.search() Term: ["+term.getTerm()+"]");					
					resultIDs.addAll(term.getDocIDs());
					
					Set<Integer> set = new HashSet<Integer>(resultIDs);
					resultIDs.clear();
					resultIDs.addAll(set);
					//System.out.println("Results: "+results);
					
					updateScores(term);
				}
			}
		}
	}
	
	
	/**
	 * Sort search results by scores
	 * */
	private void sortResults() {
		List<Map.Entry<Integer, Double> > list = new LinkedList<Map.Entry<Integer, Double>>(results.entrySet());
        
        //Sort list (results are in ascending order)
        Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() { 
            public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) { 
                return (o1.getValue()).compareTo(o2.getValue()); 
            } 
        }); 
        
        //Revise scores map and results list
        Map<Integer, Double> sortedScores = new LinkedHashMap<Integer, Double>();
        ArrayList<Integer> sortedResults = new ArrayList<Integer>();
        
        for (int i=list.size(); i>0; i--) {
        	Map.Entry<Integer, Double> tmp = list.get(i-1);
        	sortedScores.put(tmp.getKey(), tmp.getValue());
        	sortedResults.add(tmp.getKey());
        }
        
        results = sortedScores;
        resultIDs = sortedResults;
	}
	
	
	
	/*
	public static void main(String[] args) throws IOException {
		Utility util = new Utility();
		
		String test1 = "operating system";
		String test2 = "computers graphical";
		String test3 = "cryptographic security";
		
		String test7 = "farmer cocoa";
		
		long start = System.nanoTime();
		
		VectorSpaceModel vsm1 = new VectorSpaceModel(util.COURSES);
		vsm1.search(test1);
		
		//VectorSpaceModel vsm2 = new VectorSpaceModel(util.REUTERS);
		//vsm2.search(test7);
		
		System.out.println("\nVectorSpaceModel - Elapsed Time(ms): "+(System.nanoTime()-start)/1000000);
	}*/
}
