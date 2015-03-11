package db;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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

public class wikilinksIndex {

	private static MongoClient mongoClient; 
	private static DB db;
	private static DBCollection table;
	/*Constructor*/
	public wikilinksIndex(String tbName) { //tb = {inlinks, outlinks}
		try {
			MongoClientOptions mongoOptions = new MongoClientOptions.Builder().autoConnectRetry(true).connectionsPerHost(100).socketTimeout(60000).connectTimeout(60000).maxWaitTime(180000).threadsAllowedToBlockForConnectionMultiplier(3).build();
			mongoClient = new MongoClient (Variables.currentDB , mongoOptions); 			
			db = mongoClient.getDB("wikilinks");
			table = db.getCollection(tbName);
		} catch (UnknownHostException uke){
			uke.printStackTrace();
		}
	}
	
	public void destroy(){
		mongoClient.close();
	}
		
	/* @param Wikipedia title
	 * @return Wikipedia inlinks(or outlinks) List */
	public ArrayList<String> getLinks (String title) {
		title = title.replaceAll(" ", "_");
		title = URLEncoder.encode(title);
		db.requestStart();
		ArrayList<String> PageCollection = new ArrayList<String>();			
		BasicDBObject query = new BasicDBObject(); 
		query.put( "page", title);
		BasicDBObject fields = new BasicDBObject("link",true).append("_id",false);		
		DBObject obj = table.findOne(query, fields);
		if(obj != null) {
			JSONParser jp = new JSONParser();
			JSONArray jarr = null;
			try { 
				jarr = (JSONArray) jp.parse(obj.get("link").toString());
			} catch (ParseException e) {
				jarr = new JSONArray();
			}
			//System.out.println("Link Freq = "+obj.get("link").toString());
			for(int i = 0; i < jarr.size(); i++)
			{
				String e = jarr.get(i).toString();
				e = e.replaceAll("_", " ");
				PageCollection.add(URLDecoder.decode(e));
			}
		}		
		db.requestDone();
		return PageCollection;		
	}//getLinks

		
	/*Semantic relatedness between two page titles by D.Milne method */
	public double relatedness(String Pa, String Pb){
		double rel=0.0;
		ArrayList<String> PaInlinks = getLinks(Pa);
		int inA = PaInlinks.size();
		HashSet<String> inlinksA = new HashSet<>(); inlinksA.addAll(PaInlinks);

		ArrayList<String> PbInlinks = getLinks(Pb);
		int inB = PbInlinks.size();
		HashSet<String> inlinksB = new HashSet<>(); inlinksB.addAll(PbInlinks);//System.out.println(Pa+" InlinksA = "+inlinksA.size()+", "+Pb+" InlinksB = "+inlinksB.size());

		inlinksB.retainAll(inlinksA);
		double comLinks = inlinksB.size();
		//System.out.println("inB = "+inB +", inA = "+inA+" combined inlinks = "+Math.max(inA,inB));
		/*If the two pages do not share common inlinks, they are not related*/
		//System.out.println("Common inlinks = "+comLinks);
		if((comLinks > 0.0) && (Math.min(inA, inB) > 0 )){
			double numerator = Math.log((double)Math.max(inA,inB)) - Math.log(comLinks); 
			/*ln 12165935 = 16.3141503911 */
			double denomenator = 16.3141503911 - Math.log(Math.min(inA, inB)); 
			rel = 1 - (numerator / denomenator);
		}
		return rel;
	}//End rel(pa , pb )
	
	
	
	public static void main(String[] args){
		//String query = "Buenos Aires";
		//query = query.replaceAll(" ", "_");
		wikilinksIndex inIndex = new wikilinksIndex("inlinks");
//		System.out.println("Res = "+inIndex.getLinks(query));
//		System.out.println("PageId of Isreal = ");
//		ArrayList<String> res =  inIndex.getLinks(query);
//		for(String e : res){
//			System.out.println(URLDecoder.decode(e));
//		}
		System.out.println(inIndex.relatedness("War_hawk","Likud" ));
		
		
		
//		wikilinksIndex outIndex = new wikilinksIndex("outlinks");
//		System.out.println("Title of page#42 = " + outIndex.getLinks("Israel"));
	}//main

}//class
