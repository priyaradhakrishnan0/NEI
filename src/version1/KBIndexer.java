package version1;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import tac.model.Entity;
import tac.model.Entity.Category;
import Variables.Variables;

/**
 *
 * @author priya.r
 */
public class KBIndexer extends DefaultHandler {

	    public static void main(String[] args) throws Exception{
        KBIndexer indexer = new KBIndexer(Variables.TACKBdir, Variables.TACKBindex);
        indexer.index();
    }
    
    //-------------------------------------------------------------------------------
    
    private File kbDir;
    
    public KBIndexer(String kbDirName, String kbIndexDirName) throws Exception {
        this.kbDir = new File(kbDirName);
        if (!kbDir.isDirectory()) {
            throw new Exception("Resource not a directory");
        }
        directory = FSDirectory.open(new File(kbIndexDirName));
        writer = new IndexWriter(directory, new IndexWriterConfig(Version.LUCENE_43, KBIndexer.getAnalyzer()));
    }

    public void index() {
        ArrayList<File> files = new ArrayList<File>();
        ArrayList<File> subFiles = new ArrayList<File>();
        files.addAll(Arrays.asList(kbDir.listFiles()));
        do {
            for (File file : files) {
                System.out.println("Indexing " + file.getAbsolutePath() + " ...");
                if (file.isDirectory()) {
                    subFiles.addAll(Arrays.asList(file.listFiles()));
                } else {
                    parse(file);
                }
            }
            files.clear();
            files.addAll(subFiles);
            subFiles.clear();
        } while (!files.isEmpty());
        try {
            writer.close();
        } catch (CorruptIndexException ex) {
            Logger.getLogger(KBIndexer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(KBIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void parse(File file) {
        SAXParser parser;
        try {
            parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(file, this);
        } catch (Exception ex) {
            Logger.getLogger(KBIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private Entity entity;
//    private String linkEntity;
    private Category category;
    private String fact;
    private StringBuffer buffer = new StringBuffer();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        if (qName.equals("entity")) {
            entity = new Entity();
            entity.setId(attributes.getValue("id"));
            entity.setName(attributes.getValue("name"));
            entity.setType(attributes.getValue("type"));
            entity.setSource("wiki:" + attributes.getValue("wiki_title"));
        } else if (qName.equals("link")) {
            String linkEntity = attributes.getValue("entity_id");
            linkEntity = (linkEntity == null) ? "" : linkEntity + "~";
            buffer.append(linkEntity);
        } else if (qName.equals("facts")) {
            category = entity.new Category();
            category.setName(attributes.getValue("class").replaceAll("(?i)Infobox(_)*( )*", ""));
        } else if (qName.equals("fact")) {
            buffer.delete(0, buffer.length());
            fact = attributes.getValue("name");
        } else if (qName.equals("wiki_text")) {
            buffer.delete(0, buffer.length());
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("entity")) {
            System.out.println("Indexing entity => " + entity.getName().toString());
            index(entity);
            entity = null;
        } else if (qName.equals("facts")) {
            entity.addCategory(category);
        } else if (qName.equals("fact")) {
            category.addFact(fact, buffer.toString());
        } else if (qName.equals("wiki_text")) {
            entity.setContextText(buffer.toString());
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        buffer.append(ch, start, length);
    }
    private Directory directory;
    private IndexWriter writer;

    private void index(Entity entity) {
        Document doc = new Document();

        doc.add(new StringField("id", entity.getId(), Field.Store.YES));
        doc.add(new StringField("type", entity.getType(), Field.Store.YES));
        doc.add(new StringField("name", entity.getName(), Field.Store.YES));
        doc.add(new StringField("source", entity.getSource(), Field.Store.YES));
        doc.add(new StringField("class", entity.getClasses(), Field.Store.YES));
        doc.add(new StringField("context", entity.getContextText(), Field.Store.YES));
        doc.add(new StringField("properties", entity.getAllProperties(), Field.Store.YES));

        ((Field) doc.getField("name")).setBoost(2.0f);
        ((Field) doc.getField("class")).setBoost(1.25f);
        ((Field) doc.getField("properties")).setBoost(1.5f);

        try {
            writer.addDocument(doc);
        } catch (Exception ex) {
            Logger.getLogger(KBIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static Analyzer getAnalyzer() {
        Analyzer analyzer = null;
        try {
            analyzer = new StandardAnalyzer(Version.LUCENE_43, new StringReader(stopWords.replaceAll(" ", "\n")));// new FileReader(stopWordsFileName));
        } catch (Exception ex) {
            Logger.getLogger(KBIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return analyzer;
    }
    private static final String stopWords = "a abaft aboard about above abstract abstraction across afore aforesaid after again against agin ago aint albeit alFha all allowing almost alone along alongside already also although always am american amid amidst among amongst an and anent ani another any anybody anyone anything are aren't around as aslant aspect astride at athwart away b back bar barring base be because been before behind being below beneath beside besides best better between betwixt beyond both but c by -by can cannot can't case certain circa close concerning considering cos could couldn't couldst d dare dared daren't dares daring despite did didn't didnt different directly disseminate do does doesn't doing done don't dont dost doth down during durst e each early either em english enough ere et even ever every everybody everyone everything exactly example except excepting f failing far few fig first find finding five following for four from fully g get getting gonna gotta greater h ha had hadn't hard has hasn't hast hath have haven't having he he'd he'll her here here's hers herself he's high him himself his home how howbeit however how's i id identifie if ill i'm immediately implementing important in initiate input inside instantly into introduction is isn't it it'll it's its itself i've ive j just k l large last later least left less lest let's like likewise little living long m make many may mayn't me merely mid midst might mightn't mine minus more most much must mustn't my myself n near 'neath need needed needing needn't needs neither never nevertheless new next nigh nigher nighest nisi no no-one nobody none nor not nothing notwithstanding now o o'er off often of on once one oneself onli only onto open or other otherwise ought oughtn't our ours ourselves out output outside over own p past pending per performing perhaps plus possible present previous probably propose provided providing public q qua quite r rather re real realize really require respecting right round s same sans save saving second several shall shalt shan't she shed shell she's short should shouldn't show since six small so some somebody someone something sometimes soon special specific specification still such summat supposing sure t than that that'd that'll that's the thee thei their theirs their's them themselves then there thereby there's these they they'd they'll they're they've thi thine this tho those thou though three thro' through throughout thru thu thyself till to today together too touching toward towards true 'twas 'tween 'twere 'twill 'twixt two 'twould typical u under underneath unless unlike until unto up upon upto us use used usually v various versus very via vice vis-a-vis w wa wanna wanting was wasn't way we we'd well were weren't wert we've what whatev whatever what'll what's when whencesoever whenever when's where whereas where's whether which whichever whichsoever while whilst who who'd whoever whole who'll whom whore who's whose whoso whosoever why will with within without wont would wouldn't wouldst x y ye yet yield you you'd you'll your you're yours yourself yourselves you've z";
}
