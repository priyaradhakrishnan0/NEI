package version1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import Variables.Variables;
import tac.model.Entity;
import utility.Logger;
/*Key class of this package.
 * Starting from query string input till entity mention output is handled here. */
public class Disambiguator {

	private static Disambiguator instance = null;
	//private static boolean debug;
	//private static KBAnchorIndexer kbAnchorIndexer;
	//private static WikiPageIndexer wikiPageIndexer;
	//private static RitterChunks rc;
	//private static PruneBottomPages pp;
	//private static InlinkIndex inlinkIndex;
	//private static Pruning pruning;
	//private static MongoERDEntities mee;
	public static BufferedWriter bw;
	private static KBSearcher kbsearcher;
	private static final int NTHREDS = 4;
	/*Constructor*/
	public Disambiguator(){
		kbsearcher = new KBSearcher(Variables.TACKBindex);
	}
	
	/*Singleton implementation*/
	public static Disambiguator getInstance(){
		if(instance!=null){
			return instance;
		}
		instance = new Disambiguator();
		return instance;
	}
	
	public static void main(String[] args) {
		Disambiguator disambiguator = new Disambiguator();
		
		//System.out.println(disambiguator.kbsearcher.searchById("E0185756").getName());
		ArrayList<String> mentionList = new ArrayList<String>();
		mentionList.add("US");
		//mentionList.add("India");
		
//		System.out.println(disambiguator.kbsearcher.searchById("E0463961").getName());
//		System.out.println(disambiguator.kbsearcher.searchById("E0510611").getName());
//		mentionList.add(disambiguator.kbsearcher.searchById("E0463961").getName());
//		mentionList.add(disambiguator.kbsearcher.searchById("E0510611").getName());
		disambiguator.printFeatures(mentionList);
		
//		System.out.println(disambiguator.getDisambiguations().get("NHS").size());
//		System.out.println(disambiguator.getDisambiguations().get("OP").size());		
		
//		ArrayList<String> mentionList = new ArrayList<String>();
//		mentionList.add("Zues");
//		mentionList.add("Artemis");
//		disambiguator.printFeatures(mentionList);
		
		//disambiguator.getResultsFormatted("0","apple is a fruit");
		//disambiguator.getResultsFormatted("1", "john harmon football player");
		//disambiguator.printFeatures("pakistan news new york");
		//**disambiguator.printFeatures("apple is a fruit");
		//disambiguator.printFeatures("brooks brothers");
		//disambiguator.printFeatures("bowflex power pro");
		//disambiguator.printFeatures("ritz carlton lake las vegas");
		//disambiguator.printFeatures("I am going to buy apple fruit today");
		//disambiguator.printFeatures("bowflex power pro");
		//disambiguator.getResultsFormatted("1","bowflex power pro");
		
		//disambiguator.printFeatures(args[0]);
		//disambiguator.getResultsFormatted("1", args[0]);
	}

	/*Returns a true/false indicating disambiguations written to (Variables.TACOutputDir)/disambiguations.txt)*/
	public static boolean features(ArrayList<String> mentionMasterList){		
		boolean disambiguationsWritten = false;	
		Logger.clearFile(Variables.TACOutputDir.concat("disambiguations.txt"));//clear contents of disambiguations.txt
		
	    ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);
	    for (String a:mentionMasterList) {	    	
	    	System.out.println("Disambiguating Mention a = "+a); //String a = mentionMasterList.get(0);
			MentionDisambiguator md = new MentionDisambiguator(a);
			executor.execute(md);
	    } 
	    // This will make the executor accept no new threads and finish all existing threads in the queue
	    executor.shutdown(); 
	    // Wait until all threads are finish
	    try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			disambiguationsWritten = true;
		} catch (InterruptedException e) {		
			e.printStackTrace();
		}
	    System.out.println("Finished all threads");
		
		return disambiguationsWritten;
	}//end features()	

	/*printout disambiguation features in CSV format*/
	public void printFeatures(ArrayList<String> mentionList){
		features(mentionList);
		HashMap<String, ArrayList<String>> disambiguationFeatures = getDisambiguations(); 
		System.out.println("Disambiguations "+disambiguationFeatures.size());
		String output = "";
		int count = 0;
		if(disambiguationFeatures.size()>0){
			for(String j:disambiguationFeatures.keySet()){
				//output +="\n"+j+";";
				for(String feature : disambiguationFeatures.get(j)){
					output += feature+";";
					count++;
					if(count%5==0){
						output += j+";\n";
					}
				}	//+disambiguationFeatures.get(j)+";"+disambiguationFeatures.get(j)[1]+";"+disambiguationFeatures.get(j)[2]+"\n";		
			}
			Logger.clearFile(Variables.TACOutputDir.concat("disambiguations.txt"));//clear contents of disambiguations.txt
		}
		System.out.println(output);
	}//End printFeatures()

	/*read features from disambiguations.txt*/
	public HashMap<String, ArrayList<String>> getDisambiguations(){
		HashMap<String, ArrayList<String>> disambiguationFeatures = new HashMap<String, ArrayList<String>>();
		String disamFile = Variables.TACOutputDir.concat("disambiguations.txt"); //	System.out.println("Dism file ="+disamFile);
		try {
			BufferedReader br = new BufferedReader(new FileReader(disamFile));			
			String sCurrentLine;			 
			while ((sCurrentLine = br.readLine()) != null) {
				System.out.println(sCurrentLine);
				if(sCurrentLine.contains("#")){
					String[] splits = sCurrentLine.split("#");
					String mention = splits[0];
					mention = mention.replace("|", System.getProperty("line.separator"));
					String features = splits[1];
					features = features.replace("[", "");
					features = features.replace("]", "");
					ArrayList<String> featureList = new ArrayList<String>();
					for(String feature : features.split(", ")){
						featureList.add(feature);
					}
					disambiguationFeatures.put(mention, featureList);	System.out.println(mention+"  "+featureList);				
				}
			}			
		} catch (FileNotFoundException e1) {
			Logger.logOut(disamFile+" File not found");
			e1.printStackTrace();
		} catch (IOException e2) {
			Logger.logOut("Reading error with "+disamFile);
			e2.printStackTrace();
		}
		return disambiguationFeatures ;
	}


}//End class Disambiguator

