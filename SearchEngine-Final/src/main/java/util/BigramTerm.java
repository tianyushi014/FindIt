package util;

import java.util.ArrayList;
import java.util.List;

public class BigramTerm {
	
	private String token;
	private List<String> words;
	
	
	public BigramTerm() {}
	
	public BigramTerm(String token) {
		setToken(token);
		setWords(new ArrayList<String>());
	}
	
	public BigramTerm(String token, List<String> words) {
		setToken(token);
		setWords(words);
	}
	
	
	public void setToken(String token) {
		this.token = token;
	}
	
	public void setWords(List<String> words) {
		this.words = words;
	}
	
	
	public String getToken() {
		return token;
	}
	
	public List<String> getWords() {
		return words;
	}
	
	
	@Override
	public String toString() {
		return "Bigram{"+"token='"+getToken()+"words='"+getWords()+"}";
	}
}
