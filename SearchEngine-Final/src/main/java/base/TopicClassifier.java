package base;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import util.DocObj;
import util.Term;
import util.Utility;


/*
 * Source Acknowledgement:
 * 
 * The cosineSimilarity method is copied from
 * https://stackoverflow.com/questions/520241/how-do-i-calculate-the-cosine-similarity-of-two-vectors
 * 
 * */


/**
 * Process:
 * 1. Read topic file for all topics
 * 2. Read JSON file for all documents
 * 3. Read weighted index for each keyword's weight in each document
 * 4. Divide all documents into training docs and testing docs
 * 5. Build a doc-weights map, for each entry:
 * 		Key = Integer docID
 * 		Value = List<Double> weights => weights of each keyword (topic word)
 * 6. For each testing doc,
 * 		6.1 Calculate doc distance
 * 		6.2 Choose k=1, select the nearest document and take its topic
 * */
public class TopicClassifier {

	private Utility util;
	
	private TextProcessor analyzer;
	
	private static String selection;
	private static String outPath;
	
	private static String inFileTopics;
	private static String inFileJson;
	private static String inFileIndex;
	private static String outFile;
	
	private List<String> allTopics;
	private List<DocObj> allDocs;
	private Map<String, Term> indexMap;

	private Map<Integer, DocObj> trainingMap;
	private Map<Integer, DocObj> testingMap;
	
	private List<DocObj> trainingDocs;
	private List<DocObj> testingDocs;
	
	private Map<Integer, Double[]> weightsMap;
	private List<DocObj> results;;
	
