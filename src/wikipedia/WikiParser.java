package wikipedia;

import java.awt.print.PageFormat;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import edu.jhu.nlp.wikipedia.PageCallbackHandler;
import edu.jhu.nlp.wikipedia.WikiPage;
import edu.jhu.nlp.wikipedia.WikiPageIterator;
import edu.jhu.nlp.wikipedia.WikiXMLParser;
import edu.jhu.nlp.wikipedia.WikiXMLParserFactory;

//import as4.MyWikiXMLParser;

public class WikiParser {

	/**
	 * @param args
	 */
	MyHandler handler = new MyHandler();
	
	public static void main(String[] args) {
		WikiParser wikiParser = new WikiParser();
		//String read = null, wikiFileName =null, inputFile=null ;
		//String inDirPath = "./Wiki_Split_Files";//for VM
		String inDirPath = "/home/priya/wikiDump";//for local machine
		String inputFile = inDirPath+"/enwiki-latest-pages-articles.xml";
		wikiParser.parseFile(inputFile);		
	}//main
	
	public void parseFile(String readFile)
	{		
	

		WikiXMLParser wxp = WikiXMLParserFactory.getSAXParser(readFile);
		try {

		wxp.setPageCallback(handler);
		wxp.parse();	
		if(handler.keyphraseIndexer.getQueueSize() > 0){
			handler.keyphraseIndexer.emptyQueue();
		}
		System.out.println("In queue :"+handler.keyphraseIndexer.getQueueSize());		
		handler.keyphraseIndexer.destroy();
		
		}catch(Exception e) {
			System.out.println("WikiXMLParser reading exception");
			e.printStackTrace();
		}
		wxp = null;	

	}//End parseFile
}

class MyHandler implements PageCallbackHandler {
	String indexPath = "/home/priya/Desktop/WebMining/201050035_as5/luceneIndex/";		
	KeyphraseIndexer keyphraseIndexer = new KeyphraseIndexer();
    NERfetcher n = new NERfetcher();
    
	public void process(WikiPage page) {
		
		if(page.getTitle() != null){				
			String titleText = page.getTitle().trim(); //	System.out.println(" 1)"+titleText);
			String wikitext = page.getText().trim();
			if(wikitext != null){
				ArrayList<String> keyNE = n.extractEntities(wikitext); //System.out.println("Keyphrases "+keyNE);				
				keyphraseIndexer.addToIndexQueue(titleText, keyNE);				
			}
			
		}//End if title	
		
	}//End process
}//class MyHandler