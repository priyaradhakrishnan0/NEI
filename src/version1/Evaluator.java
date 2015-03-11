package version1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utility.FileLoader;
import Variables.Variables;

public class Evaluator {
	/* parameter#0 Q, #1 Qc, #2 Sn, #3 Snc, #4 Gnil, #5 S */
	
	public static void main(String[] args) {
		Evaluator evaluator = new Evaluator();

		evaluator.getKbSubKbCrossValidation("Solution.tab.kbSubKb.ploch.100");

//		double eliminated = ( 117 ) / 5;
//		evaluator.getKbSubKbAccuracy("Solution.tab.kbSubKb.ploch.40", (int)(2*eliminated));

//		ArrayList<Integer> parameter = evaluator.getEvalParams("Solution.tab.tagme.rhoAVG", Variables.TACtestLinks);
//		double P = (double)(parameter.get(1) - parameter.get(3)) / (double)(parameter.get(5)-parameter.get(2)); System.out.println("P = "+P);
//		double R = (double)(parameter.get(1) - parameter.get(3)) / (double)(parameter.get(0)-parameter.get(4)); System.out.println("R = "+R);
//		double Acc = (double)parameter.get(1) / (double)parameter.get(0); System.out.println("Micro Accuracy = "+Acc);
//		double Pnil = (double)parameter.get(3) / (double)parameter.get(2); System.out.println("Pnil = "+Pnil);
//		double Rnil = (double)parameter.get(3) / (double)parameter.get(4); System.out.println("Rnil = "+Rnil);
	}//main()
	
