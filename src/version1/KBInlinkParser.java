package version1;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import Variables.Variables;

public class KBInlinkParser extends DefaultHandler {
    private File kbDir;
    private Map<String, ArrayList<String> > KBInlinkMap = new HashMap<String, ArrayList<String>>();
    
    public static void main(String[] args) throws Exception{
        KBInlinkParser kbaInlinkParser = new KBInlinkParser(Variables.TACKBdir);
        kbaInlinkParser.start(); //to parse TACKBDir and get Map
		//kbaInlinkParser.writeMapToFile(kbaInlinkParser.KBAnchorMap, Variables.TACKBsortedIndexFile);
        //kbaParser.writeFileToMap(Variables.TACKBsortedIndexFile);
        System.out.println("Loaded map size = "+kbaInlinkParser.KBInlinkMap.size());
        //kbaInlinkParser.printAnchorMap(kbaInlinkParser.KBInlinkMap);
    }
    
    //-------------------------------------------------------------------------------

    public KBInlinkParser(String kbDirName) throws Exception {
        this.kbDir = new File(kbDirName);
        if (!kbDir.isDirectory()) {
            throw new Exception("Resource not a directory");
        }
    }//End contructor

    public void start() {
        ArrayList<File> files = new ArrayList<File>();
        files.addAll(Arrays.asList(kbDir.listFiles()));
        KBAnchorParser kbaParser;
		try {
			kbaParser = new KBAnchorParser(Variables.TACKBdir);
		    for (File file : files) {
			    System.out.println("KBInlinkParsing " + file.getAbsolutePath() + " ...");
		        parse(file); 	        //System.out.println("Entities parsed = "+ KBAnchorMap.size());
			    System.out.println("Entities Found here = "+ KBInlinkMap.size());
			    if(kbaParser.writeMapToFile(KBInlinkMap, Variables.indexFilesOutDir+file.getName())){
			    	KBInlinkMap.clear();
			    	KBInlinkMap = new HashMap<String, ArrayList<String>>();
		    	}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	    
    }

    public void parse(File file) {
        SAXParser parser;
        try {
            parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(file, this);
        } catch (Exception ex) {
            Logger.getLogger(KBAnchorParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String entityId;
    private String linkEntityId;
    private StringBuffer buffer = new StringBuffer();
    ArrayList<String> EidList = new ArrayList<String>();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    	
       if (qName.equals("entity")) {
    	   entityId = attributes.getValue("id");                  
       } else if (qName.equals("link")) {
            linkEntityId = attributes.getValue("entity_id");
            if(linkEntityId!=null){
            	if(KBInlinkMap.containsKey(linkEntityId)){
            		ArrayList<String> outlinks = KBInlinkMap.get(linkEntityId);
            		outlinks.add(entityId);
            		KBInlinkMap.put(linkEntityId, outlinks);
            	}
            	else{
            		ArrayList<String> outlinks = new ArrayList<>();
            		outlinks.add(entityId);
            		KBInlinkMap.put(linkEntityId, outlinks);
            	}
            }
       } //End link
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
    }
    
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        buffer.append(ch, start, length);
    }
    public void printAnchorMap(Map<String, ArrayList<String>> AnchorMap){
		for(String anchor : AnchorMap.keySet()){
			System.out.println(anchor + "  "+ AnchorMap.get(anchor));
		}
	}//End of printAnchorMap
    
    public Map<String, ArrayList<String> > getKBInlinkMap(){
    	return this.KBInlinkMap;
    }
}
