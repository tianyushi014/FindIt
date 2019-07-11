package base;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

import util.DocObj;
import util.Utility;


/**
 * Process:
 * 1. Find files from given directory path
 * 2. Extract needed information from each file
 * 3. Create DocObj objects
 * 5. Output a formatted JSON file for all objects
 * */
public class Corpus {

	private Utility util;
	
	//private static int selection;
	private static String selection;
	
	private static String inPath;
	private static String outFile;		
	
	private List<Document> localFiles;
	private List<DocObj> docs;
	
	/*
	public Corpus(int collection) {
		util = new Utility();
		
		localFiles = new ArrayList<Document>();
		docs = new ArrayList<DocObj>();
		
		selection = collection;
		if (selection == util.COURSES) {
			inPath = util.inCourses;
			outFile = util.outCourses+"JSON.json";
		} else if (selection == util.REUTERS) {
			inPath = util.inReuters;
			outFile = util.outReuters+"JSON.json";
		}		
	} */
	
	public Corpus(String collection) {
		util = new Utility();
		
		localFiles = new ArrayList<Document>();
		docs = new ArrayList<DocObj>();
		
		selection = collection;
		inPath = util.inPath+selection;
		outFile = util.outPath+selection+util.outJSON;
	}
	
	
	public List<DocObj> getDocs() {
		return docs;
	}
	
	
	/**
	 * Build Corpus
	 * @throws IOException 
	 * */
	public void build() throws IOException {
		docs.clear();
		
		localFiles.clear();
		localFiles = getLocalDocs();
		
		File file = new File(outFile);
		if (!file.exists() || !file.isFile()) {
			if (selection.equals(util.COURSES))
				createCourses();
			else if (selection.equals(util.REUTERS))
				createNews();
			writeJsonStream();
		}
    }
	
	/**
	 * Delete Output file
	 * */
	public void delete() {
		File file = new File(outFile);
		if (file.isFile() && file.exists()) {
			file.delete();
		}
	}
	
	
	/**
	 * Get local resources from input directory
	 * @throws IOException 
	 * */
	private List<Document> getLocalDocs() throws IOException {
		File directory = new File(inPath);
		if (!directory.exists() || !directory.isDirectory()) {
			System.out.println("Invalid Directory");
			System.exit(util.ExitInvalidInPath);
		}	
		//System.out.print("|--");
		//System.out.println(directory.getName());
		
		String[] fileNames = directory.list();
		Arrays.sort(fileNames);		
		for (int i=0; i<fileNames.length; i++) {
			File file = new File(directory.getPath(), fileNames[i]);
			Document doc = Jsoup.parse(file, "UTF-8");
			localFiles.add(doc);			
			//System.out.print("  |--");
			//System.out.println(file.getName());
		}
		
		System.out.println("Corpus.getLocalDocs() Input: "+inPath);
		System.out.println("Corpus.getLocalDocs() Output-Size: "+localFiles.size()+"\n");
		return localFiles;
    }
	
	
	/**
	 * Create Course objects
	 * @throws IOException 
	 * */
	private void createCourses() throws IOException {
		for (int i=0; i<localFiles.size(); i++) {
			Document doc = localFiles.get(i);						
			
			Elements titles = doc.getElementsByClass("courseblocktitle");
	        Elements descriptions = doc.getElementsByClass("courseblockdesc");
	        
	        List<String> specialCourses = new ArrayList<String>();
	        specialCourses.add("5200");
	        specialCourses.add("5380");
	        
	        int count = 0;
	        for (int j=0; j<titles.size(); j++) {
	        	String title = titles.get(j).text();
	        	String description = descriptions.get(count).text();
	        	
	        	String courseNum = title.substring(4,8);
	            if (specialCourses.contains(courseNum)) {
	            	docs.add(new DocObj(j, "", title, ""));
	            } else {
	            	docs.add(new DocObj(j, "", title, description));
	            	count++;
	            }
	        }
		}
	}	
	
	
	/**
	 * Create Reuters objects
	 * @throws IOException 
	 * */
	private void createNews() throws IOException {
		int count=0;
		for (int i=0; i<localFiles.size(); i++) {
			Document doc = localFiles.get(i);		
			
			Elements reuters = doc.getElementsByTag("REUTERS");
			//System.out.println("Reuters-"+i+" Size: "+reuters.size());
			
			for (int j=0; j<reuters.size(); j++) {
				String topic = reuters.get(j).select("TOPICS").text();
				String title = reuters.get(j).select("TITLE").text();
				
				Element textEle = reuters.get(j).select("TEXT").get(0);
				textEle.select("TITLE").remove();
				String text = textEle.text();
				
				docs.add(new DocObj(count, topic, title, text));
				count++;
			}
		}
	}
	
	
	/**
	 * Output JSON file
	 * @throws IOException 
	 * */
	private void writeJsonStream() throws IOException {
		Gson gson = new GsonBuilder().create();

		OutputStream stream = new FileOutputStream(outFile);
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(stream, "UTF-8"));
        writer.setIndent("  ");
        
        writer.beginArray();
        for (DocObj doc : docs) {
            gson.toJson(doc, DocObj.class, writer);
        }
        writer.endArray();
        writer.close();
        
        System.out.println("Corpus.writeJsonStream() [Documents] Output-Size: "+docs.size()+"\n");
    }
	
	
	/*
	/**
	 * Create JSON objects
	 *
	private JSONArray createJson() {
		JSONArray jsonArr = new JSONArray();
		
		if (selection==util.COURSES) {
			for (int i=0; i<courses.size(); i++) {
				JSONObject jsonObj = new JSONObject();
				jsonObj.put("docID", i);
				jsonObj.put("topic", ""); 	//-> to keep JSON file in the same format
				jsonObj.put("title", courses.get(i).getTitle());
				jsonObj.put("content", courses.get(i).getDescription());
				jsonArr.add(jsonObj);
			}
		} else if (selection==util.REUTERS) {
			for (int i=0; i<news.size(); i++) {
				JSONObject jsonObj = new JSONObject();
				jsonObj.put("docID", i);
				jsonObj.put("topic", news.get(i).getTopic());
				jsonObj.put("title", news.get(i).getTitle());
				jsonObj.put("content", news.get(i).getText());
				jsonArr.add(jsonObj);
			}
		}
		
		return jsonArr;
	}
	
	/**
	 * Output JSON file
	 * @throws IOException 
	 *
	private void writeJson() throws IOException{
		new JsonIO().writeJsonFile(outFile, createJson());
	}
	*/
	
	
	
	
	/**/
	public static void main(String[] args) throws IOException {
		Utility util = new Utility();
		
		long start = System.nanoTime();
		
		Corpus corpus1 = new Corpus(util.COURSES);
		corpus1.build();
		
		//Corpus corpus2 = new Corpus(util.REUTERS);
		//corpus2.build();
		
		System.out.println("Corpus - Elapsed Time(ms): "+(System.nanoTime()-start)/1000000);
	} 
}
