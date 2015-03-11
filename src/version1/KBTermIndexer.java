package version1;

import java.io.File;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;

import tac.model.Entity;
import tac.model.Entity.Category;
import Variables.Variables;

/**
 * Index all terms in a KB
 * @author priya.r
 */
public class KBTermIndexer extends DefaultHandler {

	    public static void main(String[] args) throws Exception{
        KBTermIndexer indexer = new KBTermIndexer();
		kbaParser = new KBAnchorParser(Variables.TACKBdir);
		kbaParser.writeFileToMap(Variables.TACKBsortedIndexFile);//load the KBAAnchopMap
        indexer.kbDir = new File(Variables.TACKBdir);
        indexer.index();
        //indexer.getTermFreq("anchor");
    }
    
    //-------------------------------------------------------------------------------
		private static MongoClient mongoClient; 
		private static DB db;
		private static DBCollection table;
		private static KBAnchorParser kbaParser;
		/*Constructor*/
		public KBTermIndexer () {
			try {
				MongoClientOptions mongoOptions = new MongoClientOptions.Builder().connectionsPerHost(100).autoConnectRetry(true).socketTimeout(5000).connectTimeout(5000).maxWaitTime(2000).build();
				mongoClient = new MongoClient (Variables.TACKBTermIndexer , mongoOptions); 			
				db = mongoClient.getDB("KBtermDB");
				table = db.getCollection("KBterms");		        
			} catch (UnknownHostException uke){
				uke.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		
		public void destroy(){
			mongoClient.close();
		}

		public void indexDoc (String term, int termFreq) {
			BasicDBObject doc = new BasicDBObject("term", term.replaceAll("\\s+", " "));
			table.update(doc,new BasicDBObject("$set", new BasicDBObject("termFreq", termFreq)), true, false);			
		}

		/* increments  termFreq by 1*/
		public void updateDoc (String term) {
			BasicDBObject query = new BasicDBObject("term", term.replaceAll("\\s+", " "));
			table.update(query,new BasicDBObject("$inc", new BasicDBObject("termFreq",1)), false, false);
		}

	    
    /* Returns number of times 'anchor' occurs in KB corpus */
    public int getTermFreq (String anchor) {
		db.requestStart();
		int totalFreq = 0;			
		BasicDBObject query = new BasicDBObject(); // create an empty query 
		query.put( "term", anchor);
		BasicDBObject fields = new BasicDBObject("termFreq",true).append("_id",false);		
		DBObject obj = table.findOne(query, fields);//System.out.println("num of results = "+curs.count());
		if(obj != null) { //System.out.println("Freq = "+o.get("totalFreq").toString());
			totalFreq =  (int)obj.get("termFreq");
		}		
		db.requestDone();
		return totalFreq;
	}//End getTermFreq()    
    private File kbDir;     

    public void index() {
        ArrayList<File> files = new ArrayList<File>();
        ArrayList<File> subFiles = new ArrayList<File>();
        files.addAll(Arrays.asList(kbDir.listFiles()));
        do {
            for (File file : files) {
                System.out.println("Indexing " + file.getAbsolutePath() + " ...");
                if (file.isDirectory()) {
                    subFiles.addAll(Arrays.asList(file.listFiles()));
                } else {
                    parse(file);
                }
            }
            files.clear();
            files.addAll(subFiles);
            subFiles.clear();
        } while (!files.isEmpty());

    }

    private void parse(File file) {
        SAXParser parser;
        try {
            parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(file, this);
        } catch (Exception ex) {
            Logger.getLogger(KBTermIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private Entity entity;
    private Category category;
    private String fact;
    private StringBuffer buffer = new StringBuffer();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        if (qName.equals("entity")) {
            entity = new Entity();
            entity.setId(attributes.getValue("id"));
            entity.setName(attributes.getValue("name"));
            entity.setType(attributes.getValue("type"));
            entity.setSource("wiki:" + attributes.getValue("wiki_title"));
        } else if (qName.equals("link")) {
            String linkEntity = attributes.getValue("entity_id");
            linkEntity = (linkEntity == null) ? "" : linkEntity + "~";
            buffer.append(linkEntity);
        } else if (qName.equals("facts")) {
            category = entity.new Category();
            category.setName(attributes.getValue("class").replaceAll("(?i)Infobox(_)*( )*", ""));
        } else if (qName.equals("fact")) {
            buffer.delete(0, buffer.length());
            fact = attributes.getValue("name");
        } else if (qName.equals("wiki_text")) {
            buffer.delete(0, buffer.length());
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("entity")) {
            System.out.println("Indexing entity => " + entity.getName().toString());
            index(entity);
            entity = null;
        } else if (qName.equals("facts")) {
            entity.addCategory(category);
        } else if (qName.equals("fact")) {
            category.addFact(fact, buffer.toString());
        } else if (qName.equals("wiki_text")) {
            entity.setContextText(buffer.toString());
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        buffer.append(ch, start, length);
    }

    private void index(Entity entity) {
        //name
    	if(getTermFreq(entity.getName()) > 0){//name present in termKB
    		updateDoc(entity.getName());
    	} else {//insert new entry
    		indexDoc(entity.getName(),1);
    	}
        //context
    	if(entity.getContextText() != null){ //System.out.println("Context text : "+entity.getContextText());
    		ArrayList<String> waitWords = new ArrayList<String>();
    		String waiter = null;
    		for(String corpusWord : entity.getContextText().split(" ")){
    			corpusWord = corpusWord.trim();
    			  				
    			if(isKBEntityName(corpusWord)){//check the KBentityname for this term,
    				processTerm(corpusWord);	        			
				} else {//not a KBentityname
					if(waitWords.size()>1){
						String newCorpusWord = corpusWord;
						corpusWord = waiter.concat(" ").concat(corpusWord);
						if(waitWords.contains(corpusWord)){
							processTerm(corpusWord);
							waitWords.clear();
							waiter = null;
						} else {
							waitWords = getKBEntitiesStartingOn(corpusWord);
							if(waitWords.size()>1){ 
								waiter = corpusWord;
								continue;
							} else {
								processTerm(waiter);
								processTerm(newCorpusWord);
							}
						}

					} else {
						waitWords = getKBEntitiesStartingOn(corpusWord);
						if(waitWords.size()>1){ 
							waiter = corpusWord;
							continue;
						} else {
							processTerm(corpusWord);
						}
					}
				}        			        			
    			
    		}//End for each word
    	}
    	//"properties"
    	HashMap<String,String> infoStringMap = entity.getAllPropertiesMap(); 
    	if(infoStringMap.size() > 0){
    		for(String fact : infoStringMap.keySet()){
    			processTerm(infoStringMap.get(fact));    			
    		}//End for each word
    	}

    }//End index(Entity)
    /*returns list of KB entity-name that starts with the given word*/
    private ArrayList<String>  getKBEntitiesStartingOn(String word){
    	ArrayList<String> retList = new ArrayList<String>();
    	Set<String> anchors = kbaParser.getKBAnchorMap().keySet();
    	Iterator<String> it = anchors.iterator();
    	while(it.hasNext()){
    		String anchor = it.next();
    		if(anchor.startsWith(word)){
    			retList.add(anchor);
    		}
    	}    	
    	return retList;
    }//End getKBEntitiesStartingOn
    /*Is the term a KBanchor or not*/
    private boolean isKBEntityName(String term){    	
    	if(kbaParser.KBAnchorMap.containsKey(term)){
    		return true;
    	} else {
    		return false;
    	}    	
    }//End isKBEntityName
    private void processTerm(String term){
    	
		if(stopWords.contains(term)){
			//skip this word
		} else {
			if(getTermFreq(term) > 0){//word present in termKB
		   		updateDoc(term);
	    	} else {
	    		indexDoc(term, 1);	    		
	    	}
		}
		
    }//End processTerm

    private static final String stopWords = "a abaft aboard about above abstract abstraction across afore aforesaid after again against agin ago aint albeit alFha all allowing almost alone along alongside already also although always am american amid amidst among amongst an and anent ani another any anybody anyone anything are aren't around as aslant aspect astride at athwart away b back bar barring base be because been before behind being below beneath beside besides best better between betwixt beyond both but c by -by can cannot can't case certain circa close concerning considering cos could couldn't couldst d dare dared daren't dares daring despite did didn't didnt different directly disseminate do does doesn't doing done don't dont dost doth down during durst e each early either em english enough ere et even ever every everybody everyone everything exactly example except excepting f failing far few fig first find finding five following for four from fully g get getting gonna gotta greater h ha had hadn't hard has hasn't hast hath have haven't having he he'd he'll her here here's hers herself he's high him himself his home how howbeit however how's i id identifie if ill i'm immediately implementing important in initiate input inside instantly into introduction is isn't it it'll it's its itself i've ive j just k l large last later least left less lest let's like likewise little living long m make many may mayn't me merely mid midst might mightn't mine minus more most much must mustn't my myself n near 'neath need needed needing needn't needs neither never nevertheless new next nigh nigher nighest nisi no no-one nobody none nor not nothing notwithstanding now o o'er off often of on once one oneself onli only onto open or other otherwise ought oughtn't our ours ourselves out output outside over own p past pending per performing perhaps plus possible present previous probably propose provided providing public q qua quite r rather re real realize really require respecting right round s same sans save saving second several shall shalt shan't she shed shell she's short should shouldn't show since six small so some somebody someone something sometimes soon special specific specification still such summat supposing sure t than that that'd that'll that's the thee thei their theirs their's them themselves then there thereby there's these they they'd they'll they're they've thi thine this tho those thou though three thro' through throughout thru thu thyself till to today together too touching toward towards true 'twas 'tween 'twere 'twill 'twixt two 'twould typical u under underneath unless unlike until unto up upon upto us use used usually v various versus very via vice vis-a-vis w wa wanna wanting was wasn't way we we'd well were weren't wert we've what whatev whatever what'll what's when whencesoever whenever when's where whereas where's whether which whichever whichsoever while whilst who who'd whoever whole who'll whom whore who's whose whoso whosoever why will with within without wont would wouldn't wouldst x y ye yet yield you you'd you'll your you're yours yourself yourselves you've z";
}//End class
