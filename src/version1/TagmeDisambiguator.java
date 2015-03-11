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
import tac.model.Doc;
import tac.preprocess.DocProcessor;
import utility.Cleaner;
import utility.FileLoader;
import utility.Logger;
import db.wikiPageIndex;
import db.wikilinksIndex;
/*Key class of this package.
 * Starting from query string input till entity mention output is handled here. */
public class TagmeDisambiguator {

	private static TagmeDisambiguator instance = null;
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
	public TagmeDisambiguator(){

		try {
			kbsearcher = new KBSearcher(Variables.TACKBindex);
		} catch (Exception e1) {
			Logger.logOut("IO exception: Disambiguator:: Unable to access TACKBindex");
			e1.printStackTrace();
		}

	}
	
	/*Singleton implementation*/
	public static TagmeDisambiguator getInstance(){
		if(instance!=null){
			return instance;
		}
		instance = new TagmeDisambiguator();
		return instance;
	}
	
	public static void main(String[] args) {
		TagmeDisambiguator tdisambiguator = new TagmeDisambiguator();
		
//		Logger.clearFile(Variables.TACOutputDir.concat("tagmeDisambiguations.txt"));//clear contents of disambiguations.txt
//		ArrayList<Integer> tauList = new ArrayList<Integer>();
//		EntityLinker el = new EntityLinker();
//		tauList.add(1);
//		tauList.add(2);
//		tauList.add(3);
//		tauList.add(5);
//		tauList.add(10);
//		tauList.add(20);
//		tauList.add(30);	
//
//		for(Integer tau : tauList){
//			Doc testDoc = el.getTestDoc("APW_ENG_20070613.0711.LDC2009T13"); 
//			ArrayList<String> mList = new ArrayList<String>();
//			mList.add("Colette Avital");
//			features(testDoc, mList,tau);
//			testDoc = el.getTestDoc("APW_ENG_20080930.0379.LDC2009T13");
//			mList.clear();
//			mList.add("Axel Miller");
//			features(testDoc, mList,tau);
//			mList.clear();
//			testDoc = el.getTestDoc("APW_ENG_20070921.1181.LDC2009T13");
//			mList.add("Chiharu Icho");
//			features(testDoc, mList, tau);
//		}
		
		
		int tau = 2; //sence cutoff threshold  
		double epsilon = 0.3; //30% relation to best rel valuue
		EntityLinker el = new EntityLinker();
//		Doc testDoc = el.getTestDoc("APW_ENG_20070613.0711.LDC2009T13"); 
		Doc testDoc = el.getTestDoc("AFP_ENG_20071026.0429.LDC2009T13");
//		Doc testDoc = el.getTestDoc("APW_ENG_20070921.1181.LDC2009T13");
		ArrayList<String> mentionList = new ArrayList<String>();
//        mentionList.add("parliament speaker");
//		mentionList.add("Colette Avital");
		mentionList.add("PZU");
//		mentionList.add("Chiharu Icho");
		tdisambiguator.features(testDoc, mentionList, tau, epsilon);
			
		
//		System.out.println(disambiguator.kbsearcher.searchById("E0463961").getName());
//		System.out.println(disambiguator.kbsearcher.searchById("E0510611").getName());
//		mentionList.add(disambiguator.kbsearcher.searchById("E0463961").getName());
//		mentionList.add(disambiguator.kbsearcher.searchById("E0510611").getName());
		
		
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

	/*Returns a true/false indicating disambiguations written to (Variables.TACOutputDir)/tagmeDisambiguations.txt)*/
	public static boolean features(Doc document,ArrayList<String> mustHaveMentions, int tau, double epsilon){
		//HashMap<String, ArrayList<String>> mentionSolutionMap = new HashMap<>();
		boolean disambiguationsWritten = false;	
		DocProcessor docProcessor= new DocProcessor();

		Map<String, String> mentionEContext = docProcessor.getContext(document.getText(), mustHaveMentions, 2);//EntityContext(EC)
		 
//		for (String mention : mentionEContext.keySet()){
//			ArrayList<String> disambiguation = tagmeDisambiguator.features(mention,mentionEContext.get(mention));
//			if(disambiguation.isEmpty()){//skip
//			}
//			else{
//				//if(searcher.searchByClass(disambiguation.get(1)) != null)
//				mentionSolutionMap.put(mention, disambiguation);
//			}
//		}//for mention

		Logger.clearFile(Variables.TACOutputDir.concat("tagmeDisambiguations.txt"));//clear contents of disambiguations.txt
		
	    ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);
	    for (String mention:mentionEContext.keySet()) {	    	
	    	System.out.println("Disambiguating Mention = "+mention); //String a = mentionMasterList.get(0);
			TagmeMentionDisambiguator md = new TagmeMentionDisambiguator(mention, mentionEContext.get(mention), tau, epsilon);
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
	public void printFeatures(Doc document,ArrayList<String> mentionList){

		int tau = 20;//Powerlaw distribution or 80-20 rule
		double epsilon = 0.7; //70% relation to best rel valuue
		features(document, mentionList, tau, epsilon); 
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
					if(count%2==0){
						output += j+";\n";
					}
				}	//+disambiguationFeatures.get(j)+";"+disambiguationFeatures.get(j)[1]+";"+disambiguationFeatures.get(j)[2]+"\n";		
			}
			Logger.clearFile(Variables.TACOutputDir.concat("tagmeDisambiguations.txt"));//clear contents of disambiguations.txt
		}
		System.out.println(output);
	}//End printFeatures()

	/*read features from disambiguations.txt*/
	public HashMap<String, ArrayList<String>> getDisambiguations(){
		HashMap<String, ArrayList<String>> disambiguationFeatures = new HashMap<String, ArrayList<String>>();
		String disamFile = Variables.TACOutputDir.concat("tagmeDisambiguations.txt"); 	//System.out.println("Dism file ="+disamFile);
		try {
			BufferedReader br = new BufferedReader(new FileReader(disamFile));			
			String sCurrentLine;			 
			while ((sCurrentLine = br.readLine()) != null) { System.out.println(sCurrentLine);
				if(sCurrentLine.contains("#")){
					String[] splits = sCurrentLine.split("#");
					String mention = splits[0];
					String features = splits[1];
					features = features.replace("[", "");
					features = features.replace("]", "");
					ArrayList<String> featureList = new ArrayList<String>();
					for(String feature : features.split(", ")){
						featureList.add(feature);
					}
					disambiguationFeatures.put(mention, featureList);	//System.out.println(mention+"  "+featureList);				
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

class TagmeMentionDisambiguator implements Runnable { //threadName is a
   Thread t;
   private String qMention, mentionContext;
   private int tau;
   private double epsilon;
   private KBAnchorIndexer kbAnchorIndexer;
   private AnchorIndexer anchorIndexer;
   private PruneBottomPages pp;
   private wikilinksIndex inlinkIndex;
   private wikiPageIndex wikiPageIndex;
   private KBSearcher kbsearcher;
   private Cleaner cleaner;
   private HashMap<String, ArrayList<String>> mentionMentionListMap;
	//KBsubKB
	FileLoader fileLoader;
	HashMap<String, Long> KBqueriesMap;
	List<Long> eliminatedEntries;
	//KBsubKB
   TagmeMentionDisambiguator(String mention, String mentionCon, int t, double e){
	   qMention = mention; mentionContext = mentionCon; tau = t; epsilon = e;
       pp = new PruneBottomPages();
       inlinkIndex = new wikilinksIndex("inlinks");
       anchorIndexer = new AnchorIndexer();
       kbAnchorIndexer = new KBAnchorIndexer();
       wikiPageIndex = new wikiPageIndex();
       cleaner = new Cleaner();
		//KBsubKB
		fileLoader = new FileLoader();
		KBqueriesMap = fileLoader.loadKBqueriesIdMap();
		eliminatedEntries = new ArrayList<Long>(KBqueriesMap.values());
		//KBsubKB
       try {
			kbsearcher = new KBSearcher(Variables.TACKBindex);
		} catch (Exception e1) {
			Logger.logOut(" Exception: Disambiguator Thread :"+qMention +": Unable to access TACKBindex");
			e1.printStackTrace();
		}
       System.out.println("Creating thread on " +  qMention );
   }//end constructor
   
   public void run() {
	    String unmodifiedA = qMention;
	    qMention = qMention.replace(System.getProperty("line.separator"), " ");
    	java.util.Date date= new java.util.Date();
        System.out.println("Running thread on " +  qMention + " @ "+new Timestamp(date.getTime()));
	    HashMap<String,Double> memo = new HashMap<>();
		Map<Long,Integer> Pga = anchorIndexer.getPagesMap(qMention);
		
		//KBsubKB
//		ArrayList<Long> eliminatedEntries = new ArrayList<Long>();
//		eliminatedEntries.add(10526826L);//EL012726	E0428365 Louisiana_Five #1	
//		eliminatedEntries.add(153718L);	//EL012203	E0745565 Chester
//		eliminatedEntries.add(1481642L);//EL006579	Tia_Mowry
//		eliminatedEntries.add(3161374L);//EL000304	Barnhill_Arena
//		eliminatedEntries.add(72886L);	//EL012264	Memphis_Grizzlies#5
//		eliminatedEntries.add(1077890L);//EL014009	Michael_Szymanczyk
//		eliminatedEntries.add(3648793L);//EL013705	Alberto_Medina
//		eliminatedEntries.add(55846L);  //EL009140	Bob_Dole#8
//		eliminatedEntries.add(333143L);//EL012626	Austin_Motor_Company
//		eliminatedEntries.add(5704445L);//EL011333	Powszechny_Zakład_Ubezpieczeń #10
		//KBsubKB 
		
		if(Pga.size() > 0){
			ArrayList<String> allContextMentions = anchorIndexer.mentions(mentionContext);
			allContextMentions = cleaner.removeStopWords(allContextMentions); System.out.println("Neighbour count ="+allContextMentions.size()+" ; "+mentionContext);
			ArrayList<String> contextMentions = new ArrayList<String>();
			if(allContextMentions.contains(qMention)){ //System.out.println("contains Qmention");
				int indexOfqMention = allContextMentions.indexOf(qMention);
				for(int i=indexOfqMention-2; i < (indexOfqMention +2); i++){
					if(i<0 || i>allContextMentions.size())
						continue;
					contextMentions.add(allContextMentions.get(i));
				} //System.out.println("contextMentions = "+contextMentions);
			} else {
				contextMentions.add(qMention);
				contextMentions.addAll(allContextMentions); //System.out.println("contextMentions = "+contextMentions);
			}//contextMentions populated
			ArrayList<String> disambiguations = new ArrayList<String>();//Final features list
			for(String a : contextMentions){				
				Pga = anchorIndexer.getPagesMap(a);
				
				//KBsubKB //System.out.println("Before eliminating senses : No of senses ="+Pga.size());
				Set<Long> PgaTemp = Pga.keySet();
				for (Long pagea : PgaTemp){					
					if(eliminatedEntries.contains(pagea)){
						Pga.remove(pagea); System.out.println("KBsubKB : Removed entry "+pagea);
					}
				} //System.out.println("After eliminating senses : No of senses ="+Pga.size());
				//KBsubKB 	
				
				int linkFreqA = 0; 
				for(Long pgId:Pga.keySet()){linkFreqA += Pga.get(pgId);}
				double linkProbability = (linkFreqA * 1.0)/(linkFreqA + kbAnchorIndexer.getTotalFreq(a)); //System.out.println(a+" - lp = "+linkProbability);
				memo.clear();
				double[] relA = new double[Pga.size()];
				double[] prA = new double[Pga.size()];
				String[] pgAtitles = new String[Pga.size()];
				int PgAiteration = 0;
				//System.out.println("Pruning from "+Pga.size());// By power law restricting to top 20% of sences.
				Pga = pp.prune(Pga,tau);	
				//System.out.println("Pruned to "+Pga.size()); 					
				//System.out.println(a+" - Pa count ="+Pga.size()+" Pga "+Pga);
				for(Long i : Pga.keySet()){
					String Pa = wikiPageIndex.getTitle(i); //System.out.println(" Pa = "+Pa);
	
					ArrayList<String> PaInlinks = inlinkIndex.getLinks(Pa); 
					HashSet<String> inlinksA = new HashSet<>(); inlinksA.addAll(PaInlinks);
					double PRA;
					if(memo.containsKey(a+"!@#$"+i)){
						PRA = memo.get(a+"!@#$"+i);
					} else{
						int[] PRa = anchorIndexer.getPageCountInPages(a, i);	//System.out.println("PRa "+PRa[0]+", "+PRa[1]);				
						PRA = (1.0*PRa[1]) / PRa[0];
						memo.put(a+"!@#$"+i, PRA);
					} //System.out.println("Prior prob Pr(Pa/a) = "+ PRA);
					double[] vote = new double[contextMentions.size()];
					for(int j=0;j<contextMentions.size();++j){ 
						String b = contextMentions.get(j); // System.out.println(" b = "+b);					
						double sum = 0.0;
						vote[j] = 0.0;
						if(b.equalsIgnoreCase(a)){ 
							//System.out.println("skip. Do nothing."+b);
						} else {
							//Map<String, Integer> Pgb = kbAnchorIndexer.getPagesMap(b,0.1); 	//ASSUMPTION : variance restriction = 0.1 ( i.e 10%)
							Map<Long, Integer> Pgb = anchorIndexer.getPagesMap(b); //System.out.println("Pages for b = "+Pgb.size());
							//System.out.println("Pruning voting mention '"+b+"' from "+Pgb.size());// By power law restricting to top 20% of sences.
							Pgb = pp.prune(Pgb, tau); //System.out.println("Pruned to "+Pgb.size());
							for(Long pgid:Pgb.keySet()){
								String Pb = wikiPageIndex.getTitle(pgid);
								ArrayList<String> PbInlinks = inlinkIndex.getLinks(Pb);
								//get common inlinks
								HashSet<String> inlinksB = new HashSet<>(); inlinksB.addAll(PbInlinks);//System.out.println(Pa+" InlinksA = "+inlinksA.size()+", "+Pb+" InlinksB = "+inlinksB.size());
								inlinksB.retainAll(inlinksA);
								double comLinks = inlinksB.size();
								
								if( comLinks > 0.0){
									double PRB;
									if(memo.containsKey(b+"!@#$"+pgid)){
										PRB = memo.get(b+"!@#$"+pgid);
									}
									else{
										int[] PRb = anchorIndexer.getPageCountInPages(b, pgid);
										PRB = (1.0*PRb[1]) / PRb[0]; 					
										memo.put(b+"!@#$"+pgid, PRB);
									}  //System.out.println(b+" Prior Pr(Pb/b) = "+PRB);
									//if(PRB > 0.1){	///ASSUMPTION : Ignoring Pb with less than 0.1 PRB in calculating relatedness
									double numerator = Math.log((double)Math.max(PaInlinks.size(),PbInlinks.size())) - Math.log(comLinks); //ln 12165935 = 16.3141503911 
									double denomenator = 16.3141503911 - Math.log(Math.min(PaInlinks.size(),PbInlinks.size())); 
									double rel = 1 - (numerator / denomenator); //System.out.println(b+", Page of b = "+pgid+", page = "+i+", rel = "+rel); 
									if(rel > 0){
										sum += rel * PRB;
									}
									//}
								}//if common inlinks present
								
							}//for each Pb of Pg(b)
							//vote[j] = sum ; 
							if(Pgb.size()>0){
								vote[j] = sum / Pgb.size(); // For vote normalization.
							}
						}//end if
						relA[PgAiteration] += vote[j];	
						
					}//for each b
					//System.out.println("relA["+PgAiteration+"] = "+relA[PgAiteration]);
					pgAtitles[PgAiteration] = Pa;
					prA[PgAiteration] = PRA;
					++PgAiteration;				
				}//for each Pga
					
				//rel(Pbest)
				double maxRelA = 0.0, maxPrA = 0.0; int bestSense = 0;
				for (int counter = 0; counter < PgAiteration; counter++)
				{
				    if (relA[counter] > maxRelA)
				    {
				        maxRelA = relA[counter]; 
				    }
				} System.out.println(" maxRelA = "+maxRelA);
				//Almost best senses list
				for (int counter = 0; counter < PgAiteration; counter++)
				{
					if(relA[counter] > (epsilon * maxRelA)){ 
						if(prA[counter] > maxPrA){
							maxPrA = prA[counter]; //System.out.println("best sense Assigned, relA = "+relA[counter]+" , prA = "+prA[counter]);
							bestSense = counter;
						}
					} else {
						//System.out.println("Rejected sense : "+pgAtitles[counter]+" relA = "+relA[counter]);
					}
				}
				
				disambiguations.add(a);//element 1 = neighbor mention
				if(PgAiteration > 0){//In case Pga size becomes 0 and no iteration through Pga, cannot assign disambiguation.
					disambiguations.add(pgAtitles[bestSense]);//element 2 = disambiguated page
				} else {
					disambiguations.add("NIL");
				}
				disambiguations.add(String.valueOf(linkProbability));//element 3 = neighbor mention's link probability
				//printout disambuguations of a
			
			}//for a
			Logger.disambiguationOut(qMention, disambiguations,'T');

		} else {
			System.out.println("Unable to disambiguate mention without anchor representation"+qMention);
		} //if Pga is empty
	    //cleanup
		System.out.println("Thread on " +  qMention + " exiting @ "+ new Timestamp(date.getTime()));
	    inlinkIndex.destroy();
	    anchorIndexer.destroy();
	    kbAnchorIndexer.destroy();
	    wikiPageIndex.destroy();

   }//end run 
	   
	public void start ()
	{
	      System.out.println("Starting thread on " +  qMention );
	      if (t == null)
	      {
	         t = new Thread (this, qMention);
	         t.start ();
	      }
	}

}//End class TagmeMentionDisambiguator