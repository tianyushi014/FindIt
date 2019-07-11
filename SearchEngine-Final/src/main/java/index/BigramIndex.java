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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import base.TextProcessor;
import util.BigramTerm;
import util.DocObj;
import util.Utility;


/**
 * Process:
 * 1. Read processed JSON file from given directory path
 * 2. Iterate all documents
 * 3. For each document, generate bigrams of each word
 * 4. Update bigram index
 * 5. Output a formatted JSON file for bigram index
 * */
public class BigramIndex {

	private Utility util;
	
	private static String selection;
	private static String outPath;
	
	private static String inFile;
	private static String outFile;
	
	private TextProcessor analyzer;
	private List<DocObj> docs;
	private Map<String, BigramTerm> bigramMap;
	
	
	public BigramIndex(String collection) {
		util = new Utility();
		
		selection = collection;
		outPath = util.outPath+selection;
		
		inFile = outPath+util.outJSON;	
		outFile = outPath+util.outBigramIndex;
		
		analyzer = new TextProcessor(util.normalization, util.stopwordRemoval, false);
		docs = new ArrayList<DocObj>();
		bigramMap = new HashMap<String, BigramTerm>();
	}
	
	
	public Integer getNumOfDocs() {
		return docs.size();
	}
	
	public Map<String, BigramTerm> getIndex() {
		return bigramMap;
	}
	
	
	/**
	 * Build bigram index
	 * @throws IOException 
	 */
	public void build() throws IOException {
		readJsonStream();
		
		iterateDocs();
		
		File file = new File(outFile);
		if (!file.exists() || !file.isFile()) {
			writeJsonStream();
		}
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
	 * Update index by each document
	 * @throws IOException
	 * */
	private void updateIndexForDoc(DocObj doc) throws IOException {
		List<String> allTokens = new ArrayList<String>();
		
		List<String> topicTokens = analyzer.analyze(doc.getTopic());
		allTokens.addAll(topicTokens);
		
		List<String> titleTokens = analyzer.analyze(doc.getTitle());
		allTokens.addAll(titleTokens);
		
		List<String> contentTokens = analyzer.analyze(doc.getContent());
		allTokens.addAll(contentTokens);
		
		//Update bigram-words index
		for (String token : allTokens) {
			String[] bigrams = generateBigrams(token);
			
			for (String bigram : bigrams) {
				if (bigramMap.containsKey(bigram)) {
					//BigramTerm tmp = bigramMap.get(bigram);
					List<String> tmp = bigramMap.get(bigram).getWords();
					if (!tmp.contains(token)) {
						tmp.add(token);						
						bigramMap.get(bigram).setWords(tmp);
						//bigramMap.put(bigram, new BigramTerm(bigram, tmp));
					}
				} else {
					BigramTerm term = new BigramTerm(bigram, new ArrayList<String>(Arrays.asList(token)));
					bigramMap.put(bigram, term);
				}
			}
		}
	}
	
	
	/**
	 * Iterate all Documents
	 * @throws IOException
	 * */
	private void iterateDocs() throws IOException {
		//for (int i=0; i<5; i++) {
		for (int i=0; i<docs.size(); i++) {
			updateIndexForDoc(docs.get(i));
		}
	}
	
	
	/**
	 * Read JSON file
	 * @throws IOException 
	 * */
	private void readJsonStream() throws IOException {
		docs.clear();
		
		Gson gson = new GsonBuilder().create();

		InputStream stream = new FileInputStream(inFile);
		JsonReader reader = new JsonReader(new InputStreamReader(stream, "UTF-8"));       
        
		reader.beginArray();     
		while (reader.hasNext()) {
        	DocObj doc = gson.fromJson(reader, DocObj.class);
            docs.add(doc);
        }
        reader.close(); 
        
        //System.out.println("BigramIndex.readJsonStream() Input: "+inFile);
        System.out.println("BigramIndex.readJsonStream() [JSON] Output-Size: "+docs.size()+"\n");
	}
	
	
	/**
	 * Output JSON file for weighted inverted index
	 * @throws IOException 
	 * */
	private void writeJsonStream() throws IOException {
		Gson gson = new GsonBuilder().create();

		OutputStream stream = new FileOutputStream(outFile);
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(stream, "UTF-8"));
        writer.setIndent("  ");
        
        writer.beginArray();
        for (BigramTerm term : bigramMap.values()) {
            gson.toJson(term, BigramTerm.class, writer);
        }
        writer.endArray();
        writer.close();
        
        System.out.println("BigramIndex.writeJsonStream() [BigramMap] Output-Size: "+bigramMap.size()+"\n");
    }
	
	
	
	/**/
	public static void main(String[] args) throws IOException {
		Utility util = new Utility();
		
		long start = System.nanoTime();

		//BigramIndex bi1 = new BigramIndex(util.COURSES);
		
		BigramIndex bi2 = new BigramIndex(util.REUTERS);
		bi2.build();
		
		System.out.println("BigramIndex - Elapsed Time(ms): "+(System.nanoTime()-start)/1000000);
	}

}
