package dictionary;


import static dictionary.Crossword.checkDifferent;
import static dictionary.Crossword.getEqualLengthDict;
import static dictionary.Crossword.getWordPairs;
import static dictionary.Crossword.sanitize;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;


public class Test {

	public static void main(String[] args) throws IOException, ClassNotFoundException, Exception {
        ArrayList<Clue> wordPairs = checkDifferent(getWordPairs(sanitize(getEqualLengthDict())));
        Crossword cw = new Crossword(wordPairs);
        cw.buildIndex();
        
        System.out.println();
    } 
	      
}
