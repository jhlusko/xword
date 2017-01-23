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
        int count;
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
                this.count = 0;
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
                
                System.out.println("Answers for Across:");
		for (Entry e : entries){
                    if (e.isHorizontal){
                        System.out.println(e.wordGroup.words.get(0) + "/" + e.wordGroup.words.get(1));
                    }
                }
                System.out.println("Answers for Down:");
		for (Entry e : entries){
                    if (!e.isHorizontal){
                        System.out.println(e.wordGroup.words.get(0) + "/" + e.wordGroup.words.get(1));
                    }
                }
		
		
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
				String title = num + "-Square-Crossword-" + time;
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
        

        for (int i = 0; i < p.horizontalWords.size(); i++){
            if (i > 0){
                if (p.horizontalWords.get(i).length() > p.horizontalWords.get(i - 1).length()){
                    return;
                }
            }
        }
        
        
        
        if (p.verticalWords.size() == 3 ){
            this.count++;
            if (count % 1000 == 0){
                System.out.println(count);
            }
        }
        
        if (p.verticalWords.size() > 3 ){
            completePuzzle(p);
        }
        
        
        int numRows = p.horizontalWords.size();
        int numColumns = p.verticalWords.size();
        String lastVerticalWord = p.verticalWords.get(numColumns - 1);
        int lastVerticalLength = (lastVerticalWord.length() - 1)/2;
        String lastHorizontalWord = p.horizontalWords.get(numRows - 1);
        int hLength = (lastHorizontalWord.length() - 1)/2;
        
        if (numRows == numColumns){
            //complete last row
            if (hLength < numColumns + 1){
                fillSquare(p, hLength - 1, numRows - 1);
            }
            
            //add new column
            else{
                if (p.verticalWords.size() % 2 == 0){
                    p.verticalWords.add("*/*");
                }
                else {
                    p.verticalWords.add("-/-");
                }
                buildPuzzle(p);
            }
        }
        else{
            //complete last column
            String penultimateVerticalWord = p.verticalWords.get(numColumns - 2);
            int penultimateVerticalLength = (penultimateVerticalWord.length() - 1)/2;
            if (lastVerticalLength < numRows + 1 && penultimateVerticalLength == numRows + 1){
                fillSquare(p, numColumns - 1, lastVerticalLength - 1);
            }

            //complete last row
            if (hLength < numColumns + 1){
                fillSquare(p, hLength - 1, numRows - 1);
            }
            //add new row
            else{
                    if (p.horizontalWords.size() % 2 == 0){
                        p.horizontalWords.add("*/*");
                    }
                    else {
                        p.horizontalWords.add("-/-");
                    }
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
            for (LetterGroup firstGrandChild: child.rightKids){
                for (LetterGroup secondGrandChild: child.rightKids){
                    if (firstGrandChild != secondGrandChild){
                        ArrayList<String> horizontalWords = new ArrayList<String>();
                        horizontalWords.add(firstGrandChild.name);
                        horizontalWords.add("-" + secondGrandChild.name.charAt(2) + "/-" + secondGrandChild.name.charAt(6));
                        ArrayList<String> verticalWords = new ArrayList<String>();
                        verticalWords.add(secondGrandChild.name);
                        verticalWords.add("-" + firstGrandChild.name.charAt(2) + "/-" + firstGrandChild.name.charAt(6));
                        cw.buildPuzzle(new Puzzle(verticalWords, horizontalWords));
                    }
                }
            }
        }
        
        System.out.println(cw.puzzles.size());
    } 

    void completePuzzle(Puzzle p) {
        this.clear();
        for (int i = 0; i < p.horizontalWords.size(); i++){
            if (i % 2 == 0){
                this.addEntry(new Entry(this.index.get(p.horizontalWords.get(i)).clue, new Point (0, i), 0, true));
            }
            else{
                String stub = p.horizontalWords.get(i).replace("-", "");
                if (stub.equals("/")){
                    return;
                }
                String parts[] = stub.split("/");
                LetterGroup word = this.index.get(parts[0] + "*/" + parts[1] + "*");
                if(word == null){
                    return;
                }
                int offset = word.clue.words.get(0).indexOf(parts[0]);
                this.addEntry(new Entry(word.clue, new Point (0, i), offset, true));                    
                
            }
        }
        for (int i = 0; i < p.verticalWords.size(); i++){
            if (i % 2 == 0){
                this.addEntry(new Entry(this.index.get(p.verticalWords.get(i)).clue, new Point (i, 0), 0, false));
            }
            else{
                String stub = p.verticalWords.get(i).replace("-", "");
                if (stub.equals("/")){
                    return;
                }
                String parts[] = stub.split("/");
                LetterGroup word = this.index.get(parts[0] + "*/" + parts[1] + "*");
                if(word == null){
                    return;
                }
                int offset = word.clue.words.get(0).indexOf(parts[0]);
                this.addEntry(new Entry(word.clue, new Point (i, 0), offset, false));
            }
        }
        confirmExport();
    }
    
    public void confirmExport() {
        this.display();
        System.out.println("Export crossword? (y/n)");
        String input = this.scanner.next();
        if (input.equals("y")) {
            System.out.println("Exporting crossword!");
            try {
                this.export();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("??? your input: " + input);
        }
    }

    private String extendWord(String word, String letterPair) {
        String[] parts = word.split("/");
        return parts[0] + letterPair.charAt(0) + "/" + parts[1] + letterPair.charAt(2);
    }

    private void fillSquare(Puzzle p, int column, int row) {
        String verticalWord = p.verticalWords.get(column).replace("-", "");
        String horizontalWord = p.horizontalWords.get(row).replace("-", "");
        HashSet<String> intersection = new HashSet<String>();
        if (verticalWord.equals("/")){
            intersection = this.index.get(horizontalWord).getRightLetterPairs();
        }
        else if (horizontalWord.equals("/")){
            intersection = this.index.get(verticalWord).getRightLetterPairs();
        }
        else{
            HashSet<String> verticalOptions = this.index.get(verticalWord).getRightLetterPairs();
            HashSet<String> horizontalOptions = this.index.get(horizontalWord).getRightLetterPairs();
            verticalOptions.retainAll(horizontalOptions);
            intersection = verticalOptions;
        }

        for (String letterPair: intersection){
            ArrayList<String> vertCopy = new ArrayList<String>(p.verticalWords);
            ArrayList<String> horCopy = new ArrayList<String>(p.horizontalWords);
            vertCopy.set(column, extendWord(p.verticalWords.get(column), letterPair));
            horCopy.set(row, extendWord(p.horizontalWords.get(row), letterPair));
            if (letterPair.equals("*/*")){
//                completePuzzle(p);
            }
            buildPuzzle(new Puzzle(vertCopy, horCopy));
        }
    }

    
}