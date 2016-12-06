package dictionary;


public class Test {

	public static void main(String[] args) {
	
		String s = "abc/123";
                String[] parts = s.split("/");
                System.out.println(s.substring(0, s.length() - 1));

//		for (int j = 1; j <= parts[0].length(); j++){
//			for (int i = 0; i <= parts[0].length() - j  ;i++){
//				System.out.println(parts[0].substring(i, i+j) + parts[1].substring(i, i+j));
//			}
//		}
		
	}
	      
}
/*
a1
b2
c3
ab12
bc23
abc123
*/