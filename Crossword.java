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
	
	
	private static final int WORD_LENGTH = 5;
	HashMap<String, LetterGroup> index;
	HashMap<Point, String> squares;
        HashMap<Point, HashSet<LetterGroup>> candidates;
        ArrayList<String> enders;
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
		this.candidates = new HashMap<Point, HashSet<LetterGroup>>();
                this.enders = new ArrayList<String>();
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

	private void buildIndex() {
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
            
            //add word to parents in tree (done here instead of recursive function to avoid doubling overlapping parents)
            String[] parts = fullName.split("/");
            for (int j = 1; j <= parts[0].length(); j++){
                for (int i = 0; i <= parts[0].length() - j  ;i++){
                    this.index.get(parts[0].substring(i, i+j) + "/" 
                            + parts[1].substring(i, i+j)).numWords++;
                }
            }
		
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

    private void buildPuzzle() {
        //if first time, add "*/*" at (0, -1)
        if (this.candidates.size() == 0){
            HashSet<LetterGroup> options = new HashSet<LetterGroup>();
            options.add(this.index.get("*/*"));
            this.candidates.put(new Point(0, -1), options);
        }
        
        //find current (empty) row
        int workingRow = 0;
        while (this.candidates.containsKey(new Point(0, workingRow))){
            workingRow++;
        }

        //if word can't be extended, finish puzzle
        HashSet<LetterGroup> optionsForExtension = getOptions(this.candidates.get(new Point(0, workingRow - 1)));
        if (optionsForExtension.isEmpty()){
            System.out.println("no candidate at (0, " + workingRow + "), moving to completePuzzle");
            this.candidates.remove(new Point(0, workingRow - 1));
            this.candidates.remove(new Point(1, workingRow - 1));
            this.completePuzzle();
            return;
        }

        //create new lowest candidate in 0 column
        HashSet<LetterGroup> options = getOptions(this.candidates.get(new Point(0, workingRow - 1)));
        //even rows are the start of a horizontal word
        HashSet<LetterGroup> refinedOptions = new HashSet<>();
        if (workingRow % 2 == 0){
            for (LetterGroup option : options){
                if (this.index.get("*" + option.lastLetters.charAt(0) + "/*" + option.lastLetters.charAt(1)) != null){
                    refinedOptions.add(option);
                }
            }
        }
        else{
            for (LetterGroup option : options){
                if (this.index.get(option.lastLetters.charAt(0) + "*/" + option.lastLetters.charAt(1)) + "*" != null){
                    refinedOptions.add(option);
                }
            }
        }
        this.candidates.put(new Point(0, workingRow), refinedOptions);
        printOptions(new Point(0, workingRow));

        //create complement in "1" column
        if (workingRow == 0){
            this.candidates.put(new Point(1, 0), getOptions(this.candidates.get(new Point(0, 0))));
            printOptions(new Point(1, 0));

        }
        //find all letterPairs which could extend it vertically
        else{
            HashSet<String> verticalPairs = new HashSet<String>();
            for (LetterGroup parentOption: this.candidates.get(new Point(1, workingRow-1))){
                for (LetterGroup currentOption: parentOption.rightKids){
                    verticalPairs.add(currentOption.lastLetters);
                }
            }
            //find the subset of matching letterPairs which could extend it horizontally, remove nonmatches
            HashSet<String> intersectionPairs = new HashSet<>();
            refinedOptions = new HashSet<>();
            for (LetterGroup parentOption: this.candidates.get(new Point(0, workingRow))){
                for (LetterGroup currentOption: parentOption.rightKids){
                    if(verticalPairs.contains(currentOption.lastLetters)){
                        String[] parts = currentOption.name.replace("*", "").split("/");
                        //odd rows are the end of a horizontal word
                        if (workingRow % 2 == 1){
                            if (this.index.get(parts[0] + "*/" + parts[1] + "*") != null){
                                intersectionPairs.add(currentOption.lastLetters);
                                refinedOptions.add(parentOption);
                                break;
                            }
                        }
                        else{
                            intersectionPairs.add(currentOption.lastLetters);
                            refinedOptions.add(parentOption);
                            break;
                        }
                    }
                }
            }
            this.candidates.put(new Point(0, workingRow), refinedOptions);
            System.out.print("refined: ");
            printOptions(new Point(0, workingRow));

            this.candidates.put(new Point(1, workingRow), getOptions(this.candidates.get(new Point(1, workingRow-1))));
            printOptions(new Point(1, workingRow));
            System.out.println();
            

        }
        
        buildPuzzle();
    }

    private void completePuzzle() {
        //get workingRow
        int lowestFullRow = 0;
        while (this.candidates.containsKey(new Point(0, lowestFullRow + 1)) && this.candidates.containsKey(new Point(1, lowestFullRow + 1))){
            lowestFullRow++;
        }
        System.out.println("this.candidates.get(new Point(0, lowestFullRow)).size(): " + this.candidates.get(new Point(0, lowestFullRow)).size());
        int endingColumn = (this.candidates.get(new Point(0, lowestFullRow)).size() < this.candidates.get(new Point(1, lowestFullRow)).size()) ? 0 : 1;
        int workingColumn = (endingColumn + 1) % 2;
        HashSet<LetterGroup> workingOptions = new HashSet<>();
        
        //add ending word to puzzle
        Iterator<LetterGroup> iter = this.candidates.get(new Point(endingColumn, lowestFullRow)).iterator();
        while (iter.hasNext()){
            if (this.entries.size() > 1){
                    break;
                }
            LetterGroup word = iter.next();
            this.clear();
            this.addEntry(new Entry(word.clue, new Point(endingColumn,0), 0, false));
            
            System.out.println("options: " + this.candidates.get(new Point(workingColumn, lowestFullRow)).size());
            
            //find letterPair options for working column, from bottom to top
           option_search:
           for (LetterGroup option: this.candidates.get(new Point(workingColumn, lowestFullRow))){
                int workingRow = 0;
                while (workingRow <= lowestFullRow){
                    //check if workingColumn has a match for endingColumn
                    ArrayList <String> extendedLetterPairOptions = new ArrayList<>();
                    if (endingColumn == 0){
                        extendedLetterPairOptions = this.index.get(option.getLetterPairAt(workingRow, false, true)).getRightLetterPairs();
                    }
                    else{
                        extendedLetterPairOptions = this.index.get(option.getLetterPairAt(workingRow, false, true)).getLeftLetterPairs();
                    }
                    String letterPairToMatch = this.squares.get(new Point(endingColumn, workingRow)).charAt(0) + "/" +
                            this.squares.get(new Point(endingColumn, workingRow)).charAt(1);
                    if (extendedLetterPairOptions.contains(letterPairToMatch)){
                        System.out.println("MATCH! ON " + workingRow);
                        workingRow++;
                    }
                    else{
                        continue option_search;
                    }
                }
                workingOptions.add(option);
           }
           System.out.println("workingOptions.size(): " +workingOptions.size());
        }
    }
                    
                    
//                    if (workingRow % 2 == 0){
//                        LetterGroup word = this.index.get("*" + this.squares.get(new Point(0, workingRow)).charAt(0) + 
//                            this.squares.get(new Point(1, workingRow)).charAt(0) + "/*" +
//                            this.squares.get(new Point(0, workingRow)).charAt(1) + 
//                            this.squares.get(new Point(1, workingRow)).charAt(1));
//                        if (word != null){
//                            this.addEntry(new Entry(word.clue, new Point(0, workingRow), 0, true));
//                        }
//                    }
//                    else{
//                        LetterGroup word = this.index.get(this.squares.get(new Point(0, workingRow)).charAt(0) + 
//                            this.squares.get(new Point(1, workingRow)).charAt(0) + "*/" +
//                            this.squares.get(new Point(0, workingRow)).charAt(1) + 
//                            this.squares.get(new Point(1, workingRow)).charAt(1) + "*");
//                        if (word != null){
//                            this.addEntry(new Entry(word.clue, new Point(1, workingRow), word.name.length(), true));
//                        }
//                    }
//                    workingRow++;
//                }
//                if (this.entries.size() > 1){
//                    break;
//                }
//            }
//        }
//        this.display();
//    }

    private void printOptions(Point point) {
        System.out.println(this.candidates.get(point).size() + " options at ("+point.x +", "+point.y+")");
        if(this.candidates.get(point).size() == 1){
            for (LetterGroup lg : this.candidates.get(point)){
                System.out.println(lg.name + "is last option at ("+point.x +", "+point.y+")");
            }
        }
    }

    private HashSet<LetterGroup> getOptions(HashSet<LetterGroup> parentOptions) {
        HashSet<LetterGroup> options = new HashSet<LetterGroup>();
        for (LetterGroup parentOption: parentOptions){
            for (LetterGroup currentOption: parentOption.rightKids){
//                if (currentOption.name.endsWith("*")){
//                    this.enders.add(currentOption.name);
//                }
                options.add(currentOption);
            }
        }
        //return deep clone of options
        return new HashSet<LetterGroup>(options);
    }

    private boolean validOption(LetterGroup option, int row) {
        while (row >=0){
            String leftLetterPair = this.squares.get(new Point(0, row));
            LetterGroup leftLetterGroup = this.index.get(leftLetterPair.charAt(0) + "/" + leftLetterPair.charAt(1));
            ArrayList<String> rightLetterList = new ArrayList<String>();
            for (LetterGroup child : leftLetterGroup.rightKids){
                rightLetterList.add(child.lastLetters);
            }
            String[] parts = option.name.replace("*", "").split("/");
            String rightLetterPair = parts[0].charAt(row) + "" + parts[1].charAt(row);
            if (!rightLetterList.contains(rightLetterPair)){
                return false;
            }
            row--;
        }
        return true;
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
        cw.buildPuzzle();
        
//        LetterGroup testWord = cw.index.get("F/P");
//        ArrayList<String> rightLetterPairs = testWord.getRightLetterPairs();
//        for (String pair: rightLetterPairs){
//            System.out.println(pair);
//        }
//        for (LetterGroup word: testWord.rightKids){
//            System.out.println(word.name);
//        }
//        ArrayList<String> lefttLetterPairs = testWord.getLeftLetterPairs();
//        for (String pair: lefttLetterPairs){
//            System.out.println(pair);
//        }
//        for (LetterGroup word: testWord.leftKids){
//            System.out.println(word.name);
//        }
        

        

        
        
    }

}