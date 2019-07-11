package base;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import util.DocObj;
import util.JsonIO;
import util.Utility;


/**
 * Process:
 * 1. Read JSON file from given directory path (JSON output path)
 * 2. Iterate through all Documents
 * 3. For each document, text-process each field and find distinct terms
 * 4. Output a formatted JSON file for all processed documents
 * */
public class Dictionary {

	private Utility util;
	private TextProcessor analyzer;
	
	//private static int selection;
	private static String selection;
	private static String outPath;
	
	private static String inFile;
	private static String outFileJson;
	private static String outFileDic;
	
	private List<DocObj> docs;
	private List<String> dictionary;
	
	private JSONArray processedDocs;
	
	
	public Dictionary(String outputPath, boolean[] filters) {}
	
	public Dictionary(String collection) {
		util = new Utility();	
		analyzer = new TextProcessor(util.normalization, util.stopwordRemoval, util.stemming);
		
		docs = new ArrayList<DocObj>();
		dictionary = new ArrayList<String>();
		
		processedDocs = new JSONArray();

		selection = collection;
		outPath = util.outPath+selection;
		
		inFile = outPath+util.outJSON;	
		outFileJson = outPath+util.outProcessedJSON;
		outFileDic = outPath+util.outDictionary;
	}
	
	
	public String getSelection() {
		return selection;
	}
	
	public Integer getNumOfDocs() {
		return docs.size();
	}
	
	public List<String> getDictionary() {
		return dictionary;
	}
	
	
	/**
	 * Build dictionary
	 * @throws IOException 
	 * */
	public void build() throws IOException {
		//System.out.println("Dictionary.build() Start Reading");
		JSONArray docs = new JsonIO().readJsonFile(inFile);
		//System.out.println("Dictionary.build() Finish Reading");
		
		iterateDocs(docs);
		
		File fileJson = new File(outFileJson);
		if (!fileJson.exists() || !fileJson.isFile()) {
			writeJson();
		}
		
		File fileDic = new File(outFileDic);
		if (!fileDic.exists() || !fileDic.isFile()) {
			writeJsonStream();
		}
		
		/*
		File file = new File(inFile);
		if (file.exists() && file.isFile()) {
			readJsonStream();
			processDocs();
			
			//iterateDocs(docs);
			
			File fileJson = new File(outFileJson);
			File fileDic = new File(outFileDic);
			if (!fileJson.exists() || !fileJson.isFile() || !fileDic.exists() || !fileDic.isFile()) {
				writeJsonStream();
			}
		}*/
	}	
	
	/**
	 * Delete output file
	 * */
	public void delete() {
		File file1 = new File(outFileJson);
		if (file1.exists() && file1.isFile()) {
			file1.delete();
		}
		
		File file2 = new File(outFileDic);
		if (file2.exists() && file2.isFile()) {
			file2.delete();
		}
	}
	
	
	/**
	 * Text Processing
	 * */
	private List<String> processText(String input) {
		TextProcessor analyzer = new TextProcessor(util.normalization, util.stopwordRemoval, util.stemming);
    	List<String> tokens = analyzer.analyze(input);	
    	return tokens;
	}
	
