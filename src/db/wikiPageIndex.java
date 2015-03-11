package db;

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

public class wikiPageIndex {
	private static MongoClient mongoClient; 
	private static DB db;
	private static DBCollection table;
	/*Constructor*/
	public wikiPageIndex() {
		try {
			MongoClientOptions mongoOptions = new MongoClientOptions.Builder().autoConnectRetry(true).connectionsPerHost(100).socketTimeout(60000).connectTimeout(60000).maxWaitTime(2000).threadsAllowedToBlockForConnectionMultiplier(3).build();
			mongoClient = new MongoClient (Variables.wikiPageIndexer , mongoOptions); 			
			db = mongoClient.getDB("wikiPageDB");
			table = db.getCollection("wikiPageTitle");
		} catch (UnknownHostException uke){
			uke.printStackTrace();
		}
	}
	
	public void destroy(){
		mongoClient.close();
	}
		
	/* @return Wikipedia title of given page-id */
	public String getTitle (Long pageId) { 	//System.err.println("Pag0" + pageId);
		String resTitle = null;
		db.requestStart();
		BasicDBObject query = new BasicDBObject(); 
		query.put( "pageId", pageId);
		BasicDBObject fields = new BasicDBObject("title",true).append("_id",false);		
		DBObject obj = table.findOne(query, fields);//System.out.println("num of results = "+curs.count());
		if(obj != null) {
			resTitle = obj.get("title").toString();			
		}		
		db.requestDone();
		return resTitle;
	}//End getTitle()

	/* @param Wikipedia title
	 * @return Wikipedia pageid  */
	public Long getPageId (String title) {
		Long resPg = (long) 0;
		db.requestStart();
		BasicDBObject query = new BasicDBObject(); 
		query.put( "title", title);
		BasicDBObject fields = new BasicDBObject("pageId",true).append("_id",false);		
		DBObject obj = table.findOne(query, fields);//System.out.println("num of results = "+curs.count());
		if(obj != null) {
			resPg = Long.parseLong(obj.get("pageId").toString(), 10);			
		}		
		db.requestDone();
		return resPg;
	}//End getPageId()

	public static void main(String[] args){
		wikiPageIndex wikiIndex = new wikiPageIndex();
		System.out.println("PageId of Isreal = " + wikiIndex.getPageId("Israel"));
		Long a = wikiIndex.getPageId("Israel");
		System.out.println("Title of page#42 = " + wikiIndex.getTitle(a));
	}//main

}//class
