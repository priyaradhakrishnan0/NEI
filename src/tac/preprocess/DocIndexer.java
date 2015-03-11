package tac.preprocess;

import utility.Logger;
import version1.KBIndexer;
import Variables.Variables;
import tac.model.Doc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class DocIndexer {
    
    private File dataDir;
    private Directory directory;
    private IndexWriter writer;
    private DocParser parser;

    public DocIndexer(String dataDirName, String dataIndexDirName) throws Exception {
        this.dataDir = new File(dataDirName);
        if (!dataDir.isDirectory()) {
            throw new Exception("Resource not a directory -" + dataDir.getAbsolutePath());
        }
        directory = FSDirectory.open(new File(dataIndexDirName));
        writer = new IndexWriter(directory, new IndexWriterConfig(Version.LUCENE_43, KBIndexer.getAnalyzer()));
        parser = new DocParser();
    }//constructor

    public void index() {
        ArrayList<File> files = new ArrayList<File>();
        ArrayList<File> subFiles = new ArrayList<File>();
        files.addAll(Arrays.asList(dataDir.listFiles()));
        do {
            for (File file : files) {
                Logger.log("Indexing - " + file.getAbsolutePath());
                if (file.isDirectory()) {
                    subFiles.addAll(Arrays.asList(file.listFiles()));
                } else {
                    parser.reset();
                    parser.parse(file);
                    index(parser.getDocumentList());
                }
            }
            files.clear();
            files.addAll(subFiles);
            subFiles.clear();
        } while (!files.isEmpty());
        try {
            writer.close();
        } catch (Exception ex) {
            Logger.log("Parsing exception : "+DocIndexer.class.getName());
        }
    }//index()

    private void index(List<Doc> docList) {
        for (Doc doc : docList) {
            if (doc == null) {
                Logger.log("Document object is null, so skipping");
                continue;
            }

            Document luceneDoc = new Document();

            luceneDoc.add(new StringField("id", doc.getId(), Field.Store.YES));
            luceneDoc.add(new StringField("path", doc.getPath(), Field.Store.YES));
            luceneDoc.add(new StringField("type", doc.getType(), Field.Store.YES));
            luceneDoc.add(new StringField("date", doc.getDateTime(), Field.Store.YES));
            luceneDoc.add(new StringField("header", doc.getHeader(), Field.Store.YES));
            luceneDoc.add(new StringField("text", doc.getText(), Field.Store.YES));
            
//  TODO          ((Field) luceneDoc.getField("header") ).setBoost(1.5f);
//            ((Field) luceneDoc.getField("text")).setBoost(1.25f);

            try {
                writer.addDocument(luceneDoc);
            } catch (Exception ex) {
                Logger.log("Exception in indexing"+DocIndexer.class.getName());
                ex.printStackTrace();
            }
        }
    }//index(docList)

    public static void main(String[] args) throws Exception {
        DocIndexer indexer = new DocIndexer(Variables.TACDocDir, Variables.TAC2010DocIndex);
        indexer.index();
//        DocIndexer indexer = new DocIndexer("/home/priya/Datasets/LDC2009E57/TAC_2009_KBP_Evaluation_Source_Data/data/bc", Variables.TACDocIndex);
//        indexer.index();
    }
}//class()