	public ArrayList<Integer> getEvalParams(String sysOutput, String gold){
		ArrayList<Integer> parameters = new ArrayList<Integer>();		
		File sysFile = new File(Variables.TACOutputDir.concat(sysOutput));
		File goldFile = new File(gold);

		Map<String, String> goldMap = new HashMap<String, String>();
		Map<String, String> sysMap = new HashMap<String, String>();
		BufferedReader inp;
		try {
			System.out.println("Reading "+goldFile.getName());
			inp = new BufferedReader (new FileReader(goldFile.getAbsolutePath()));	
			String line = null;
			while( (line = inp.readLine()) !=null ){
				line = line.trim(); 
				String[] goldValues = line.split("\t");
				goldMap.put(goldValues[0], goldValues[1]);					
			}	
			inp.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		parameters.add(goldMap.size());	System.out.println("|Q| = "+goldMap.size()); //parameter#1 Q

		
		try {
			System.out.println("Reading "+sysFile.getName());
			inp = new BufferedReader (new FileReader(sysFile.getAbsolutePath()));	
			String line = null;
			while( (line = inp.readLine()) !=null ){
				line = line.trim(); 
				String[] sysValues = line.split("\t");
				sysMap.put(sysValues[0], sysValues[1]);					
			}	
			inp.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
				
		int Qec = 0, Gnil = 0, Sn = 0, Snc = 0, S = 0, Rn = 0; //Rnc nil recalled but not correct
		for(String gId : goldMap.keySet()){
			String gEntity = goldMap.get(gId);
			if(gEntity.startsWith("NIL")){
				++Gnil;
				if(sysMap.containsKey(gId)){ 
					if(sysMap.get(gId).trim().startsWith("NIL")){
						++Snc;
						String sysEntity = sysMap.get(gId); //System.out.println("Qid = "+gId+", gold = "+gEntity+", sys ="+sysEntity);
					}else {
						++Rn;
					}
				} 
			}
			
			if(sysMap.containsKey(gId)){ 
				String sysEntity = sysMap.get(gId); System.out.println("Qid = "+gId+", gold = "+gEntity+", sys ="+sysEntity); 
				if(goldMap.get(gId).equals(sysMap.get(gId))){
					++Qec;					
				}
			}
		}//for gId
		System.out.println("Q Entity Correct ="+Qec); System.out.println("System recalled nil = "+(Snc+Rn));
		int Qc = Qec + Snc;
		parameters.add(Qc);System.out.println("Qcorrect = "+Qc); //parameter#2 Qc
				
		for(String sId : sysMap.keySet()){
			if(sysMap.get(sId).trim().startsWith("NIL")){
				++Sn;
			}			
		}		
		parameters.add(Sn); System.out.println("SYSnil = "+Sn); //parameter#3 Sn
		parameters.add(Snc); System.out.println("SYSnilCorrect = "+Snc); //parameter#4
		parameters.add(Gnil); System.out.println("GOLDnil = "+Gnil);//parameter#5
		parameters.add(sysMap.size()); System.out.println("|SYS| = "+sysMap.size()); //parameter#6
		return parameters;
	}//getEvalParams()
	
	
	public ArrayList<Double> getKbSubKbAccuracy(String sysOutput, int eliminatedCount){
		ArrayList<Double> results = new ArrayList<Double>();
		double accuracy = 0.0, precision = 0.0, recall = 0.0;
		int truePositive = 0, falsePositive = 0, falseNegative = 0, trueNegative = 0;
		ArrayList<Integer> parameters = new ArrayList<Integer>();		
		//sys
		File sysFile = new File(Variables.TACOutputDir.concat(sysOutput));
		Map<String, String> sysMap = new HashMap<String, String>();
		int NILcount = 0;
		BufferedReader inp;
		try {
			System.out.println("Reading "+sysFile.getName());
			inp = new BufferedReader (new FileReader(sysFile.getAbsolutePath()));	
			String line = null;
			while( (line = inp.readLine()) !=null ){
				line = line.trim(); 
				String[] sysValues = line.split("\t");
				sysMap.put(sysValues[0], sysValues[1]);					
				if(sysValues[1].trim().startsWith("N")){
					NILcount++;
				}
			}	
			System.out.println("NIL count = "+NILcount);
			inp.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		accuracy = (double) NILcount / (double) eliminatedCount;		
		System.out.println("Acc_kbSubKb = "+accuracy);
		results.add(accuracy);
		
//gold for KBsubKB
/*ploch
 		FileLoader solnfileLoader = new FileLoader();
		List<String> eliminatedEntries = new ArrayList<String>(solnfileLoader.loadKBqueriesMap().keySet()); */ 

//tagme
 		FileLoader solnfileLoader = new FileLoader();
		List<String> eliminatedEntries = new ArrayList<String>(solnfileLoader.loadKBqueriesIdMap().keySet()); 

/*tagme
 		ArrayList<String> goldLink = new ArrayList<String>();
		goldLink.add("EL012726"); //goldLink.add("E0428365");	
		goldLink.add("EL012203"); //goldLink.add("E0745565");// Chester
		goldLink.add("EL006579"); //goldLink.add("Tia_Mowry");
		goldLink.add("EL000304"); //goldLink.add("Barnhill_Arena");
		goldLink.add("EL012264"); //goldLink.add("Memphis_Grizzlies");
		goldLink.add("EL014009"); //goldLink.add("Michael_Szymanczyk");
		goldLink.add("EL013705"); //goldLink.add("Alberto_Medina");
		goldLink.add("EL009140"); //goldLink.add("Bob_Dole");
		goldLink.add("EL012626"); //goldLink.add("Austin_Motor_Company");
		goldLink.add("EL011333"); //goldLink.add("Powszechny_Zakład_Ubezpieczeń");
		System.out.println("Total queries |Q| = "+goldLink.size());		 
		ArrayList<String> eliminatedEntries = new ArrayList<String>();
		for(int i=0; i<eliminatedCount; ++i){
			eliminatedEntries.add(goldLink.get(i));
		}
		System.out.println("Queries in KB = "+(goldLink.size() - eliminatedEntries.size())); //
		*/
		System.out.println("OOKB queries = "+eliminatedEntries.size());
		
		for(String sysId :sysMap.keySet()){
			String sysLink = sysMap.get(sysId); 
			if(sysLink.trim().startsWith("N")){ //marked NIL
				if(eliminatedEntries.contains(sysId)){//real NIL
					++truePositive;
				} else {// Entity present in kb 
					++falsePositive;
				}
			} else { //marked inKB
				if(eliminatedEntries.contains(sysId)){//NIL
					++falseNegative;
				} else { //present in KB
					++trueNegative;
				}
			}
		}
		System.out.println("TP = "+truePositive+", FP ="+falsePositive+", FN ="+falseNegative+", TN="+trueNegative);
		precision = (double) truePositive / (truePositive + falsePositive);
		results.add(precision);
		System.out.println("Precision = "+precision);
		recall = (double) truePositive / (truePositive + falseNegative);
		results.add(recall);
		System.out.println("Recall = "+ recall);
		return results;
	}//end getKbSubKbAccuracy()

	public ArrayList<Double> getKbSubKbCrossValidation(String sysOutput){
		ArrayList<Double> results = new ArrayList<Double>();
		double accuracy = 0.0, precision = 0.0, recall = 0.0;
		
		ArrayList<Integer> parameters = new ArrayList<Integer>();		
		//sys
		File sysFile = new File(Variables.TACOutputDir.concat(sysOutput));
		Map<String, String> sysMap = new HashMap<String, String>();
		int NILcount = 0;
		BufferedReader inp;
		try {
			System.out.println("Reading "+sysFile.getName());
			inp = new BufferedReader (new FileReader(sysFile.getAbsolutePath()));	
			String line = null;
			while( (line = inp.readLine()) !=null ){
				line = line.trim(); 
				String[] sysValues = line.split("\t");
				sysMap.put(sysValues[0], sysValues[1]);					
				if(sysValues[1].trim().startsWith("N")){
					NILcount++;
				}
			}	
			System.out.println("Total NIL count = "+NILcount);
			inp.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
//gold for KBsubKB
/*ploch */
 		FileLoader solnfileLoader = new FileLoader();
		List<String> subKbEntries = new ArrayList<String>(solnfileLoader.loadKBqueriesMap().keySet());  //
		
/*tagme//		
		FileLoader solnfileLoader = new FileLoader();
		List<String> subKbEntries = new ArrayList<String>(solnfileLoader.loadKBqueriesIdMap().keySet()); */ 

		
		int adder = subKbEntries.size()/5;   
		System.out.println("sub KB size "+subKbEntries.size()+ ", Adder = "+adder);
		for(int k =0; k < 5; ++k){
			int width = 5; //for 20% make it 1, for 40% make this 2 and so on..
			int lowerBound = k*adder, upperBound = lowerBound + adder*width;
			System.out.println("Eliminating "+lowerBound +" to "+upperBound);
			int truePositive = 0, falsePositive = 0, falseNegative = 0, trueNegative = 0, Ncount = 0;//reset
			ArrayList<String> eliminatedEntries = new ArrayList<String>();
			for(String j : subKbEntries){				
				if(subKbEntries.indexOf(j) >= lowerBound && subKbEntries.indexOf(j) < upperBound){
					eliminatedEntries.add(j);
				}//if
			}//for
			System.out.println("OOKB queries = "+eliminatedEntries.size());
//			if(eliminatedEntries.size() < width*adder){
//				continue;
//			}
			for(String eliminatedId : eliminatedEntries){
				if(sysMap.containsKey(eliminatedId)){
					if(sysMap.get(eliminatedId).trim().startsWith("N")){
						Ncount += 1;
					}
				}
			}
			System.out.println(" Ncount = "+Ncount);
			accuracy += (double) Ncount / (double) eliminatedEntries.size();		
			System.out.println("Acc_kbSubKb = "+accuracy);
			results.add(accuracy);			
			for(String sysId :sysMap.keySet()){
				String sysLink = sysMap.get(sysId); 
				if(sysLink.trim().startsWith("N")){ //marked NIL
					if(eliminatedEntries.contains(sysId)){//real NIL
						++truePositive;
					} else {// Entity present in kb 
						++falsePositive;
					}
				} else { //marked inKB
					if(eliminatedEntries.contains(sysId)){//NIL
						++falseNegative;
					} else { //present in KB
						++trueNegative;
					}
				}
			}
			System.out.println("TP = "+truePositive+", FP ="+falsePositive+", FN ="+falseNegative+", TN="+trueNegative);
			precision += (double) truePositive / (truePositive + falsePositive);
			recall += (double) truePositive / (truePositive + falseNegative);
					
		}//for k
		
		results.add(precision/5); System.out.println("Precision = "+precision/5);
		results.add(recall/5); System.out.println("Recall = "+ recall/5);
		results.add(accuracy/5);System.out.println("Accuracy = "+accuracy/5);
		return results;
	}//end getKbsubKb crossvalidation

	
	
}//end class
