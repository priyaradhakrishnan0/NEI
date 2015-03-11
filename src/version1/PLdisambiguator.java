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
import utility.FileLoader;
import utility.Logger;
	/*Key class of this package.
	 * Starting from query string input till entity mention output is handled here. */
	public class PLdisambiguator {

		private static PLdisambiguator instance = null;
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
		public PLdisambiguator(){
			kbsearcher = new KBSearcher(Variables.TACKBindex);
		}
		
		/*Singleton implementation*/
		public static PLdisambiguator getInstance(){
			if(instance!=null){
				return instance;
			}
			instance = new PLdisambiguator();
			return instance;
		}
		
		public static void main(String[] args) {
			PLdisambiguator disambiguator = new PLdisambiguator();
			
			//System.out.println(disambiguator.kbsearcher.searchById("E0185756").getName());
			ArrayList<String> mentionList = new ArrayList<String>();
			mentionList.add("US");
			mentionList.add("India");
			disambiguator.features(mentionList);
			disambiguator.getDisambiguations(); 			

//			System.out.println(disambiguator.kbsearcher.searchById("E0463961").getName());
//			System.out.println(disambiguator.kbsearcher.searchById("E0510611").getName());
//			mentionList.add(disambiguator.kbsearcher.searchById("E0463961").getName());
//			mentionList.add(disambiguator.kbsearcher.searchById("E0510611").getName());
//			disambiguator.printFeatures(mentionList);
			
//			System.out.println(disambiguator.getDisambiguations().get("NHS").size());
//			System.out.println(disambiguator.getDisambiguations().get("OP").size());		
			
//			ArrayList<String> mentionList = new ArrayList<String>();
//			mentionList.add("Zues");
//			mentionList.add("Artemis");
//			disambiguator.printFeatures(mentionList);
			
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
			Logger.clearFile(Variables.TACOutputDir.concat("PLdisambiguations.txt"));//clear contents of disambiguations.txt
			
		    ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);
		    for (String a:mentionMasterList) {	    	
		    	System.out.println("Disambiguating Mention a = "+a); //String a = mentionMasterList.get(0);
				PLmentionDisambiguator md = new PLmentionDisambiguator(a);
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
		/*public void printFeatures(ArrayList<String> mentionList){
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
				Logger.clearFile(Variables.TACOutputDir.concat("PLdisambiguations.txt"));//clear contents of disambiguations.txt
			}
			System.out.println(output);
		}//End printFeatures()
		*/

		/*read features from disambiguations.txt*/
		public HashMap<String, ArrayList<String>> getDisambiguations(){
			HashMap<String, ArrayList<String>> disambiguationFeatures = new HashMap<String, ArrayList<String>>();
			String disamFile = Variables.TACOutputDir.concat("PLdisambiguations.txt"); //	System.out.println("Dism file ="+disamFile);
			try {
				BufferedReader br = new BufferedReader(new FileReader(disamFile));			
				String sCurrentLine;			 
				while ((sCurrentLine = br.readLine()) != null) {
					//System.out.println(sCurrentLine);
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
		}//End getDisambiguations()


	}//End class Disambiguator

	class PLmentionDisambiguator implements Runnable { //threadName is a
	   Thread t;
	   private String a;
	   private KBAnchorIndexer kbAnchorIndexer;
	   private PruneBottomPages pp;
	   private InlinkIndex inlinkIndex;
	   private KBSearcher kbsearcher;
	  
	   
	   PLmentionDisambiguator(String mention ){
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
	        
			HashMap<String, ArrayList<String>> KBDisambiguations = new HashMap<>();//KBid to [features]
			ArrayList<String> disambiguations = new ArrayList<String>();//Final features list
			kbsearcher.setResultsCount(3);
			
			//#1 search in KBtitle and wiki_title
			List<Entity> elist = kbsearcher.searchByName(a.toLowerCase());
			if(elist.size() > 0){
				Iterator<Entity> it = elist.iterator();
				while(it.hasNext()){
					Entity e = (Entity) it.next();
					ArrayList<String> featureValue = new ArrayList<String>();
					featureValue.add("0:"+e.getId()); //E13 //KBentityValue.add(e.getName().replace(" ", "_")); //E15
					featureValue.add("1:"+kbsearcher.getResultScoreMap().get(e).toString()); 
					featureValue.add("2:1.0");
					KBDisambiguations.put(e.getId(),featureValue); //System.out.println(e.getId()+KBentityValue);
				}        	
			} else { //search in source
				elist = kbsearcher.searchBySource(a.toLowerCase());
				if(elist.size() > 0){
					Iterator<Entity> it = elist.iterator();
					while(it.hasNext()){
						Entity e = (Entity) it.next();        		
						ArrayList<String> featureValue = new ArrayList<String>();
						featureValue.add("0:"+ e.getId()); //E13 //KBentityValue.add(e.getName().replace(" ", "_")); //E15
						featureValue.add("1"+kbsearcher.getResultScoreMap().get(e).toString()); 
						featureValue.add("2:1.0");
						KBDisambiguations.put(e.getId(),featureValue);
					}        	
				} else { System.out.println("No entity found with name "+a); }
			}
			
			//#2 search in anchor and redirect
			Map<String,Integer> Pga = kbAnchorIndexer.getPagesMap(a);
			if(Pga.size()>0){
				int linkFreqA = 0; 
				for(String pgId:Pga.keySet()){linkFreqA += Pga.get(pgId);}

				if(Pga.size()>25){ //System.out.println("Pruning from "+Pga.size());///ASSUMPTION : Top 25 pages are good enough candidate set.
					Pga = pp.prune(a, Pga);	
				}					
				System.out.println(a+" - Pa count ="+Pga.size());
				for(String i : Pga.keySet()){ //System.out.println("Pa = "+i);
					double PRA = 1.0*Pga.get(i)/linkFreqA; // System.out.println("PrA = "+PRA));

					if(KBDisambiguations.containsKey(i)){
						ArrayList<String> currentfeatures = KBDisambiguations.get(i);
						currentfeatures.add("3:"+PRA);
						KBDisambiguations.put(i, currentfeatures);
					} else {//do not overwrite
						ArrayList<String> featureValue = new ArrayList<String>();
						featureValue.add("0:"+ i);
						featureValue.add("3:"+PRA);						
						KBDisambiguations.put(i,featureValue);
					}
					
				}//for each Pga
			} else { System.out.println("No anchor found as "+a); } //if Pga is not empty			
			
			//#3 search in context
			elist = kbsearcher.search(a.toLowerCase());
			if(elist.size() > 0){
				Iterator<Entity> it = elist.iterator();
				while(it.hasNext()){
					Entity e = (Entity) it.next();        		
					if(KBDisambiguations.containsKey(e.getId())){
						ArrayList<String> currentfeatures = KBDisambiguations.get(e.getId());
						currentfeatures.add("4:1.0");
						KBDisambiguations.put(e.getId(), currentfeatures);			
					} else {//do not overwrite
						ArrayList<String> featureValue = new ArrayList<String>();
						featureValue.add("0:"+e.getId()); //E13 //KBentityValue.add(e.getName().replace(" ", "_")); //E15
						featureValue.add("1:"+kbsearcher.getResultScoreMap().get(e).toString()); featureValue.add("4:1.0");		
						KBDisambiguations.put(e.getId(),featureValue);
					}
				}        	
			} else { System.out.println("No entity found with context "+a); }
			
			//#4 search in infobox attribute values
			elist = kbsearcher.searchByProperties(a.toLowerCase());
			if(elist.size() > 0){
				Iterator<Entity> it = elist.iterator();
				while(it.hasNext()){
					Entity e = (Entity) it.next();        		
					if(KBDisambiguations.containsKey(e.getId())){
						ArrayList<String> currentfeatures = KBDisambiguations.get(e.getId());
						currentfeatures.add("5:1.0");
						KBDisambiguations.put(e.getId(), currentfeatures);			
					} else {//do not overwrite
						ArrayList<String> featureValue = new ArrayList<String>();
						featureValue.add("0:"+e.getId()); //E13 //KBentityValue.add(e.getName().replace(" ", "_")); //E15
						featureValue.add("1:"+kbsearcher.getResultScoreMap().get(e).toString()); featureValue.add("5:1.0");		
						KBDisambiguations.put(e.getId(),featureValue);
					}
				}        	
			} else {
				elist = kbsearcher.searchByClass(a.toLowerCase());
				if(elist.size() > 0){
					Iterator<Entity> it = elist.iterator();
					while(it.hasNext()){
						Entity e = (Entity) it.next();        		
						if(KBDisambiguations.containsKey(e.getId())){
							ArrayList<String> currentfeatures = KBDisambiguations.get(e.getId());
							currentfeatures.add("5:1.0");
							KBDisambiguations.put(e.getId(), currentfeatures);			
						} else {//do not overwrite
							ArrayList<String> featureValue = new ArrayList<String>();
							featureValue.add("0:"+e.getId()); //E13 //KBentityValue.add(e.getName().replace(" ", "_")); //E15
							featureValue.add("1:"+kbsearcher.getResultScoreMap().get(e).toString()); featureValue.add("5:1.0");		
							KBDisambiguations.put(e.getId(),featureValue);
						}
					}        	
				}  else { System.out.println("No category found with name "+a); } 
			}		
			
			//KBsubKB
			FileLoader fileLoader = new FileLoader();
			HashMap<String, String> KBqueriesMap = fileLoader.loadKBqueriesMap();
			//KBsubKB
			
			for(String kbIndex :  KBDisambiguations.keySet()){
				//KBsubKB //eliminating entities to simulate subKB
				if(KBqueriesMap.containsValue(kbIndex)){
					System.out.println("KbSubKb : Eliminating the entry "+kbIndex);
					continue;
				}
				//KBsubKB
				disambiguations.addAll(KBDisambiguations.get(kbIndex));									
			}

			if(!disambiguations.isEmpty()){
				//printout disambuguations of a
				Logger.disambiguationOut(modifiedA, disambiguations, 'P');   
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