class MentionDisambiguator implements Runnable { //threadName is a
   Thread t;
   private String a;
   private KBAnchorIndexer kbAnchorIndexer;
   private PruneBottomPages pp;
   private InlinkIndex inlinkIndex;
   private KBSearcher kbsearcher;
  
   
   MentionDisambiguator(String mention ){
	   a = mention; 
       pp = new PruneBottomPages();
       inlinkIndex = new InlinkIndex();
       kbAnchorIndexer = new KBAnchorIndexer();
       kbsearcher = new KBSearcher(Variables.TACKBindex);
       System.out.println("Creating thread on " +  a );
   }//end constructor
   
   public void run() {
	    String modifiedA = a.replace(System.getProperty("line.separator"), "|");;
	    a = a.replace(System.getProperty("line.separator"), " ");
    	java.util.Date date= new java.util.Date();
        System.out.println("Running thread on " +  a + " @ "+new Timestamp(date.getTime()));
	   
		HashMap<String, ArrayList<String>> KBDisambiguations = new HashMap<>();//KBid to [lucenecsore, rank]
		HashMap<String, ArrayList<String>> AnchorDisambiguations = new HashMap<>();//KBid to [rel, PrA, lp]
		ArrayList<String> disambiguations = new ArrayList<String>();//Final features list
		int count = 0; kbsearcher.setResultsCount(3);
		//search in name
		List<Entity> elist = kbsearcher.searchByName(a.toLowerCase());
		if(elist.size() > 0){
			Iterator<Entity> it = elist.iterator();
			while(it.hasNext()){
				Entity e = (Entity) it.next();
				ArrayList<String> KBentityValue = new ArrayList<String>();
				KBentityValue.add("0:"+e.getId()); //E13
				//KBentityValue.add(e.getName().replace(" ", "_")); //E15
				KBentityValue.add("1:"+kbsearcher.getResultScoreMap().get(e).toString()); KBentityValue.add("2:1.0");
				KBDisambiguations.put(e.getId(),KBentityValue); //System.out.println(e.getId()+KBentityValue);
			}        	
		} //else { System.out.println("No entity found with name "+a); }
		//search in source
		elist = kbsearcher.searchBySource(a.toLowerCase());
		if(elist.size() > 0){
			Iterator<Entity> it = elist.iterator();
			while(it.hasNext()){
				Entity e = (Entity) it.next();        		
				ArrayList<String> KBentityValue = new ArrayList<String>();
				KBentityValue.add("0:"+e.getId()); //E13
				//KBentityValue.add(e.getName().replace(" ", "_")); //E15
				KBentityValue.add("1:"+kbsearcher.getResultScoreMap().get(e).toString()); KBentityValue.add("2:2.0");
				if(KBDisambiguations.containsKey(e.getId())){
				} else {//add only if not present
					KBDisambiguations.put(e.getId(),KBentityValue);
				}
			}        	
		} //else { System.out.println("No entity found with name "+a); }
		//search in context
		elist = kbsearcher.search(a.toLowerCase());
		if(elist.size() > 0){
			Iterator<Entity> it = elist.iterator();
			while(it.hasNext()){
				Entity e = (Entity) it.next();        		
				ArrayList<String> KBentityValue = new ArrayList<String>();
				KBentityValue.add("0:"+e.getId()); //E13
				//KBentityValue.add(e.getName().replace(" ", "_")); //E15
				KBentityValue.add("1:"+kbsearcher.getResultScoreMap().get(e).toString()); KBentityValue.add("2:3.0");
				if(KBDisambiguations.containsKey(e.getId())){
				} else {//do not overwrite
					KBDisambiguations.put(e.getId(),KBentityValue);
				}
			}        	
		} //else { System.out.println("No entity found with name "+a); }
		//search in infobox attribute values
		elist = kbsearcher.searchByProperties(a.toLowerCase());
		if(elist.size() > 0){
			Iterator<Entity> it = elist.iterator();
			while(it.hasNext()){
				Entity e = (Entity) it.next();        		
				ArrayList<String> KBentityValue = new ArrayList<String>(); //
				KBentityValue.add("0:"+e.getId()); //E13
				//KBentityValue.add(e.getName().replace(" ", "_"));//E15
				KBentityValue.add("1:"+kbsearcher.getResultScoreMap().get(e).toString()); KBentityValue.add("2:4.0");
				if(KBDisambiguations.containsKey(e.getId())){
				} else {//add only if not present
					KBDisambiguations.put(e.getId(),KBentityValue);
				}
			}        	
		} //else { System.out.println("No entity found with name "+a); }		

		Map<String,Integer> Pga = kbAnchorIndexer.getPagesMap(a);
		if(Pga.size()>0){
			int linkFreqA = 0; 
			for(String pgId:Pga.keySet()){linkFreqA += Pga.get(pgId);}
			double linkProbability = (linkFreqA * 1.0)/(linkFreqA + kbAnchorIndexer.getTotalFreq(a)); System.out.println(a+" - lp = "+linkProbability);

			if(Pga.size()>25){ //System.out.println("Pruning from "+Pga.size());///ASSUMPTION : Top 25 pages are good enough candidate set.
				Pga = pp.prune(a, Pga);	
			}					
			System.out.println(a+" - Pa count ="+Pga.size());
			for(String i : Pga.keySet()){ //System.out.println("Pa = "+i);
				double PRA = 1.0*Pga.get(i)/linkFreqA; // System.out.println("Prior prob Pr(Pa/a) = "+((1.0*PRa[1]) / PRa[0]));
				ArrayList<String> currentDisambiguations = new ArrayList<String>();
				//currentDisambiguations.add(String.valueOf(relA[PgAiteration]));
				currentDisambiguations.add("3:"+String.valueOf(PRA));
				currentDisambiguations.add("4:"+String.valueOf(linkProbability));
				AnchorDisambiguations.put(i, currentDisambiguations);	
				
			}//for each Pga
		}//if Pga is not empty			
		//System.out.println(" Matching pages count ="+count+", total pages = "+KBDisambiguations.size());
		if( (KBDisambiguations.size()+AnchorDisambiguations.size()) > 0){			
				
			for(String kbIndex :  KBDisambiguations.keySet()){
				ArrayList<String> currentDisambiguations = KBDisambiguations.get(kbIndex);
				if(AnchorDisambiguations.containsKey(kbIndex)){
					currentDisambiguations.addAll(AnchorDisambiguations.get(kbIndex));					
				} 				
				disambiguations.addAll(currentDisambiguations);									
			}
			
			for(String anchorIndex : AnchorDisambiguations.keySet()){
				if(KBDisambiguations.containsKey(anchorIndex)){//do nothing as it is already added					
				} else {
					ArrayList<String> currentDisambiguations = new ArrayList<String>();
					currentDisambiguations.add("0:"+anchorIndex);//E13 KB_id
					//currentDisambiguations.add(kbsearcher.searchById(anchorIndex).getName().replace(" ", "_"));//E15  wiki_name,
					//currentDisambiguations.add("0.0");currentDisambiguations.add("0.0"); //Lucene score, rank
					currentDisambiguations.addAll(AnchorDisambiguations.get(anchorIndex));						
					disambiguations.addAll(currentDisambiguations);						
				}
			}
			
			//printout disambuguations of a
			Logger.disambiguationOut(modifiedA, disambiguations, 'b');	    
		    
		} else {
			System.out.println("Unable to disambiguate mention "+a);
		}
	    //cleanup
		System.out.println("Thread on " +  a + " exiting @ "+ new Timestamp(date.getTime()));
	    inlinkIndex.destroy();
	    kbAnchorIndexer.destroy();

}//end run 
	   
	public void start ()
	{
	      System.out.println("Starting thread on " +  a );
	      if (t == null)
	      {
	         t = new Thread (this, a);
	         t.start ();
	      }
	}

}//End class MentionDisambiguator
