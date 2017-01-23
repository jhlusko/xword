package dictionary;

import dictionary.*;
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
import java.util.logging.Level;
import java.util.logging.Logger;

public class Crossword {

    public static ArrayList<String> TOPWORDS = topWords();

    public static ArrayList<String> topWords() {
        try {
            ArrayList<String> topWordsBuilder = new ArrayList<>();
            Scanner sc = new Scanner(new File("/home/jamie/Downloads/words.txt"));
            while (sc.hasNext()) {
                String line = sc.next();
                String word = line.substring(1, line.indexOf(","));
                topWordsBuilder.add(word);
                sc.nextLine();
            }
            return topWordsBuilder;

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Crossword.class.getName()).log(Level.SEVERE, null, ex);
            return new ArrayList<String>();
        }
    }

    private static ArrayList<WordGroup> removeUncommon(ArrayList<WordGroup> dictionary) {
        ArrayList<WordGroup> filtered = new ArrayList<>();
        for (WordGroup w : dictionary) {
            ArrayList<String> words = new ArrayList<>();
            for (String word : w.words.get(0).split("\\+")) {
                words.add(word);
            }
            for (String word : w.words.get(1).split("\\+")) {
                words.add(word);
            }
            boolean valid = true;
            for (String word : words) {
                if (!(TOPWORDS.contains(word.toLowerCase()))) {
                    valid = false;
                }
            }
            if (valid) {
                filtered.add(w);
            }
        }
        return filtered;
    }

    HashMap<Point, String> squares;
    HashMap<Integer, ArrayList<Point>> rows;
    HashMap<Integer, ArrayList<Point>> columns;
    ArrayList<Entry> entries;
    int xMin;
    int xMax;
    int yMin;
    int yMax;
    ArrayList<WordGroup> dictionary;
    ArrayList<Point> badOrigins;
    Scanner scanner;

    public Crossword(ArrayList<WordGroup> wordPairs) {
        this.dictionary = wordPairs;
        this.squares = new HashMap<Point, String>();
        this.rows = new HashMap<Integer, ArrayList<Point>>();
        this.columns = new HashMap<Integer, ArrayList<Point>>();
        this.entries = new ArrayList<Entry>();
        this.xMin = 0;
        this.xMax = 0;
        this.yMin = 0;
        this.yMax = 0;
        this.badOrigins = new ArrayList<Point>();
        this.scanner = new Scanner(System.in);
    }

    public static IDictionary getDictionary() throws IOException {
        // construct the URL to the Wordnet dictionary directory
        URL url = new URL("file", null, "/usr/share/wordnet");
        // construct the dictionary object and open it
        IDictionary dict = new Dictionary(url);
        dict.open();
        return dict;
    }

    public static void addWordsToDict(WordGroup wordGroup, java.util.List<IWord> words) {
        for (Iterator<IWord> i = words.iterator(); i.hasNext();) {
            String word = i.next().getLemma();
            if (word.length() < 14) {
                wordGroup.add(word);
            }
        }
    }

    public void addRelatedWords(WordGroup wordGroup, IDictionary dict, ISynset synset, Pointer pointerType) {
        java.util.List<ISynsetID> hypernyms = synset.getRelatedSynsets(pointerType);
        for (ISynsetID sid : hypernyms) {
            addWordsToDict(wordGroup, dict.getSynset(sid).getWords());
        }
    }

    public ArrayList<String> getLetterPairs(String[] args) {
        ArrayList<String> LetterPairs = new ArrayList<String>();
        for (char first = 'a'; first <= 'z'; first++) {
            for (char second = 'a'; second <= 'z'; second++) {
                LetterPairs.add((String.valueOf(first) + String.valueOf(second)));
            }
        }
        return LetterPairs;
    }

    public static ArrayList<WordGroup> getEqualLengthDict() throws IOException {
        IDictionary dict = getDictionary();

        ArrayList<WordGroup> synDict = new ArrayList<WordGroup>();
        ArrayList<WordGroup> equalLengthDict = new ArrayList<WordGroup>();

        int index = 0;
        for (Iterator<ISynset> i = dict.getSynsetIterator(POS.NOUN); i.hasNext();) {
//			System.out.println(index);
//			System.out.print("Synsets: ");
            ISynset synset = i.next();

            synDict.add(new WordGroup(synset.getGloss()));

            addWordsToDict(synDict.get(index), synset.getWords());
            //addRelatedWords(synDict.get(index), dict, synset, Pointer.HYPONYM);
            //addRelatedWords(synDict.get(index), dict, synset, Pointer.HYPERNYM);
            index++;
        }
        for (WordGroup w : synDict) {
            Collections.sort(w.words, new SortByWordLength());
            //System.out.println(w.words.toString());
            while (w.words.size() != 0) {
                if (w.words.size() == 1) {
                    w.words.remove(0);
                    break;
                }
                int length = w.words.get(0).length();
                int index1 = 0;
                java.util.ArrayList<String> newWords = new ArrayList<String>();
                //if second word is different length remove 1st word
                if (w.words.size() > 1 && length != w.words.get(++index1).length()) {
                    w.words.remove(0);
                } //if second word is of same length, check following words
                else {
                    //while there are more words
                    while (w.words.size() > ++index1) {
                        //check those words are the same length
                        if (length != w.words.get(index1).length()) {
                            //once you reach a diff-length word, end search
                            break;
                        }
                    }
                    //while there are more equal-length words
                    while (--index1 != -1) {
                        newWords.add(w.words.remove(index1));
                    }
                    WordGroup newWordGroup = new WordGroup(w.clue, newWords);
//				System.out.println("new words:");
//	    		newWordGroup.printInfo();
                    equalLengthDict.add(newWordGroup);
                }
            }
        }
        return equalLengthDict;
    }

    public static ArrayList<WordGroup> getWordPairs(ArrayList<WordGroup> equalLengthDict) {
        ArrayList<WordGroup> wordPairs = new ArrayList<WordGroup>();
        for (WordGroup w : equalLengthDict) {
            int firstWordIndex = 0;
            int secondWordIndex = 1;
            while (firstWordIndex < w.words.size()) {
                while (secondWordIndex < w.words.size()) {
                    wordPairs.add(new WordGroup(w.clue, new ArrayList<String>(Arrays.asList(w.words.get(firstWordIndex), w.words.get(secondWordIndex)))));
                    wordPairs.add(new WordGroup(w.clue, new ArrayList<String>(Arrays.asList(w.words.get(secondWordIndex), w.words.get(firstWordIndex)))));
                    secondWordIndex++;
                }
                firstWordIndex++;
                secondWordIndex = firstWordIndex + 1;
            }
        }
        return wordPairs;
    }

    public static java.util.ArrayList<WordGroup> sanitize(ArrayList<WordGroup> equalLengthDict) {
        for (WordGroup wordGroup : equalLengthDict) {
            for (int i = 0; i < wordGroup.words.size(); i++) {
                Pattern p = Pattern.compile("[0-9]");
                Matcher m = p.matcher(wordGroup.words.get(i));
                if (m.find()) {
                    wordGroup.words.remove(i);
                    continue;
                }
                wordGroup.words.set(i, wordGroup.words.get(i).toUpperCase());
                wordGroup.words.set(i, wordGroup.words.get(i).replaceAll("[^A-Z]", "+"));
            }
        }
        return equalLengthDict;
    }

    public WordGroup confirmWord(WordGroup word) {
        word.printInfo();
        System.out.println("Add word to crossword or export crossword? (y/n/e)");
        String input = this.scanner.next();
        if (input.equals("n")) {
            this.dictionary.remove(word);
        } else if (input.equals("y")) {
            System.out.println("Adding word to crossword!");
            return word;
        } else if (input.equals("e")) {
            System.out.println("Exporting crossword!");
            try {
                this.export();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        } else {
            System.out.println("??? your input: " + input);
        }
        return null;

    }

    public void addEntry(Entry e) {
        for (int i = 0; i < e.wordGroup.words.get(0).length(); i++) {
            Point point = new Point(e.point.x + i * ((e.isHorizontal) ? 1 : 0) - e.offset * ((e.isHorizontal) ? 1 : 0), e.point.y + i * ((e.isHorizontal) ? 0 : 1) - e.offset * ((e.isHorizontal) ? 0 : 1));
            String letterPair = "" + e.wordGroup.words.get(0).charAt(i) + e.wordGroup.words.get(1).charAt(i);
            squares.put(point, letterPair);
            if (rows.get(point.y) != null) {
                rows.get(point.y).add(point);
            } else {
                rows.put(point.y, new ArrayList<Point>(Arrays.asList(point)));
            }
            if (columns.get(point.x) != null) {
                columns.get(point.x).add(point);
            } else {
                columns.put(point.x, new ArrayList<Point>(Arrays.asList(point)));
            }

            if (point.x < xMin) {
                xMin = point.x;
            }
            if (point.x > xMax) {
                xMax = point.x;
            }
            if (point.y < yMin) {
                yMin = point.y;
            }
            if (point.y > yMax) {
                yMax = point.y;
            }
        }
        entries.add(e);
        this.dictionary.remove(e.wordGroup);

    }

    public void printOne(int num) {
        System.out.print("..");
        int i = 1;
        for (int x = xMin; x <= xMax; x++) {
            System.out.print(i % 10);
            i++;
        }
        System.out.println();
        System.out.print("..");
        for (int x = xMin; x <= xMax; x++) {
            System.out.print(Math.abs(x) % 10);
        }
        i = 1;
        System.out.println();
        for (int y = yMin; y <= yMax; y++) {
            System.out.print(i % 10);
            i++;
            System.out.print(Math.abs(y) % 10);
            for (int x = xMin; x <= xMax; x++) {
                if (squares.get(new Point(x, y)) != null) {
                    System.out.print(squares.get(new Point(x, y)).charAt(num));
                } else {
                    System.out.print('.');
                }
            }
            System.out.println();
        }

    }

    public void printClues(String orientation) {
        int count = 1;
        int yoffset = (this.yMin == 0) ? 1 : -1 * this.yMin + 1;
        int xoffset = (this.xMin == 0) ? 1 : -1 * this.xMin + 1;
        for (Entry e : entries) {
            if (orientation == "horizontal") {
                if (e.isHorizontal) {
                    System.out.println(count + ". [" + String.valueOf(e.point.x - e.offset) + ", " + e.point.y + "]"
                            + " [" + e.origin.x + ", " + e.origin.y + "] "
                            + "[" + String.valueOf(e.origin.x + xoffset) + ", " + String.valueOf(e.origin.y + yoffset) + "] " + e.wordGroup.clue);
                    count++;
                }
            }
            if (orientation == "vertical") {
                if (!e.isHorizontal) {
                    System.out.println(count + ". [" + e.point.x + ", " + String.valueOf(e.point.y - e.offset) + "]"
                            + " [" + e.origin.x + ", " + e.origin.y + "] "
                            + "[" + String.valueOf(e.origin.x + xoffset) + ", " + String.valueOf(e.origin.y + yoffset) + "] " + e.wordGroup.clue);
                    count++;
                }
            }
        }
    }

    public void display() {
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

    public Point findNextOrigin() {
        int x = xMin + (int) (Math.random() * ((xMax - xMin) + 1));
        x = (x % 2 == 0) ? x : x + 1;
        int y = yMin + (int) (Math.random() * ((yMax - yMin) + 1));
        y = (y % 2 == 0) ? y : y + 1;
        if (this.squares.get(new Point(x, y)) != null
                && !this.badOrigins.contains(new Point(x, y))
                && ((this.squares.get(new Point(x + 1, y)) == null && this.squares.get(new Point(x - 1, y)) == null)
                || (this.squares.get(new Point(x, y + 1)) == null && this.squares.get(new Point(x, y - 1)) == null))) {
            return new Point(x, y);
        } else {
            return findNextOrigin();
        }
    }

    public void addRandomWord() {
        Point origin = findNextOrigin();
        if (origin == null) {
            System.out.println("NO CANDIDATE ORIGIN FOUND");
            this.display();
        } else {
            boolean isHorizontal = (this.squares.get(new Point(origin.x + 1, origin.y)) == null);
            Entry candidate = findWord(origin, origin, isHorizontal);
            if (candidate == null) {
                this.badOrigins.add(origin);
                addRandomWord();
            } else {
                this.addEntry(candidate);
            }
        }
    }

    public static ArrayList<WordGroup> checkDifferent(ArrayList<WordGroup> wordPairs) {
        ArrayList<WordGroup> diffWords = new ArrayList<WordGroup>();
        wordloop:
        for (WordGroup w : wordPairs) {
            int num_diff_letters = 0;
            for (int i = 0; i < w.words.get(0).length(); i++) {
                if (w.words.get(0).charAt(i) != w.words.get(1).charAt(i)) {
                    num_diff_letters++;
                    if (num_diff_letters > 2) {
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
        for (int num = 0; num < 2; num++) {
            BufferedWriter writer;
            try {
//				File file = new File("/home/jamie/Downloads/xwords.xml");
                String title = num + "-Double-Crossword-" + time;
                File file = new File("/home/jamie/Downloads/" + title + ".xml");
                writer = new BufferedWriter(new FileWriter(file));
                writer.write("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
                writer.write("<Puzzles Version=\"1.0\">\n");
                writer.write("<Puzzle>\n");
                writer.write("<Title>" + title + "</Title>\n");
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
                for (int y = yMin; y <= yMax; y++) {
                    writer.write("<Row>");
                    for (int x = xMin; x <= xMax; x++) {
                        if (squares.get(new Point(x, y)) != null) {
                            writer.write(squares.get(new Point(x, y)).charAt(num));
                        } else {
                            writer.write(".");
                        }
                    }
                    writer.write("</Row>\n");
                }
                writer.write("</Grid>\n");
                writer.write("<Clues>\n");

                List<Entry> across = new ArrayList<Entry>();
                List<Entry> down = new ArrayList<Entry>();
                for (Entry e : this.entries) {
                    if (e.isHorizontal) {
                        across.add(e);
                    } else {
                        down.add(e);
                    }
                }
                Comparator<Entry> sortEntries = new Comparator<Entry>() {
                    public int compare(Entry e1, Entry e2) {
                        if (e1.point.y == e2.point.y) {
                            return Integer.compare(e1.point.x, e2.point.x);
                        } else {
                            return Integer.compare(e1.point.y, e2.point.y);
                        }
                    }
                };

                int yoffset = (this.yMin == 0) ? 1 : -1 * this.yMin + 1;
                int xoffset = (this.xMin == 0) ? 1 : -1 * this.xMin + 1;
                HashMap<Point, Integer> clueOrigins = new HashMap<Point, Integer>();
                int clueSeq = 0;
                int clueNum;
                Collections.sort(across, sortEntries);
                for (Entry e : across) {
                    if (clueOrigins.get(new Point(e.origin.x + xoffset, e.origin.y + yoffset)) != null) {
                        clueNum = clueOrigins.get(new Point(e.origin.x + xoffset, e.origin.y + yoffset));
                    } else {
                        clueNum = ++clueSeq;
                        clueOrigins.put(new Point(e.origin.x + xoffset, e.origin.y + yoffset), new Integer(clueNum));
                    }
                    writer.write("<Clue Row=\"" + String.valueOf(e.origin.y + yoffset) + "\" " + ""
                            + "Col=\"" + String.valueOf(e.origin.x + xoffset) + "\" "
                            + "Num=\"" + String.valueOf(clueNum) + "\" "
                            + "Dir=\"Across\" "
                            + "Ans=\"" + e.wordGroup.words.get(num).toUpperCase() + "\">"
                            + e.wordGroup.clue + "</Clue>\n");
                }
                Collections.sort(down, sortEntries);
                for (Entry e : down) {
                    if (clueOrigins.get(new Point(e.origin.x + xoffset, e.origin.y + yoffset)) != null) {
                        clueNum = clueOrigins.get(new Point(e.origin.x + xoffset, e.origin.y + yoffset));
                    } else {
                        clueNum = ++clueSeq;
                        clueOrigins.put(new Point(e.origin.x + xoffset, e.origin.y + yoffset), new Integer(clueNum));
                    }
                    writer.write("<Clue Row=\"" + String.valueOf(e.origin.y + yoffset) + "\" " + ""
                            + "Col=\"" + String.valueOf(e.origin.x + xoffset) + "\" "
                            + "Num=\"" + String.valueOf(clueNum) + "\" "
                            + "Dir=\"Down\" "
                            + "Ans=\"" + e.wordGroup.words.get(num).toUpperCase() + "\">"
                            + e.wordGroup.clue + "</Clue>\n");
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
//		FileOutputStream fileOut;
//		fileOut = new FileOutputStream("/home/jamie/Downloads/wordPairs.ser");
//		ObjectOutputStream out = new ObjectOutputStream(fileOut);
//		out.writeObject(this.dictionary);
//		out.close();
//		fileOut.close();
//        this.display();
//        System.exit(0);

    }

    public boolean addDoubleWord() {
        boolean isHorizontal;
        int xStart = (xMin % 2 == 0) ? xMin : xMin + 1;
        int yStart = (yMin % 2 == 0) ? yMin : yMin + 1;
        for (int y = yStart; y <= yMax; y += 2) {
            for (int x = xStart; x <= xMax; x += 2) {
                if (squares.get(new Point(x, y)) != null) {
                    if (squares.get(new Point(x + 1, y)) == null && squares.get(new Point(x - 1, y)) == null) {
                        isHorizontal = true;
                        for (int nearest = 2; x + nearest <= xMax && x - nearest >= xMin; nearest++) {
                            if (squares.get(new Point(x + nearest, y)) != null) {
                                Entry e = findWord(new Point(x, y), new Point(x + nearest, y), isHorizontal);
                                if (e != null) {
                                    this.addEntry(e);
                                    return true;
                                }
                            }
                            if (squares.get(new Point(x - nearest, y)) != null) {
                                Entry e = findWord(new Point(x, y), new Point(x - nearest, y), isHorizontal);
                                if (e != null) {
                                    this.addEntry(e);
                                    return true;
                                }
                            }

                        }
                    }
                    if (squares.get(new Point(x, y + 1)) == null && squares.get(new Point(x, y - 1)) == null) {
                        isHorizontal = false;
                        for (int nearest = 2; y + nearest <= yMax && y - nearest >= yMin; nearest++) {
                            if (squares.get(new Point(x, y + nearest)) != null) {
                                Entry e = findWord(new Point(x, y), new Point(x, y + nearest), isHorizontal);
                                if (e != null) {
                                    this.addEntry(e);
                                    return true;
                                }
                            }
                            if (squares.get(new Point(x, y - nearest)) != null) {
                                Entry e = findWord(new Point(x, y), new Point(x, y - nearest), isHorizontal);
                                if (e != null) {
                                    this.addEntry(e);
                                    return true;
                                }
                            }

                        }
                    }
                }
            }
        }
        return false;
    }

    public Entry findWord(Point origin, Point secondPoint, boolean isHorizontal) {
        String firstLetterPair = this.squares.get(origin);
        String secondLetterPair = this.squares.get(secondPoint);
        //for each wordPair in dict
        int end;
        end = new Double(Math.random() * this.dictionary.size()).intValue();
        wordloop:
        for (int i = end; i < this.dictionary.size(); i++) {
            WordGroup w = this.dictionary.get(i);
            //for each letterPair in the wordPair
            for (int offset = 0; offset < w.words.get(0).length(); offset++) {
                //check if letterPair matches origin's letterpair
                if (w.words.get(0).charAt(offset) == firstLetterPair.charAt(0) && w.words.get(1).charAt(offset) == firstLetterPair.charAt(1)) {
                    //find and check intersection points
                    if (isHorizontal) {
                        //short circuit if secondLetterPair doesn't match
                        if (offset + secondPoint.x - origin.x >= 0 && offset + secondPoint.x - origin.x < w.words.get(0).length()
                                && w.words.get(0).charAt(offset + secondPoint.x - origin.x) == secondLetterPair.charAt(0)
                                && w.words.get(1).charAt(offset + secondPoint.x - origin.x) == secondLetterPair.charAt(1)) {
                            int k = 0;
                            if (this.squares.get(new Point(origin.x - offset - 1, origin.y)) == null && this.squares.get(new Point(origin.x - offset + w.words.get(0).length(), origin.y)) == null) {
                                for (int j = origin.x - offset; j <= origin.x - offset + w.words.get(0).length() - 1; j++) {
                                    //all intersections must have same letterPair
                                    ArrayList<Point> row = rows.get(origin.y);
                                    if (this.squares.get(new Point(j, origin.y)) == null && (this.squares.get(new Point(j, origin.y + 1)) != null || this.squares.get(new Point(j, origin.y - 1)) != null)) {
                                        continue wordloop;
                                    }
                                    if (row.contains(new Point(j, origin.y)) == true && !squares.get(new Point(j, origin.y)).equals("" + w.words.get(0).charAt(k) + w.words.get(1).charAt(k))) {
                                        continue wordloop;
                                    }
                                    k++;
                                }
//								if (confirmWord(w) == null){
//									continue wordloop;
//								}
                                return new Entry(w, origin, offset, isHorizontal);
                            }
                        }
                    } else //						//short circuit if secondLetterPair doesn't match
                    if (offset + secondPoint.y - origin.y >= 0 && offset + secondPoint.y - origin.y < w.words.get(0).length() && w.words.get(0).charAt(offset + secondPoint.y - origin.y) == secondLetterPair.charAt(0) && w.words.get(1).charAt(offset + secondPoint.y - origin.y) == secondLetterPair.charAt(1)) {
                        //k is position in word
                        int k = 0;
                        //j is position in row - both refer to same letter/location
                        if (this.squares.get(new Point(origin.x, origin.y - offset - 1)) == null && this.squares.get(new Point(origin.x, origin.y - offset + w.words.get(0).length())) == null) {
                            for (int j = origin.y - offset; j <= origin.y - offset + w.words.get(0).length() - 1; j++) {
                                //all intersections must have same letterPair
                                ArrayList<Point> column = columns.get(origin.x);
                                if (this.squares.get(new Point(origin.x, j)) == null && (this.squares.get(new Point(origin.x + 1, j)) != null || this.squares.get(new Point(origin.x - 1, j)) != null)) {
                                    continue wordloop;
                                }
                                if (column.contains(new Point(origin.x, j)) == true && !squares.get(new Point(origin.x, j)).equals("" + w.words.get(0).charAt(k) + w.words.get(1).charAt(k))) {
                                    continue wordloop;
                                }
                                k++;
                            }
//								if (confirmWord(w) == null){
//									continue wordloop;
//								}
                            return new Entry(w, origin, offset, isHorizontal);
                        }
                    }
                }
            }
        }
        return null;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        double sum = 0;
        double best = 99;
        int numExported = 0;
        ArrayList<WordGroup> wordPairs = removeUncommon(checkDifferent(getWordPairs(sanitize(getEqualLengthDict()))));
        for (int j = 0; j < 1000; j++) {
//                FileInputStream fileIn = new FileInputStream("/tmp/employee.ser");
//	        ObjectInputStream in = new ObjectInputStream(fileIn);
//	        ArrayList<WordGroup> wordPairs = (ArrayList<WordGroup>) in.readObject();
//	        in.close();
//	        fileIn.close();
            Crossword cw = new Crossword(wordPairs);
            WordGroup initialWord = wordPairs.get((int) (Math.random() * wordPairs.size()));
            cw.addEntry(new Entry(initialWord, new Point(0, 0), 0, true));
            double numSingle = 1;
            int numDouble = 0;
            while (numDouble < 10) {
                boolean addedDouble = cw.addDoubleWord();
                if (addedDouble) {
                    numDouble++;
//				System.out.println("s: " + numSingle + ", d: " + numDouble);
                } else {
                    cw.addRandomWord();
                    numSingle++;
//				System.out.println("s: " + numSingle + ", d: " + numDouble);
                }
                if (numSingle + numDouble > 100) {
                    break;
                }
            }
            double ratio = numSingle / numDouble;
            System.out.println("Final ratio: " + ratio);
            sum += ratio;
            best = (ratio < best) ? ratio : best;
            System.out.println("Best ratio: " + best);
            System.out.println("Average ratio: " + sum / j + 1);
            System.out.println("Num attempts: " + 1 + j);
            System.out.println("Num exported: " + numExported);
            System.out.println();
            if (ratio < 3) {
                cw.display();
                cw.export();
                numExported++;
            }

//		cw.display();
//		cw.export();
        }
    }

//	public static void main(String[] args) throws IOException, ClassNotFoundException {
//            ArrayList<WordGroup> wordPairs = removeUncommon(checkDifferent(getWordPairs(sanitize(getEqualLengthDict()))));
//            FileOutputStream fileOut;
//            fileOut = new FileOutputStream("/home/jamie/Downloads/wordPairs.ser");
//            ObjectOutputStream out = new ObjectOutputStream(fileOut);
//            out.writeObject(wordPairs);
//            out.close();
//            fileOut.close();
//            FileInputStream fileIn = new FileInputStream("/home/jamie/Downloads/wordPairs.ser");
//            ObjectInputStream in = new ObjectInputStream(fileIn);
//            ArrayList<WordGroup> wordPairs = (ArrayList<WordGroup>) in.readObject();
//            in.close();
//            fileIn.close();
//            Crossword cw = new Crossword(wordPairs);
//
//            WordGroup initialWord = wordPairs.get((int)(Math.random()*wordPairs.size()));
//            cw.addEntry(new Entry(initialWord, new Point(0, 0), 0, true));
//            while(true){
//                boolean addedDouble = cw.addDoubleWord();
//                if (!addedDouble){
//                        cw.addRandomWord();
//                }
//            }
//	}
}
