package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Term {

	//Term
	private String token;
	
	//ID of Documents which contains the term
	private List<Integer> docIDs;
	
	//Term frequency for each document (which contains the term)
	private List<Integer> freqs; 
	
	//Next tokens and freqs, key=token, value=token freq
	private Map<String, Integer> nextTokens;
	
	//Term weight for each document (which contains the term)
	private List<Double> weights;
	
	
	public Term(String term) {
		this.token = term;
		
		//docIDs = new ArrayList<Integer>();
		//freqs = new ArrayList<Integer>();
		//nextTokens = new HashMap<String, Integer>();
		//weights = new ArrayList<Double>();
		
		setDocIDs(new ArrayList<Integer>());
		setFreqs(new ArrayList<Integer>());
		setNextTokens(new HashMap<String, Integer>());
		setWeights(new ArrayList<Double>());
	}
	
	
	public void setToken(String term) {
		this.token = term;
	}
	
	public void setDocIDs(List<Integer> docIDs) {
		this.docIDs = docIDs;
	}
	
	public void setFreqs(List<Integer> freqs) {
		this.freqs = freqs;
	}
	
	public void setNextTokens(Map<String, Integer> nextTokens) {
		this.nextTokens = nextTokens;
	}
	
	public void setWeights(List<Double> weights) {
		this.weights = weights;
	}
	
	
	public String getToken() {
		return token;
	}
	
	public List<Integer> getDocIDs() {
		return docIDs;
	}
	
	public List<Integer> getFreqs() {
		return freqs;
	}
	
	public Map<String, Integer> getNextTokens() {
		return nextTokens;
	}
	
	public List<Double> getWeights() {
		return (List<Double>) weights;
	}
	
	
	public Integer getNextTokenFreq(String token) {
		return nextTokens.get(token);
	}
	
	public void updateNextTokenFreq(String token, Integer freq) {
		nextTokens.put(token, freq);
	}
	
	
	
	@Override
    public String toString() {
		//return "Weighted{"+"term='"+getTerm()+"words='"+getWords()+"}";
        return "["+getToken()+
        		"] -> DocIDs: "+getDocIDs()+
        		" -> Freqs: "+getFreqs()+
        		" -> NextTokens: "+getNextTokens()+
        		" -> Weights: "+getWeights();
    }
}
