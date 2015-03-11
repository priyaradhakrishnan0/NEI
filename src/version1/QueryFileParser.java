package version1;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import tac.model.Doc;
import utility.Logger;
import Variables.Variables;

public class QueryFileParser extends DefaultHandler {
	static String doc_last = "";
	static String docid_text = "";
	static String name_text = "";
	static String queryId = "";
	
	public QueryFileParser(){
		handler = new DefaultHandler();
	}
	
	private DefaultHandler handler;
	boolean name = false;
	boolean docid = false;
		
	public static Map<String, ArrayList<String>>docMentionMap = new HashMap<String, ArrayList<String>>();
	//for every query add id and mention in mentionList
	
	public void startElement(String uri, String localName,String qName, 
			Attributes attributes) throws SAXException {
		//System.out.println("Start Element :" + qName);
		if (qName.equalsIgnoreCase("query")) {
			queryId = attributes.getValue("id");
		}
		if (qName.equalsIgnoreCase("name")) {
			name = true;
		}
		if (qName.equalsIgnoreCase("docid")) {
			docid = true;			
		}
	}
	
	public void endElement(String uri, String localName,
			String qName) throws SAXException {
		//System.out.println("End Element :" + qName);
		if (qName.equalsIgnoreCase("name")) {
			name = false;
		}
		if (qName.equalsIgnoreCase("docid")) {
			docid = false;
		}
		if (qName.equalsIgnoreCase("query")) {
			ArrayList<String> mentionList = new ArrayList<String>();
			if(docMentionMap.containsKey(docid_text)){
				mentionList = docMentionMap.get(docid_text);				
			} 
			mentionList.add(queryId);
			mentionList.add(name_text); System.out.println("Doc = "+docid_text+"  mentionList = "+mentionList);
			docMentionMap.put(docid_text, mentionList);			
			docid_text = "";
			name_text = "";
		}			
	}		

	public void characters(char ch[], int start, int length) throws SAXException {
		//System.out.println(new String(ch, start, length));
		if (name) {
			name_text += new String(ch, start, length).trim();
		}
		if (docid) {
			docid_text += new String(ch, start, length).trim(); 				
		}
	}

	public static void main(String args[]){
		//parseDocument(Variables.TACtrainQueries);//E15 query.xml
		parseDocument("/home/priya/Datasets/LDC2014E15_TAC_2014_KBP_English_Entity_Linking_Training_AMR_Queries_and_Knowledge_Base_Links_V1.1/data/0.xml");
		for(String mention:docMentionMap.keySet()){
			System.out.println(mention+"  "+docMentionMap.get(mention).size()+"\n "+docMentionMap.get(mention));
		}		
	}//main()
	
	public static void parseDocument(String filename) {
		try{
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(false);
			factory.setNamespaceAware(false);					
			InputStream    xmlInput  = new FileInputStream(filename);		
			DefaultHandler handler   = new QueryFileParser();			
			SAXParser saxParser = factory.newSAXParser();					
			saxParser.parse(xmlInput, handler);
		} catch(SAXParseException w){
			w.printStackTrace();		
		} catch (FileNotFoundException e1) {
			Logger.logOut("Unable to find file:"+filename);
			e1.printStackTrace();
		} catch (IOException e2) {
			Logger.logOut("Unable to open file:"+filename);
			e2.printStackTrace();
		} catch (SAXException e3) {
			e3.printStackTrace();
		} catch (ParserConfigurationException e4) {
			e4.printStackTrace();
		}			
	}//parseDocument()
}
