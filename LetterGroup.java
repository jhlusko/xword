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
        //no "/"
        String[] parts = this.name.split("/");
        return parts[0].charAt(parts[0].length() - 1) + "" + parts[1].charAt(parts[0].length() - 1);
    }
    
    public String getLetterPairAt(int index, boolean starred, boolean slash){
        if (starred){
            String[] parts = this.name.split("/");
            return parts[0].charAt(index) + "/" + parts[1].charAt(index);
        }
        else{
            String[] parts = this.name.replace("*", "").split("/");
            if (slash){
                return parts[0].charAt(index) + "/" + parts[1].charAt(index);
            }
            else{
                return parts[0].charAt(index) + "" + parts[1].charAt(index);
            }
        }
    }
    
    public ArrayList<String> getLeftLetterPairs(){
        ArrayList<String> leftLetterPairs = new ArrayList<>();
        for (LetterGroup child: this.leftKids){
            leftLetterPairs.add(child.getLetterPairAt(0, true, true));
        }
        return leftLetterPairs;
    }

    public ArrayList<String> getRightLetterPairs(){
        ArrayList<String> rightLetterPairs = new ArrayList<>();
        for (LetterGroup child: this.rightKids){
            String[] parts = child.name.split("/");
            rightLetterPairs.add(child.getLetterPairAt(parts[0].length() - 1, true, true));
        }
        return rightLetterPairs;
    }

 
}
