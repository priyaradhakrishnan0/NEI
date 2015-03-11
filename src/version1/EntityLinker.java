package version1;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.util.packed.MonotonicAppendingLongBuffer.Iterator;

import db.wikiPageIndex;
import db.wikiabstractsIndex;
import db.wikilinksIndex;
import Variables.Variables;
import tac.model.Doc;
import tac.model.Entity;
import tac.model.Query;
import tac.preprocess.DocProcessor;
import utility.FileLoader;
import utility.Helper;
import utility.Logger;
import utility.SimilarityFinder;
import utility.WikiOperator;

public class EntityLinker {

	static NamedEntityExtractor NEE = new NamedEntityExtractor();//static object. So load classifier just once
	LinkClassifier lc;
	static HashMap<String, String> NILs = new HashMap<>();
	static int NILcount = 1;
	KBSearcher searcher = new KBSearcher(Variables.TACKBindex);
	
	public static void main(String[] args){
		EntityLinker el = new EntityLinker();
		//ArrayList<ArrayList<String>> trainDataset = el.getFeatures(Variables.TACtrainQueries, false); //nonNIL classifier
		//ArrayList<ArrayList<String>> trainDataset = el.getFeatures(Variables.TACtrainQueries, true); //NIL classifier
		//System.out.println(" Train dataset size = " + trainDataset.size());
		el.getSolutionForTest(Variables.TACtestQueries, false); //nonNIL classification
		//el.getSolutionForTest(Variables.TACtestQueries, true); //NIL classification
	}//main
	
	/*Training feature generation.*/
	public ArrayList<ArrayList<String>> getFeatures(String xmlQueryFilename, boolean NILclassifier){
		ArrayList<ArrayList<String>> finalFeatureList = new ArrayList<ArrayList<String>>(); 
		
		QueryFileParser qfParser = new QueryFileParser();
		qfParser.docMentionMap.clear();	
		qfParser.parseDocument(xmlQueryFilename); System.out.println("Documents in the Query file = "+qfParser.docMentionMap.size());
		
		ArrayList<String> qidMentionList = new ArrayList<String>();
		ArrayList<String> qMentionList = new ArrayList<String>();
		for(String qDocument : qfParser.docMentionMap.keySet()){
			System.out.println("Current doc : "+qDocument);
			qidMentionList = qfParser.docMentionMap.get(qDocument); System.out.println("Num of qidMentions = "+qidMentionList.size());
			Map<String, String>mentionQidMap = new HashMap<String, String>();
			for(int pointer=1; pointer < qidMentionList.size(); ++pointer){
				mentionQidMap.put(qidMentionList.get(pointer), qidMentionList.get(pointer-1));//mention : Qid
				++pointer;
			}
			System.out.println("Num of qMentions = "+mentionQidMap.size());
			
			qMentionList = new ArrayList(mentionQidMap.keySet());
			/*CHANGE HERE to change MD methods*/
			//HashMap<String, ArrayList<String>> mentionECmap = baselineMD(qMentionList);
			HashMap<String, ArrayList<String>> mentionECmap = plochMD(qDocument,qMentionList);
			HashMap<String, ArrayList<String>> NILmentionMap = new HashMap<String, ArrayList<String>>();//Maps mention#NILfeatures.
			if(NILclassifier){
				NILmentionMap = getNILmap(mentionECmap);
			}//if NIL 
			
			for(int pointer=1; pointer < qidMentionList.size(); ++pointer){
				String qId = qidMentionList.get(pointer-1);
				String mention = qidMentionList.get(pointer);
				//read golden title for this mention
				String goldenEntity = getGoldenMention(qId,true); System.out.println("Mention = "+mention+" Gold = "+goldenEntity);
				if (goldenEntity != null) {										

					if(NILclassifier){
						if(NILmentionMap.containsKey(mention)){
							ArrayList<String> datapoint = new ArrayList<String>();
							if(goldenEntity.startsWith("NIL")){
								System.out.println(" Positive NIL Sample : "+mention);
								datapoint.add("1");									
							} else if(goldenEntity.startsWith("E")){
								System.out.println(" Negative NIL Sample : "+mention);
								datapoint.add("0");									
							} 
							datapoint.addAll(NILmentionMap.get(mention));	
							printFeatures(datapoint);
							finalFeatureList.add(datapoint);
						}//if NILfeats 
					} else { // candidate classifier
						if(mentionECmap.containsKey(mention)){				
							ArrayList<String> flist = mentionECmap.get(mention);
							java.util.Iterator<String> it = flist.iterator();
							ArrayList<String> datapoint = new ArrayList<String>();
	
							while(it.hasNext()){
								String element = it.next();
								if(element.split(":")[1].startsWith("E")){
									System.out.println("Query ="+qId+",linked entity = "+element.replace("0:", "")+" and Gold = "+ goldenEntity);
									if(datapoint.isEmpty()){
										//skip
									} else {
										printFeatures(datapoint);
										finalFeatureList.add(datapoint);								
										datapoint = new ArrayList<String>();								
									}
									if(element.replaceAll("0:", "").matches(goldenEntity)){
										System.out.println(" Positive Sample : "+mention);									
										datapoint.add("1");									
									} else { //negative sample
										System.out.println(" Negative Sample : "+mention);
										datapoint.add("0");
									}	
								} else {
									datapoint.add(element);
								}
							}//while
							//to count in the last candidate *REVISIT*
							if(!datapoint.isEmpty()){ 	
								printFeatures(datapoint);
								finalFeatureList.add(datapoint);
							}
								
						} else {
							System.out.println(qId+" FAILED to map "+mention);
						}
					}//if candidate classifier (nonNIL)
				}//if golden mention found

				++pointer;
			}//for	mention
		}//for doc	
		return finalFeatureList;
	}//getFeatures()

