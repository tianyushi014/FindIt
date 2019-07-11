package util;

public class DocObj {

	private int docID;
	
	private String topic;
	private String title;
    private String content;
    
    //private int numOfWords;
    
    
    public DocObj() {}
    
    public DocObj(int docID, String topic, String title, String content) {
    	setDocID(docID);
    	setTopic(topic);
    	setTitle(title);
    	setContent(content);
    	//setNumOfWords();
    }
    
    
    public void setDocID(int docID) {
    	this.docID = docID;
    }
    
    public void setTopic(String topic) {
    	this.topic = topic;
    }
    
    public void setTitle(String title) {
    	this.title = title;
    }
    
    public void setContent(String content) {
    	this.content = content;
    }
    
    /*
    public void setNumOfWords() {
    	List<String> words = Arrays.asList(topic+title+content.split(" "));
    	words.remove("");
		numOfWords = words.size();
    }*/
    
    
    public int getID() {
    	return docID;
    }
    
    public String getTopic() {
    	return topic;
    }
    
    public String getTitle() {
    	return title;
    }
    
    public String getContent() {
    	return content;
    }
    
    /*
    public int getnumOfWords() {
    	return numOfWords;
    } */
    
    
    @Override
    public String toString() {
        return getTopic()+"\n"+getTitle()+"\n"+getContent();
    }
    
}
