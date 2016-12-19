package dictionary;

import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Crossword {
	
	
	private static final int WORD_LENGTH = 9;

    
	HashMap<String, LetterGroup> index;
	HashMap<Point, String> squares;
        ArrayList<Puzzle> puzzles;
        HashMap<Integer, ArrayList<Point>> rows;
	HashMap<Integer, ArrayList<Point>> columns;
	ArrayList<Entry> entries;
	int xMin;
	int xMax;
	int yMin;
	int yMax;
	ArrayList<Clue> dictionary;
	ArrayList<Point>badOrigins;
	Scanner scanner;

        public Crossword(ArrayList<Clue> wordPairs){
		this.dictionary = wordPairs;
                this.index = new HashMap<String, LetterGroup>();
		this.squares = new HashMap<Point, String>();
		this.puzzles = new ArrayList<Puzzle>();
                this.rows = new HashMap<Integer, ArrayList<Point>>();
		this.columns = new HashMap<Integer, ArrayList<Point>>();
		this.entries = new ArrayList<Entry>();
		this.xMin = 0;
		this.xMax = 0;
		this.yMin = 0;
		this.yMax = 0;
		this.badOrigins = new ArrayList<Point>();
		this.scanner = new Scanner (System.in);
	}
	
	public static  IDictionary getDictionary () throws IOException {
		// construct the URL to the Wordnet dictionary directory
		URL url = new URL ( "file" , null , "/usr/share/wordnet" ) ;
		// construct the dictionary object and open it
		IDictionary dict = new Dictionary ( url ) ;
		dict.open();
		return dict;
	}

	public static void addWordsToDict (Clue wordGroup, java.util.List<IWord> words, int wordLength) {
		for (Iterator <IWord> i = words.iterator(); i.hasNext();) {
			String word = i.next().getLemma();
			if (word.length() < wordLength){
				wordGroup.add(word);
			}
		}
	}

	public void addRelatedWords (Clue wordGroup, IDictionary dict, ISynset synset, Pointer pointerType) {
		java.util.List<ISynsetID> hypernyms = synset.getRelatedSynsets(pointerType ) ;
		for ( ISynsetID sid : hypernyms ) {
			addWordsToDict(wordGroup, dict.getSynset(sid).getWords(), WORD_LENGTH);
		}
	}

	public static ArrayList<Clue> getEqualLengthDict() throws IOException{
		IDictionary dict = getDictionary();
		
		ArrayList<Clue> synDict = new ArrayList<Clue>();
		ArrayList<Clue> equalLengthDict = new ArrayList<Clue>();
		
		int index = 0;
		for (Iterator <ISynset> i = dict.getSynsetIterator(POS.NOUN); i.hasNext();){
//			System.out.println(index);
//			System.out.print("Synsets: ");
			ISynset synset = i.next();
			
			synDict.add(new Clue(synset.getGloss()));
			
			addWordsToDict(synDict.get(index), synset.getWords(), WORD_LENGTH);
			//addRelatedWords(synDict.get(index), dict, synset, Pointer.HYPONYM);
			//addRelatedWords(synDict.get(index), dict, synset, Pointer.HYPERNYM);
			index ++;
		}
		for (Clue w : synDict){
			Collections.sort(w.words, new SortByWordLength());
			//System.out.println(w.words.toString());
			while (w.words.size() != 0){
				if (w.words.size() == 1){
					w.words.remove(0);
					break;
				}
				int length = w.words.get(0).length();
				int index1 = 0;
				java.util.ArrayList<String> newWords = new ArrayList<String>();
				//if second word is different length remove 1st word
				if (w.words.size() > 1 && length != w.words.get(++index1).length()){
					w.words.remove(0);
				}
				//if second word is of same length, check following words
				else{
					//while there are more words
					while(w.words.size() > ++index1){
						//check those words are the same length
						if (length != w.words.get(index1).length()){
							//once you reach a diff-length word, end search
							break;
						}
					}
					//while there are more equal-length words
					while (--index1 != -1){
						newWords.add(w.words.remove(index1));
	    			}
	    		Clue newWordGroup = new Clue(w.clue, newWords);
//				System.out.println("new words:");
//	    		newWordGroup.printInfo();
				equalLengthDict.add(newWordGroup);
				}
			}
		}
		return equalLengthDict;
	}

	public static ArrayList<Clue> getWordPairs(ArrayList<Clue> equalLengthDict){
		ArrayList<Clue> wordPairs = new ArrayList<Clue>();
		for (Clue w : equalLengthDict){
			int firstWordIndex = 0;
			int secondWordIndex = 1;
			while (firstWordIndex < w.words.size()){
	    		while (secondWordIndex < w.words.size()){
	    			wordPairs.add(new Clue(w.clue, new ArrayList<String>(Arrays.asList(w.words.get(firstWordIndex), w.words.get(secondWordIndex)))));
	    			wordPairs.add(new Clue(w.clue, new ArrayList<String>(Arrays.asList(w.words.get(secondWordIndex), w.words.get(firstWordIndex)))));
	    			secondWordIndex++;
	    		}
	    		firstWordIndex++;
	    		secondWordIndex = firstWordIndex + 1;
			}
		}
		return wordPairs;
	}

	public static java.util.ArrayList<Clue> sanitize(ArrayList<Clue> equalLengthDict){
		for (Clue wordGroup : equalLengthDict){
			for (int i = 0 ; i < wordGroup.words.size(); i++){
				Pattern p = Pattern.compile("[0-9]");
				Matcher m = p.matcher(wordGroup.words.get(i));
				if (m.find()){
					wordGroup.words.remove(i);
					continue;
				}
				wordGroup.words.set(i, wordGroup.words.get(i).toUpperCase());
				wordGroup.words.set(i, wordGroup.words.get(i).replaceAll("[^A-Z]", "+"));
			}
		}
		return equalLengthDict;
	}

	
	public void addEntry(Entry e){
            for (int i = 0; i< e.wordGroup.words.get(0).length(); i++){
                Point point = new Point(e.point.x + i*((e.isHorizontal)?1:0) - e.offset*((e.isHorizontal)?1:0), e.point.y + i*((e.isHorizontal)?0:1) - e.offset*((e.isHorizontal)?0:1));
                String letterPair = "" + e.wordGroup.words.get(0).charAt(i) + e.wordGroup.words.get(1).charAt(i);
                addSquare(point, letterPair);
            }
            entries.add(e);
            this.dictionary.remove(e.wordGroup);
	}

        public void addSquare(Point point, String letterPair){
            squares.put(point, letterPair);
            if (rows.get(point.y) != null){
                rows.get(point.y).add(point); 
            } else {
                rows.put(point.y, new ArrayList<Point>(Arrays.asList(point)));
            }
            if (columns.get(point.x) != null){
                columns.get(point.x).add(point); 
            } else {
                columns.put(point.x, new ArrayList<Point>(Arrays.asList(point)));
            }

            if (point.x < xMin){
                xMin = point.x;
            }
            if (point.x > xMax){
                xMax = point.x;
            }
            if (point.y < yMin){
                yMin = point.y;
            }
            if (point.y > yMax){
                yMax = point.y;
            }
        }
		
	public void printOne(int num){
		System.out.print("..");
		int i = 1;
		for (int x = xMin; x <= xMax; x++){
			System.out.print(i%10);
			i++;
		}
		System.out.println();
		System.out.print("..");
		for (int x = xMin; x <= xMax; x++){
			System.out.print(Math.abs(x)%10);
		}
		i = 1;
		System.out.println();
		for (int y = yMin; y <= yMax; y++){
			System.out.print(i%10);
			i++;
			System.out.print(Math.abs(y)%10);
			for (int x = xMin; x <= xMax; x++){
				if (squares.get(new Point(x, y)) != null){
					System.out.print(squares.get(new Point(x, y)).charAt(num));
				}
				else{
					System.out.print('.');
				}
			}
			System.out.println();
		}
		
	}
	
	public void printClues(String orientation){
		int count = 1;
		int yoffset = (this.yMin == 0) ? 1 : -1*this.yMin+1;
		int xoffset = (this.xMin == 0) ? 1 : -1*this.xMin+1;
		for (Entry e : entries){
			if (orientation == "horizontal"){
				if (e.isHorizontal){
					System.out.println(count + ". [" + String.valueOf(e.point.x - e.offset) + ", " + e.point.y + "]" + 
				" [" + e.origin.x + ", " + e.origin.y + "] " +
				"[" + String.valueOf(e.origin.x + xoffset) + ", " + String.valueOf(e.origin.y + yoffset)+ "] "  + e.wordGroup.clue);
					count++;
				}
			}
			if (orientation == "vertical"){
				if (!e.isHorizontal){
					System.out.println(count + ". [" + e.point.x + ", " + String.valueOf(e.point.y - e.offset) + "]" + 
				" [" + e.origin.x + ", " + e.origin.y + "] " +
				"[" + String.valueOf(e.origin.x + xoffset) + ", " + String.valueOf(e.origin.y + yoffset)+ "] "  + e.wordGroup.clue);
					count++;
				}
			}
		}
	}
	
	public void display(){
		System.out.println("First Crossword:");
		System.out.println();
		printOne(0);
		System.out.println();
		
		System.out.println("Second Crossword:");
		System.out.println();
		printOne(1);
		System.out.println();
		
		System.out.println("Across:");
		printClues("horizontal");
		System.out.println("Down:");
		printClues("vertical");
		
		
	}

	
	public static ArrayList<Clue> checkDifferent(ArrayList<Clue> wordPairs) {
		ArrayList<Clue> diffWords = new ArrayList<Clue>();
		wordloop:
		for (Clue w : wordPairs){
			int num_diff_letters = 0;
			for (int i = 0; i < w.words.get(0).length(); i++){
				if (w.words.get(0).charAt(i) != w.words.get(1).charAt(i)){
					num_diff_letters++;
					if (num_diff_letters > 2){
						diffWords.add(w);
						continue wordloop;
					}
				}
			}
		}
		return diffWords;
	}
	
	public void export() throws IOException {
		String time = String.valueOf(new Date().getTime());
		for (int num = 0; num < 2; num++){
			BufferedWriter writer;
			try {
//				File file = new File("/home/jamie/Downloads/xwords.xml");
				String title = num + "-Double-Crossword-" + time;
				File file = new File("/home/jamie/Downloads/" + title + ".xml");
				writer = new BufferedWriter(new FileWriter(file));
				writer.write("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
				writer.write("<Puzzles Version=\"1.0\">\n");
				writer.write("<Puzzle>\n");
				writer.write("<Title>"+title+"</Title>\n");
				writer.write("<Author>Jamie Hlusko</Author>\n");
				writer.write("<Size>\n");
				writer.write("<Rows>");
				writer.write(String.valueOf(this.yMax - this.yMin + 1));
				writer.write("</Rows>");
				writer.write("<Cols>");
				writer.write(String.valueOf(this.xMax - this.xMin + 1));
				writer.write("</Cols>\n");
				writer.write("</Size>\n");
				writer.write("<Grid>\n");
				for (int y = yMin; y <= yMax; y++){
					writer.write("<Row>");
					for (int x = xMin; x <= xMax; x++){
						if (squares.get(new Point(x, y)) != null){
							writer.write(squares.get(new Point(x, y)).charAt(num));
						}
						else{
							writer.write(".");
						}
					}
					writer.write("</Row>\n");
				}
				writer.write("</Grid>\n");
				writer.write("<Clues>\n");
				
				List<Entry> across = new ArrayList<Entry>();
				List<Entry> down = new ArrayList<Entry>();
				for (Entry e : this.entries){
					if (e.isHorizontal){
						across.add(e);
					}
					else{
						down.add(e);
					}
				}
				Comparator<Entry> sortEntries = new Comparator<Entry>(){
					public int compare(Entry e1, Entry e2){
						if (e1.point.y == e2.point.y){
							return Integer.compare(e1.point.x, e2.point.x);
						}
						else{
							return Integer.compare(e1.point.y, e2.point.y);
						}
					}
				};
				
				int yoffset = (this.yMin == 0) ? 1 : -1*this.yMin+1;
				int xoffset = (this.xMin == 0) ? 1 : -1*this.xMin+1;
				HashMap<Point, Integer> clueOrigins = new HashMap<Point, Integer>();
				int clueSeq = 0;
				int clueNum;
				Collections.sort(across, sortEntries);
				for (Entry e: across){
					if (clueOrigins.get(new Point(e.origin.x + xoffset, e.origin.y + yoffset)) != null){
						clueNum =  clueOrigins.get(new Point(e.origin.x + xoffset, e.origin.y + yoffset));
					}
					else{
						clueNum = ++clueSeq;
						clueOrigins.put(new Point(e.origin.x + xoffset, e.origin.y + yoffset), new Integer(clueNum));
					}
					writer.write("<Clue Row=\"" + String.valueOf(e.origin.y + yoffset) + "\" " + "" +
							"Col=\"" + String.valueOf(e.origin.x + xoffset) + "\" " +
							"Num=\"" + String.valueOf(clueNum) + "\" " +
							"Dir=\"Across\" " + 
							"Ans=\"" + e.wordGroup.words.get(num).toUpperCase() + "\">" + 
							e.wordGroup.clue + "</Clue>\n");
				}
				Collections.sort(down, sortEntries);
				for (Entry e: down){
					if (clueOrigins.get(new Point(e.origin.x + xoffset, e.origin.y + yoffset)) != null){
						clueNum =  clueOrigins.get(new Point(e.origin.x + xoffset, e.origin.y + yoffset));
					}
					else{
						clueNum = ++clueSeq;
						clueOrigins.put(new Point(e.origin.x + xoffset, e.origin.y + yoffset), new Integer(clueNum));
					}
					writer.write("<Clue Row=\"" + String.valueOf(e.origin.y + yoffset) + "\" " + "" +
							"Col=\"" + String.valueOf(e.origin.x + xoffset) + "\" " +
							"Num=\"" + String.valueOf(clueNum) + "\" " +
							"Dir=\"Down\" " + 
							"Ans=\"" + e.wordGroup.words.get(num).toUpperCase() + "\">" + 
							e.wordGroup.clue + "</Clue>\n");
				}
				writer.write("</Clues>");
				writer.write("</Puzzle>");
				writer.write("</Puzzles>");
				
				writer.flush();
		        writer.close();
				
			} catch (IOException e3) {
				e3.printStackTrace();
			}
			
			
		}
		FileOutputStream fileOut;
		fileOut = new FileOutputStream("/home/jamie/Downloads/wordPairs.ser");
		ObjectOutputStream out = new ObjectOutputStream(fileOut);
		out.writeObject(this.dictionary);
		out.close();
		fileOut.close();
		this.display();
		System.exit(0);
		
	}

	void buildIndex() {
		for (Clue clue:this.dictionary){
			buildClueIndex(clue);
		}
	}

	private void buildClueIndex(Clue clue) {
            //creates leaf
            String fullName = "*" + clue.words.get(0) + "*/*" + clue.words.get(1) + "*";
            LetterGroup leaf = new LetterGroup(fullName, clue);
            this.index.put(fullName, leaf);
            
            //create tree
            build_tree(leaf);
            		
	}

    private void build_tree(LetterGroup child) {
        if (child.name.length() ==3){
            return;
        }
        
        String[] parts = child.name.split("/");
        String leftParentName = parts[0].substring(0, parts[0].length() - 1) 
                + "/" + parts[1].substring(0, parts[1].length() - 1);
        String rightParentName = parts[0].substring(1, parts[0].length()) 
                + "/" + parts[1].substring(1, parts[1].length());
        if (this.index.get(leftParentName) == null){
            LetterGroup leftParent = new LetterGroup(leftParentName, child.clue);
            leftParent.rightKids.add(child);
            this.index.put(leftParent.name, leftParent);
        }
        else{
            this.index.get(leftParentName).rightKids.add(child);
        }
        build_tree(this.index.get(leftParentName));
        if (this.index.get(rightParentName) == null){
            LetterGroup rightParent = new LetterGroup(rightParentName, child.clue);
            rightParent.leftKids.add(child);
            this.index.put(rightParentName, rightParent);
        }
        else{
            this.index.get(rightParentName).leftKids.add(child);
        }
        build_tree(this.index.get(rightParentName));
        
        
    }

    void buildPuzzle(Puzzle p) {
        
        if (p.horizontalWords.size() > 3){
            p.printPuzzle();
        }
        
        int column;
        int row = p.horizontalWords.size() - 1;
        for (column = 0; column < p.verticalWords.size() ; column++){
            if (p.verticalWords.get(column).numLetters() < p.horizontalWords.size() + 1){ //+1 for the first column of all ****
                row = p.verticalWords.get(column).numLetters() - 1;
                break;
            }
        }
        if (column < p.verticalWords.size()){
            HashSet<String> verticalOptions = p.verticalWords.get(column).getRightLetterPairs();
            HashSet<String> horizontalOptions = p.horizontalWords.get(row).getRightLetterPairs();
            verticalOptions.retainAll(horizontalOptions);
            for (String letterPair: verticalOptions){
                if (letterPair.equals("*/*")){
                    p.printPuzzle();
                }
                ArrayList<LetterGroup> vertCopy = new ArrayList<LetterGroup>(p.verticalWords);
                ArrayList<LetterGroup> horCopy = new ArrayList<LetterGroup>(p.horizontalWords);
                vertCopy.set(column, p.verticalWords.get(column).extend(letterPair));
                horCopy.set(row, p.horizontalWords.get(row).extend(letterPair));
                buildPuzzle(new Puzzle(vertCopy, horCopy));
            }
        }
        else{
            if (p.horizontalWords.size() == p.verticalWords.size()){
                p.verticalWords.add(this.index.get("*/*"));
                buildPuzzle(p);
            }
            else{
                p.horizontalWords.add(this.index.get("*/*"));
                buildPuzzle(p);
            }
        }
    }
    

    private void clear() {
        this.squares = new HashMap<Point, String>();
        this.rows = new HashMap<Integer, ArrayList<Point>>();
        this.columns = new HashMap<Integer, ArrayList<Point>>();
        this.entries = new ArrayList<Entry>();
        this.xMin = 0;
        this.xMax = 0;
        this.yMin = 0;
        this.yMax = 0;
    }
    
        
    public static void main(String[] args) throws IOException, ClassNotFoundException, Exception {
        ArrayList<Clue> wordPairs = checkDifferent(getWordPairs(sanitize(getEqualLengthDict())));
        Crossword cw = new Crossword(wordPairs);
        cw.buildIndex();
//        LetterGroup test = cw.index.get("*I/*O");
        for (LetterGroup child: cw.index.get("*/*").rightKids){
            ArrayList<LetterGroup> initialLetterGroup = new ArrayList<LetterGroup>();
            initialLetterGroup.add(child);
            cw.buildPuzzle(new Puzzle(new ArrayList<LetterGroup>(initialLetterGroup), new ArrayList<LetterGroup>(initialLetterGroup)));
        }
        System.out.println(cw.puzzles.size());
        for (Puzzle p : cw.puzzles){
//            System.out.println(p.leftWord.name);
//            System.out.println(p.rightWord.name);
//            System.out.println();
            cw.completePuzzle(p);
        }
        System.out.println(cw.puzzles.size());
    } 

    void completePuzzle(Puzzle p) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
}