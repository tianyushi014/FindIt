package util;

public class Utility {

	public final String inPath = "data/input/";
	public final String outPath = "data/output/";
	
	public final String COURSES = "Courses";
	public final String REUTERS = "Reuters";
	
	public final String inTopics = "all-topics-strings.lc.txt";
	
	public final String outJSON = "/JSON.json";
	public final String outKNN = "/KNN.json";
	public final String outProcessedJSON = "/ProcessedJSON.json";
	public final String outDictionary = "/Dictionary.json";
	public final String outBigramIndex = "/BigramIndex.json";
	public final String outWeightedIndex = "/WeightedIndex.json";
	public final String outThesaurus = "/Thesaurus.json";
	
	
	/*
	public final String inCourses = inPath+COURSES;
	public final String outCourses = outPath+COURSES;
	
	public final String inReuters = inPath+REUTERS;
	public final String outReuters = outPath+REUTERS;
	*/
	
	
	public final boolean normalization = true;
	public final boolean stopwordRemoval = true;
	public final boolean stemming = true;
	public final boolean[] filters = {normalization, stopwordRemoval, stemming};
	
	
	public final int ExitInvalidInPath = 1000;
	public final int ExitInvalidInFile = 1001;
	public final int ExitErrorInDictionary = 2001;
	public final int ExitErrorInThesaurusIndex = 2002;
}