	/*Populate NIL feature map*/
	public HashMap<String, ArrayList<String>> getNILmap(HashMap<String,ArrayList<String>>mentionECmap){
		HashMap<String, ArrayList<String>> NILmap = new HashMap<String, ArrayList<String>>();
		//NILmap disambiguation features
		for (String mention : mentionECmap.keySet()){
			ArrayList<String> flist = mentionECmap.get(mention);
			java.util.Iterator<String> it = flist.iterator();
			int candidateCount = 0;
			double fValue = 0.0, praMAX = 0.0, praMIN = 0.0, praSUM =0.0; //feature#3
			double ecMAX = 0.0, ecMIN = 0.0, ecSUM =0.0;/*feature#6*/ double lcoMAX = 0.0, lcoMIN = 0.0, lcoSUM =0.0; /*feature#7 */
			double lciMAX = 0.0, lciMIN = 0.0, lciSUM =0.0;/*feature#8*/ double lcaMAX = 0.0, lcaMIN = 0.0, lcaSUM =0.0; /*feature#9*/
			double dcMAX = 0.0, dcMIN = 0.0, dcSUM =0.0; //feature#10
			ArrayList<String> datapoint = new ArrayList<String>();//holds features of this mention
			String entityId = null;
			while(it.hasNext()){
				String element = it.next();
				int fKey = Integer.valueOf(element.split(":")[0]);
				String fv = element.split(":")[1];
				if(fv.startsWith("E")){
					//skip
				} else {
					fValue = Double.valueOf(fv);
				}
				switch(fKey){
					case 0:
						++candidateCount;
					case 3:
						praSUM += fValue;
						if(fValue >praMAX) praMAX = fValue;
						if(fValue < praMIN)	praMIN = fValue;
						break;
					case 6:
						ecSUM += fValue;
						if(fValue > ecMAX) ecMAX = fValue;
						if(fValue < ecMIN)	ecMIN = fValue;
						break;
					case 7:
						lcoSUM += fValue;
						if(fValue > lcoMAX) lcoMAX = fValue;
						if(fValue < lcoMIN)	lcoMIN = fValue;
						break;
					case 8:
						lciSUM += fValue;
						if(fValue > lciMAX) lciMAX = fValue;
						if(fValue < lciMIN)	lciMIN = fValue;
						break;
					case 9:
						lcaSUM += fValue;
						if(fValue > lcaMAX) lcaMAX = fValue;
						if(fValue < lcaMIN)	lcaMIN = fValue;
						break;
					case 10:
						dcSUM += fValue;
						if(fValue > dcMAX)  dcMAX = fValue;
						if(fValue < dcMIN)	dcMIN = fValue;
						break;
					default:
						break;
				}						
			}//while
			double praMean = 0.0, ecMean = 0.0, lcoMean = 0.0, lciMean = 0.0, lcaMean = 0.0, dcMean = 0.0;
			if(candidateCount > 0){
				praMean = (double) praSUM / candidateCount; 
				ecMean = (double) ecSUM / candidateCount;
				lcoMean = (double) lcoSUM / candidateCount;
				lciMean = (double) lciSUM / candidateCount; 
				lcaMean = (double) lcaSUM / candidateCount;
				dcMean = (double) dcSUM / candidateCount;
			}
			//pra features 1 to 5.
			datapoint.add("1:"+praSUM); datapoint.add("2:"+praMAX); datapoint.add("3:"+praMean); datapoint.add("4:"+(praMAX -praMIN)); datapoint.add("5:"+(praMAX - praMean));
			//ec features 6 to 10.
			datapoint.add("6:"+ecSUM); datapoint.add("7:"+ecMAX); datapoint.add("8:"+ecMean); datapoint.add("9:"+(ecMAX -ecMIN)); datapoint.add("10:"+(ecMAX - ecMean));  
			//lco features 11 to 15
			datapoint.add("11:"+lcoSUM); datapoint.add("12:"+lcoMAX); datapoint.add("13:"+lcoMean); datapoint.add("14:"+(lcoMAX - lcoMIN)); datapoint.add("15:"+(lcoMAX - lcoMean));  
			//lci features 16 to 20.
			datapoint.add("16:"+lciSUM); datapoint.add("17:"+lciMAX); datapoint.add("18:"+lciMean); datapoint.add("19:"+(lciMAX - lciMIN)); datapoint.add("20:"+(lciMAX - lciMean));
			//lca features 21 to 25.
			datapoint.add("21:"+lcaSUM); datapoint.add("22:"+lcaMAX); datapoint.add("23:"+lcaMean); datapoint.add("24:"+(lcaMAX - lcaMIN)); datapoint.add("25:"+(lcaMAX - lcaMean));  
			//dc features 26 to 30
			datapoint.add("26:"+dcSUM); datapoint.add("27:"+dcMAX); datapoint.add("28:"+dcMean); datapoint.add("29:"+(dcMAX - dcMIN)); datapoint.add("30:"+(dcMAX - dcMean));  

			NILmap.put(mention, datapoint); 
			System.out.println("mention = "+mention+", NIL flist ="+datapoint);
		}//for NIlmention
		
		return NILmap;
	}
	
