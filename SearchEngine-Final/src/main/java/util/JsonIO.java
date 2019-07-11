package util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.sf.json.JSONArray;

public class JsonIO {

	
	/**
	 * Write JSON file to disk
	 * @throws IOException 
	 * */
	public void writeJsonFile(String outPath, JSONArray jsonArr) throws IOException {
		Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		gson = new GsonBuilder().setPrettyPrinting().create();
		FileWriter writer = new FileWriter(outPath);
		gson.toJson(jsonArr, writer);
		writer.close();
	}
	
	
	
	
	/**
	 * Read JSON file from disk
	 * @throws IOException 
	 * */
	public JSONArray readJsonFile(String inPath) throws IOException {
		File file = new File(inPath);		
		String input = FileUtils.readFileToString(file, "UTF-8");
		
		JSONArray jsonArr = new JSONArray();		
		jsonArr = JSONArray.fromObject(input);	
		return jsonArr;
	}
	
	
	/*
	public static void readStream(InputStream stream) throws IOException {
		JsonReader reader = new JsonReader(new InputStreamReader(stream, "UTF-8"));
        Gson gson = new GsonBuilder().create();

        // Read file in stream mode
        reader.beginArray();
        while (reader.hasNext()) {
            // Read data into object model
            Person person = gson.fromJson(reader, Person.class);
            if (person.getId() == 0 ) {
                System.out.println("Stream mode: " + person);
            }
            break;
        }
        reader.close();
	}
	*/
	
	public static void main(String[] args) throws IOException {
		File file = new File("data/output/Courses/CoursesJSON2.txt");		
		String input = FileUtils.readFileToString(file, "UTF-8");
		
		System.out.println(input);
	}
}
