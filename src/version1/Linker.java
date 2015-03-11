package version1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import utility.Helper;
import Variables.Variables;

public class Linker {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Linker linker = new Linker();
//		double[] vals = {2.4169323444366455,0.1,10.0,10.0,0};
//		System.out.println("Classified = "+linker.classify(vals));
		Helper.showMapList(linker.link());
	}//main()
	
	public Map<String, ArrayList<String>> link(){
		Map<String, ArrayList<String>> mentionSolutionMap = new HashMap<String, ArrayList<String>>();
		HashMap<String, String> NILs = new HashMap<>();
		int NILcount = 1;
		Disambiguator disam = new Disambiguator();
		//disam.features(mentionList);				
		Map<String, ArrayList<String>>featureMap =  disam.getDisambiguations();				
		
		for(String mention : featureMap.keySet()){
			ArrayList<String> solutionList = new ArrayList<String>();
			//classify the mention
			double maxConfidence = -10.0;
			String maxConfLink ="";
			double[] values = new double[5];
			for(int m=1;m<featureMap.get(mention).size();m++){			
				if(m%5!=0){
					values[(m-1)%5] = Double.parseDouble(featureMap.get(mention).get(m));
				}
				else{
					values[(m-1)%5] = 0;
					String probs = classify(values);
					if(probs.contains(",")){
						String[] prob = probs.split(",");					
	
						double prob1 = Double.valueOf(prob[1]); 
						if(prob1 > maxConfidence ){ 
							maxConfidence = prob1; // only when classlabel is one, that mapping is taken.
							maxConfLink = featureMap.get(mention).get(m-5);
						}
						System.out.println("Max conf = "+maxConfidence);
					}
				}		
			}//end for feature list
			
			if(maxConfidence < 0.01||maxConfLink==""){ //tau value
				if(NILs.containsKey(mention)){
					solutionList.add(NILs.get(mention));//#1 solution
					solutionList.add("1.0");//#2 solution
					mentionSolutionMap.put(mention, solutionList);
				}
				else{
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
					solutionList.add(value);//#1 solution
					solutionList.add("1.0");//#2 solution
					mentionSolutionMap.put(mention, solutionList);
				}
				System.out.println("NIL mention Detected : "+mention+" = "+solutionList);
			}
			else{
				solutionList.add(maxConfLink);
				solutionList.add(String.valueOf(maxConfidence));
				mentionSolutionMap.put(mention, solutionList);
			}

		}//for mention	
		return mentionSolutionMap;
	}//link()
	
	
	/*Contacts ELServer TACclassifier.
	 * Returns prediction probability of class 0 and 1*/
	public String classify(double[] values) {
		String parse_string="";
		HttpURLConnection uc = null;
		InputStream is = null;
		String charset = "UTF-8";
		try {
			String queryString = "f1="+values[0]+"&f2="+values[1]+"&f3="+values[2]+"&f4="+values[3]; System.out.println(queryString);
			URL url = new URL(Variables.TACclassifier+"?"+queryString);			
			
			uc= (HttpURLConnection) url.openConnection();
			uc.setRequestProperty("Accept-Charset", charset);
			uc.setRequestMethod("POST");
			uc.setDoOutput(true);
			uc.connect();

			int rspCode = uc.getResponseCode(); System.out.println("Response code "+rspCode);
			if (rspCode == 200) {
				is = uc.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				parse_string = br.readLine();
				is.close();
			} 
			uc.disconnect();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException io){
			io.printStackTrace();
		} finally {
		    if (is != null) {
		        try {
		            is.close();
		        } catch (IOException e) {
		        }
		    }
		    if (uc != null) {
		        uc.disconnect();
		    }
		}
		return parse_string;

	}//classify()


}//end class
