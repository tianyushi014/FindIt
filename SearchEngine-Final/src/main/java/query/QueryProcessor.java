package query;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import base.TextProcessor;
import util.BigramTerm;
import util.Utility;


/**
 * Process:
 * 1. Read bigram index file from given directory path
 * 2. Boolean Model:
 * 		2.1 Loop through all elements, handle wildcard (if any)
 * 		2.2 Text-processing
 * 		2.3 Transform query into Post-fix format
 * 3. Vector Space Model:
 * 		3.1 Text-processing
 * */
public class QueryProcessor {

	private Utility util;
	
	private static String selection;
	private static String outPath;
	
	private static String inFile;
	
	private TextProcessor analyzer;
	
	private Map<String, BigramTerm> bigramMap;	
	private WildcardHandler wcHandler;
	private List<String> results;
	
	//Boolean Model
	private final List<String> operators = Arrays.asList("AND", "OR", "NOT");	
	
	
	public QueryProcessor(Map<String, BigramTerm> index) {
		util = new Utility();
		analyzer = new TextProcessor(util.normalization, util.stopwordRemoval, util.stemming);	
		
		bigramMap = index;
		
		results = new ArrayList<String>();	
		wcHandler = new WildcardHandler(bigramMap);
	}	
	
	
	public QueryProcessor(String collection) {
		util = new Utility();
		analyzer = new TextProcessor(util.normalization, util.stopwordRemoval, util.stemming);	
		
		selection = collection;
		outPath = util.outPath+selection;
		
		inFile = outPath+util.outBigramIndex;
		
		bigramMap = new HashMap<String, BigramTerm>();
		try {
			readJsonStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		results = new ArrayList<String>();
		wcHandler = new WildcardHandler(bigramMap);
	}	
	
	/**
	 * Initial index
	 * @throws IOException
	 * */
	private void readJsonStream() throws IOException {
		bigramMap.clear();
		
		//Read JSON file in stream mode
		Gson gson = new GsonBuilder().create();

		InputStream stream = new FileInputStream(inFile);
		JsonReader reader = new JsonReader(new InputStreamReader(stream, "UTF-8"));
        
        reader.beginArray();
        while (reader.hasNext()) {
            BigramTerm bigram = gson.fromJson(reader, BigramTerm.class);
            bigramMap.put(bigram.getToken(), bigram);
            //break;
        }
        reader.close();
        
        System.out.println("QueryProcessor.readJsonStream() [BigramMap] Output-Size: "+bigramMap.size()+"\n");
	}
	
	
	public Map<String, BigramTerm> getIndex() {
		return bigramMap;
	}
	
	public List<String> getQuery() {
		return results;
	}
	
	public String getInFile() {
		return inFile;
	}
	
	
	/**
	 * For boolean model, handle wildcard (if any), transform from infix to post fix
	 * */
	public List<String> booleanQuery(String query) {
		results.clear();
		
		//Pre-processing for easier split operands and operators
		query = query.replaceAll("\\(", "( ");
		query = query.replaceAll("\\)", " )");		
		
		List<String> tmpQuery = new ArrayList<String>();		
		String[] elements = query.split(" ");
		
		//Loop through all elements, handle wildcard (if any)
		for (String element : elements) {
			System.out.println("QueryProcessor.booleanQuery() Element: "+element);
			
			if (element.contains("*")) {
				List<String> words = wcHandler.handle(element);
				
				int size = words.size();
				for (int i=0; i<size; i++) {
					if (i!=size-1) {
						tmpQuery.add("(");
						tmpQuery.add(words.get(i));
						tmpQuery.add("OR");
					} else {
						tmpQuery.add(words.get(words.size()-1));						
						for (int j=0; j<size-1; j++) {
							tmpQuery.add(")");
						}
					}
				}
			} else {
				tmpQuery.add(element);
			}
		}
		
		//Transform query into Post-fix format
		results = infixToPostfix(tmpQuery);
		
		System.out.println("\nQueryProcessor.booleanQuery() Input: "+query);
		System.out.println("QueryProcessor.booleanQuery() Output: "+results+"\n");
		return results;
	}
	
	
	/**
	 * Transform the query from infix format into postfix format
	 * 
	 * @param a query in infix format
	 * @return a list of strings, each string is an operand/operator
	 * */
	private List<String> infixToPostfix(List<String> elements) {
		//System.out.println("\nQueryProcessor.infixToPostfix() Input: "+elements);
		
		List<String> res = new ArrayList<String>();
		Stack<String> stack = new Stack<String>(); 
		
		for (String element : elements) {
			if (operators.contains(element)) {
				stack.push(element);
			} else if (element.equals("(")) {
				
			} else if (element.equals(")")) {
				while (!stack.isEmpty() && !stack.peek().equals("(")) {
					res.add(stack.pop());
				}
				
				if (!stack.isEmpty()) {
					if (!stack.peek().equals("(")) {
						return new ArrayList<String>();
					}
					stack.pop();
				}
			} else {
				res.add(element);
			}
		}
		
		if (!stack.isEmpty()) {
			res.add(stack.pop());
		}
		
		//System.out.println("QueryProcessor.infixToPostfix() Output: "+res);
		return res;
	}

	
	/**
	 * For vector space model, text processing (normalize, remove stopword, stemming)
	 * */
	public List<String> vsmQuery(String query) {
		results.clear();
		
		//Tokenize the query
		results = analyzer.analyze(query);
		
		System.out.println("QueryProcessor.vsmQuery() Input: "+query);
		System.out.println("QueryProcessor.vsmQuery() Output: "+results+"\n");
		return results;
	}
	
	
	
	/*
	public static void main(String[] args) {
		Utility util = new Utility();
		
		String test1 = "(operating AND system)";
		String test2 = "(comput* AND graph*)";
		String test3 = "(crypto* OR security)";
		
		String test4 = "(function AND logic) OR principle";
		String test5 = "(NOT function) AND (logic OR principle)";
		String test6 = "(farm* AND cocoa)";
		
		String test11 = "operating system";
		String test12 = "computers graphical";
		String test13 = "cryptographic security";
		
		long start = System.nanoTime();
		
		QueryProcessor processor1 = new QueryProcessor(util.COURSES);
		//processor1.booleanQuery(test2);
		processor1.vsmQuery(test11);
		
		//QueryProcessor processor2 = new QueryProcessor(util.REUTERS);
		//processor2.booleanQuery(test6);
		
		System.out.println("QueryProcessor - Elapsed Time(ms): "+(System.nanoTime()-start)/1000000);

	} */
}