	/*SIEL mention detector*/
	public HashMap<String, ArrayList<String>> baselineMD( ArrayList<String>qMentionList){
		Map<String, ArrayList<String>> mentionEFmap = new HashMap<String, ArrayList<String>>();
		Logger.clearFile(Variables.TACOutputDir.concat("disambiguations.txt"));//clear contents of disambiguations.txt b4 writting to it.
		Disambiguator disam = new Disambiguator();
		disam.features(qMentionList);
		mentionEFmap = disam.getDisambiguations();	System.out.println("Total mentions in current doc = "+mentionEFmap.size());
		return (HashMap<String, ArrayList<String>>) mentionEFmap;
	}//baselineMD()
	
	/*Ploch mention detector*/
	public HashMap<String, ArrayList<String>> plochMD(String qDocument, ArrayList<String>qMentionList){
		HashMap<String, ArrayList<String>> mentionECmap = new 	HashMap<String, ArrayList<String>>();
		Doc qDoc = getTestDoc(qDocument);//Doc(qDocument);
		if(qDoc.getText()!=null){
			mentionECmap = getPlochFeatures(qDoc,qMentionList);
		}//if doc-context found
		return (HashMap<String, ArrayList<String>>) mentionECmap;
	}//plochMD()
	
	/*Ploch candidates and disambiguation features*/
	public HashMap<String, ArrayList<String>> getPlochFeatures(Doc qDoc, ArrayList<String>qMentionList){
		
		Logger.clearFile(Variables.TACOutputDir.concat("PLdisambiguations.txt"));//clear contents of disambiguations.txt b4 writting to it.
		PLdisambiguator PLdisam = new PLdisambiguator();
		PLdisam.features(qMentionList);
		HashMap<String, ArrayList<String>> mentionECmap = PLdisam.getDisambiguations();	System.out.println("Total mentions in current doc = "+mentionECmap.size());
		//Ploch Disambiguation Features - start
		HashMap<String, ArrayList<String>> mentioncandFmap = new HashMap<String, ArrayList<String>>();//Maps mention#candidateEntity to Disambiguation Features.
		DocProcessor docProcessor= new DocProcessor();
		WikiOperator wikiOperator = new WikiOperator();
		SimilarityFinder sim = new SimilarityFinder();
		wikilinksIndex inIndex = new wikilinksIndex("inlinks");
		wikilinksIndex outIndex = new wikilinksIndex("outlinks");
		wikiabstractsIndex wikiAbstractWordIndex = new wikiabstractsIndex("words");
					
		Map<String, String> mentionEContext = docProcessor.getContext(qDoc.getText(), qMentionList, 2);//EntityContext(EC)
 
		for (String mention : mentionECmap.keySet()){
			System.out.println("MENTION "+mention+ "  " + mentionEContext.get(mention));
			HashMap<String, Double> bowVector = null, ecVector = null, lciVector = null, lcoVector = null, lcaVector=null;
			//BOW				
			ArrayList<String> al = new ArrayList<String>();
			al.add(mentionEContext.get(mention)); //System.out.println("BOW word list "+al);
			bowVector = sim.buildFeatureVector(al);
		
			//EC document vector
			String NEtaggedLine =  NEE.getNERmarked(mentionEContext.get(mention)); //System.out.println(NEtaggedLine);
			ArrayList<String> neTypeList = getInLineNE(NEtaggedLine);//System.out.println("Mentions found in context ="+neTypeList.size());
			ArrayList<String> neList = new ArrayList<String>();//Map<String,String> neTypeMap = new HashMap<String,String>();					
			for(String neType : neTypeList){
				String[] ne = neType.split("~");
				neList.add(ne[0]);//neTypeMap.put(ne[0], ne[1]);
			} 
			System.out.println("doc List "+neList);
			if(! neList.isEmpty()){
				//EC
				ArrayList<String> docVectorList = wikiOperator.getWikiEquivalents(neList); 	//System.out.println("doc wikiNE list"+docVectorList);
				ecVector = sim.buildFeatureVector(docVectorList);					
				//LC-out
				ArrayList<String> lcoVectorList = new ArrayList<String>();
				for(String wikiNE:docVectorList){
					lcoVectorList.addAll( outIndex.getLinks(wikiNE));
				} //System.out.println("LCO list "+lcoVectorList);
				lcoVector = sim.buildFeatureVector(lcoVectorList);					
				//LC-In
				ArrayList<String> lciVectorList = new ArrayList<String>();
				for(String wikiNE:docVectorList){
					lciVectorList.addAll( inIndex.getLinks(wikiNE));
				} //System.out.println("LCI list "+lciVectorList);
				lciVector = sim.buildFeatureVector(lciVectorList);
				//LCAall
				lcoVectorList.addAll(lciVectorList);
				lcaVector = sim.buildFeatureVector(lcoVectorList);
				
			}//if NEs found
			
			ArrayList<String> candList = new ArrayList<String>();
			for(String feat : mentionECmap.get(mention)){
				if(feat.startsWith("0:E")){
					ArrayList<String> df = new ArrayList<String>();//disamb features
					String entity = (feat.split(":")[1]);
					String candidate = searcher.searchById(entity).getName();

					ArrayList<String> candVectorList = new ArrayList<String>();
					System.out.println("Candidate = "+candidate+", Entity = "+entity);
					candVectorList.addAll(inIndex.getLinks(candidate));System.out.println("cands after adding inlinks "+ candVectorList.size());
					candVectorList.addAll(outIndex.getLinks(candidate)); System.out.println("cands after adding outlinks "+ candVectorList.size());
					//System.out.println("Cand vector list "+candVectorList);
					
					double BOWcosSim = 0.0;
					if(!candVectorList.isEmpty()){
						HashMap<String, Double> candVector = sim.buildFeatureVector(candVectorList); //System.out.println("Candidate vector c = "+candVector);
						
						if(! neList.isEmpty()){ 	//System.out.println("EC vector = "+ecVector);
							double ECcosSim = 0.0, LCOcosSim = 0.0, LCIcosSim = 0.0, LCAcosSim = 0.0;
							ECcosSim = sim.calculateCosineSimilarity(ecVector, candVector);
							System.out.println("EC Cos sim = " + ECcosSim); //System.out.println("LCO vector = "+ lcoVector);
							LCOcosSim = sim.calculateCosineSimilarity(lcoVector, candVector);
							System.out.println("LCO Cos sim = " + LCOcosSim); //System.out.println("LCI vector = "+ lciVector);
							LCIcosSim = sim.calculateCosineSimilarity(lciVector, candVector);
							System.out.println("LCI Cos sim = " + LCIcosSim); //System.out.println("LCA vector = "+ lcaVector);
							LCAcosSim = sim.calculateCosineSimilarity(lcaVector, candVector);
							System.out.println("LCA Cos sim = " + LCAcosSim);
							df.add("6:"+ECcosSim); df.add("7:"+LCOcosSim); df.add("8:"+LCIcosSim); df.add("9:"+LCAcosSim);
						}//if ecVector is filled 
					}//if candVector found		
					
					ArrayList<String> candAbstract = wikiAbstractWordIndex.getWords(candidate); //System.out.println("Words in candidate's abstract : "+candAbstract);
					if(!candAbstract.isEmpty() && (bowVector != null)){
						HashMap<String, Double> candAbstractVector = sim.buildFeatureVector(candAbstract);							
						BOWcosSim = sim.calculateCosineSimilarity(bowVector, candAbstractVector);
						System.out.println("BOW Cos sim = " + BOWcosSim);
					}					
					
					df.add("10:"+BOWcosSim);
					System.out.println(mention+"#"+entity+" Disambiguation features "+df);
					mentioncandFmap.put(mention.replaceAll(" ", "_")+"#"+entity, df);
				}//if Entityname
			}//for feat
			System.out.println("Found disambiguations for : "+mentioncandFmap.size());						

		}//for disambiguated mention
				
		//update ME map with disambiguation features
		for (String mention : mentionECmap.keySet()){
			ArrayList<String> flist = mentionECmap.get(mention);
			java.util.Iterator<String> it = flist.iterator();
			ArrayList<String> featureList = new ArrayList<String>();//holds all features of this mention
			ArrayList<String> datapoint = new ArrayList<String>();//holds features of this candidate
			String entityId = null;
			while(it.hasNext()){
				String element = it.next();
				if(element.split(":")[1].startsWith("E")){
					if(datapoint.isEmpty()){
						//skip
					} else {
						String dfKey = mention.replaceAll(" ", "_")+"#"+entityId.replace("0:", ""); //System.out.println("Appending dis features of "+dfKey);
						if(mentioncandFmap.containsKey(dfKey)){ //System.out.println("Added DF for "+dfKey);
							datapoint.addAll(mentioncandFmap.get(dfKey));//System.out.println("Expanded f list now "+datapoint);
						}
						featureList.addAll(datapoint);								
						datapoint = new ArrayList<String>();								
					}
					entityId = element;
				} 
				datapoint.add(element);
			}//while
			//to count in the last candidate
			String dfKey = mention.replaceAll(" ", "_")+"#"+entityId.replace("0:", "");
			if(mentioncandFmap.containsKey(dfKey)){
				datapoint.addAll(mentioncandFmap.get(dfKey));
				featureList.addAll(datapoint);
			}						
			mentionECmap.put(mention, featureList); 
			System.out.println("mention = "+mention+", flist ="+flist.size()+", featureList ="+featureList.size());
		}//mention
		//Ploch Disambiguation Features -end
		//close db connections 
		inIndex.destroy(); outIndex.destroy(); wikiAbstractWordIndex.destroy();
		return (HashMap<String, ArrayList<String>>) mentionECmap;
	}//getplochFeatures()
	

