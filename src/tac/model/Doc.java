/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tac.model;

public class Doc {
    private String id="";
    private String type="";
    private String dateTime="";
    private String header="";
    private String footer="";
    private String text="";
    private String entities="";
    private String path="";
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }    

    public String getEntities() {
        return entities;
    }

    public void setEntities(String entities) {
        this.entities = entities;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
    
}
