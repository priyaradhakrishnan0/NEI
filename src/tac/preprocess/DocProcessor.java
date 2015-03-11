package tac.preprocess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Variables.Variables;
import tac.model.Doc;
import utility.Logger;
import version1.DocSearcher;



public class DocProcessor {

	public static void main(String[] args) throws IOException {
			DocProcessor dp = new DocProcessor();
		
//			List<String> contentList = new ArrayList<String>();
//			contentList.add("CLINTON-DET Mich");
//			contentList.add("delegates stand (fairly) united Fence-mending at all party levels goes on after lengthy nominee race By Mark Hornbeck and Gordon Trowbridge The Detroit News DENVER -- Barack Obama's and Hillary Clinton's Michigan contingents are on a mission to come together during the Democratic National Convention this week -- but they're not there yet".toLowerCase());
//			contentList.add("Some rank-and-file delegates, however, say there are still some differences to work out, hurt feelings to be soothed and Clinton delegates to bring into the fold".toLowerCase());
//			contentList.add("Michigan is one of the states with more Clinton delegates than Obama delegates -- 69-59".toLowerCase());
//			contentList.add("Today is Clinton day at the party summit".toLowerCase());
//			contentList.add("She will address the convention this evening".toLowerCase());
//			String entity = "clinton";
//			System.out.println("Context : "+ dp.extractContext(contentList,entity,2));
			
			DocSearcher ds = new DocSearcher(Variables.TAC2010DocIndex);
			Doc doc = ds.searchById("APW_ENG_20010420.0729.LDC2007T07");
			ArrayList<String> m = new ArrayList<String>();
			m.add("Abbott");
			System.out.println(dp.getContext(doc.getText(), m, 2));
			
	}//main
	
	public Map<String, String> getContext(String docContents,ArrayList<String> mentionList, int contextSize){
		Map<String, String> mentionContextMap = new HashMap<String, String>();
		String[] sentences = docContents.split("\n");
		
		if(sentences.length > 0) {
//			ArrayList<String> sentenceList = new ArrayList<String>();
//			for(String sent : sentences){
//				sentenceList.add(sent.toLowerCase());
//			}
			for(String mention : mentionList){
				try {
					String context = extractContext(new ArrayList(Arrays.asList(sentences)), mention, contextSize);
					mentionContextMap.put(mention, context);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else {
			System.out.println("Document body has single line. No context");
		}
		return mentionContextMap;
	}
	
	public String extractContext(List<String> file_content, String entity, int contextSize) throws IOException {

		ArrayList<String> context = new ArrayList<String>();
		//Logger.log("Figuring out the context ....");
		String tokens[] = entity.split(" ");
		int i, j, k, pos = 0, count, last_pos = 0;
		String temp, str;
		// token checking of the query phrase
		for (j = 0; j < tokens.length; j++) {
			//Logger.log("    Query Entity ", tokens[j]);
			last_pos = 0; // reset it to every token, so that you can get the context completely
			for (i = 0; i < file_content.size(); i++) {
				str = file_content.get(i).toLowerCase();
				//Logger.log("    Statement ", str);
				if (str.contains(tokens[j].toLowerCase()) && i >= last_pos) {
					pos = i;// file_content.indexOf(str);
					//Logger.log("    Position "+ pos);
					// 2 sentences before query word
					for (k = pos, count = 0; k > 0 && count < contextSize; count++) {
						temp = file_content.get(--k);
						if (!(context.contains(temp))) {
							context.add(temp);
						}
						//Logger.log(	"    Adding to context, the Previous line ", temp);
					}
					// line containing the query word
					temp = file_content.get(pos);
					if (!(context.contains(temp))) {
						context.add(temp);
					}
					// 2 sentences after query word
					for (k = pos + 1, count = 0; k < file_content.size() && count < 2; count++) {
						temp = file_content.get(k++);
						if (!(context.contains(temp))) {
							context.add(temp);
						}
						//Logger.log("    Adding to context, the Next line ",temp);
						// Logger.log((i+1)+" : "+ rep_clust.get(i));
					}
					last_pos = pos + count + 1;
					// pos= file_content.indexOf(tokens[i].toLowerCase());
				}
				}
			}
		
		//Logger.log("    context with query word : "+ context);
		StringBuilder cont = new StringBuilder();
		for(String s: context) {
			cont.append(s);
			cont.append(' ');
		}
		return cont.toString();
}

}//class
