
package version1;

import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.File;
import java.lang.Math;

import Variables.Variables;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
/*Class - creates a mongoDB of KB_id to KB inlink page list
 *  Typical record KB_id, link[ link1, link2, .. linkN]
 *  */
public class InlinkIndex {
	private  MongoClient mongoClient; 
	private  DB db;
	private  DBCollection table;
	
	//constructor
	public InlinkIndex() {
		try {
			MongoClientOptions mongoOptions = new MongoClientOptions.Builder().autoConnectRetry(true).connectionsPerHost(100).socketTimeout(60000).connectTimeout(30000).maxWaitTime(60000).threadsAllowedToBlockForConnectionMultiplier(3).build();			
			mongoClient = new MongoClient(Variables.TAClinkIndexer , mongoOptions);		
			db = mongoClient.getDB("TACinlinks");
			table = db.getCollection("TACinlinks");
		} catch (UnknownHostException uke){
		     uke.printStackTrace();
		}
	}//End constructor
	
	public void destroy(){
		mongoClient.close();
	}
	
	public DBObject indexDoc (String entityId, String[] linkList) {
		BasicDBObject doc = new BasicDBObject("entityId", entityId).				
				append("linkList", linkList);
		return doc;
	}
		
	/*Populate Inlink index using KBMap */
	public void indexKBMap (Map<String, ArrayList<String> > kbInlinkMap) {
		ArrayList<DBObject> anchorArr = new ArrayList<DBObject>();
		int count = 0;
		for(String inlink : kbInlinkMap.keySet()){
			List<String> EidList = kbInlinkMap.get(inlink);
			/*ArrayList to Array Conversion */
			String linkList[]= EidList.toArray(new String[EidList.size()]);
			//System.out.println(" Entry : "+inlink+" has "+linkList.length+" with EidList size "+EidList.size()+" and kbInLink size "+kbInlinkMap.get(inlink).size());
			anchorArr.add(indexDoc(inlink,linkList));
			count++;
			if(count%10000==0){
				table.insert(anchorArr);		
				anchorArr.clear(); //System.out.println(count);
			}
		}//End for inlink
		if(count%10000!=0){
			table.insert(anchorArr);		
			anchorArr.clear();
			System.out.println(count);
		}
	}//End indexKBMap
	
	/*Query Inlink index*/	
	/*Returns total number of in-links to the given entity_Id*/
	public int getInlinkFreq (String entityId) {
		db.requestStart();		
		int inlinkFreq = 0;		
		BasicDBObject query = new BasicDBObject(); 
		query.put( "entityId", entityId);
		BasicDBObject fields = new BasicDBObject("linkList",true).append("_id",false);		
		DBCursor curs = table.find(query, fields);
		while(curs.hasNext()){
			DBObject obj = curs.next(); 
		    String catObject = obj.get("linkList").toString();	
			inlinkFreq += catObject.split(",").length;				
		}		
		db.requestDone();
		return inlinkFreq;		
	}//End getTotalLinkFreq()
	
	/*Returns list of in-links to the given entityId*/
	public ArrayList<String> getInlinks (String entityId) {
		db.requestStart();
		ArrayList<String> catList = new ArrayList<String>();
		BasicDBObject query = new BasicDBObject(); 
		query.put( "entityId", entityId);
		BasicDBObject fields = new BasicDBObject("linkList",true).append("_id",false);		
		DBCursor curs = table.find(query, fields); //System.out.println("num of results = "+curs.count());
		while(curs.hasNext()){
			DBObject obj = curs.next(); 
		    String catObject = obj.get("linkList").toString();	//System.out.println(catObject);
    		catObject = catObject.replaceAll("\\[", "");
			catObject = catObject.replaceAll("\\]", "");
			for (String cat :  catObject.split(",")){
				cat = cat.replaceAll("\"", "");
				catList.add(cat);
			}			
		}		
		db.requestDone();
		return catList;		
	}//End getInLinks()
	
