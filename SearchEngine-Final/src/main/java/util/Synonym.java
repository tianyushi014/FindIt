package util;

public class Synonym {

	private String[] pair;
	private Double similarity;
	
	
	public Synonym(String[] words) {
		setPair(words);
	}

	public Synonym(String[] words, Double similarity) {
		setPair(words);
		setSimilarity(similarity);
	}

	
	public void setPair(String[] pair) {
		this.pair = pair;
	}
	
	public void setSimilarity(Double similarity) {
		this.similarity = similarity;
	}
	
	
	public String[] getPair() {
		return pair;
	}
	
	public Double getSimilarity() {
		return similarity;
	}
	
	
	@Override
    public String toString() {
		return "["+getPair()[0]+","+getPair()[1]+"] -> "+getSimilarity();
    }
}
