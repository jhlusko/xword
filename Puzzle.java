/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dictionary;

import java.util.ArrayList;

/**
 *
 * @author jamie
 */
class Puzzle {
    
    public ArrayList<LetterGroup> verticalWords;
    public ArrayList<LetterGroup> horizontalWords;

    
    public Puzzle(ArrayList<LetterGroup> verticalWords, ArrayList<LetterGroup> horizontalWords){
        this.verticalWords = verticalWords;
        this.horizontalWords = horizontalWords;
        
    }

    void printPuzzle() {
        for (LetterGroup word: this.horizontalWords){
            System.out.println(word.name);
        }
        System.out.println();
    }
        
}