	public int getCommonInlinkCount(String entityId1, String entityId2){
		ArrayList<String> inList1 = getInlinks(entityId1); //System.out.println(" 1 = "+inList1);
		ArrayList<String> inList2 = getInlinks(entityId2); //System.out.println(" 2 = "+inList2);
		HashSet<String> inlinks1 = new HashSet<>();
		HashSet<String> inlinks2 = new HashSet<>();
		inlinks1.addAll(inList1);
		inlinks2.addAll(inList2);
		inlinks1.retainAll(inlinks2); //System.out.println("Common links = "+inlinks1.size());
		return inlinks1.size();
	}
	
	/*Semantic relatedness between two page titles by D.Milne method */
	public double relatedness(String entityId1, String entityId2){
		double rel=0.0;
		int inA = getInlinkFreq(entityId1);
		int inB = getInlinkFreq(entityId2); // System.out.println("inB = "+inB +", inA = "+inA+" combined inlinks = "+Math.max(inA,inB));
		/*If the two pages do not share common inlinks, they are not related*/
		double comLinks = getCommonInlinkCount(entityId1, entityId2); //System.out.println("Common inlinks = "+comLinks);
		if((comLinks > 0.0) && (Math.min(inA, inB) > 0 )){
			double numerator = Math.log((double)Math.max(inA,inB)) - Math.log(comLinks); 
			/*ln 12165935 = 16.3141503911 */
			double denomenator = 16.3141503911 - Math.log(Math.min(inA, inB)); 
			rel = 1 - (numerator / denomenator);
		}
		return rel;
	}//End rel(pa , pb )
	
	public static void main(String[] args) {
		InlinkIndex inlinkIndex = new InlinkIndex();
		
		/*Parse KB records to individual sorted files.*/ 
//		try {
//			ArrayList<File> files = new ArrayList<File>();
//	        files.addAll(Arrays.asList(new File(Variables.TACKBdir).listFiles()));
//			try {
//			    for (File file : files) {
//			    	KBInlinkParser kbInlinkParser = new KBInlinkParser(Variables.TACKBdir);
//				    System.out.println("KBInlinkParsing " + file.getAbsolutePath() + " ...");
//			        kbInlinkParser.parse(file); 	        //System.out.println("Entities parsed = "+ KBAnchorMap.size());
//				    System.out.println("Entities Found here = "+ kbInlinkParser.getKBInlinkMap().size());
//				    KBAnchorParser kbaParser  = new KBAnchorParser(Variables.TACKBdir);
//				    kbaParser.writeMapToFile(kbInlinkParser.getKBInlinkMap(), Variables.indexFilesOutDir+file.getName());				    
//				}
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}	    
			
//			/*join indivial files into single KBMap and create Inlink Index*/
//			Map<String, ArrayList<String>> KBInlinkMap = new HashMap<String, ArrayList<String>>();			
//			for(File readFile : new File(Variables.indexFilesOutDir).listFiles()){	
//				System.out.println("Processing file : "+readFile.getAbsoluteFile());
//				Charset charset = Charset.forName("US-ASCII");
//				BufferedReader reader = Files.newBufferedReader(Paths.get(readFile.getAbsolutePath(),""), charset);
//				String line = null;
//			    while ((line = reader.readLine()) != null) {
//			        String[] lineSplits = line.split(",");
//			        KBInlinkMap.put(lineSplits[0], new ArrayList(Arrays.asList(lineSplits[1])));
//			    }
//			    System.out.println("KBinlinkMap size now = "+KBInlinkMap.size());	        	
//	        }
//			inlinkIndex.indexKBMap(KBInlinkMap);
//			
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		/*Query Inlink index*/
		//System.out.println(inlinkIndex.getInlinkFreq("E0463961"));
		//System.out.println(inlinkIndex.getInlinkFreq("E0510611"));
		//System.out.println("rel(E0463961, E0510611) = "+ inlinkIndex.relatedness("E0463961", "E0510611"));	
		//System.out.println("rel(Ritz-Carlton_Hotel_Company, Lake_Las_Vegas) = "+ inlinkIndex.relatedness("Ritz-Carlton_Hotel_Company","Lake_Las_Vegas"));

		
	}

}
