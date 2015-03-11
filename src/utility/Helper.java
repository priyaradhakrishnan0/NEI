package utility;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tac.model.Doc;
import version1.DocSearcher;
import Variables.Variables;

public class Helper {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		 HashMap<String,String> infoStringMap = new HashMap<String, String>();
		 infoStringMap.put("a", "1");
		 infoStringMap.put("b", "2");
		 infoStringMap.put("c", "3");
		 //Helper.getString(infoStringMap.keySet(), ",");
		 Helper.getString(infoStringMap, ",",";\n\t\t");
	}//End main()
	
	public static String getString( Set<String> keyset, String joiningElement ){
		String combinedString = null;
		Iterator<String> it = keyset.iterator(); 
		if(it.hasNext()){
			combinedString = (String)it.next();			
		}
		while(it.hasNext()){
			combinedString += joiningElement;
			combinedString += it.next();			
		}
		System.out.println("Combined string = "+combinedString);
		return combinedString;		
	}//End getString(set, join)
	
	public static String getString( ArrayList<String> keyset, String joiningElement ){
		String combinedString = null;
		Iterator<String> it = keyset.iterator(); 
		if(it.hasNext()){
			combinedString = (String)it.next();			
		}
		while(it.hasNext()){
			combinedString += joiningElement;
			combinedString += it.next();			
		}
		//System.out.println("Combined string = "+combinedString);
		return combinedString;		
	}//End getString(sarraylist, join)
	
	public static String getString( Map<String, String> facts, String joiningElement, String factSeperator ){
		String combinedString = null;
		String key = null;
		Iterator<String> it = facts.keySet().iterator(); 
		if(it.hasNext()){
			key = (String)it.next();
			combinedString = key;
			combinedString += joiningElement;
			combinedString += facts.get(key);		
		}//End if first map element
		while(it.hasNext()){
			combinedString += factSeperator;
			key = (String)it.next();
			combinedString += key;
			combinedString += joiningElement;
			combinedString += facts.get(key);			
		}
		System.out.println("Combined string = "+combinedString);
		return combinedString;		
	}//End getString(map, join, delimitter)
    
	public static void showMap(Map<String, String> inMap){
		for(String ne : inMap.keySet()){
			System.out.println(ne+" = "+inMap.get(ne));
		}
	}//end showMap
	
	public static void showMapList(Map<String, ArrayList<String>> inMap){
		for(String mention : inMap.keySet()){
			System.out.println(mention+" = "+inMap.get(mention));
		}
	}//end showMapList

    public static List<String> getDocumentContext(String docId, String entity, int startOffset, int endOffset) throws IOException, Exception {
        List<String> context = null;

        // search the document
        DocSearcher searcher = new DocSearcher(Variables.TAC2009DocIndex);//revisit to use 2010 doc index
        Doc doc = searcher.searchById(docId);

        //get the context
        if (doc != null) {
            Logger.log("        Document fetched " , doc.getId());
            String content = doc.getText();
            Logger.log("        Contents are ", content);
            
            if(content.contains("\n")){
            	String[] sentences = content.split("\n");
                //String entity = content.substring(startOffset, endOffset);
            	context = getContext(Arrays.asList(sentences), Arrays.asList(entity), 2);
                Logger.log("        Context is "+context);
            }           
        }
        
        return context;
    }

    public static List<String> getContext(List<String> sentences, List<String> keyWords, int contextSize) throws IOException {
        
        Set<String> contextSentences = new HashSet<String>();
        Logger.log("        fetching context among "+ sentences.size() + " sentences");
                
        for (String keyWord : keyWords) {
        
            for (int i = 0; i < sentences.size(); i++) {
            
                //Logger.log("        searching for '" + keyWord + "' in sentence - " + sentences.get(i));
                
                if (sentences.get(i).contains(keyWord)) {
                    
                    Logger.log("        found a match for - " + keyWord + ",so adding context...");
                    int contextStartIndex = ((i - contextSize) < 0) ? 0 : (i - contextSize);
                    int contextEndIndex = ((i + contextSize) >= sentences.size()) ? sentences.size()-1 : (i + contextSize);
                    
                    //Logger.log("        start=" + contextStartIndex + " , end=" + contextEndIndex);
                    for (int j = contextStartIndex; j <= contextEndIndex; j++) {
                        contextSentences.add(sentences.get(j));
                        //Logger.log("        adding sentence id " + j);
                    }
                }
            }
        }
        
        return new ArrayList<String>(contextSentences);
    }

}//class
