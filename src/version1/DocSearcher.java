package version1;

import utility.Logger;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import Variables.Variables;
import tac.preprocess.DocIndexer;
//import tac.i.KBIndexer;
import tac.model.Doc;
import tac.model.Entity;

public class DocSearcher {

    private final Directory dataIndexDirectory;
    private IndexSearcher searcher;
    private int resultsCount=1;
    
    public DocSearcher(String dataIndexDirName) throws IOException {
        this.dataIndexDirectory = FSDirectory.open(new File(dataIndexDirName));
        this.searcher = new IndexSearcher(DirectoryReader.open(dataIndexDirectory));
    }//constructor  
    
    public static void main(String[] args){
    	DocSearcher ds;
    	/*train - uses 2009 doc Index
    	try {
			ds = new DocSearcher(Variables.TAC2009DocIndex);
			Doc doc = ds.searchById("ALHURRA_NEWS13_ARB_20051123_130100-1.LDC2006E92");
			System.out.println("Id = "+doc.getId()+", Header = "+doc.getHeader()+", Type = "+doc.getType()+", Text = "+doc.getText());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
    	
    	//test - Uses 2010 doc Index
		try {
			ds = new DocSearcher(Variables.TAC2010DocIndex);
			//Doc doc = ds.searchById("LTW_ENG_19960820.0031.LDC2007T07"); //2009 query
			Doc doc = ds.searchById("eng-WL-11-174646-13000609");//2010 query
			System.out.println("Id = "+doc.getId()+", Header = "+doc.getHeader()+", Type = "+doc.getType()+", Text = "+doc.getText());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	//End test*/

    }//main()

    public List<Doc> search(String queryString) {
        Query query = null;
        try {
            QueryParser parser = new QueryParser(Version.LUCENE_43, "text", KBIndexer.getAnalyzer());
            query = parser.parse(queryString);
        } catch (Exception ex) {
            Logger.log("Error while parsing query");
            ex.printStackTrace();
        }
        return search(query);
    }//search(qstring)

    public List<Entity> search(String queryEntity, Map<String, Float> queryWeightMap) {
        List<Entity> resultEntities = new ArrayList<Entity>();
        BooleanQuery query = new BooleanQuery();
        Query nameQuery = getNameQuery(queryEntity);
        Query classQuery = getClassQuery(queryWeightMap);
        Query contextQuery = getContextQuery(queryWeightMap);
        Query propQuery = getPropertyQuery(queryWeightMap);

        query.add(nameQuery, BooleanClause.Occur.SHOULD);
        query.add(classQuery, BooleanClause.Occur.SHOULD);
        query.add(contextQuery, BooleanClause.Occur.SHOULD);
        query.add(propQuery, BooleanClause.Occur.SHOULD);

        return resultEntities;
    }

    public List<Doc> search(Query query) {
        if (query == null) {
            return null;
        }
        List<Doc> resultDataDocs = new ArrayList<Doc>();
        try {
            TopDocs matchDocs = searcher.search(query, 10);
            for (ScoreDoc scoreDoc : matchDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);

                Doc dataDoc = new Doc();
                dataDoc.setId(doc.get("id"));
                dataDoc.setPath(doc.get("path"));
                dataDoc.setType(doc.get("type"));
                dataDoc.setDateTime(doc.get("date"));
                dataDoc.setHeader(doc.get("header"));
                dataDoc.setText(doc.get("text"));

                resultDataDocs.add(dataDoc);
            }
        } catch (IOException ex) {
            Logger.log("Error while searching doc index"); ex.printStackTrace();
        }
        return resultDataDocs;
    }

    public Doc searchById(String id) throws CorruptIndexException, IOException {
        TermQuery idQuery = new TermQuery(new Term("id", id));
        Logger.log("        Searching doc index with query "+idQuery);
        
        TopDocs matchDocs = searcher.search(idQuery, 10);
        Doc dataDoc = null;
        int length = matchDocs.scoreDocs.length;
        if (length > 1) {
            throw new RuntimeException("Multiple documents with same name");
        } else if (length == 1) {
            Document doc = searcher.doc(matchDocs.scoreDocs[0].doc);
            dataDoc = new Doc();
            dataDoc.setId(doc.get("id"));
            dataDoc.setPath(doc.get("path"));
            dataDoc.setType(doc.get("type"));
            dataDoc.setDateTime(doc.get("date"));
            dataDoc.setHeader(doc.get("header"));
            dataDoc.setText(doc.get("text"));
        }
        return dataDoc;
    }

    private Query getNameQuery(String queryEntity) {
        Query nameQuery = null;
        try {
            QueryParser parser = new QueryParser(Version.LUCENE_43, "name", KBIndexer.getAnalyzer());
            nameQuery = parser.parse(queryEntity);
        } catch (ParseException ex) {
            Logger.log("Error while parsing entity name in doc index"); ex.printStackTrace();
        }
        return nameQuery;
    }

    private Query getClassQuery(Map<String, Float> queryWeightMap) {
        return getWeightedQuery(queryWeightMap, "class");
    }

    private Query getContextQuery(Map<String, Float> queryWeightMap) {
        return getWeightedQuery(queryWeightMap, "context");
    }

    private Query getPropertyQuery(Map<String, Float> queryWeightMap) {
        return getWeightedQuery(queryWeightMap, "properties");
    }

    private Query getWeightedQuery(Map<String, Float> queryWeightMap, String fieldName) {
        Query query = new BooleanQuery();
        for (String phrase : queryWeightMap.keySet()) {
            PhraseQuery phraseQuery = new PhraseQuery();
            for (String word : phrase.split(" ")) {
                phraseQuery.add(new Term(fieldName, word));
            }
            phraseQuery.setBoost(queryWeightMap.get(phrase));
            phraseQuery.setSlop(3);         // need to investigate on this value
        }
        return query;
    }
}