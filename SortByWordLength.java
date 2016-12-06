package dictionary;

import java.util.Comparator;

public class SortByWordLength implements Comparator<String> {
	public int compare(String word1, String word2) {
        return word1.length() - word2.length();
    }
}