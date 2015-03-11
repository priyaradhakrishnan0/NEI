package tac.model;

import java.util.HashMap;
import java.util.Map;

//import edu.stanford.nlp.ie.pascal.InfoTemplate;

import utility.Helper;

/**
 *
 * @author msoundar
 */
public class Entity {

    private String id="";
    private String name="";
    private String type="";
    private String source="";
    private Map<String, Category> categoryMap;
    private String contextText="";

    public Entity(){
        categoryMap = new HashMap<String,Category>();
    }
    public String getClasses() {
        return Helper.getString(categoryMap.keySet(),",");
    }
    public String getProperty(String key) {
        String value = null;
        if (categoryMap != null) {
            for (String categoryName : categoryMap.keySet()) {
                Category category = categoryMap.get(categoryName);
                value = category.getFact(key);
                if (value != null) {
                    break;
                }
            }
        }
        return value;
    }

    public void addCategory(Category category) {
        categoryMap.put(category.getName(), category);
    }


    public Map<String, Category> getCategoryPropertyMap() {
        return categoryMap;
    }
    public String getAllProperties(){
        HashMap<String,String> infoStringMap = new HashMap<String, String>();
        for(String key : categoryMap.keySet()){
            Category category = categoryMap.get(key);
            infoStringMap.put(key, category.getFactsString());
        }
        return Helper.getString(infoStringMap, "-", ";\n\t");
    }//getAllProperties

    public HashMap<String,String> getAllPropertiesMap(){
        HashMap<String,String> infoStringMap = new HashMap<String, String>();
        for(String key : categoryMap.keySet()){
            Category category = categoryMap.get(key);
            infoStringMap.put(key, category.getFactsString());
        }
        return infoStringMap;
    }//End getAllPropertiesMap
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = (id==null)?"":id;
    }

    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = (name==null)?"":name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = (type==null)?"":type;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = (source==null)?"":source;
    }

    public String getContextText() {
        return contextText;
    }

    public void setContextText(String contextText) {
        this.contextText = (contextText==null)?"":contextText;
    }
    
    public class Category {
        private String name;
        private Map<String, String> facts;
        public Category(){
            facts = new HashMap<String, String>();
        }

        public void addFact(String key, String value) {
            facts.put(key, value);
        }
        public String getFact(String key) {
            return facts.get(key);
        }
        public String getFactsString(){
            return Helper.getString(facts, " - ", ";\n\t\t");
        }
        
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
        
    }
}