	/**
	 * Find all distinct terms
	 * */
	private JSONObject findDistinctTermsInDoc(JSONObject doc) {
		JSONObject processedDoc = new JSONObject();
		
		Iterator<?> keys = doc.keys();
		
		while(keys.hasNext()) {
		    String key = (String)keys.next();
		    
		    if (key.equals("docID")) {
		    	processedDoc.put(key, doc.get(key));
		    } else {
		    	List<String> tokens = processText(doc.get(key).toString());
		    	
		    	String tmp = "";	
		    	for (int j=0; j<tokens.size(); j++) {
		    		String token = tokens.get(j);
		    		tmp += token;
		    		tmp += " ";
		    		
		    		if (!dictionary.contains(token)) {
		    			dictionary.add(token);
		    		};
		    	}
		    	processedDoc.put(key, tmp);
		    }
		}
		
		return processedDoc;
	}
	
	
	private void findDistinctTerms(List<String> tokens) {
		String tmp = "";	
    	for (int j=0; j<tokens.size(); j++) {
    		String token = tokens.get(j);
    		tmp += token;
    		tmp += " ";
    		
    		if (!dictionary.contains(token)) {
    			dictionary.add(token);
    		};
    	}
	}
	
	
	/**
	 * Iterate documents
	 * */
	private void processDocs() {
		for (int i=0; i<docs.size(); i++) {
			DocObj doc = docs.get(i);
			
			List<String> topicTokens = analyzer.analyze(doc.getTopic());
		    List<String> titleTokens = analyzer.analyze(doc.getTitle());
		    List<String> contentTokens = analyzer.analyze(doc.getContent());
		    
		    docs.get(i).setTopic(topicTokens.toString());
		    docs.get(i).setTitle(titleTokens.toString());
		    docs.get(i).setContent(contentTokens.toString());
		    
		    findDistinctTerms(topicTokens);
		    findDistinctTerms(titleTokens);
		    findDistinctTerms(contentTokens);
		}
	}
	
	
	/**/
	/**
	 * Iterate documents
	 * */
	private void iterateDocs(JSONArray docs) {
		for (int i=0; i<docs.size(); i++) {
			JSONObject doc = docs.getJSONObject(i);
			JSONObject processedDoc = findDistinctTermsInDoc(doc);
			processedDocs.add(processedDoc);
			
			//if (i%100 ==0)
			//	System.out.println("Dictionary.iterateDocs() Count: "+i);
		}
	}
	
	
	/**
	 * Create JSON objects
	 * */
	private JSONArray createJson() {
		JSONArray jsonArr = new JSONArray();
		
		for (String term : dictionary) {
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("term", term);
			
			jsonArr.add(jsonObj);
		}
		
		return jsonArr;
	}
	
	/**
	 * Output JSON file
	 * @throws IOException 
	 * */
	private void writeJson() throws IOException{
		new JsonIO().writeJsonFile(outFileJson, processedDocs);
		//new JsonIO().writeJsonFile(outFileDic, createJson());
	}
	
	
	
	/**
	 * Read JSON file
	 * @throws IOException 
	 * */
	private void readJsonStream() throws IOException {
		//Read JSON file in stream mode
		Gson gson = new GsonBuilder().create();

		InputStream stream = new FileInputStream(inFile);
		JsonReader reader = new JsonReader(new InputStreamReader(stream, "UTF-8"));       
        reader.beginArray();
        
        while (reader.hasNext()) {
        	/*
        	reader.beginObject();   	
        	
        	while (reader.hasNext()) {
        		String key = reader.nextName();      		
        		String value = reader.nextString();
        		
        		if (!key.equals("docID")) {
        			List<String> tokens = analyzer.analyze(value);
        		}
        	}
        	*/
            DocObj doc = gson.fromJson(reader, DocObj.class);
            docs.add(doc);
            //break;
        }
        reader.close();
        
        System.out.println("Dictionary.readJsonStream() Output-Size: "+docs.size());
	}
	
	/**
	 * Output JSON file
	 * @throws IOException 
	 * */
	private void writeJsonStream() throws IOException {
		Gson gson = new GsonBuilder().create();

		OutputStream streamDic = new FileOutputStream(outFileDic);
        JsonWriter writerDic = new JsonWriter(new OutputStreamWriter(streamDic, "UTF-8"));
        writerDic.setIndent("  ");
        
        writerDic.beginArray();
        for (String term : dictionary) {
            gson.toJson(term, String.class, writerDic);
        }
        writerDic.endArray();
        writerDic.close();
    }
	
	
	/**/
	public static void main(String[] args) throws IOException {
		Utility util = new Utility();
		
		long start = System.nanoTime();
		
		//Dictionary dic1 = new Dictionary(util.COURSES);
		//dic1.build();
		
		Dictionary dic2 = new Dictionary(util.REUTERS);
		dic2.build();
		
		System.out.println("Dictionary - Elapsed Time(ms): "+(System.nanoTime()-start)/1000000);
	}
}
