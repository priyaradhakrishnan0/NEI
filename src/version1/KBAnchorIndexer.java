package version1;

import java.io.BufferedReader;
import java.io.File;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;

/*Class - creates a mongoDB of KBanchor to KB page index (i.e Eid)
 *  Typical record anchor|link freq|total freq|page id|freq of anchor in page id
 *  */
public class KBAnchorIndexer {
	private  MongoClient mongoClient; 
	private  DB db;
	private  DBCollection table;
	private  KBTermIndexer kbtermIndexer;
	/*Constructor*/
	public KBAnchorIndexer () {
		try {
			MongoClientOptions mongoOptions = new MongoClientOptions.Builder().autoConnectRetry(true).connectionsPerHost(100).socketTimeout(60000).connectTimeout(30000).maxWaitTime(60000).threadsAllowedToBlockForConnectionMultiplier(3).build();			
			mongoClient = new MongoClient (Variables.currentDB , mongoOptions); 			
			db = mongoClient.getDB("KBanchorDB");
			table = db.getCollection("KBanchors");
			kbtermIndexer = new KBTermIndexer();
		} catch (UnknownHostException uke){
			uke.printStackTrace();
		}
	}
	
	public void destroy(){
		mongoClient.close();
	}

	public DBObject indexDoc (String anchor, int totalFreq, Map<String, Integer> pages) {
		ArrayList<DBObject> pageList = new ArrayList<DBObject>();
		for(String Eid : pages.keySet()){
			BasicDBObject page_doc = new BasicDBObject("pageId", Eid).append("pageFreq", pages.get(Eid));
			pageList.add(page_doc);
		}		
		BasicDBObject doc = new BasicDBObject("anchor", anchor.replaceAll("\\s+", " ").replaceAll("–", "-")).
				append("totalFreq", totalFreq).
				append("pages", pageList);				
		return doc;
	}

	public void indexKBMap (Map<String, ArrayList<String> > KBAnchorMap) {

		ArrayList<DBObject> anchorArr = new ArrayList<DBObject>();
		int count = 0;
		for(String anchor : KBAnchorMap.keySet()){
			ArrayList<String> EidList = KBAnchorMap.get(anchor);
			Map<String, Integer> pages = new HashMap<String, Integer>();
			for(int i=0;i<EidList.size();++i){
				String[] EidSplit = EidList.get(i).split("\\|");
				if(EidSplit.length==2){
					if(EidSplit[1].matches("\\d+")){	//Numeric string validation test
						pages.put(EidSplit[0], Integer.parseInt(EidSplit[1]));							
					}
				}
			}//End for Eid
			anchorArr.add(indexDoc(anchor, kbtermIndexer.getTermFreq(anchor), pages));
			System.out.println("indexed anchor = "+anchor+" , Pages "+pages);
			count++;
			if(count%10000==0){
				table.insert(anchorArr);		
				anchorArr.clear(); System.out.println("Indexed "+count+" .. ");
			}
		}//End for anchor

		if(count%10000!=0){
			table.insert(anchorArr);		
			anchorArr.clear();
			System.out.println("Final AchorDB size = "+count);
		}

	}//end indexKBMap

	/* Returns number of times 'anchor' occurs in TAC KB as a hyperlink */
	public int getTotalLinkFreq (String anchor) {
		db.requestStart();
		int totalFreq = 0;			
		BasicDBObject query = new BasicDBObject(); 
		query.put( "anchor", anchor);
		BasicDBObject fields = new BasicDBObject("pages",true).append("_id",false);		
		DBCursor curs = table.find(query, fields);//System.out.println("No of results = "+curs.count());
		db.requestDone();
		while(curs.hasNext()){
			DBObject obj = curs.next(); 
			JSONParser jp = new JSONParser();
			JSONArray jarr = null;
			try {
				jarr = (JSONArray) jp.parse(obj.get("pages").toString());
			} catch (ParseException e) {
				jarr = new JSONArray();
			}
			//System.out.println("Link Freq = "+o.get("anchPageFreq").toString());
			for(int i = 0; i < jarr.size(); i++)
			{
				JSONObject objects = (JSONObject) jarr.get(i);
				if(objects.get("pageFreq") != null){ //System.out.println("pageFreq = "+objects.get("pageFreq"));
					totalFreq +=  Integer.parseInt( objects.get("pageFreq").toString() );
				}				
			}
		}		

		return totalFreq;		
	}//End getTotalLinkFreq()	

	/* Returns number of times 'anchor' occurs in Wikipedia, but is NOT a hyperlink */
	public int getTotalFreq (String anchor) {
		db.requestStart();
		int totalFreq = 0;			
		BasicDBObject query = new BasicDBObject(); // create an empty query 
		query.put( "anchor", anchor);
		BasicDBObject fields = new BasicDBObject("totalFreq",true).append("_id",false);		
		DBObject obj = table.findOne(query, fields);//System.out.println("num of results = "+curs.count());
		if(obj != null) { //System.out.println("Freq = "+o.get("totalFreq").toString());
			totalFreq =  (int)obj.get("totalFreq");
		}		
		db.requestDone();
		return totalFreq;
	}//End getTotalFreq()
		
