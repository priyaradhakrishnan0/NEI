package utility;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import com.mongodb.util.JSONSerializers;


public class WikiOperator {
	public static void main(String[] args){
		WikiOperator wikiop = new WikiOperator();
		List<String> l = new ArrayList<String>();
		l.add("Abbott"); l.add("Israel"); l.add("Jaathi");
		System.out.println("Wiki eq : "+wikiop.getWikiEquivalents(l));
//		System.out.println(wikiop.getWikiEquivalent("Jaathi"));
	}//main()

	/*Query wikipedia.
	* input ;title of the page
	* output : Method returns list of most probable wiki-title for the given titleList */
	public ArrayList<String> getWikiEquivalents(List<String> titles)
	{
		ArrayList<String> wikiTitles = new ArrayList<String>();
		
		System.getProperties().put("proxySet", true);
		System.getProperties().put("proxyHost", "proxy.iiit.ac.in");
		System.getProperties().put("proxyPort", "8080");
		URL url = null;
		URLConnection tf = null;
		BufferedReader in = null;
		String inputLine,data="";
		//ArrayList<String> querys = new ArrayList<String>();
		StringBuilder titleString = new StringBuilder();
		//System.out.println("num titles = "+titles.size());
		for(String title : titles){
			titleString.append(title).append("|");
		}
		titleString.deleteCharAt(titleString.length()-1);
		System.out.println("titles Qed :"+titleString.toString());
		try 
		{
			Thread.currentThread().sleep(3000);	
			String urlStr = "http://en.wikipedia.org/w/api.php?action=query&format=json&titles="+URLEncoder.encode(titleString.toString());
			url = new URL(urlStr);
			tf = url.openConnection(); 
			tf.setRequestProperty("User-agent", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");

			in = new BufferedReader(new InputStreamReader(tf.getInputStream()));
			data = "";
			while ((inputLine = in.readLine()) != null)
			{
				data += inputLine;
			}
	
			in.close();
			if(data.isEmpty())
			{	
				//System.out.println("read data is of size "+data.length()+"after closing URL connection");
			//} else {
				System.out.println(" No data read from URL connection");
			}
			//process the read data
			JSONParser jp = new JSONParser();
			
			JSONObject object =  (JSONObject) jp.parse(data);
			JSONObject result =  (JSONObject) object.get("query"); //System.out.println("result = "+object.get("query"));		
			JSONObject pages = (JSONObject)  result.get("pages"); //System.out.println("pages = "+result.get("pages"));
	
			JSONArray pgArray = new JSONArray();
			for(Object key: pages.keySet()){
				pgArray.add(pages.get(key));
			} //System.out.println("PgArray "+pgArray);
		
			for(int i = 0; i < pgArray.size(); i++)
			{
				JSONObject pg = (JSONObject) pgArray.get(i);
				if(pg.containsKey("pageid")){//if not missing
					wikiTitles.add(pg.get("title").toString());
					Logger.wikiOut(pg.get("title")+"#"+pg.get("pageid"));
//					for(Object key: pg.keySet()){
//						System.out.println(key+" ++ "+pg.get(key));
//					}
				}
			}
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		return wikiTitles;		
	}//End getWikiEquivalents
	
	
	/*Query wikipedia.
	* input ;title of the page
	* output : Method returns most probable wiki-title for the given title */
	public String getWikiEquivalent(String title)
	{
		JSONObject pg = null;
		
		System.getProperties().put("proxySet", true);
		System.getProperties().put("proxyHost", "proxy.iiit.ac.in");
		System.getProperties().put("proxyPort", "8080");
		URL url = null;
		URLConnection tf = null;
		BufferedReader in = null;
		String inputLine,data="";
		//ArrayList<String> querys = new ArrayList<String>();
		String titleFirstWord= null;
		int startinfoboxName = 22;
		try 
		{
			Thread.currentThread().sleep(3000);	
			String urlStr = "http://en.wikipedia.org/w/api.php?action=query&format=json&titles="+URLEncoder.encode(title);
			url = new URL(urlStr);
			tf = url.openConnection(); 
			tf.setRequestProperty("User-agent", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");
			//System.out.println("Output in /home/priya/Ontology/Data/output/List.txt");
			in = new BufferedReader(new InputStreamReader(tf.getInputStream()));
			data = "";
			while ((inputLine = in.readLine()) != null)
			{
				data += inputLine;
			}
	
			in.close();
			if(!data.isEmpty())
			{	
				System.out.println("read data is of size "+data.length()+"after closing URL connection");
			} else {
				System.out.println(" No data read from URL connection");
			}
			//process the read data
			JSONParser jp = new JSONParser();
			
			JSONObject object =  (JSONObject) jp.parse(data);
			JSONObject result =  (JSONObject) object.get("query"); //System.out.println("result = "+object.get("query"));		
			JSONObject pages = (JSONObject)  result.get("pages"); //System.out.println("pages = "+result.get("pages"));
	
			JSONArray pgArray = new JSONArray();
			for(Object key: pages.keySet()){
				pgArray.add(pages.get(key));
			} //System.out.println("PgArray "+pgArray);
		
			for(int i = 0; i < pgArray.size(); i++)
			{
				pg = (JSONObject) pgArray.get(i);			
				for(Object key: pg.keySet()){
					System.out.println(key+" ++ "+pg.get(key));
				}									
			}
		
		} catch (Exception e) {
			e.printStackTrace();
		}

		if(pg.containsKey ("pageid")){
			Logger.wikiOut(pg.get("title")+"#"+pg.get("pageid"));
			return (String) pg.get("title");
		} else {
			return null;
		}
		
	}//End getWikiEquivalent	
	
}
