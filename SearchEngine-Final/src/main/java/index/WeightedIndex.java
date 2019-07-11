package index;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import util.Term;
import util.DocObj;
import util.Utility;


/**
 * Weighted, inverted index, including bigram models (field nextTokens & freq)
 * 
 * Process:
 * 1. Read processed JSON file from given directory path
 * 2. Get terms from given dictionary
 * 3. Build inverted index
 * 		3.1 Initial index by terms
 * 		3.2 Iterate all documents
 * 		3.3 For each document, update index (docID, frequency, nextTokens & freq)
 * 4. Calculate weights of each term in each document
 * 5. Output a formatted JSON file for weighted index
 * */
public class WeightedIndex {

	private Utility util;
	
	private static String selection;
	private static String outPath;
	
	private static String inFileJson;
	private static String inFileDic;
	private static String outFile;
	
	//Map: key=docIDs, value=max frequency
	private Map<Integer, Integer> maxFreqs;
	
	//Map: key=docIDs, value=number of tokens in document
	private Map<Integer, Integer> numOfTokens;
	
	private List<DocObj> docs;
	private Map<String, Term> termMap;
	
	
	public WeightedIndex(String collection) {
		util = new Utility();
		
		selection = collection;
		outPath = util.outPath+selection;
		
		inFileJson = outPath+util.outProcessedJSON;	
		inFileDic = outPath+util.outDictionary;
		outFile = outPath+util.outWeightedIndex;
		
		maxFreqs = new HashMap<Integer, Integer>();
		numOfTokens = new HashMap<Integer, Integer>();
		
		docs = new ArrayList<DocObj>();
		termMap = new HashMap<String, Term>();	
	}
	
	
	public Integer getNumOfDocs() {
		return docs.size();
	}
	
	public Map<Integer, Integer> getMaxFreqs() {
		return maxFreqs;
	}
	
	public Map<String, Term> getIndex() {
		return termMap;
	}
	
	
	/**
	 * Build weighted inverted index.
	 * @throws IOException
	 * */
	public void build() throws IOException {
		maxFreqs.clear();
		
		readJsonStreamJson();
		readJsonStreamDic();
		
		iterateDocs();
		calculateWeights();
		
		File file = new File(outFile);
		if (!file.exists() || !file.isFile()) {
			writeJsonStream();
		}
	}
	
	
	/**
	 * Read JSON file
	 * @throws IOException 
	 * */
	private void readJsonStreamJson() throws IOException {
		//System.out.println("WeightedIndex.readJsonStreamJson() Start");
		
		docs.clear();
		
		Gson gson = new GsonBuilder().create();

		InputStream stream = new FileInputStream(inFileJson);
		JsonReader reader = new JsonReader(new InputStreamReader(stream, "UTF-8"));       
        
		reader.beginArray();     
        while (reader.hasNext()) {
        	DocObj doc = gson.fromJson(reader, DocObj.class);
            docs.add(doc);
        }
        reader.close(); 
        
        System.out.println("WeightedIndex.readJsonStreamJson() [ProcessedJson] Output-Size: "+docs.size()+"\n");
	}
	
	/**
	 * Read JSON file
	 * @throws IOException 
	 * */
	private void readJsonStreamDic() throws IOException {
		//System.out.println("WeightedIndex.readJsonStreamDic() Start");
		
		termMap.clear();
		
		Gson gson = new GsonBuilder().create();

		InputStream stream = new FileInputStream(inFileDic);
		JsonReader reader = new JsonReader(new InputStreamReader(stream, "UTF-8"));       
        
		reader.beginArray();     
        while (reader.hasNext()) {
        	String token = gson.fromJson(reader, String.class);
            termMap.put(token, new Term(token));
        }
        reader.close(); 
        
        System.out.println("WeightedIndex.readJsonStreamDic() [WeightedIndexMap] Output-Size: "+termMap.size()+"\n");
	}
	
	
	/**
	 * Find all tokens in document
	 * */
	private List<String> findAllTokens(DocObj doc) {
		List<String> allTokens = new ArrayList<String>();
		
		/*
		List<String> topicTokens = Arrays.asList(doc.getTopic().split(" "));
		allTokens.addAll(topicTokens);		
		List<String> titleTokens = Arrays.asList(doc.getTitle().split(" "));
		allTokens.addAll(titleTokens);
		List<String> contentTokens = Arrays.asList(doc.getContent().split(" "));
		allTokens.addAll(contentTokens);
		*/
		String topic = doc.getTopic();
		String title = doc.getTitle();
		String content = doc.getContent();	
		String all = topic+title+content;
		
		allTokens = Arrays.asList(all.split(" "));
		//allTokens.remove("");
		
		/*
		for (String word : allTokens)
			System.out.print("["+word+"] ");
		System.out.println();
		*/
		return allTokens;
	}
	