	/* Returns List of Entity-id of entities the string 'anchor' points to in TAC KB */
	public ArrayList<String> getPages (String anchor) {
		db.requestStart();
		ArrayList<String> PageCollection = new ArrayList<String>();			
		BasicDBObject query = new BasicDBObject(); 
		query.put( "anchor", anchor);
		BasicDBObject fields = new BasicDBObject("pages",true).append("_id",false);		
		DBCursor curs = table.find(query, fields); //System.out.println("num of results = "+curs.count());
		db.requestDone();
		while(curs.hasNext()) {
			DBObject obj = curs.next(); 
			JSONParser jp = new JSONParser();
			JSONArray jarr = null;
			try {
				jarr = (JSONArray) jp.parse(obj.get("pages").toString());
			} catch (ParseException e) {
				jarr = new JSONArray();
			}
			//System.out.println("Link Freq = "+o.get("anchPageFreq").toString());
			for(int i = 0; i < jarr.size(); i++)
			{
				JSONObject objects = (JSONObject) jarr.get(i);
				PageCollection.add(objects.get("pageId").toString());
			}
		}		

		return PageCollection;		
	}//End getPages()

	/* Returns map of entity-ids to number of inlinks to those pages. 
	 * Entity ids are Entities the string 'anchor' points to in TAC KB */
	public Map<String, Integer> getPagesMap (String anchor) {
		db.requestStart();
		Map<String, Integer> PageCollection = new HashMap<String, Integer>();			
		BasicDBObject query = new BasicDBObject(); 
		query.put( "anchor", anchor);		
		BasicDBObject fields = new BasicDBObject("pages", true).append("_id",false);		
		DBCursor curs = table.find(query, fields); //System.out.println("num of results = "+curs.count());
		db.requestDone();
		while(curs.hasNext()) {
			DBObject obj = curs.next();			
			JSONParser jp = new JSONParser();
			JSONArray jarr = null;
			try {
				jarr = (JSONArray) jp.parse(obj.get("pages").toString());
			} catch (ParseException e) {
				jarr = new JSONArray();
			}
			for(int i = 0; i < jarr.size(); i++)
			{
				JSONObject objects = (JSONObject) jarr.get(i);
				String pId = (String)(objects.get("pageId"));
				int pValue =  Integer.parseInt(objects.get("pageFreq").toString());
				if(PageCollection.containsKey(pId)){
					pValue = PageCollection.get(pId)+ pValue;
				} 
				PageCollection.put(pId, pValue);
			}
		}
		return PageCollection;		
	}//End getPagesMap()
	
	/* Returns map of Entity-ids to number of inlinks to those pages. 
	 * Only those pages whose inlinks contribute to 'restriction' % of the total inlinks are returned.
	 * Entity ids are pages the string 'anchor' points to in Wikipedia. */
	public Map<String, Integer> getPagesMap (String anchor, double restriction) {
		db.requestStart();
		Map<String, Integer> PageCollection = new HashMap<String, Integer>();			
		BasicDBObject query = new BasicDBObject(); 
		query.put( "anchor", anchor);		
		BasicDBObject fields = new BasicDBObject("pages",true).append("_id",false);		
		DBCursor cur = table.find(query, fields);
		db.requestDone();
		int freq = getTotalLinkFreq(anchor); //System.out.println(" freq check = "+restriction*freq);
		while(cur.hasNext()) {
			DBObject obj = cur.next();			
			JSONParser jp = new JSONParser();
			JSONArray jarr = null;
			try {
				jarr = (JSONArray) jp.parse(obj.get("pages").toString());
			} catch (ParseException e) {
				jarr = new JSONArray();
			} 
			for(int i = 0; i < jarr.size(); i++)
			{
				JSONObject objects = (JSONObject) jarr.get(i);
				String pId = (String)(objects.get("pageId"));
				int pValue = Integer.parseInt(objects.get("pageFreq").toString());
							
				if(PageCollection.containsKey(pId)){
					pValue = PageCollection.get(pId)+ pValue;
				} //System.out.println("Pid ="+pId+", Pvalue = "+pValue);
				if( (pValue/(1.0*freq)) >restriction){
					PageCollection.put(pId, pValue);
				}		
			}
		}
		return PageCollection;		
	}//End getPagesMap()
	
	
	/* Returns two member integer array. 
	 * member 0 = total number of inlinks for the string anchor.
	 * member 1 = number of inlinks to given EntityId from the String anchor.*/
	public int[] getPageCountInPages (String anchor, String EntityId) {
		db.requestStart();
		int[] PageCountResults  = new int[2];;
		int pageCount = 0;
		int totalCount = 0;
		BasicDBObject query = new BasicDBObject(); 
		query.put("anchor", anchor);
		BasicDBObject fields = new BasicDBObject("pages",true).append("_id",false);		
		DBCursor cur = table.find(query, fields);//System.out.println("Pages Total = "+curs.count());
		db.requestDone();
		while(cur.hasNext()) {
			DBObject obj = cur.next();			
			JSONParser jp = new JSONParser();
			JSONArray jarr = null;
			try {
				jarr = (JSONArray) jp.parse(obj.get("pages").toString());
			} catch (ParseException e) {
				jarr = new JSONArray();
			}			
			for(int i = 0; i < jarr.size(); i++)
			{
				JSONObject objects = (JSONObject) jarr.get(i);
				String pId = (String)(objects.get("pageId"));
				if(EntityId.equalsIgnoreCase(pId)){
					pageCount +=  Integer.parseInt(objects.get("pageFreq").toString());
				}			
				totalCount +=  Integer.parseInt(objects.get("pageFreq").toString());
			}
		}
		PageCountResults[0] = totalCount;
		PageCountResults[1] = pageCount; //System.out.println("Pages matching = "+pageCount);
		return PageCountResults;		
	}//End getPageCountInPages()