	/*Test. Given queries and mentions, generate solution (system linking). NEI E82 */
	public void getSolutionForTest(String xmlQueryFilename, boolean NILclassifier){

		if(NILclassifier){
			lc = new LinkClassifier(NILclassifier);
		} else {
			lc = new LinkClassifier();//loads model file
		}
		
		//Read queries by doc
		QueryFileParser qfParser = new QueryFileParser();
		qfParser.docMentionMap.clear();	
		qfParser.parseDocument(xmlQueryFilename);//E13-E54-E82 query.xml
		System.out.println("Documents in the Query file = "+qfParser.docMentionMap.size());//System.out.println(" :: "+qfParser.docMentionMap); 
		ArrayList<String> qidMentionList = new ArrayList<String>();
		ArrayList<String> mentionList = new ArrayList<String>();		
		Map<String,ArrayList<String>> solvedMentionMap = new HashMap<String, ArrayList<String>>();//mentions solved so far
		KBSearcher searcher = new KBSearcher(Variables.TACKBindex);
		//KBsubKB
		FileLoader fileLoader = new FileLoader();
		HashMap<String, Long> KBqueriesMap = fileLoader.loadKBqueriesIdMap();
		//KBsubKB
		
//		//KBsubKB
//		ArrayList<String> KbSubKbQueries = new ArrayList<String>();
//		KbSubKbQueries.add("EL012726");//	E0428365 Louisiana_Five #1	
//		KbSubKbQueries.add("EL012203");//	E0745565 Chester
//		KbSubKbQueries.add("EL006579");//	Tia_Mowry
//		KbSubKbQueries.add("EL000304");//	Barnhill_Arena
//		KbSubKbQueries.add("EL012264");//	Memphis_Grizzlies#5
//		KbSubKbQueries.add("EL014009");//	Michael_Szymanczyk
//		KbSubKbQueries.add("EL013705");//	Alberto_Medina
//		KbSubKbQueries.add("EL009140");//	Bob_Dole#8
//		KbSubKbQueries.add("EL012626");//	Austin_Motor_Company
//		KbSubKbQueries.add("EL011333");//	Powszechny_Zakład_Ubezpieczeń #10
		//KBsubKB 
        
		for(String document : qfParser.docMentionMap.keySet()){
			System.out.println("Current doc : "+document);
			qidMentionList = qfParser.docMentionMap.get(document); System.out.println("Num of qidMentions = "+qidMentionList.size()+"  :: "+qidMentionList);
			Map<String, String>mentionQidMap = new HashMap<String, String>();
			//KBsubKB
			boolean KbSubKbProceed = false;
			for(int pointer=0; pointer < qidMentionList.size(); ++pointer){
				if(KBqueriesMap.containsKey(qidMentionList.get(pointer))){
					KbSubKbProceed = true;
					pointer = qidMentionList.size(); //break looping
				}
				++pointer;
			}
			//KBsubKB
			if(KbSubKbProceed){	
				for(int pointer=1; pointer < qidMentionList.size(); ++pointer){
					mentionQidMap.put(qidMentionList.get(pointer), qidMentionList.get(pointer-1));//mention : Qid
					++pointer;
				}
				System.out.println("Num of Qmentions = "+mentionQidMap.size()+" :: "+mentionQidMap);
				//link the mention only if its not already linked
				boolean readDoc = false;
				for(String m : mentionQidMap.keySet()){
					if(!solvedMentionMap.containsKey(m)){
						readDoc = true;//read document. 					
					} 
				}
				if(readDoc){
					//Disambiguate all possible mentions in this document
					Doc testDoc = getTestDoc(document);
					
				    if(testDoc != null){
						//Map<String, ArrayList<String>> mentionSolutionMap = baselineLK(testDoc, new ArrayList(mentionQidMap.keySet()));
						//Map<String, ArrayList<String>> mentionSolutionMap = plochLK(testDoc, new ArrayList(mentionQidMap.keySet()), NILclassifier); //Candidate classifier
						Map<String, ArrayList<String>> mentionSolutionMap = tagmeLK(testDoc, new ArrayList(mentionQidMap.keySet()));
						System.out.println("Total mentions in current doc = "+mentionSolutionMap.size());
			
						for(int pointer=1; pointer < qidMentionList.size(); ++pointer){
							String qId = qidMentionList.get(pointer-1);
							String mention = qidMentionList.get(pointer);
							StringBuilder sb = new StringBuilder();
							if(mentionSolutionMap.containsKey(mention)){							
								ArrayList<String> solution = mentionSolutionMap.get(mention);						
								//sb.append("\n"+ qId+"\t"+solution.get(1)+"\t"+solution.get(0)+"\t"+solution.get(2)+"\t"); //baselineLK
								sb.append("\n"+ qId+"\t"+solution.get(0)+"\t"+solution.get(1)+"\t");//plochLK and tagmeLK
								System.out.println("SOLUTION for :"+mention+sb.toString());
		//						if(!NILclassifier){
		//							if(solution.get(1).startsWith("E")){
		//								System.out.println("Mapped to Entity = "+searcher.searchById(solution.get(1)).getName());
		//							}
		//						}
								if(!solution.get(0).equalsIgnoreCase("NIL")){
									solvedMentionMap.put(mention, solution);
								}
							} else {
								sb.append("\n"+ qId+"\t NIL \t 0.0 \t");//tagmeLK NIL
								System.out.println(" FAILED to link  "+mention+" of Query "+qId);
							}
							Logger.solutionTab(sb);
							++pointer;
						}//end for mention of query	
				    } else {
						System.out.println(" FAILED to link queries of doc "+document);
					} //end if Doc
				} else {
					
					for(int pointer=1; pointer < qidMentionList.size(); ++pointer){
						String qId = qidMentionList.get(pointer-1);
						String mention = qidMentionList.get(pointer);
						if(solvedMentionMap.containsKey(mention)){
							ArrayList<String> solution = solvedMentionMap.get(mention);
							StringBuilder sb = new StringBuilder();
							sb.append("\n"+ qId+"\t"+solution.get(0)+"\t"+solution.get(1)+"\t");//tagmeLK
							Logger.solutionTab(sb);System.out.println("SOLUTION for :"+mention+sb.toString());
						} else {
							System.out.println(qId+"Unable to read solvedMentionMap for "+mention+" of Query "+qId );
						}
						++pointer;
					}//end for mention of query
					
				}//end if readDoc
			}//end if KbsubKB
		}//end for doc
	}//getSolutionForTest()
	
