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
    
    public ArrayList<String> verticalWords;
    public ArrayList<String> horizontalWords;

    
    public Puzzle(ArrayList<String> verticalWords, ArrayList<String> horizontalWords){
        this.verticalWords = verticalWords;
        this.horizontalWords = horizontalWords;
        
    }

    void printPuzzle() {
        for (String word: this.horizontalWords){
            System.out.println(word);
        }
        System.out.println();
    }
        
}
