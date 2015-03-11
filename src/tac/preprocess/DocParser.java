package tac.preprocess;

import utility.Logger;
import tac.model.Doc;
import Variables.Variables;

import java.io.File;
import java.io.FileInputStream;
import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class DocParser extends DefaultHandler{

    
    private String dataFolderName;
    private String dataFileName;
    private List<Doc> docList;
    private StringBuffer buffer = new StringBuffer();

    public DocParser(){
        dataFolderName = Variables.TACDocDir;
        docList = new ArrayList<Doc>();
    }
    
    public static void main(String args[]){
		DocParser parser = new DocParser();
		File file = new File("/home/priya/Datasets/LDC2009E57/TAC_2009_KBP_Evaluation_Source_Data/data/bc/ALHURRA_NEWS13_ARB_20050412_130100-2.LDC2006E92.sgm");
		parser.parse(file);
		System.out.println("Docs found = "+ parser.getDocumentList().size());
	}//main()
    
    public void parse(File file) {
        SAXParser parser;
        try {
            dataFileName = file.getAbsolutePath();
            parser = SAXParserFactory.newInstance().newSAXParser();
            InputStream inputstream = new FileInputStream(file);
            Reader reader = new InputStreamReader(inputstream, "UTF-8");
            InputSource is = new InputSource(reader);
            is.setEncoding("UTF-8");
            parser.parse(is, this);  
            reader.close();
        } catch (Exception ex) {
            Logger.log("Parser Exception while parsing doc " + file.getAbsolutePath());
            ex.printStackTrace();
        }
    }

    public void parse(InputStream in) {
        SAXParser parser;
        try {
            parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(in, this);
        } catch (Exception ex) {
            Logger.log("Exception while parsing stream ");
        }
    }

    private Doc doc;
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        if (qName.equals("DOC")) {
            doc = new Doc();
            clearBuffer();
        } else if (qName.equals("DOCTYPE")) {
            clearBuffer();
            doc.setType(attributes.getValue("SOURCE"));
        } else if (qName.equals("DATETIME")
                || qName.equals("DATE")
                || qName.equals("BODY")) {
            clearBuffer();
        } else if (qName.equals("fact")) {
        } else if (qName.equals("wiki_text")) {
        }
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("DOCID")) {
            doc.setId(buffer.toString().trim());
            doc.setPath(dataFileName.replace(dataFolderName, "").substring(1));
        } else if (qName.equals("DOCTYPE")) {
            doc.setType(doc.getType() + buffer.toString());
        } else if (qName.equals("DATETIME") || qName.equals("DATE")) {
            doc.setDateTime(buffer.toString());
        } else if (qName.equals("BODY")) {
            doc.setText(buffer.toString());
        } else if (qName.equals("HEADER")
                || qName.equals("HEADLINE")
                || qName.equals("KEYWORD")
                || qName.equals("POSTER")
                || qName.equals("SUBJECT")) {
            doc.setHeader(buffer.toString());
        } else if (qName.equals("TRAILER")
                || qName.equals("FOOTER")) {
//            data.setHeader(buffer.toString());
        } else if (qName.equals("DOC")){
            docList.add(doc);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        buffer.append(ch, start, length);
    }
    
    private void clearBuffer() {
        buffer.delete(0, buffer.length());
    }

    public List<Doc> getDocumentList() {
        return this.docList;
    }

    public void reset() {
        docList.clear();
    }
}
