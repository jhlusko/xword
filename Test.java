package dictionary;


import java.io.File;
import java.io.IOException;
import java.util.Date;



public class Test {

public static void main(String[] args) {

	try {
		 
	      File file = new File("/home/jamie/Documents/" + new Date().getTime() + "-Crossword" + 1 + ".txt");
	      
	      if (file.createNewFile()){
	        System.out.println("File is created!");
	      }else{
	        System.out.println("File already exists.");
	      }
	      
  	} catch (IOException e) {
	      e.printStackTrace();
	}
}

}
