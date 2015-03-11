package db;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
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

public class wikiabstractsIndex {
	
		private static MongoClient mongoClient; 
		private static DB db;
		private static DBCollection table;
		/*Constructor*/
		public wikiabstractsIndex(String tbName) { //tb = {abstracts, words}
			try {
				MongoClientOptions mongoOptions = new MongoClientOptions.Builder().autoConnectRetry(true).connectionsPerHost(100).socketTimeout(60000).connectTimeout(60000).maxWaitTime(180000).threadsAllowedToBlockForConnectionMultiplier(3).build();
				mongoClient = new MongoClient (Variables.currentDB , mongoOptions); 			
				db = mongoClient.getDB("wikiabstracts");
				table = db.getCollection(tbName);
			} catch (UnknownHostException uke){
				uke.printStackTrace();
			}
		}
		
		public void destroy(){
			mongoClient.close();
		}
			
	/* @param Wikipedia title
	 * @return Wikipedia abstract of the title */
	public String getAbstract (String title) {
		title = title.replaceAll(" ", "_");
		title = URLEncoder.encode(title);
		String abs = null;
		db.requestStart();
		BasicDBObject query = new BasicDBObject(); 
		query.put( "key", title);
		BasicDBObject fields = new BasicDBObject("value",true).append("_id",false);		
		DBObject obj = table.findOne(query, fields);
		if(obj != null) {
			 abs = obj.get("value").toString();
		}		
		db.requestDone();
		return abs;		
	}//getAbstract
		
		
	/* @param Wikipedia title
	 * @return Wikipedia abstract words List */
	public ArrayList<String> getWords (String title) {
		title = title.replaceAll(" ", "_");
		title = URLEncoder.encode(title);
		db.requestStart();
		ArrayList<String> PageCollection = new ArrayList<String>();			
		BasicDBObject query = new BasicDBObject(); 
		query.put( "key", title);
		BasicDBObject fields = new BasicDBObject("value",true).append("_id",false);		
		DBObject obj = table.findOne(query, fields);
		if(obj != null) {
			JSONParser jp = new JSONParser();
			JSONArray jarr = null;
			try { 
				jarr = (JSONArray) jp.parse(obj.get("value").toString());
			} catch (ParseException e) {
				jarr = new JSONArray();
			}
			//System.out.println("Link Freq = "+obj.get("link").toString());
			for(int i = 0; i < jarr.size(); i++)
			{
				PageCollection.add(jarr.get(i).toString());
			}
		}		
		db.requestDone();
		return PageCollection;		
	}//getWords

		public static void main(String[] args){
			String query = "Swedish Space Corporation";//"`Abbas Koshteh";//"Abbas and Templecombe";//"Buenos Aires";
			wikiabstractsIndex abstractIndex = new wikiabstractsIndex("abstracts");
			System.out.println("Res = "+  abstractIndex.getAbstract(query));
			wikiabstractsIndex wordIndex = new wikiabstractsIndex("words");
			System.out.println("Words = "+ wordIndex.getWords(query));
		}//main

	}//class



