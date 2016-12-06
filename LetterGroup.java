package dictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class LetterGroup {

    public String name;
    public HashSet<LetterGroup> kids;
    public int numWords;
    private Clue clue;

    public LetterGroup(String name){
            this.name = name;
            this.kids = new HashSet<LetterGroup>();
            this.numWords = 0;
            this.clue = null;
    }

    public LetterGroup(String name, Clue clue){
            this.name = name;
            this.kids = new HashSet<LetterGroup>();
            this.numWords = 0;
            this.clue = clue;
    }

    public LetterGroup(String name, HashSet<LetterGroup> kids){
            this.name = name;
            this.kids = kids;
            this.numWords = 0;
            this.clue = null;
    }

    public LetterGroup topKid(){
        Iterator<LetterGroup> it = this.kids.iterator();
        LetterGroup topKid = it.next();
        while(it.hasNext()){
            LetterGroup current = it.next();
            if (current.numWords > topKid.numWords){
                topKid = current;
            }
        }
        return topKid;
    }
    
    public String firstPair(){
        String[] pairs = this.name.split("/");
        return pairs[0].charAt(0) + "/" + pairs[1].charAt(0);
    }

    public String lastPair(){
        String[] pairs = this.name.split("/");
        return pairs[0].charAt(pairs[0].length() - 1) + "/" + pairs[1].charAt(pairs[0].length() - 1);
    }

    
}
