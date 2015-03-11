package utility;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import Variables.Variables;

public class Cleaner {

	//private String stopWordFile = "/media/New Volume/Datasets/english.stop";
	public HashSet stopWordSet;
	
	public Cleaner(){
		stopWordSet = new HashSet();  
		try {
			BufferedReader br = new BufferedReader(new FileReader(Variables.stopWordFile));			
			String sCurrentLine;			 
			while ((sCurrentLine = br.readLine()) != null) { //System.out.println(sCurrentLine);
				stopWordSet.add(sCurrentLine);				
			}			
		} catch (FileNotFoundException e1) {
			Logger.logOut(Variables.stopWordFile+" File not found");
			e1.printStackTrace();
		} catch (IOException e2) {
			Logger.logOut("Reading error with "+Variables.stopWordFile);
			e2.printStackTrace();
		}		
	}//constructor
	
	public void destroy(){
		stopWordSet.clear();
	}//destructor
	
	public static void main(String[] arg){
		Cleaner cleaner = new Cleaner();
		System.out.println(cleaner.isStopWord("about"));
	}//main
	
	public boolean isStopWord(String sequence){
		boolean YesNo = false;
		if(stopWordSet.contains(sequence)){
			YesNo = true;
		}
		return YesNo;
	}//isStopWord
	
	public ArrayList<String> removeStopWords(ArrayList<String> wordList){
		ArrayList<String> cleanList = new ArrayList<String>();
		for(String word : wordList){
			if(stopWordSet.contains(word)){
				//dont add
			} else {
				cleanList.add(word);
			}			
		}
		return cleanList;
	}//removeStopWords
	
}//class