	/* Get Solution (mention - link pairs) provided by current(IIIT TAC) system for the given docName*/
	public Map<String, ArrayList<String>> baselineLK(Doc document, ArrayList<String> mustHaveMentions){
		Map<String, ArrayList<String>> mentionSolutionMap = new HashMap<String, ArrayList<String>>();
		System.out.println("header length + "+document.getHeader().length());
		String NEtaggedLine =  NEE.getNERmarked(document.getHeader()); //System.out.println(NEtaggedLine);
		ArrayList<String> mentionTypeList = getInLineNE(NEtaggedLine);System.out.println("Mentions found in headline ="+mentionTypeList.size());
		Map<String,String> mentionTypeMap = new HashMap<String,String>();
		
		for(String mentionType : mentionTypeList){
			String[] men = mentionType.split("~");
			mentionTypeMap.put(men[0], men[1]);
		}
		NEtaggedLine = NEE.getNERmarked(document.getText()); //System.out.println(NEtaggedLine);
		mentionTypeList = getInLineNE(NEtaggedLine);System.out.println("Mentions found in posts ="+mentionTypeList.size());
		for(String mentionType : mentionTypeList){
			String[] men = mentionType.split("~");
			mentionTypeMap.put(men[0], men[1]);
		}
				
		if(!mustHaveMentions.isEmpty()){		
			Disambiguator disam = new Disambiguator();
			disam.features(mustHaveMentions);				
			Map<String, ArrayList<String>>featureMap =  disam.getDisambiguations();				
			
			for(String mention : featureMap.keySet()){
				ArrayList<String> solutionList = new ArrayList<String>();
				//get mentionType
				String mentionType = "UKN";
				if(mentionTypeMap.containsKey(mention)){
					mentionType = mentionTypeMap.get(mention);
				}//if
				System.out.println(mention +" Final type :"+mentionType);
				solutionList.add(mentionType);//#1 solution
				//classify the mention
				double maxConfidence = -10.0;
				String maxConfLink ="";
				ArrayList<String> flist = featureMap.get(mention);
				java.util.Iterator<String> it = flist.iterator();					
				double[] values = new double[5];	//index 0 for EntityId and 1 to 4 for features				
				String entityId = null;
				
				while(it.hasNext()){
					String element = it.next();
					String[] elements = element.split(":");
					if(elements[1].startsWith("E")){
						if(values.length == 0){
							//skip
						} else {
							double[] prob_estimates = lc.evaluate(values,lc.liveModel);		
							if(prob_estimates[1] > maxConfidence ){ 
								maxConfidence = prob_estimates[1]; 	// only when classlabel is one, that mapping is taken.
								maxConfLink = entityId; System.out.println("Max conf = "+maxConfidence);
							}								
							values = new double[5];								
						}
						entityId = elements[1];
					} else { 
						values[Integer.valueOf(elements[0])] = Double.valueOf(elements[1]);
					}
				}//while
				//to count in the last candidate
				if(values.length != 0){
					double[] prob_estimates = lc.evaluate(values,lc.liveModel);		
					if(prob_estimates[1] > maxConfidence ){ 
						maxConfidence = prob_estimates[1]; // only when classlabel is one, that mapping is taken.
						maxConfLink = entityId; System.out.println("Max conf = "+maxConfidence);
					}
					
				}//if
				/*for(int m=1;m<featureMap.get(mention).size();m++){			
					if(m%5!=0){
						String val = featureMap.get(mention).get(m).split(":")[1];
						values[(m-1)%5] = Double.parseDouble(val);
					}
					else{
						values[(m-1)%5] = 0;
						double[] prob_estimates = lc.evaluate(values,lc.liveModel);		
				
						if(prob_estimates[2] == 1.0 ){ 
							maxConfidence = prob_estimates[1];
							// only when classlabel is one, that mapping is taken.
							maxConfLink = featureMap.get(mention).get(m-5).split(":")[0];
						}
						System.out.println("Max conf = "+maxConfidence);
					}		
				}//end for feature list
				*/
				
				String solutionEntity = baselineLinker(mention, maxConfidence);//TAC 2014 Linker
				if(solutionEntity == null){
					solutionEntity = maxConfLink;
				}
				solutionList.add(solutionEntity);//#2 solution
				solutionList.add(String.valueOf(maxConfidence));//#3 solution
				mentionSolutionMap.put(mention, solutionList);				
	
			}//for mention	
		} else {//if mentionList not null
			System.out.println("NER did not detect qMention as mention");
		}
		return mentionSolutionMap;
	}//baselineLK()

