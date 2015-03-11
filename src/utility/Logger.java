package utility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.sql.Timestamp;
import java.util.Date;

import Variables.Variables;
import tac.model.Entity;

public class Logger {

	public static void main(String[] args) {
		Logger logger = new Logger();
		logger.logOut("Hello");
	}
	
	private static String outputDirectory;
	    
    public Logger() {
    	outputDirectory = Variables.TACOutputDir; 
    	//open this file in append mode
    }   

    public void log(){
		//TODO	
	}
    

	public static void log(String logHead, String logMessage){
		System.out.println(logHead + " : "+ logMessage);	
	}
	
	public static void log( String logMessage){
		System.out.println( logMessage);	
	}	
	public static void log(String logHead, Entity entity){
		System.out.println(logHead );
		System.out.println(" Id : "+entity.getId());
		System.out.println(" Type : "+entity.getType());
		System.out.println(" Name : "+entity.getSource());
		System.out.println(" Source : "+entity.getClasses());
		System.out.println(" Context : "+entity.getContextText());
		System.out.println(" Properties : "+entity.getAllProperties());
	}
	
	public enum level {
	    SEVERE, MODERATE
	    //, MINOR, NOTIFY 
	}
	
	//Level level;
	public static void log(level l, String logMessage){
		switch(l){
		case SEVERE:
			System.out.println("Severe log : "+ logMessage);
			break;
		case MODERATE:
			System.out.println("Moderate log : "+ logMessage);
			break;		
		default:
			System.out.println(" log : "+ logMessage);
			break;
		}//End switch			
	}

	/*Append EL solution to solution.txt*/
	public static boolean solutionOut(StringBuilder soln) {
    	boolean writting = false;
		try {
			File file = new File(Variables.TACOutputDir.concat("Solution.csv"));
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			} 
			FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);//open in append mode
			BufferedWriter bw = new BufferedWriter(fw);
			String content = soln.toString(); //System.out.println(content);
			if(content.length()>0){
				writting = true;
			}
			bw.write(content);
			bw.close(); //System.out.println("Done");
 		} catch (IOException e) {
			e.printStackTrace();
		}
		return writting;
    }//End solutionOut
	
	public static boolean solutionTab(StringBuilder soln) {
    	boolean writting = false;
		try {
			File file = new File(Variables.TACOutputDir.concat("Solution.tab"));
			if (!file.exists()) {
				file.createNewFile();
			} 
			FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);//open in append mode
			BufferedWriter bw = new BufferedWriter(fw);
			String content = soln.toString(); //System.out.println(content);
			if(content.length()>0){
				writting = true;
			}
			bw.write(content);
			bw.close(); //System.out.println("Done");
 		} catch (IOException e) {
			e.printStackTrace();
		}
		return writting;
    }//End solutionTab
	

	/*Append wiki API results to wikiOut.txt*/
	public static boolean wikiOut(String content) {
    	boolean writting = false;
		try {
			File file = new File(Variables.TACOutputDir.concat("wikiOut.txt"));
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			} 
			FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);//open in append mode
			BufferedWriter bw = new BufferedWriter(fw);
			if(content.length()>0){
				writting = true;
			}
			bw.write(content);
			bw.close(); //System.out.println("Done");
 		} catch (IOException e) {
			e.printStackTrace();
		}
		return writting;
    }//End wikiOut
	
	/*Append querries to query.txt*/
	public static boolean queryOut(StringBuilder query) {
    	boolean writting = false;
		try {
			File file = new File(Variables.TACOutputDir.concat("query.xml.run4"));
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			} 
			FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);//open in append mode
			BufferedWriter bw = new BufferedWriter(fw);
			String content = query.toString(); //System.out.println(content);
			if(content.length()>0){
				writting = true;
			}
			bw.write(content);
			bw.close(); //System.out.println("Done");
 		} catch (IOException e) {
			e.printStackTrace();
		}
		return writting;
    }//End queryOut

	/*Append disambiguation to disambiguations.txt*/
	public static boolean disambiguationOut(String a, ArrayList<String> disambiguation, char bPT) {
		String filename = null;
		switch (bPT) {
		case 'b':
			filename = Variables.TACOutputDir.concat("disambiguations.txt");	
			break;
		case 'P':
			filename = Variables.TACOutputDir.concat("PLdisambiguations.txt");	
			break;
		case 'T':
			filename = Variables.TACOutputDir.concat("tagmeDisambiguations.txt");	
			break;
		default:
			break;
		}
		
		
    	boolean writting = false;
		try {
			File file = new File(filename);
			// if file doesnt exists, then create it
			if (!file.exists()) { file.createNewFile(); } 
			FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);//open in append mode
			BufferedWriter bw = new BufferedWriter(fw);
			//System.out.println(a +"#"+ disambiguation);
			if(disambiguation.size()>0){
				writting = true;
			}
			bw.write(a + "#"+ disambiguation+"\n");
			bw.flush();
			bw.close(); //System.out.println("Done");
 		} catch (IOException e) {
			e.printStackTrace();
		}
		return writting;
    }//End disamOut
	
	/*Clear contents of a file*/
	public static void clearFile(String filename){
		try {
			File file = new File(filename);		
			PrintWriter writer = new PrintWriter(file);
			writer.print("");
			writer.close();
 		} catch (IOException e) {
			e.printStackTrace();
		}		
	}//clearFile
	
	
	/*log message in log.<timestamp> file*/
    public static boolean logOut(String message) {
    	boolean writting = false;
		try {
			java.util.Date date= new java.util.Date();
			//File file = new File(Variables.TACOutputDir.concat("log.").concat(new java.sql.Timestamp(date.getTime()).toString()));
			@SuppressWarnings("deprecation")
			File file = new File(Variables.TACOutputDir.concat("log.txt"));
			//File file = new File(Variables.TACOutputDir.concat("log.").concat(String.valueOf(Math.random())));
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			} 
			FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);
			BufferedWriter bw = new BufferedWriter(fw);
			String content = message +"\n";
			//System.out.println(content);
			if(content.length()>0){
				writting = true;
			}
			bw.write(content);
			bw.close(); //System.out.println("Done");
 		} catch (IOException e) {
			e.printStackTrace();
		}
		return writting;
    }//End logOut
}
