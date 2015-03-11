package utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimilarityFinder {
	
	public static void main(String[] args){
		SimilarityFinder sf = new SimilarityFinder();
		ArrayList<String> l = new ArrayList<String>();
//		l.add("Israeli helicopter gunships rocketed a motorcade in southern bombing in 1992 that killed 29 people, about a month after Lebanon, killing Hezbollah leader Sheik Abbas Musawi, Nasrallah's predecessor. Mughniyeh was accused of engineering the Argentine attack");
		l.add("Abbott"); l.add("Israel"); l.add("Jaathi"); l.add("Abbott Laboratories");
		HashMap<String, Double> featureVectorL = sf.buildFeatureVector(l);
		System.out.println("L = "+featureVectorL);
		ArrayList<String> m = new ArrayList<String>();
//		m.add("abbas"); m.add("koshteh"); m.add("village");//, bamyan, province, northern, central, afghanistan
		m.add("Embasy"); m.add("Israel"); m.add("Bomb"); m.add("Israeli Army");
		HashMap<String, Double> featureVectorM = sf.buildFeatureVector(m);
		System.out.println("M = "+featureVectorM);
		System.out.println(sf.calculateCosineSimilarity(featureVectorL, featureVectorM));
		
	}//main()
	
	/*calculate feature word to tf feature map
	 * @param features The list of words
	 * @return feature to tf value map*/
	public HashMap<String, Double> buildFeatureVector(ArrayList<String> features){
		HashMap<String, Double> featureVector = new HashMap<String, Double>();
		double wordFraction = (double) 1 / features.size(); //System.out.println("Word fraction = "+wordFraction); 
		for(String f : features){
			for(String feature : f.split(" ")){
				if(featureVector.containsKey(feature)){
					double freq = featureVector.get(feature)+ wordFraction;
					featureVector.put(feature, freq);
				} else {
					featureVector.put(feature, wordFraction);	
				}
				//System.out.println(feature +" - "+wordFraction);
			}//for word
		}//for input feature
		return featureVector;
	}//end buildFeatureVector
	
	/**
	* calculate the cosine similarity between feature vectors
	* The feature vector is represented as HashMap<String, Double> of feature and value(tf or tf.idf)
	* @param firstFeatures The feature vector of the first document
	* @param secondFeatures The feature vector of the second document
	* @return the similarity measure
	*/
	public static Double calculateCosineSimilarity(HashMap<String, Double> firstFeatures, HashMap<String, Double> secondFeatures) {
		Double similarity = 0.0;
		Double sum = 0.0; // the numerator of the cosine similarity
		Double fnorm = 0.0; // the first part of the denominator of the cosine similarity
		Double snorm = 0.0; // the second part of the denominator of the cosine similarity
		for(String featureName : firstFeatures.keySet()) {
			if(secondFeatures.containsKey(featureName)){
				sum = sum + firstFeatures.get(featureName) * secondFeatures.get(featureName);
			}
		} 
		fnorm = calculateNorm(firstFeatures);
		snorm = calculateNorm(secondFeatures);
		if((fnorm > 0) && (snorm > 0)){
			Double sim = sum / (fnorm * snorm);
			if( !Double.isNaN(sim)){
				similarity = sim;
			}
		}
		return similarity;
	}

	/*** calculate the norm of one feature vector
	* @param feature to value mapping
	* @return
	*/
	public static Double calculateNorm(HashMap<String, Double> feature) {
		Double norm = 0.0;
		for(String featureName : feature.keySet()){
			norm = norm + Math.pow(feature.get(featureName), 2);
		}
		return Math.sqrt(norm);
	}
	
}//class