	public String baselineLinker(String mention, double maxConfidence){
		String solnEntity = null;
		
		if(maxConfidence < 0.015){ //tau value
			if(NILs.containsKey(mention)){
				solnEntity = NILs.get(mention);
			} else{
				String value = Integer.toString(NILcount);
				if(value.length()<4){
					String to_add = "0";
					for(int k = 1;k<4-value.length();k++)
						to_add = to_add.concat("0");
					value = to_add.concat(value);
					value = "NIL".concat(value);
				}
				NILcount+=1;
				NILs.put(mention, value);
				solnEntity = value;
			}
			System.out.println("NIL mention Detected : "+mention+" = "+solnEntity);
		} 		
		return solnEntity;
	}//baselineLinker
	
	/* Ploch Solution (mention - link pairs) system for the given docName*/
	public Map<String, ArrayList<String>> plochLK(Doc document, ArrayList<String> mustHaveMentions, boolean NILclassifier){
		Map<String, ArrayList<String>> mentionSolutionMap = new HashMap<String, ArrayList<String>>();
				
		if(!mustHaveMentions.isEmpty()){		
			HashMap<String, ArrayList<String>>featureMap =  getPlochFeatures(document, mustHaveMentions);				
			HashMap<String, ArrayList<String>> NILmentionMap = new HashMap<String, ArrayList<String>>();//Maps mention#NILfeatures.

			if(NILclassifier){ //NIL classifier
				System.out.println("ATTEMPTING NIL classification");
				NILmentionMap = getNILmap(featureMap); System.out.println("NILmap size "+NILmentionMap.size());
				for(String mention:NILmentionMap.keySet()){
					ArrayList<String> solutionList = new ArrayList<String>(); 
					if(!NILmentionMap.get(mention).isEmpty()){
						double[] values = new double[31];
						for(String val:NILmentionMap.get(mention)){
							String[] v = val.split(":");
							values[Integer.valueOf(v[0])] = Double.valueOf(v[1]);
						}
						if(values.length != 0){
							double[] prob_estimates = lc.evaluate(values,lc.liveModel);		
							if(prob_estimates[2] == 1.0 ){
								solutionList.add("NIL");//#1 solution
								solutionList.add(String.valueOf(prob_estimates[1])); System.out.println("Mention :"+mention+" maps to NIL");
							} else {
								solutionList.add("E00000");//#1 solution
								solutionList.add(String.valueOf(prob_estimates[0])); //#2 solution								
							}
						}//if values
						mentionSolutionMap.put(mention, solutionList);			
					} else {
						System.out.println("Could not classify NIL mention : "+mention);
					}
				}//mention
			} else {//if nonNIL 
				System.out.println("ATTEMPTING Candidate classification");
				for(String mention : featureMap.keySet()){
					ArrayList<String> solutionList = new ArrayList<String>();					
					ArrayList<String> flist = featureMap.get(mention);
					java.util.Iterator<String> it = flist.iterator();					
					double maxConfidence = 0.0;
					String maxConfLink ="NIL";
					double[] values = new double[11];	//index 0 for EntityId and 1 to 10 for features				
					String entityId = null;
					
					while(it.hasNext()){
						String element = it.next();
						String[] elements = element.split(":");
						if(elements[1].startsWith("E")){
							if(values.length == 0){
								//skip
							} else {
								double[] prob_estimates = lc.evaluate(values,lc.liveModel);		
								if(prob_estimates[2] == 1.0 ){ 
									maxConfidence = prob_estimates[1]; 	// only when classlabel is one, that mapping is taken.
									maxConfLink = entityId; System.out.println("Max conf = "+maxConfidence);
								}								
								values = new double[11];								
							}
							entityId = elements[1];
						} else { 
							values[Integer.valueOf(elements[0])] = Double.valueOf(elements[1]);
						}
					}//while
					//to count in the last candidate
					if(values.length != 0){
						double[] prob_estimates = lc.evaluate(values,lc.liveModel);		
						if(prob_estimates[2] == 1.0 ){ 
							maxConfidence = prob_estimates[1]; // only when classlabel is one, that mapping is taken.
							maxConfLink = entityId; System.out.println("Max conf = "+maxConfidence);
						}
						
					}//if
					solutionList.add(maxConfLink);//#1 solution
					solutionList.add(String.valueOf(maxConfidence));//#2 solution
					mentionSolutionMap.put(mention, solutionList);				
				}//for mention
			}//nonNIL classifier
		} //if mentionList not null
			
		return mentionSolutionMap;
	}//plochLK()

