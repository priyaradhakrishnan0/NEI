package version1;

import java.util.ArrayList;
import java.util.HashMap;

import db.wikilinksIndex;

public class Pruning {
	
	public static void main(String[] args){
		Pruning pruning = new Pruning();
		TagmeDisambiguator td = TagmeDisambiguator.getInstance();
		HashMap<String, ArrayList<String>> disambiguations = td.getDisambiguations();
		
		if(disambiguations.containsKey("Colette Avital")){
			ArrayList<String> neighborDisambiguations = disambiguations.get("Colette Avital");
			System.out.println("Before Pruning, no of mentions = "+ neighborDisambiguations.size());	
			ArrayList<String> mentionRhoList = pruning.coherence(neighborDisambiguations);
		}
	}

	/*Calculate coherence between its candidate annotation Page of one mention with 
	 * candidate annotation Page of the other anchors in the query sentence. This is combined with
	 * link probability to get the pruning score.
	 * Returns map of mention to pruning score.*/
	public ArrayList<String> coherence(ArrayList<String> disambiguationFeatures){
		wikilinksIndex inlinkIndex = new wikilinksIndex("inlinks");
		
		ArrayList<String> mentionPruneScroreList = new ArrayList<String>();
		//populate REL matrix
		HashMap<String,Double> relMap = new HashMap<>();		
//		ArrayList<String> disambiguations = new ArrayList<String>();
//		for(int i=1; i<disambiguationFeatures.size(); i++){			
//			disambiguations.add(disambiguationFeatures.get(i));
//			i=i+2;
//		} System.out.println("No of mentions = "+disambiguations.size()); //System.out.println(disambiguations);

		for(int i=1; i<disambiguationFeatures.size(); i++){
			String left = disambiguationFeatures.get(i);
			for(int j=1; j<disambiguationFeatures.size(); j++){
				String right = disambiguationFeatures.get(j);
				if( j > i ){
					double rel = inlinkIndex.relatedness(left,right); //System.out.println("left ="+left+" right ="+right+" rel = "+rel);
					relMap.put(left+"!@#$"+right, rel);
					relMap.put(right+"!@#$"+left, rel);
				}		
				j=j+2;
			}//end for right
			i=i+2;
		} //System.out.println("Num of relMap entries = "+relMap.size());	
		//calculate coherence
		for(int i=1; i<disambiguationFeatures.size(); i++){
			String si = disambiguationFeatures.get(i);
			double Rel = 0;
			int length = 0; //System.out.println("Sense = "+si);
			for(int j=1; j<disambiguationFeatures.size(); j++){
				String sj = disambiguationFeatures.get(j);
		
				if(si.equals(sj)){
					continue;
				}
				length ++;
				if(relMap.containsKey(si+"!@#$"+sj)){
					Rel += relMap.get(si+"!@#$"+sj);
				} else if( relMap.containsKey(sj+"!@#$"+si)){
					Rel += relMap.get(sj+"!@#$"+si);
				} else {
					double rel = inlinkIndex.relatedness(si,sj);
					relMap.put(si+"!@#$"+sj, rel);
					relMap.put(sj+"!@#$"+si, rel);
					Rel += rel;
				}
				j = j+2;
			}//End for sj
			if(length==0){
				Rel = .3;
				length = 1;
			}
			//System.out.println("Rel = "+Rel+" length ="+length+" Coherence = "+Rel/length);
			double score = Double.parseDouble(disambiguationFeatures.get(i+1));
			
			//ASSUMPTION : Linear combination of coherence and lp. 
			//ASSUMPTION : Beta  = 0.5  i.e rho = rhoAVG
			double beta = 0.2;
			double rho = (double) beta * (Rel/length) + (double) (1 - beta) * score ;
			System.out.println("neighbor = "+disambiguationFeatures.get(i-1) + " coh = " + Rel/length + " lp = " + score + " rho = " + rho);

			//if( rho > 0.05){ ///ASSUMPTION :  Pruning score threshold = 0.05
				mentionPruneScroreList.add(disambiguationFeatures.get(i-1));//solution#1 mention
				mentionPruneScroreList.add(si);//solution#2 entity
				mentionPruneScroreList.add(String.valueOf(rho));//solution#3 rho
			//}
			i = i + 2;
		}//End for si

		inlinkIndex.destroy();
		System.out.println("Initial = "+disambiguationFeatures.size()+"  "+disambiguationFeatures);
		System.out.println("Final = "+mentionPruneScroreList.size()+"  "+mentionPruneScroreList);
		return mentionPruneScroreList;
	}//End coherence()

}//End class
