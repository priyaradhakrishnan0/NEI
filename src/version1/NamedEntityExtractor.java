/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package version1;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import utility.Helper;
import utility.Logger;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import Variables.Variables;
/**
 *
 * @author priya.r
 */

public class NamedEntityExtractor {
	
    static String serializedClassifier = Variables.serializedClassifier;
    static AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);

    private Map<String, String> namedEntityMap;

    public Map<String, String> getNamedEntityMap(Reader inputReader) throws IOException {

        if (this.namedEntityMap != null) {
            return namedEntityMap;
        }
        
        namedEntityMap = new LinkedHashMap<String,String>();
        
       //StanfordCoreNLP pipeline = new StanfordCoreNLP();
        Properties props = new Properties();
        //props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        props.put("ner.model", "stanford-corenlp-3.4-models/edu/stanford/nlp/models/ner/english.conll.4class.distsim.crf.ser.gz");
        props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        BufferedReader reader = new BufferedReader(inputReader);
        String line;
        while ((line = reader.readLine()) != null) {
            Annotation document = new Annotation(line);
            pipeline.annotate(document);
            List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
            for (CoreMap sentence : sentences) {
                for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                    String word = token.get(CoreAnnotations.TextAnnotation.class);
                    String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                    if (ne.length() > 1) {//to print only those words with a non-zero NEtype
                       // Logger.log("    word= " + word + ",  NE = " + ne);
                        namedEntityMap.put(word, ne);
                    }
                }
            }
        }

        Logger.log("    Combining NER phrases ...");

        String prev_word = null, prev_ne = null;

        //Combine adjacent keys if of same value(i.e NEtype)
        HashMap<String,String> tempMap = new HashMap<String, String>(namedEntityMap);
        
        for (Map.Entry<String, String> entry : namedEntityMap.entrySet()){
            String word = entry.getKey();
            String ne = entry.getValue();
            if (ne.equalsIgnoreCase(prev_ne)) {
                prev_word = prev_word.concat(" " + word);
            } else {

                if (prev_word != null) {
                    Logger.log("word = " + prev_word + " ne = " + prev_ne);
                    tempMap.put(prev_word, prev_ne);
                } 
                else {
                    //first pass
                    Logger.log("    word= " + word + ",  NE = " + ne);
                    tempMap.put(word, ne);
                }
                prev_word = word;
                prev_ne = ne;
            }

        }
        if (prev_word != null) {
            Logger.log(prev_word + " " + prev_ne);
            tempMap.put(prev_word, prev_ne);
        }

        //Logger.log(Helper.getString(tempMap, " = ", "\n\t"));
        
        namedEntityMap = tempMap;
        return namedEntityMap;
    }//getNamedEntityMap
    
    public Map<String, String> getNamedEntityMap(String inputSentence) {

        if (this.namedEntityMap != null) {
            return namedEntityMap;
        }
        
        namedEntityMap = new LinkedHashMap<String,String>();
        Properties props = new Properties();

        props.put("ner.model", Variables.nerModel);
        props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
        props.put("outputFormat", "inlineXML");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        Annotation document = new Annotation(inputSentence);
        pipeline.annotate(document);
        
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                Logger.log("    word= " + word + ",  NE = " + ne);
                if (ne.length() > 1) {//to print only those words with a non-zero NEtype
                    //Logger.log("    word= " + word + ",  NE = " + ne);
                    namedEntityMap.put(word, ne);
                }
            }
        }

//        Logger.log("    Combining NER phrases ..."+namedEntityMap.size());
//
//        String prev_word = null, prev_ne = null;
//
//        //Combine adjacent keys if of same value(i.e NEtype)
//        HashMap<String,String> tempMap = new HashMap<String, String>(namedEntityMap);
//        
//        for (Map.Entry<String, String> entry : namedEntityMap.entrySet()){
//            String word = entry.getKey();
//            String ne = entry.getValue();
//            if (ne.equalsIgnoreCase(prev_ne)) {
//            	tempMap.remove(prev_word);
//            	tempMap.remove(word);
//                prev_word = prev_word.concat(" " + word);                
//            } else {
//
//                if (prev_word != null) {
//                    Logger.log("word = " + prev_word + " ne = " + prev_ne);
//                    tempMap.put(prev_word, prev_ne);
//                } 
//                else {
//                    //first pass
//                    Logger.log("    word= " + word + ",  NE = " + ne);
//                    tempMap.put(word, ne);
//                }
//                prev_word = word;
//                prev_ne = ne;
//            }
//
//        }
//        if (prev_word != null) {
//            Logger.log(prev_word + " " + prev_ne);
//            tempMap.put(prev_word, prev_ne);
//        }
//
//        //Logger.log(Helper.getString(tempMap, " = ", "\n\t"));
//        Logger.log("Temp Map size ="+tempMap.size());
//        namedEntityMap = tempMap;
        return namedEntityMap;
    }

    
    public String getNERmarked(String line){
    	String NERline = null;
        NERline = classifier.classifyWithInlineXML(line);
        return NERline;
      }//End getNER

    public static void main(String args[]) throws IOException{
    	//System.out.println(System.getProperties().get("user.dir"));
    	//System.exit(0);

        NamedEntityExtractor ne = new NamedEntityExtractor();
//        FileReader reader = new FileReader(args[0]);
//        Map<String, String> neMap = ne.getNamedEntityMap(reader);
        Map<String, String> neMap = ne.getNamedEntityMap("Vasudeva Varma part of IIIT,Hyderabad. Rajiv Sangal is dean.");
//        Map<String, String> neMap = ne.getNamedEntityMap(args[0]);
        System.out.println("**********Created NE-Type map size = "+neMap.size());
        Helper.showMap(neMap);

        //System.out.println(ne.getNERmarked("Vasudeva Varma is part of IIIT,Hyderabad. "));
        //System.out.println(ne.getNERmarked(" Rajiv Sangal is dean BHU,Varanasi."));
    }
}
