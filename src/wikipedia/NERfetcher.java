package wikipedia;


import edu.stanford.nlp.classify.Classifier;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.*;
import edu.stanford.nlp.ling.CoreLabel;

import java.util.ArrayList;
import java.util.regex.*;

import Variables.Variables;

public class NERfetcher {
        Pattern entities_pat;
        AbstractSequenceClassifier<CoreLabel> classifier;

        public NERfetcher() {
                entities_pat = Pattern
                                .compile("<ORGANIZATION>([\\w\\s\\d]+)</ORGANIZATION>|<PERSON>([\\w\\s\\d]+)</PERSON>|<LOCATION>([\\w\\s\\d]+)</LOCATION>");
                classifier = CRFClassifier.getClassifierNoExceptions(Variables.serializedClassifier);
        }

        public ArrayList<String> extractEntities(String str) {
                ArrayList<String> entities = new ArrayList<String>();
                String results = classifier.classifyWithInlineXML(str);
                Matcher matcher = entities_pat.matcher(results);
                while (matcher.find()) {
                        for (int i = 1; i <= 3; i++) {
                                if (matcher.group(i) != null) {
                                        entities.add(matcher.group(i));
                                }
                        }
                }
                return entities;
        }

        public static void main(String [] args){
                NERfetcher n = new NERfetcher();
                String str = "Sachin hits a century in Calcutta.";
                System.out.println(n.extractEntities(str));
                //System.out.println(n.classifier.classifyWithInlineXML(str));
        }
}
