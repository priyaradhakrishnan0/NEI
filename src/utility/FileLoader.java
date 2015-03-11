package utility;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import db.wikiPageIndex;
import version1.KBSearcher;
import Variables.Variables;

public class FileLoader {
	
	public static void main(String[] args){
		FileLoader fileLoader = new FileLoader();
		System.out.println("Size = "+fileLoader.loadKBqueriesIdMap().size());
	}//end main
	
	/*read kbSubKb queries - ploch*/
	public HashMap<String, String> loadKBqueriesMap(){
		HashMap<String, String> disambiguationFeatures = new HashMap<String, String>();
		String disamFile = Variables.TACOutputDir.concat("ploch.kbSubKb.queries"); 	//System.out.println("Loading file ="+disamFile);
		try {
			BufferedReader br = new BufferedReader(new FileReader(disamFile));			
			String sCurrentLine;			 
			while ((sCurrentLine = br.readLine()) != null) { //System.out.println(sCurrentLine);
				String[] splits = sCurrentLine.split("\t");
				String qId = splits[0].trim();
				String entity = splits[1].trim();
				disambiguationFeatures.put(qId, entity);	//System.out.println(qId+"  "+entity);				
			}			
		} catch (FileNotFoundException e1) {
			Logger.logOut(disamFile+" File not found");
			e1.printStackTrace();
		} catch (IOException e2) {			Logger.logOut("Reading error with "+disamFile);
			e2.printStackTrace();
		}
		return disambiguationFeatures ;
	}//End loadKBqueriesMap()

	/*read kbSubKb queries - tagme*/
	public HashMap<String, Long> loadKBqueriesIdMap(){
		HashMap<String, Long> eliminatedQueris = new HashMap<String, Long>();
		String solFile = Variables.TACOutputDir.concat("tagme.kbSubKb.queries"); //file containing only nonNIL solutions //System.out.println("Loading file ="+disamFile);
		try {
			BufferedReader br = new BufferedReader(new FileReader(solFile));			
			String sCurrentLine;
			KBSearcher searcher = new KBSearcher(Variables.TACKBindex);
			wikiPageIndex wikiIndex = new wikiPageIndex();
			while ((sCurrentLine = br.readLine()) != null) { //System.out.println(sCurrentLine);
				String[] splits = sCurrentLine.split("\t");
				String qId = splits[0].trim();
				String entity = splits[1].trim();
				if(entity.startsWith("E")){			        
			        entity = searcher.searchById(entity).getName();
			        //System.out.println("Entity = "+entity);
				} 
				Long pgId = wikiIndex.getPageId(entity);
				//System.out.println("Entity = "+entity+"  PageId = " + pgId);
				
				eliminatedQueris.put(qId, pgId);	//System.out.println(qId+"  "+entity);				
			}			
		} catch (FileNotFoundException e1) {
			Logger.logOut(solFile+" File not found");
			e1.printStackTrace();
		} catch (IOException e2) {			Logger.logOut("Reading error with "+solFile);
			e2.printStackTrace();
		}
		return eliminatedQueris;
	}//End loadKBqueriesIdMap()

	
}//class
