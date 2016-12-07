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
    private Clue clue;

    public LetterGroup(String name){
            this.name = name;
            this.leftKids = new HashSet<LetterGroup>();
            this.rightKids = new HashSet<LetterGroup>();
            this.numWords = 0;
            this.clue = null;
    }

    public LetterGroup(String name, Clue clue){
            this.name = name;
            this.leftKids = new HashSet<LetterGroup>();
            this.rightKids = new HashSet<LetterGroup>();
            this.numWords = 0;
            this.clue = clue;
    }

 
}
