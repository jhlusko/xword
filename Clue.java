package dictionary;

import java.io.Serializable;
import java.util.ArrayList;

public class Clue implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ArrayList<String> words;
	String clue;
	
	public Clue(String clue){
		this.clue = clue;
		this.words = new ArrayList<String>();
	}

	public Clue(String clue, ArrayList<String> words){
		this.clue = clue;
		this.words = words;
	}

	public void add(String word) {
		words.add(word);
	}
	
	public void printInfo(){
		System.out.println("clue: " + this.clue);
		System.out.println("Words: " + this.words.toString());
		System.out.println();
	}

}
