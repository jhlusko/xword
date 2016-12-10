package dictionary;


import java.util.HashSet;


public class Test {

	public static void main(String[] args) {
	
            HashSet<String> orig = new HashSet<String>();
            orig.add("test");
            HashSet<String> clone = orig;//new HashSet<String>(orig);
            System.out.println("orig contains test: " + orig.contains("test"));
            System.out.println("clone contains test: " + clone.contains("test"));
            orig.remove("test");
            System.out.println("orig contains test: " + orig.contains("test"));
            System.out.println("clone contains test: " + clone.contains("test"));
            
		
	}
	      
}
