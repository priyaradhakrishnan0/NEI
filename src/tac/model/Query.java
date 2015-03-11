package tac.model;

import java.util.ArrayList;

public class Query {

	private String id="";
	private String name="";
	private String wikititle="";
	private String docid="";
	private long beg=0;
	private long end=0;
	private String type="";//PER/ORG/GPE/UKN
	private ArrayList<String> features = new ArrayList<String>();
	private String link="";
	private double classConfidence = 0.0;
  
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getWikititle() {
        return wikititle;
    }

    public void setWikititle(String wikititle) {
        this.wikititle = wikititle;
    }    
    
    public String getDocid() {
        return docid;
    }

    public void setDocid(String docid) {
        this.docid = docid;
    }
    
    public long getBeg() {
        return beg;
    }

    public void setBeg(long beg) {
        this.beg = beg;
    }    

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }
    public String getType() {
        return type;
    }

    public void setType(String type) {
    	this.type = type;
    }
    public ArrayList<String> getfeatureList() {
        return features;
    }

    public void setfeatureList(ArrayList<String> featureList) {
        this.features = featureList;
    }
    public String getLink() {
        return link;
    }
    public void setLink(String link) {
    	this.link = link;
    }
        
    public double getClassConfidence() {
        return classConfidence;
    }

    public void setClassConfidence(double classConfidence) {
        this.classConfidence = classConfidence;
    }   
    
}
