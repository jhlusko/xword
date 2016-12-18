/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dictionary;

/**
 *
 * @author jamie
 */
class Puzzle {
    
    public LetterGroup leftWord;
    public LetterGroup rightWord;

    
    public Puzzle(LetterGroup leftWord, LetterGroup rightWord){
        this.leftWord = leftWord;
        this.rightWord = rightWord;
        
    }
        
}
