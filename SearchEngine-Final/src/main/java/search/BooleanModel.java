package search;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import base.TextProcessor;
import util.Term;
import util.Utility;


/**
 * Process:
 * 1. Read weighted index file from given directory path
 * 2. Check if query is valid
 * 3. Iterate query
 * 		3.1 For each operand, search element and push results list into stack
 * 		3.2 For each operator, modify search result
 * */
public class BooleanModel {

	private Utility util;
	
	private static String selection;
	private static String outPath;
	
	private static String inFile;
		
	private TextProcessor analyzer;
	
	private Map<String, Term> indexMap;
	private List<Integer> results;
	
	private final List<String> operators = Arrays.asList("AND", "OR", "NOT");	
	
	
	public BooleanModel(Map<String, Term> index) {
		util = new Utility();	
		analyzer = new TextProcessor(util.normalization, util.stopwordRemoval, util.stemming);	
		
		indexMap = index;
		results = new ArrayList<Integer>();
	}
	
	
	public BooleanModel(String collection) {
		util = new Utility();		
		analyzer = new TextProcessor(util.normalization, util.stopwordRemoval, util.stemming);	
		
		indexMap = new HashMap<String, Term>();
		results = new ArrayList<Integer>();	
		
		selection = collection;
		outPath = util.outPath+selection;
		
		inFile = outPath+util.outWeightedIndex;
		
		/**/
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
		
		//Read JSON file in stream mode
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
        
        stream.close();
        System.out.println("BooleanModel.readJsonStream() [WeightedIndexMap] Input: "+inFile);
        System.out.println("BooleanModel.readJsonStream() [WeightedIndexMap] Output-Size: "+indexMap.size()+"\n");
	}
	
	
	public String getInFile() {
		return inFile;
	}
	
	
	/**
	 * Search in boolean model
	 * @throws IOException 
	 * */
	public List<Integer> search(List<String> queryPostfix) throws IOException { 
		results.clear();
		
		Stack<List<Integer>> stack = new Stack<List<Integer>>();		
		
		//Check if query is valid
		if (!queryPostfix.isEmpty()) {
			
			//Iterator through query
			for (String element : queryPostfix) {	
				
				//For each operand, search and push results list into stack
				if (!operators.contains(element)) {
					List<Integer> docIDs = searchElement(element);
					
					//Check if search successes
					if (!docIDs.isEmpty()) {
						stack.push(docIDs);
					} else {
						stack.push(new ArrayList<Integer>());
					}
				}
				//For each operator, modify search results
				else {
					List<Integer> tmp = new ArrayList<Integer>();
					
					//NOT -> one operand is needed
					if (element.equals("NOT")) {
						List<Integer> operand = stack.pop();
						
						for (int i=0; i<indexMap.size(); i++) {
							if (!operand.contains(i))
								tmp.add(i);
						}
					}
					//AND / OR -> two operands are needed
					else {
						//Check stack exception
						if (stack.size()<2) {
							System.out.println("Query Syntax Error!");
							System.exit(0);
						} else {
							List<Integer> operand1 = stack.pop();
							List<Integer> operand2 = stack.pop();
							tmp = operand1;
							
							if (element.equals("AND")) {
								tmp.retainAll(operand2);
							} else if (element.equals("OR")) {
								tmp.addAll(operand2);
								
								//Remove duplicates
								Set<Integer> set = new HashSet<Integer>(tmp);
								tmp.clear();
								tmp.addAll(set);
							}
						}
					}
					
					//Sort temporary results in ascending order
					Collections.sort(tmp);
					
					//Store results list for this operator, future operands
					stack.push(tmp);
				}
			}
			//Get final results
			results = stack.pop();
		}
		
		System.out.println("BooleanModel.search() Input: "+queryPostfix);
		System.out.println("BooleanModel.search() Output: "+results);	
		System.out.println("BooleanModel.search() Output-Size: "+results.size()+"\n");	
		return results;
	}

	
	/**
	 * Search element
	 * @throws IOException 
	 * */
	private List<Integer> searchElement(String element) throws IOException {
		readJsonStream();
		
		List<Integer> docIDs = new ArrayList<Integer>();
		System.out.println("BooleanModel.searchElement() Input: "+element);
		
		List<String> tokens = analyzer.analyze(element);
		//System.out.println("BooleanModel.searchElement() Tokens: "+tokens);
		
		if (tokens.size()>0) {
			String token = tokens.get(0);
			//System.out.println("BooleanModel.searchElement() Token: "+token);
			
			//System.out.println("BooleanModel.searchElement() IndexMap-Size: "+indexMap.size());
			
			if (indexMap.containsKey(token)) {
				Term term = indexMap.get(token);
				docIDs = term.getDocIDs();
				//System.out.println("BooleanModel.searchElement() Term: "+term);
				//System.out.println("BooleanModel.searchElement() Term-docIDs: "+term.getDocIDs());
			}
		}	
		
		System.out.println("BooleanModel.searchElement() Output: "+docIDs);
		System.out.println("BooleanModel.searchElement() Output-Size: "+docIDs.size()+"\n");
		return docIDs;		
	}
	
	
	
	
	/*
	public static void main(String[] args) throws IOException {
		Utility util = new Utility();
		
		String test1 = "(operating AND system)";
		String test2 = "(comput* AND graph*)";
		String test3 = "(crypto* OR security)";
		String test4 = "(co*nt)";
		String test5 = "(*ment)";
		String test6 = "(co*nt AND *ment)";
		
		String test7 = "(farm* AND cocoa)";
		
		long startBM = System.nanoTime();
		
		QueryProcessor processor = new QueryProcessor(util.COURSES);
		List<String> query = processor.booleanQuery(test2);	
		
		BooleanModel bm1 = new BooleanModel(util.COURSES);
		bm1.search(query);
		
		//BooleanModel bm2 = new BooleanModel(util.REUTERS);
		//bm2.search(query);
		
		System.out.println("\nBooleanModel - Elapsed Time(ms): "+(System.nanoTime()-startBM)/1000000);
	} */
}