	/* TAGME Solution (mention - link pairs) system for the given docName*/
	@SuppressWarnings("static-access")
	public Map<String, ArrayList<String>> tagmeLK(Doc document, ArrayList<String> mustHaveMentions){
		Map<String, ArrayList<String>> mentionSolutionMap = new HashMap<String, ArrayList<String>>();
		TagmeDisambiguator td = new TagmeDisambiguator();
		Pruning pruning = new Pruning();
		if(!mustHaveMentions.isEmpty()){
			td.features(document, mustHaveMentions,2, 0.3);			
			HashMap<String, ArrayList<String>> disambiguations = td.getDisambiguations();
			if(disambiguations.isEmpty()){
				System.out.println("Unable to disambiguate mentions in document : "+document.getId());
			} else {
				for(String qmention: mustHaveMentions){
					ArrayList<String> solution = new ArrayList<String>();
					ArrayList<String> neighborDisambiguations = disambiguations.get(qmention);
					if(neighborDisambiguations.isEmpty()){
						solution.add("NIL"); //solution#1
						solution.add("0.0");//solution#2
					} else {
						System.out.println("Query ="+qmention+". Before Pruning, no of mentions = "+ neighborDisambiguations.size());	
						ArrayList<String> mentionRhoList = pruning.coherence(neighborDisambiguations);
						for(int i=0; i<mentionRhoList.size(); i++){
							if(mentionRhoList.get(i).equalsIgnoreCase(qmention)){
								//solution.add(mentionRhoList.get(i));
								String solEntity = mentionRhoList.get(i+1);//solution#1 entity 
								//search TAC id of the wiki_title
								List<Entity> elist = searcher.searchByName(solEntity.toLowerCase());
								if(elist.size() > 0){
									solution.add(elist.get(0).getId());//solution#1 entity Id
								} else { 
									solution.add(solEntity);//solution#1 entity name
									System.out.println("No Entity Id entity found for the wiki title "+solEntity); 
								}						
								solution.add(mentionRhoList.get(i+2));//solution#2 rho
								i=mentionRhoList.size();//to exit for loop
							}
							i=i+2;
						}
					}//if tagme disambiguated
					mentionSolutionMap.put(qmention, solution);
				}//for qmention
			}//if doc disambiguated
		} //if mentionList not null
		
		return mentionSolutionMap;
	}//tagmeLK()


