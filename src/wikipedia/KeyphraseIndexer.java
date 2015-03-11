package wikipedia;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import Variables.Variables;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;

import db.wikiPageIndex;

/*Class - creates a mongoDB of anchor to inlink page index
 *  Typical record anchor|link freq|total freq|page id|freq of anchor in page id
 *  */
public class KeyphraseIndexer {
	private static MongoClient mongoClient; 
	private static DB db;
	private static DBCollection table;
	private static List<DBObject> listDBO = new ArrayList<DBObject>();
	
	/*Constructor*/
	public KeyphraseIndexer () {
		try {
			MongoClientOptions mongoOptions = new MongoClientOptions.Builder().autoConnectRetry(true).connectionsPerHost(100).socketTimeout(60000).connectTimeout(60000).maxWaitTime(2000).threadsAllowedToBlockForConnectionMultiplier(3).build();
			mongoClient = new MongoClient (Variables.currentDB , mongoOptions); 			
			db = mongoClient.getDB("wikiKeyphraseDB");
			table = db.getCollection("wikiKeyphrase");
		} catch (UnknownHostException uke){
			uke.printStackTrace();
		}
	}
	
	public void destroy(){
		mongoClient.close();
	}

	public void indexDoc (String title, ArrayList<String> keyphrases) {
		BasicDBObject doc = new BasicDBObject("title",title.replace(" ", "_")).
				append("keyphrases", keyphrases);
		table.insert(doc);
	}
	
	public void addToIndexQueue (String title, ArrayList<String> keyphrases){
		BasicDBObject doc = new BasicDBObject("title",title.replace(" ", "_")).
				append("keyphrases", keyphrases);
		listDBO.add(doc);
		if(listDBO.size()%1000 == 0){
			table.insert(listDBO);
			System.out.print(listDBO.size()+" ");
			listDBO.clear();
		} 
	}//end addToIndexQ()
	
	public int getQueueSize(){
		return(this.listDBO.size());
	}//getQueueSize
	
	public void emptyQueue(){
		if(listDBO.size()>0){
		  table.insert(listDBO);
		  listDBO.clear();
		}
	}//end emptyQueue

/*	public void indexSortedFile (String filename) {
		try{
			System.out.println("Indexing "+filename);
			BufferedReader bfr = new BufferedReader(new FileReader(filename));
			String line = "";
			ArrayList<DBObject> anchorArr = new ArrayList<DBObject>();
			int count = 0;
			while ( (line = bfr.readLine()) != null ) {
				if(line.contains("|")){ 
					String[] linesplit = line.split("\\|");
					if(linesplit.length==5){
						anchorArr.add(indexDoc(linesplit[0], Integer.parseInt(linesplit[2]), Integer.parseInt(linesplit[3]), Integer.parseInt(linesplit[4])));
						count++;
						if(count%10000==0){
							table.insert(anchorArr);		
							anchorArr.clear(); //System.out.println(count);
						}
					} else if(linesplit.length>5){
						anchorArr.add(indexDoc(linesplit[0], Integer.parseInt(linesplit[2]), Integer.parseInt(linesplit[3]), Integer.parseInt(linesplit[4])));
						for(int i=5;i<linesplit.length;i=i+2){
							if(linesplit[i].matches("\\d+") && linesplit[i+1].matches("\\d+")){ 	//Numeric string validation test
								anchorArr.add(indexDoc(linesplit[0], 0, Integer.parseInt(linesplit[i]), Integer.parseInt(linesplit[i+1])));
								count++;
								if(count%10000==0){
									table.insert(anchorArr);		
									anchorArr.clear(); //System.out.println(count);
								}
							}
						}//End for
					}
				}			
			}
			if(count%10000!=0){
				table.insert(anchorArr);		
				anchorArr.clear();
				System.out.println(count);
			}
			bfr.close();
		} catch(IOException ioe){
			ioe.printStackTrace();
		}
	}//end indexSortedFile()

	/* Returns number of times 'anchor' occurs in Wikipedia as a hyperlink */
	/*public int getTotalLinkFreq (String anchor) {
		db.requestStart();
		int totalFreq = 0;			
		BasicDBObject query = new BasicDBObject(); 
		query.put( "anchor", anchor);
		BasicDBObject fields = new BasicDBObject("anchor_freq",true).append("_id",false);		
		DBObject obj = table.findOne(query, fields);//System.out.println("num of results = "+curs.count());
		if(obj != null) {
			//System.out.println("Link Freq = "+o.get("anchPageFreq").toString());
			totalFreq =  (int)obj.get("anchor_freq");
		}		
		db.requestDone();
		return totalFreq;		
	}//End getTotalLinkFreq()	

	/* Returns number of times 'anchor' occurs in Wikipedia, but is NOT a hyperlink */
	/*public int getTotalFreq (String anchor) {
		db.requestStart();
		int totalFreq = 0;			
		BasicDBObject query = new BasicDBObject(); // create an empty query 
		query.put( "anchor", anchor);
		BasicDBObject fields = new BasicDBObject("total_freq",true).append("_id",false);		
		DBObject obj = table.findOne(query, fields);//System.out.println("num of results = "+curs.count());
		if(obj != null) { //System.out.println("Freq = "+o.get("totalFreq").toString());
			totalFreq =  (int)obj.get("total_freq");
		}		
		db.requestDone();
		return totalFreq;
	}//End getTotalFreq()
		
	/* Returns ArrayList of keyphrases the page contains in Wikipedia */
	public ArrayList<String> getKeyphrases (String title) {
		db.requestStart();
		ArrayList<String> PageCollection = new ArrayList<String>();			
		BasicDBObject query = new BasicDBObject(); 
		query.put( "title", title);
		BasicDBObject fields = new BasicDBObject("keyphrases",true).append("_id",false);		
		DBObject obj = table.findOne(query, fields);//System.out.println("num of results = "+curs.count());
		if(obj != null) {
			JSONParser jp = new JSONParser();
			JSONArray jarr = null;
			try {
				jarr = (JSONArray) jp.parse(obj.get("keyphrase").toString());
			} catch (ParseException e) {
				jarr = new JSONArray();
			}
			//System.out.println("Link Freq = "+o.get("anchPageFreq").toString());
			for(int i = 0; i < jarr.size(); i++)
			{
				String e = jarr.get(i).toString();
				e = e.replaceAll("_", " ");
				PageCollection.add(URLDecoder.decode(e));
			}
		}		
		db.requestDone();
		return PageCollection;		
	}//End getPages()

	