	/**
	 * Build token-freq map
	 * */
	private Map<String, Integer> buildTokenFreqMap(List<String> allTokens) {
		//Map: key=token, value=token frequency in current document
		Map<String, Integer> tokenFreqMap = new HashMap<String, Integer>();		
		
		for (String token : allTokens) {
			if (!token.equals("")) {
				if (tokenFreqMap.containsKey(token))
					tokenFreqMap.replace(token, tokenFreqMap.get(token)+1);
				else
					tokenFreqMap.put(token, 1);
			}
		}
		
		return tokenFreqMap;
	}
	
	
	/**
	 * Update index by each document
	 * */
	private void updateIndexForDoc(DocObj doc) {
		int docID = doc.getID();
		
		List<String> allTokens = findAllTokens(doc);
		numOfTokens.put(docID, allTokens.size());
		
		Map<String, Integer> tokenFreqMap = buildTokenFreqMap(allTokens);
		
		//Update index
		if (!tokenFreqMap.isEmpty()) {
			//Set max token frequency of this document
			maxFreqs.put(docID, Collections.max(tokenFreqMap.values()));
			
			List<String> distinctTokens = new ArrayList<String>();
			distinctTokens.addAll(tokenFreqMap.keySet());
			
			//Update docIDs and freqs
			for (String token : distinctTokens) {
				//Error Checking
				if (!termMap.keySet().contains(token)) {
					System.out.println("["+token+"] Not Exist In Dictionary");
					System.exit(util.ExitErrorInDictionary);
				}
				
				Term term = termMap.get(token);
				List<Integer> docIDs = term.getDocIDs();
				List<Integer> freqs = term.getFreqs();
				
				//Error Checking: docIDs should not contain the current docID as iterating docs for one time
				if (!docIDs.contains(docID)) {
					//Update docIDs
					docIDs.add(docID);
					term.setDocIDs(docIDs);
					
					//Update freqs
					freqs.add(tokenFreqMap.get(token));
					term.setFreqs(freqs);
					
					//Update index
					termMap.put(token, term);
				}
			}
			
			//Update nextTokens
			for (int i=1; i<allTokens.size()-1; i++) {
				String curr = allTokens.get(i);
				String next = allTokens.get(i+1);
				
				Term term = termMap.get(curr);
				
				//System.out.println("Current: "+curr);
				//System.out.println("Next: "+next);
				//System.out.println(term);
				
				int count = 0;
				if (term.getNextTokens().containsKey(next))
					count = term.getNextTokenFreq(next)+1;
				else
					count = 1;
				
				term.updateNextTokenFreq(next, count);					
				//System.out.println(term+"\n");
				
				termMap.put(curr, term);
			}
		}
	}
	
	/**
	 * Iterate all Documents
	 * */
	private void iterateDocs() {
		//System.out.println("WeightedIndex.iterateDocs() Start");
		//for (int i=0; i<2; i++) {
		for (int i=0; i<docs.size(); i++) {
			updateIndexForDoc(docs.get(i));
		}
	}
	
	
	/**
	 * Calculate weights of each term in each document
	 * @throws IOException
	 * */
	private void calculateWeights() throws IOException{
		//System.out.println("WeightedIndex.calculateWeights() Start");
		
		for (Term term : termMap.values()) {
			List<Integer> docIDs = term.getDocIDs();
        	List<Integer> freqs = term.getFreqs();
        	List<Double> weights = term.getWeights();
        	
        	double N = docs.size();
        	
        	for (int i=0; i<docIDs.size(); i++) {
        		//Calculate tf = log(1+freq/maxFreq)
        		double freq = freqs.get(i);
        		double maxFreq = maxFreqs.get(docIDs.get(i));
        		double tf = Math.log10(1+freq/maxFreq);
        		
        		//Calculate idf = log(N/df)
        		double df = docIDs.size();
        		double idf = Math.log10(N/df);
        		
        		//Calculate weight = tf*idf
        		double weight = tf*idf;
        		
        		//Round weight to keep max 5 decimal places
        		weight = Math.round(weight*100000.0)/100000.0;
        		weights.add(i, weight);
        	}
		}
	}
	
	
	
	
	/**
	 * Output JSON file for weighted inverted index
	 * @throws IOException 
	 * */
	private void writeJsonStream() throws IOException {
		//System.out.println("WeightedIndex.writeJsonStream() Start");
		
		Gson gson = new GsonBuilder().create();

		OutputStream stream = new FileOutputStream(outFile);
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(stream, "UTF-8"));
        writer.setIndent("  ");
        
        writer.beginArray();
        for (Term term : termMap.values()) {
            gson.toJson(term, Term.class, writer);
        }
        writer.endArray();
        writer.close();
        
        System.out.println("WeightedIndex.writeJsonStream() [WeightedIndexMap] Output-Size: "+termMap.size()+"\n");
    }
	
	
	/* */
	public static void main(String[] args) throws IOException {
		Utility util = new Utility();
		
		long start = System.nanoTime();
		
		//Index wi1 = new Index(util.COURSES);
		//wi1.build();
		
		WeightedIndex wi2 = new WeightedIndex(util.REUTERS);
		wi2.build();
		
		System.out.println("WeightedIndex - Elapsed Time(ms): "+(System.nanoTime()-start)/1000000);
	}
}