	//get the mentions from inlineXML o/p of Stanford NER parser
	public ArrayList<String> getInLineNE(String NEtaggedLine){
		ArrayList<String> mention = new ArrayList<String>();			
		String perPattern = "<PERSON>[a-zA-Z1-9.\\s]*</PERSON>";
		Pattern p = Pattern.compile(perPattern);
		Matcher mp = p.matcher(NEtaggedLine);
		while (mp.find( )) {
			String m = mp.group(0).substring(8, mp.group(0).length()-9); 
			//m = m.replace(System.getProperty("line.separator"), "");
			mention.add( m +"~PER") ;
		}

		String locPattern = "<LOCATION>[a-zA-Z1-9,\\s]*</LOCATION>";
		Pattern l = Pattern.compile(locPattern);
		Matcher ml = l.matcher(NEtaggedLine);
		while (ml.find( )) {
			String m = ml.group(0).substring(10, ml.group(0).length()-11);
			//m = m.replace(System.getProperty("line.separator"), "");
			mention.add( m+"~LOC" );
		}

		String orgPattern = "<ORGANIZATION>[a-zA-Z1-9,\\s]*</ORGANIZATION>";
		Pattern o = Pattern.compile(orgPattern);
		Matcher mo = o.matcher(NEtaggedLine);
		while (mo.find( )) {
			String m = mo.group(0).substring(14, mo.group(0).length()-15);
			//m = m.replace(System.getProperty("line.separator"), "");
			mention.add( m+"~ORG" );
		}

		return mention;
	}//End getInLineNE
	
	/*E57 NEI. Get Doc List from 2009 source docs*/
	public Doc getDoc(String docId){
    	DocSearcher ds;
    	Doc doc = new Doc();
		try {
			ds = new DocSearcher(Variables.TAC2009DocIndex);
			doc = ds.searchById(docId);
			//System.out.println("Id = "+doc.getId()+", Header = "+doc.getHeader()+", Type = "+doc.getType()+", Text = "+doc.getText());				
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return doc;
	}//getDoc

	/*E12 NEI. Get Doc List from 2010 source docs*/
	public Doc getTestDoc(String docId){
    	DocSearcher ds;
    	Doc doc = new Doc();
		try {
			ds = new DocSearcher(Variables.TAC2010DocIndex);
			doc = ds.searchById(docId);
			//System.out.println("Id = "+doc.getId()+", Header = "+doc.getHeader()+", Type = "+doc.getType()+", Text = "+doc.getText());				
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return doc;
	}//getTestDoc

	
	
	public String getGoldenMention(String qid, boolean validationVsTesting){
		String goldenEntity = null;
		String linkFile = null;
		if(validationVsTesting){//true - crossValidation trng with E15
			linkFile = Variables.TACtrainLinks;
		} else { //testing with E54/E13
			linkFile = Variables.TACtestLinks;
		}
		try {
			BufferedReader br = new BufferedReader(new FileReader(linkFile));			
			String sCurrentLine;			 
			while ((sCurrentLine = br.readLine()) != null) {
				if(sCurrentLine.contains(qid)){
					String[] gold = sCurrentLine.split("\t");
					goldenEntity = gold[1];
					return goldenEntity;
				}
			}			
		} catch (FileNotFoundException e1) {
			Logger.logOut(linkFile+" File not found");
			e1.printStackTrace();
		} catch (IOException e2) {
			Logger.logOut("Reading error with "+linkFile);
			e2.printStackTrace();
		}
		return goldenEntity;
	}//getGoldenMention()

	public void printSolution(Query q) {
		StringBuilder sb = new StringBuilder();
		//		int count = 0;
		//		for(String feature : q.getfeatureList()){
		//			if(count%6==0){				
		sb.append("\n"+"EL_"+q.getId()+"\t"+q.getLink()+"\t"+q.getType()+"\t"+q.getClassConfidence()+"\t");
		//			}
		//			sb.append(feature+",");
		//			count++;
		//		}			
		Logger.solutionTab(sb);System.out.println("SOLUTION : "+sb.toString());
	}//printSolution()

	public void printFeatures(ArrayList<String>featureList) {
		StringBuilder sb = new StringBuilder();
		int count = 1;
		for(String feature : featureList){				
			if(count%(featureList.size())==0){
				sb.append(feature+"\n"); //System.out.println("Done 1");
			} else {
				sb.append(feature+" ");
			}
			count++;			
		}			
		Logger.solutionOut(sb);System.out.println("LABELLED FEATURES : "+sb);
	}//printFeatures	
	
}//class