	/*link-probability - Probability that an occurrence of a is an anchor pointing to some Wikipedia page*/
	public double lp(String anchor){
		int totalFreq = getTotalFreq(anchor);
		int totalLinkFreq = getTotalLinkFreq(anchor);
		if((totalFreq+totalLinkFreq) > 0){
			//System.out.println("totFr = "+totalFreq+" totLinkFr = "+totalLinkFreq);
			return (double) (totalLinkFreq*1.0/(totalFreq+totalLinkFreq));
		} else {
			return 0.0;
		}
	}

	
	public static void main(String[] args) {
		KBAnchorIndexer KBanchorIndexer = new KBAnchorIndexer();
		
		/*Store KBanchorMap in mongodb*/
		/*Method 1*/
//	    KBAnchorParser kbaParser;
//		try {
//			kbaParser = new KBAnchorParser(Variables.TACKBdir);
//			kbaParser.start();
//	        //kbaParser.writeFileToMap(Variables.TACKBsortedIndexFile);
//	        System.out.println("Loaded map size = "+kbaParser.getKBAnchorMap().size());
//			KBanchorIndexer.indexKBMap(kbaParser.getKBAnchorMap());
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}  
		/*Method 2*/ /*this works */
//		try {
//			ArrayList<File> files = new ArrayList<File>();
//			files.addAll(Arrays.asList(new File(Variables.TACKBdir).listFiles()));
//			 KBAnchorParser kbaParser;
//			 int fileCount = 0;
//		    for (File file : files) {
//		    	fileCount++;
//		    	kbaParser = new KBAnchorParser(Variables.TACKBdir);
//			    System.out.println("KBAnchorParsing " + file.getAbsolutePath() + " ..."+fileCount+" out of "+files.size());
//		        kbaParser.parse(file); 	        System.out.println("Entities parsed = "+ kbaParser.getKBAnchorMap().size());
//		        KBanchorIndexer.indexKBMap(kbaParser.getKBAnchorMap());			    				    
//			}		
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		/*absolute freq i.e number of times anchor a occurs in Wikipedia not as an anchor*/
				String anchor = "samsung";
				System.out.println("Anchor : "+anchor+", Total freq : "+KBanchorIndexer.getTotalFreq(anchor));

		/*link(a) = Link freq i.e number of times a occurs in Wikipedia as an anchor*/
//				String anchor = "U.S.A";//args[1];
//				System.out.println("Anchor : "+anchor+", Total anchor freq : "+KBanchorIndexer.getTotalLinkFreq(anchor));
//				System.out.println("Anchor : "+anchor+", Total freq : "+KBanchorIndexer.getTotalFreq(anchor));
//				System.out.println("Anchor : "+anchor+", link prob : "+KBanchorIndexer.lp(anchor));
				//System.out.println("Anchor : "+anchor+", Pages list : "+KBanchorIndexer.getPages(anchor));
				//System.out.println("Anchor : "+anchor+", Pages Map : "+KBanchorIndexer.getPagesMap(anchor));
				//System.out.println("Anchor : "+anchor+", Pages Map with restriction : "+KBanchorIndexer.getPagesMap(anchor,0.1));
				//System.out.println(" Page count of E0247288 in pages "+KBanchorIndexer.getPageCountInPages(anchor, "E0247288"));

		/*freq(a) = freq i.enumber of times a occurs in Wikipedia (as an anchor or not)*/
		//		System.out.println("Anchor : "+anchor+", Freq : "+(anchorIndexer.getTotalFreq(anchor)+anchorIndexer.getTotalLinkFreq(anchor)));

		/*Pg(a) = Pages pointed to by anchor a*/
		//		System.out.println("Anchor : "+anchor+", Pages returned : "+(anchorIndexer.getPages(anchor).size()));
		//		for(int pgId:anchorIndexer.getPages(anchor)){
		//			System.out.println("Page : "+pgId);
		//		}

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