	private final int k=1;
	
	
	public TopicClassifier() {
		util = new Utility();
		
		analyzer = new TextProcessor(util.normalization, util.stopwordRemoval, util.stemming);
		
		allTopics = new ArrayList<String>();
		allDocs = new ArrayList<DocObj>();
		indexMap = new HashMap<String, Term>();
		
		trainingMap = new HashMap<Integer, DocObj>();
		testingMap = new HashMap<Integer, DocObj>();
		
		trainingDocs = new ArrayList<DocObj>();
		testingDocs = new ArrayList<DocObj>();
		
		weightsMap = new HashMap<Integer, Double[]>();
		results = new ArrayList<DocObj>();
		
		selection = util.REUTERS;
		outPath = util.outPath+selection;
		
		inFileTopics = util.inPath+util.inTopics;
		inFileJson = outPath+util.outJSON;
		inFileIndex = outPath+util.outWeightedIndex;
		outFile = outPath+util.outKNN;
		
	}

	
	/**
	 * Read topics file
	 * @throws IOException 
	 * */
	private void readJsonStreamTopics() throws IOException {
		allTopics.clear();
		
		StringBuilder contentBuilder = new StringBuilder();
		BufferedReader br = new BufferedReader(new FileReader(inFileTopics));
		
		String sCurrentLine;
        while ((sCurrentLine = br.readLine()) != null) {
            contentBuilder.append(sCurrentLine).append("\n");
        }
        br.close();
        
        allTopics = Arrays.asList(contentBuilder.toString().split("\n"));
		
		System.out.println("TopicClassifier.readJsonStreamTopics() Input: "+inFileTopics);
		//System.out.println("TopicClassifier.readJsonStreamTopics() Output: "+allTopics);
        System.out.println("TopicClassifier.readJsonStreamTopics() Output-Size: "+allTopics.size()+"\n");
	}
	
	
	/**
	 * Read JSON file
	 * @throws IOException 
	 * */
	private void readJsonStreamJson() throws IOException {
		allDocs.clear();
		
		Gson gson = new GsonBuilder().create();

		InputStream stream = new FileInputStream(inFileJson);
		JsonReader reader = new JsonReader(new InputStreamReader(stream, "UTF-8"));       
        
		reader.beginArray();     
		while (reader.hasNext()) {
        	DocObj doc = gson.fromJson(reader, DocObj.class);
            allDocs.add(doc);
        }
        reader.close(); 
        
        System.out.println("TopicClassifier.readJsonStreamJson() Input: "+inFileJson);
        System.out.println("TopicClassifier.readJsonStreamJson() [JSON] Output-Size: "+allDocs.size()+"\n");
	}
	
	
	/**
	 * Initial index
	 * @throws IOException
	 * */
	private void readJsonStreamIndex() throws IOException {
		indexMap.clear();
		
		//Read JSON file in stream mode
		Gson gson = new GsonBuilder().create();

		InputStream stream = new FileInputStream(inFileIndex);
		JsonReader reader = new JsonReader(new InputStreamReader(stream, "UTF-8"));
        
        reader.beginArray();
        while (reader.hasNext()) {
            Term term = gson.fromJson(reader, Term.class);
            indexMap.put(term.getToken(), term);
        }
        reader.close();
        
        stream.close();
        System.out.println("TopicClassifier.readJsonStreamWI() [WeightedIndexMap] Input: "+inFileIndex);
        System.out.println("TopicClassifier.readJsonStreamWI() [WeightedIndexMap] Output-Size: "+indexMap.size()+"\n");
	}
	
	
	/**
	 * Classify docs by topics
	 * @throws IOException 
	 * */
	public void classify() throws IOException {
		//Read input files
		readJsonStreamTopics();
		readJsonStreamJson();
		readJsonStreamIndex();
		
		//Divide training docs and testing docs
		divideDocs();
		
		//Build a doc-weights map
		buildWeightsMap();
		
		//Iterate each testing docs
		iterateTestingDocs();
		
		//Output JSON results
		File file = new File(outFile);
		if (!file.exists() || !file.isFile()) {
			writeJsonStream();
		}
	}
	
	
	/**
	 * Divide training docs and testing docs
	 * */
	private void divideDocs() {
		for (int i=0; i<allDocs.size(); i++) {
			DocObj doc = allDocs.get(i);
			
			for (int j=0; j<allTopics.size(); j++) {
				String topic = doc.getTopic();
				
				//if topic is empty
				if (topic.equals("")) {
					testingMap.put(i, doc);
					testingDocs.add(doc);
					break;
				}
				
				//if match with one from the list of all topics
				if (doc.getTopic().contains(allTopics.get(j))) {
					trainingMap.put(i, doc);
					trainingDocs.add(doc);
					break;
				}
				
				//if not empty, but not matched until the last topic
				if (j == allTopics.size()-1) {
					testingMap.put(i, doc);
					testingDocs.add(doc);
				}
			}
		}
		
		System.out.println("TopicClassifier.divideDocs() Training-Size: "+trainingDocs.size());
		System.out.println("TopicClassifier.divideDocs() Testing-Size: "+testingDocs.size()+"\n");
        
	}
	
	
	/**
	 * Build a doc-weights map
	 * */
	private void buildWeightsMap() {
		weightsMap.clear();
		
		//Text-processing the topic word since weighted index terms are processed
		List<String> topicTokens = analyzer.analyze(allTopics, true);
		//System.out.println(topicTokens);
		
		//Initial map
		for (int i=0; i<allDocs.size(); i++) {
			Double[] weightsInDoc = new Double[topicTokens.size()];
			Arrays.fill(weightsInDoc, 0d);
			
			weightsMap.put(i, weightsInDoc);
		}
		System.out.println("TopicClassifier.buildWeightsMap() InitialMap done");
		
		//Update each keyword's weight for each document
		for (int i=0; i<topicTokens.size(); i++) {
			String topicToken = topicTokens.get(i);
			
			if (indexMap.containsKey(topicToken)) {
				//Term term = indexMap.get(topicToken);
				//System.out.println(term);
				
				List<Integer> docIDs = indexMap.get(topicToken).getDocIDs();
				List<Double> termWeights = indexMap.get(topicToken).getWeights();
				
				for (int j=0; j<docIDs.size(); j++) {
					docIDs.get(j);
					termWeights.get(j); 
					
					Double[] weightsInDoc = weightsMap.get(docIDs.get(j));
					weightsInDoc[i] = termWeights.get(j); 
					
					weightsMap.replace(docIDs.get(j), weightsInDoc);
				}
			}
		}
		System.out.println("TopicClassifier.buildWeightsMap() UpdateWeights done");
		
		System.out.println("TopicClassifier.buildWeightsMap() Output-Size: "+weightsMap.size()+"\n");
        
	}
	
	
	/**
	 * Calculate cosine similarity between two vectors
	 * */
	private Double cosineSimilarity(Double[] vectorA, Double[] vectorB) {
		Double dotProduct = 0.0;
		Double normA = 0.0;
		Double normB = 0.0;
	    for (int i = 0; i < vectorA.length; i++) {
	        dotProduct += vectorA[i] * vectorB[i];
	        normA += Math.pow(vectorA[i], 2);
	        normB += Math.pow(vectorB[i], 2);
	    }   
	    return dotProduct/(Math.sqrt(normA)*Math.sqrt(normB));
	}
	
	
	/**
	 * Sort search results by scores
	 * */
	private Map<Integer, Double> sortResults(Map<Integer, Double> results) {
		List<Map.Entry<Integer, Double> > list = new LinkedList<Map.Entry<Integer, Double>>(results.entrySet());
        
        //Sort list (results are in ascending order)
        Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() { 
            public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) { 
                return (o1.getValue()).compareTo(o2.getValue()); 
            } 
        }); 
        
        //Revise scores map and results list
        Map<Integer, Double> sortedScores = new LinkedHashMap<Integer, Double>();
        //ArrayList<Integer> sortedResults = new ArrayList<Integer>();
        
        for (int i=list.size(); i>0; i--) {
        	Map.Entry<Integer, Double> tmp = list.get(i-1);
        	sortedScores.put(tmp.getKey(), tmp.getValue());
        	//sortedResults.add(tmp.getKey());
        }
        
        return sortedScores;
	}
	
	
	private String chooseTopics(List<Integer> docIDs) {
		String topicStr = "";
		
		//Map: key=topic, value=freq
		//Map<String, Integer> topicsMap = new HashMap<String, Integer>();
		//List<String> selectedTopics = new ArrayList<String>();
		
		for (int docID : docIDs) {
			String topic = allDocs.get(docID).getTopic();
			topicStr = topicStr+topic;
		}
		
		/*
		for (String str : selectedTopics)
			topicStr = topicStr+str;
		*/
		return topicStr;
	}
	
	
	/**
	 * Iterate each testing docs
	 * */
	private void iterateTestingDocs() {
		for (int i=0; i<allDocs.size(); i++) {
			
			//Build trainingDocID-cosineSim Map
			if (testingMap.keySet().contains(i)) {
				int testingID = i;
				Double[] testingWeights = weightsMap.get(testingID);
				
				Map<Integer, Double> sims = new HashMap<Integer, Double>();
				
				//Calculate doc distances with each training docs
				for (int trainingID : trainingMap.keySet()) {
					Double[] trainingWeights = weightsMap.get(trainingID);
					
					Double similarity = cosineSimilarity(testingWeights, trainingWeights);
					
					//similarity = Math.round(similarity*100000000.0)/100000000.0;
					sims.put(trainingID, similarity);
				}
				
				//Rank similarity
				sims = sortResults(sims);
				List<Integer> simsIDs = new ArrayList<Integer>();
				simsIDs.addAll(sims.keySet());
				
				//Get the k nearest docs
				List<Integer> kdocIDs = new ArrayList<Integer>();
				for (int n=0; n<k; n++)
					kdocIDs.add(simsIDs.get(n));
				
				//Analyze topics of k nearest docs
				String assignedTopic = chooseTopics(kdocIDs);
				
				DocObj doc = allDocs.get(i);
				doc.setTopic(assignedTopic);
				results.add(doc);
				
				System.out.println("TopicClassifier.iterateTestingDocs() ["+i+"] done");
				
			} else {
				results.add(allDocs.get(i));
			}
			
		}
	}
	
	
	
	/**
	 * Output JSON file for classfied docs
	 * @throws IOException 
	 * */
	private void writeJsonStream() throws IOException {
		Gson gson = new GsonBuilder().create();

		OutputStream stream = new FileOutputStream(outFile);
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(stream, "UTF-8"));
        writer.setIndent("  ");
        
        writer.beginArray();
        for (DocObj doc : results) {
            gson.toJson(doc, DocObj.class, writer);
        }
        writer.endArray();
        writer.close();
        
        System.out.println("TopicClassifier.writeJsonStream() [ClassifiedJson] Output-Size: "+results.size()+"\n");
    }
	
	
	
	public static void main(String[] args) throws IOException {
		//Utility util = new Utility();
		
		long start = System.nanoTime();
		
		TopicClassifier c1 = new TopicClassifier();
		c1.classify();
		
		System.out.println("\nTopicClassifier - Elapsed Time(ms): "+(System.nanoTime()-start)/1000000);

	}

}
