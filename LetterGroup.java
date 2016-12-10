package dictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class LetterGroup {

    public String name;
    public HashSet<LetterGroup> leftKids;
    public HashSet<LetterGroup> rightKids;
    public int numWords;
    public Clue clue;
    public String lastLetters;

    
    public LetterGroup(String name, Clue clue){
        this.name = name;
        this.leftKids = new HashSet<LetterGroup>();
        this.rightKids = new HashSet<LetterGroup>();
        this.numWords = 0;
        this.clue = clue;
        this.lastLetters = lastLetters();
    }

    private String lastLetters() {
        String[] parts = this.name.split("/");
        return parts[0].charAt(parts[0].length() - 1) + "" + parts[1].charAt(parts[0].length() - 1);
    }

 
}