	public static void main(String[] args) {
		KeyphraseIndexer kpIndexer = new KeyphraseIndexer();
		String anchor = "Percy_Jackson";
		ArrayList<String> al = new ArrayList<String>();
		al.add("Rick Riordan");
		kpIndexer.addToIndexQueue(anchor, al);
		System.out.println("In queue :"+kpIndexer.getQueueSize());
		if(kpIndexer.getQueueSize() > 0){
			kpIndexer.emptyQueue();
		}
		System.out.println("In queue :"+kpIndexer.getQueueSize());
		/*Store index file in mongodb*/
		//		String sortedFile = "/path/index/xet_synonym";
		//		anchorIndexer.indexSortedFile(sortedFile);

		/*absolute freq i.e number of times anchor a occurs in Wikipedia not as an anchor*/
		//		String anchor = "samsung galaxy";
		//		System.out.println("Anchor : "+anchor+", Total freq : "+anchorIndexer.getTotalFreq(anchor));

		/*link(a) = Link freq i.e number of times a occurs in Wikipedia as an anchor*/
		//		String anchor = args[1];
		//		System.out.println("Anchor : "+anchor+", Total anchor freq : "+anchorIndexer.getTotalLinkFreq(anchor));

		/*freq(a) = freq i.enumber of times a occurs in Wikipedia (as an anchor or not)*/
		//		System.out.println("Anchor : "+anchor+", Freq : "+(anchorIndexer.getTotalFreq(anchor)+anchorIndexer.getTotalLinkFreq(anchor)));

		/*Pg(a) = Pages pointed to by anchor a*/
		
//		ArrayList<String> candList = new ArrayList<String>();
//		candList.add("Paris Abbott");
//		candList.add("The World of Abbott and Costello");
//		candList.add("L. B. Abbott");
//		wikiPageIndex wikiIndex = new wikiPageIndex();
//		for(String candidate:candList){
//			List<Long> candPagIds = anchorIndexer.getPages(candidate.toLowerCase());
//			ArrayList<String> candVectorList = new ArrayList<String>();
//			for(Long candPgId : candPagIds){
//				String pg = wikiIndex.getTitle(candPgId);
//				pg = pg.replace('_', ' ');
//				System.out.println("Adding page "+pg);
//				candVectorList.add(pg);
//			}
//			System.out.println("Cand vector list "+candVectorList);
//		}
		
//		System.out.println("Anchor : "+anchor+", Pages returned : "+(anchorIndexer.getKeyphrases(anchor).size()));
//				for(String pgId:anchorIndexer.getKeyphrases(anchor)){
//					System.out.println("Page : "+pgId);
//				}

		/*Unique Pages pointed to by anchor a*/
		//		String anchor = "conference";
		//		System.out.println("Anchor : "+anchor+", Pages returned : "+(anchorIndexer.getPages(anchor).size()));
		//
		//		Map<Integer, Integer> Pb = anchorIndexer.getPagesMap(anchor);
		//		System.out.println("Anchor : "+anchor+", Unique Pages returned : "+(Pb.size()));
		//		for(int pgId:Pb.keySet()){
		//			System.out.println("Page : "+pgId+" num : "+Pb.get(pgId));
		//		}

		/*Prior Pr(p/a) = (pages in Pg(a) that is p) / ( Pg(a) )*/
		//		int pgId = Integer.parseInt(args[2]);		
		//		int[] ans = anchorIndexer.getPageCountInPages(anchor, 33364019);
		//		System.out.println("Prior Pr(p/a) = "+((1.0*ans[1]) / ans[0]));
	}

}
