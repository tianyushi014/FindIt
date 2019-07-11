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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import util.Synonym;
import util.Term;
import util.Utility;

/**
 * Similarity measurement: Jaccard similarity: (A intersection B)/(A union B)
 * Unit of comparison: documents
 * 
 * Process:
 * 1. Read weighted index JSON file from given directory path
 * 2. Build term map, get distinct terms from key set (dictionary)
 * 3. Initial thesaurus map and create SimilarityPair objects (without similarity measurements)
 * 4. Iterate all pairs, calculate co-occurrence thesaurus
 * 5. Output a formatted JSON file for weighted index
 * */
public class Thesaurus {

	private Utility util;
	
	private static String selection;
	private static String outPath;
	
	private static String inFile;
	private static String outFile;
	
	//Map: key=pair, value=SimilarityPair
	//private Map<String[], Thesaurus> thesaurusMap;
	private List<Synonym> thesauruses;
	
	//Map: key=token, value=term
	private Map<String, Term> indexMap;
	private List<String> terms;
		
	
	public Thesaurus(String collection, Map<String, Term> indexMap) {
		util = new Utility();
		
		selection = collection;
		outPath = util.outPath+selection;
		
		outFile = outPath+util.outThesaurus;
		
		//thesaurusMap = new HashMap<String[], Thesaurus>();
		thesauruses = new ArrayList<Synonym>();
		
		this.indexMap = indexMap;
		
		terms = new ArrayList<String>();
		terms.addAll(indexMap.keySet());
	}

	
	public Thesaurus(String collection) {
		util = new Utility();
		
		selection = collection;
		outPath = util.outPath+selection;
		
		inFile = outPath+util.outWeightedIndex;
		outFile = outPath+util.outThesaurus;
		
		//thesaurusMap = new HashMap<String[], Thesaurus>();
		thesauruses = new ArrayList<Synonym>();
		
		indexMap = new HashMap<String, Term>();
		
		try {
			readJsonStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		terms = new ArrayList<String>();
		terms.addAll(indexMap.keySet());
	}
	
	
	/**
	 * Read JSON file
	 * @throws IOException 
	 * */
	private void readJsonStream() throws IOException {
		//System.out.println("ThesaurusIndex.readJsonStream() Start");
		indexMap.clear();
		
		Gson gson = new GsonBuilder().create();

		InputStream stream = new FileInputStream(inFile);
		JsonReader reader = new JsonReader(new InputStreamReader(stream, "UTF-8"));       
        
		reader.beginArray();
        while (reader.hasNext()) {
            Term term = gson.fromJson(reader, Term.class);
            indexMap.put(term.getToken(), term);
        }
        reader.close();
        
        System.out.println("Thesaurus.readJsonStream() [WeightedIndexMap] Output-Size: "+indexMap.size()+"\n");
	}
	
	
	/**
	 * Build thesaurus index.
	 * @throws IOException
	 * */
	public void build() throws IOException {
		buildMatrix();
		
		File file = new File(outFile);
		if (!file.exists() || !file.isFile()) {
			writeJsonStream();
		}
	}
	
	
	/**
	 * Iterate all terms, build pair matrix and initial thesaurus map
	 * */
	private void buildMatrix() {
		for (int i=0; i<terms.size(); i++) {
			for (int j=i+1; j<terms.size(); j++) {
				String[] pair = {terms.get(i), terms.get(j)};
				Double similarity = calculateSimilarity(pair);
				
				if (similarity!=0)
					thesauruses.add(new Synonym(pair, similarity));
				//thesaurusMap.put(pair, new Thesaurus(pair, similarity));
			}
			
			//if (i%1000 == 0)
			//System.out.println("Thesaurus.buildMatrix() Index: "+i);
		}
	}
	
	
	/**
	 * Calculate similarity between a pair
	 * */
	private Double calculateSimilarity(String[] pair) {
		//Error checking
		if (pair.length!=2 || pair[0]==null || pair[1]==null) {
			System.out.println("Thesaurus.calculateSimilarity() Error: Invalid Pair");
			System.exit(util.ExitErrorInThesaurusIndex);
		}
		
		List<Integer> docIDsA = indexMap.get(pair[0]).getDocIDs();
		List<Integer> docIDsB = indexMap.get(pair[1]).getDocIDs();
		
		List<Integer> intersection = new ArrayList<Integer>();
		List<Integer> union = new ArrayList<Integer>();
		
		//intersection = (List<Integer>) CollectionUtils.intersection(docIDsA, docIDsB);
		intersection.addAll(docIDsA);
		for (int docID : docIDsA) {
			if (!docIDsB.contains(docID))
				intersection.remove((Integer) docID);
		} 
		
		if (intersection.size()==0)
			return 0d;
		
		//union = (List<Integer>) CollectionUtils.union(docIDsA, docIDsB);
		union.addAll(docIDsA);
		for (int docID : docIDsB) {
			if (!docIDsA.contains(docID))
				union.add(docID);
		}
		
		Double similarity = Double.valueOf(intersection.size())/Double.valueOf(union.size());
		similarity = Math.round(similarity*100000.0)/100000.0;
		
		//System.out.println("ThesaurusIndex.calculateSimilarity() Output:"+similarity);
		return similarity;
	}
	
	
	
	/**
	 * Output JSON file for thesaurus index
	 * @throws IOException 
	 * */
	private void writeJsonStream() throws IOException {
		//System.out.println("ThesaurusIndex.writeJsonStream() Start");
		
		Gson gson = new GsonBuilder().create();

		OutputStream stream = new FileOutputStream(outFile);
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(stream, "UTF-8"));
        writer.setIndent("  ");
        
        writer.beginArray();
        for (Synonym term : thesauruses) {
            gson.toJson(term, Synonym.class, writer);
        }
        writer.endArray();
        writer.close();
        
        System.out.println("Thesaurus.writeJsonStream() [ThesaurusIndexMap] Output-Size: "+thesauruses.size()+"\n");
    }
	
	
	
	public static void main(String[] args) throws IOException {
		Utility util = new Utility();
		
		long start = System.nanoTime();
		
		//Thesaurus index1 = new Thesaurus(util.COURSES);
		//index1.build();
		
		Thesaurus index2 = new Thesaurus(util.REUTERS);
		index2.build();

		System.out.println("Thesaurus - Elapsed Time(ms): "+(System.nanoTime()-start)/1000000);
	}

}
