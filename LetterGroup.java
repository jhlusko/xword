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
    public Clue clue;

    
    public LetterGroup(String name, Clue clue){
        this.name = name;
        this.leftKids = new HashSet<LetterGroup>();
        this.rightKids = new HashSet<LetterGroup>();
        this.clue = clue;
    }

    public String getLetterPairAt(int index, boolean starred, boolean slash){
        if (starred){
            String[] parts = this.name.split("/");
            if (slash){
                return parts[0].charAt(index) + "/" + parts[1].charAt(index);

            }
            else{
                return parts[0].charAt(index) + "" + parts[1].charAt(index);                
            }
        }
        else{
            String[] parts = this.name.replace("*", "").split("/");
            parts[0] = parts[0] + "*";
            parts[1] = parts[1] + "*";
            if (slash){
                return parts[0].charAt(index) + "/" + parts[1].charAt(index);
            }
            else{
                return parts[0].charAt(index) + "" + parts[1].charAt(index);
            }
        }
    }
    
    public HashSet<String> getLeftLetterPairs(){
        HashSet<String> leftLetterPairs = new HashSet<>();
        for (LetterGroup child: this.leftKids){
            leftLetterPairs.add(child.getLetterPairAt(0, true, true));
        }
        return leftLetterPairs;
    }

    public HashSet<String> getRightLetterPairs(){
        HashSet<String> rightLetterPairs = new HashSet<>();
        for (LetterGroup child: this.rightKids){
            String[] parts = child.name.split("/");
            rightLetterPairs.add(child.getLetterPairAt(parts[0].length() - 1, true, true));
        }
        return rightLetterPairs;
    }
    
    public int numLetters(){
        String[] parts = this.name.split("/");
        return parts[0].length();
    }

}
