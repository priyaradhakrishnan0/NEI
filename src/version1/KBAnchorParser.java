package version1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import utility.Helper;
import Variables.Variables;

/** @author priya.r */
public class KBAnchorParser extends DefaultHandler {
    private File kbDir;
    public static Map<String, ArrayList<String> > KBAnchorMap = new HashMap<String, ArrayList<String> >();//anchor to Entitylist map
    
    public static void main(String[] args) throws Exception{
        KBAnchorParser kbaParser = new KBAnchorParser(Variables.TACKBdir);
        kbaParser.start(); //to parse TACKBDir and get Map
		kbaParser.writeMapToFile(kbaParser.KBAnchorMap, Variables.TACKBsortedIndexFile);
        //kbaParser.writeFileToMap(Variables.TACKBsortedIndexFile);
        System.out.println("Loaded map size = "+kbaParser.KBAnchorMap.size());
    }
    
    //-------------------------------------------------------------------------------

    public KBAnchorParser(String kbDirName) throws Exception {
        this.kbDir = new File(kbDirName);
        if (!kbDir.isDirectory()) {
            throw new Exception("Resource not a directory");
        }
    }//End contructor

    public void start() {
    	int fileCount =0;
        ArrayList<File> files = new ArrayList<File>();
        files.addAll(Arrays.asList(kbDir.listFiles()));
        //Map<String, ArrayList<String> > KBanchorMap = new HashMap<String, ArrayList<String> >();//anchor to Entitylist map
        for (File file : files) {
        	++fileCount;
		    System.out.println("KBAparsing " + file.getAbsolutePath() + " . File "+fileCount+" of "+files.size());
	        parse(file); 	        //System.out.println("Entities parsed = "+ KBAnchorMap.size());
		    if(KBAnchorMap.size()>0){//collate repeating Eids
		    	for(String anchor:KBAnchorMap.keySet()){
		    		KBAnchorMap.put(anchor, collateEid(KBAnchorMap.get(anchor)));
		    	}
		    }
		    System.out.println("Entities Found here = "+ KBAnchorMap.size());
//		    if(writeMapToFile(KBAnchorMap, Variables.indexFilesOutDir+file.getName())){
//		    	KBAnchorMap.clear();
//        	}
		}
    }

    public void parse(File file) {
        SAXParser parser;
        try {
            parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(file, this);
		    if(KBAnchorMap.size()>0){//collate repeating Eids
		    	for(String anchor:KBAnchorMap.keySet()){
		    		KBAnchorMap.put(anchor, collateEid(KBAnchorMap.get(anchor)));
		    	}
		    }
        } catch (Exception ex) {
            Logger.getLogger(KBAnchorParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String entityId;
    private String linkEntityId;
    private String entityName = null;
    private StringBuffer buffer = new StringBuffer();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    	
       if (qName.equals("entity")) {
    	   entityId = attributes.getValue("id");
           entityName = attributes.getValue("name");            
       } else if (qName.equals("link")) {
            linkEntityId = attributes.getValue("entity_id");
            if(linkEntityId != null){
            	buffer.delete(0, buffer.length());            	
            }
       } //End link
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("entity")) { //System.out.println("Found entity end. EntityId ="+entityId+", entityName ="+entityName);
        	if(entityId != null & entityName != null){        		
        		if(KBAnchorMap.containsKey(entityName)){
        			//KBAnchorMap.get(entityName).add(entityId); //System.out.println("New EID for : "+entityName);
        			if(KBAnchorMap.get(entityName).add(entityId)){
        				KBAnchorMap.put(entityName, KBAnchorMap.get(entityName));
        			}
        		} else {
        			ArrayList<String> EidList = new ArrayList<String>();
        			EidList.add(entityId);
        			KBAnchorMap.put(entityName, EidList); //System.out.println("Added new : "+entityName);
        		}
        	}
        	entityId = null;
            entityName = null;
        } else if (qName.equals("link")) {
        	String anchor = buffer.toString(); 
        	if(linkEntityId != null){
        		//System.out.println("Anchor = "+anchor+" Eid = "+linkEntityId);
        		if(KBAnchorMap.containsKey(anchor)){
        			//KBAnchorMap.get(anchor).add(linkEntityId);
        			if(KBAnchorMap.get(anchor).add(entityId)){
        				KBAnchorMap.put(anchor, KBAnchorMap.get(anchor));
        			}
        		} else {
        			ArrayList<String> EidList = new ArrayList<String>();
        			EidList.add(linkEntityId);
        			KBAnchorMap.put(anchor, EidList);
        		}
        	}
        	linkEntityId = null;
        } 
    }
    
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        buffer.append(ch, start, length);
    }
    public static void printAnchorMap(Map<String, ArrayList<String>> AnchorMap){
		for(String anchor : AnchorMap.keySet()){
			System.out.println(anchor + "  "+ AnchorMap.get(anchor));
		}
	}//End of printAnchorMap
    //Collate duplicate entries of inlist into single entry in outlist
    public ArrayList<String> collateEid(ArrayList<String> inlist){
    	ArrayList<String> outlist = new ArrayList<String>();
    	Set<String> unique = new HashSet<String>(inlist);
    	for (String key : unique) {
    		outlist.add(key + "|" + Collections.frequency(inlist, key));
    	}
    	return outlist;
    }//End of collateEid
    
    public Map<String, ArrayList<String> > getKBAnchorMap(){
    	return this.KBAnchorMap;
    }
    public boolean writeMapToFile(Map<String, ArrayList<String> > KBanchorMap, String filename) {
    	boolean writting = false;
		try {
			File file = new File(filename);
		
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
 
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			//String content = null;
			for(String anchor : KBanchorMap.keySet()){
				
				String content = anchor +","+Helper.getString(KBanchorMap.get(anchor), ",")+"\n";
				//System.out.println(content);
				if(content.length()>0){
					writting = true;
				}
				bw.write(content);
			}
			
			
			bw.close(); //System.out.println("Done");
 
		} catch (IOException e) {
			e.printStackTrace();
		}
		return writting;
    }//End writeFile
  
	public void writeFileToMap (String filename) {
		try{
			System.out.println("Loading KBAnchorMap from "+filename);
			BufferedReader bfr = new BufferedReader(new FileReader(filename));
			String line = "";
			while ( (line = bfr.readLine()) != null ) {
				if(line.contains(",")){ 
					String[] linesplit = line.split(",");
					ArrayList<String> EidList = new ArrayList<String>();
					if(linesplit.length == 2){
						EidList.add(linesplit[1]);
					} else if(linesplit.length > 2){
						EidList.add(linesplit[1]);
						for(int i=2; i<linesplit.length; ++i){
							EidList.add(linesplit[i]);
						}
					}
					KBAnchorMap.put(linesplit[0], EidList);
				}			
			}
			bfr.close();
		} catch(IOException ioe){
			ioe.printStackTrace();
		}
	}//end writeFileToMap()
	
}//End class
