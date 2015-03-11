package version1;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
//import tac.index.KBIndexer;
import tac.model.Entity;
import Variables.Variables;
import utility.Logger;

/**
 *
 * @author priya.r
 * searches the LUCENE index of KB
 */
public class KBSearcher {
    
    public static void main(String[] args) {
        KBSearcher searcher = new KBSearcher(Variables.TACKBindex);
        System.out.println("Entity = "+searcher.searchById("E0745565").getName());
        
//        searcher.setResultsCount(1);
//        String a = "apple";//args[0];
//        System.out.println("Searching mention : "+a);
//        ArrayList<String> KBentities = new ArrayList<String>();
//        //search in name
//        List<Entity> elist = searcher.searchByName(a.toLowerCase());
//        if(elist.size() > 0){
//        	Iterator<Entity> it = elist.iterator();
//        	while(it.hasNext()){
//        		Entity e = (Entity) it.next();        		
//        		System.out.println("Entity id: "+e.getId()+", name : "+e.getName()+", score : "+searcher.getResultScoreMap().get(e)+", rank : 0.4 ");
//        		KBentities.add(e.getId());
//			}        	
//        } //else { System.out.println("No entity found with name "+a); }
//        //search in source
//        elist = searcher.searchBySource(a.toLowerCase());
//        if(elist.size() > 0){
//        	Iterator<Entity> it = elist.iterator();
//        	while(it.hasNext()){
//        		Entity e = (Entity) it.next();        		
//        		System.out.println("Entity id: "+e.getId()+", name : "+e.getName()+", score : "+searcher.getResultScoreMap().get(e)+", rank : 0.3 ");
//        		KBentities.add(e.getId());
//			}        	
//        } //else { System.out.println("No entity found with name "+a); }
//        //search in context
//        elist = searcher.search(a.toLowerCase());
//        if(elist.size() > 0){
//        	Iterator<Entity> it = elist.iterator();
//        	while(it.hasNext()){
//        		Entity e = (Entity) it.next();        		
//        		System.out.println("Entity id: "+e.getId()+", name : "+e.getName()+", score : "+searcher.getResultScoreMap().get(e)+", rank : 0.2 ");
//        		KBentities.add(e.getId());
//			}        	
//        } //else { System.out.println("No entity found with name "+a); }
//        //search in infobox attribute values
//        elist = searcher.searchByProperties(a.toLowerCase());
//        if(elist.size() > 0){
//        	Iterator<Entity> it = elist.iterator();
//        	while(it.hasNext()){
//        		Entity e = (Entity) it.next();        		
//        		System.out.println("Entity id: "+e.getId()+", name : "+e.getName()+", score : "+searcher.getResultScoreMap().get(e)+", rank : 0.1 ");
//        		KBentities.add(e.getId());
//			}        	
//        } //else { System.out.println("No entity found with name "+a); }

        
    // System.out.println("No of hits = "+searcher.getResultSize(args[0]));
        //System.out.println("No of hits = "+hits.size());
    //    System.out.println("No of hits = "+searcher.getTermFreq(args[0].toLowerCase()));
    }
    
    private Directory kbIndexDirectory;
    private IndexSearcher searcher;
    private int resultsCount=1;

    public KBSearcher(String kbIndexDirName) {
        try {
			this.kbIndexDirectory = FSDirectory.open(new File(kbIndexDirName));
			this.searcher = new IndexSearcher(DirectoryReader.open(kbIndexDirectory));
		} catch (IOException e) {
			Logger.log("Unable to access KB Index");
			e.printStackTrace();
		}
        
    }//constructor
    
    /*Returns number of occurrences of query term in Lucene index.
     * Though this gives accurate count of occurances, its very slow */
    public int getTermFreq(String queryTerm){
    	int termFreq = 0;
   
    	try {
    		IndexReader indexReader = DirectoryReader.open(kbIndexDirectory);

    	    Bits liveDocs = MultiFields.getLiveDocs(indexReader);
    	    Fields fields = MultiFields.getFields(indexReader);
    	    for (String field : fields) {
    	        TermsEnum termEnum;				
				termEnum = MultiFields.getTerms(indexReader, field).iterator(null);				
    	        BytesRef bytesRef;
    	        while ((bytesRef = termEnum.next()) != null) {
    	            if (termEnum.seekExact(bytesRef, true)) {
    	                DocsEnum docsEnum = termEnum.docs(liveDocs, null);
    	                if (docsEnum != null) {
    	                    int doc;
    	                    while ((doc = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
    	                    	//System.out.println("doc = "+ doc );
    	                    	 //System.out.println(bytesRef.utf8ToString() + " in doc " + doc + ": " + docsEnum.freq());
    	                    	if(bytesRef.utf8ToString().equals(queryTerm)){
        	                        System.out.println(bytesRef.utf8ToString() + " in doc " + doc + ": " + docsEnum.freq());
    	                    		termFreq += docsEnum.freq();
    	                    	}
    	                    }
    	                }
    	            }
    	        }
	
    	    }

   	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	System.out.println(queryTerm +" Total freq ="+termFreq);
    	return termFreq;
    }//getTermFreq

    /*Returns num of documents in Lucene index containing this term*/
    public int getTermDocFreq(String queryTerm){
    	int termDocFreq = 0;
   
    	try {
    		IndexReader indexReader = DirectoryReader.open(kbIndexDirectory);
    	    Fields fields = MultiFields.getFields(indexReader);

    	    for (String field : fields) {
    	        TermsEnum termEnum = MultiFields.getTerms(indexReader, field).iterator(null);
    	        BytesRef bytesRef;
    	        while ((bytesRef = termEnum.next()) != null) {
    	            int freq = indexReader.docFreq(new Term(field, bytesRef));
    	            if(bytesRef.utf8ToString().equals(queryTerm)){
        	            System.out.println(bytesRef.utf8ToString() + " in " + freq + " documents");
                		termDocFreq += freq;
                	}
    	        }
    	    }
    	
    	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return termDocFreq;
    }//getTermDocFreq
    
    /*Returns num of TopScoring documents in Lucene index containing this term*/
    public int getResultSize(String queryString) {
        Query query = null;
        try {
            QueryParser parser = new QueryParser(Version.LUCENE_43, "context", KBIndexer.getAnalyzer());
            query = parser.parse(queryString.toLowerCase());
        } catch (Exception ex) {
            Logger.log(Logger.level.SEVERE, ex.getClass().getName());
        }
        return getResults(query);
    }
    
    public int getResults(Query query) {
    	int resultCount = 0;
    	int maxHits = 100000;
    	if (query == null) {
            return resultCount;
        }        
        try {
        	
        	TopScoreDocCollector collector = TopScoreDocCollector.create(maxHits, true);
        	searcher.search(query, collector);
        	resultCount = collector.getTotalHits();
        	
        //	 resultCount = searcher.getIndexReader().docFreq(new Term("name", query.toString().toLowerCase()));
        } catch (IOException ex) {
            Logger.log("    There was a problem while searching the lucene ! ", ex.getClass().getName());
        }
        return resultCount;
    }//End getResults
    /*searches in context*/
    public List<Entity> search(String queryString) {
        Query query = null;
        try {
            QueryParser parser = new QueryParser(Version.LUCENE_43, "context", KBIndexer.getAnalyzer());
            query = parser.parse(queryString);
        } catch (Exception ex) {
            Logger.log(Logger.level.SEVERE, ex.getClass().getName());
        }
        return search(query);
    }
    
    private Map<Entity, Double> resultScoreMap;
    private Map<Entity, String> entityExplanationMap;

    public List<Entity> search(String queryEntity, Map<String, Double> queryWeightMap) {

        resultScoreMap = new HashMap<Entity, Double>();
        entityExplanationMap = new HashMap<Entity, String>();

        BooleanQuery query = new BooleanQuery();
        Query nameQuery = getNameQuery(queryEntity, queryWeightMap);
        Query classQuery = getClassQuery(queryWeightMap);
        Query contextQuery = getContextQuery(queryWeightMap);
        Query propQuery = getPropertyQuery(queryWeightMap);

        query.add(nameQuery, BooleanClause.Occur.SHOULD);
        query.add(classQuery, BooleanClause.Occur.SHOULD);
        query.add(contextQuery, BooleanClause.Occur.SHOULD);
        query.add(propQuery, BooleanClause.Occur.SHOULD);

        return search(query);
    }

    public Entity searchById(String entityId){
        TermQuery query = new TermQuery(new Term("id", entityId));
        List<Entity> results = search(query);
        Entity resultEntity = (results!=null && results.size()>0)? results.get(0) : null;
        return resultEntity;
    }//searchById
    
    public List<Entity> searchByName(String queryName) {
    	TermQuery query = new TermQuery(new Term("name", queryName));
        List<Entity> results = search(query);
        //Entity resultEntity = (results!=null && results.size()>0)? results.get(0) : null;
        return results;
    }//searchByName
    
    public List<Entity> searchBySource(String queryName) {
    	TermQuery query = new TermQuery(new Term("source", queryName));
        List<Entity> results = search(query);
        //Entity resultEntity = (results!=null && results.size()>0)? results.get(0) : null;
        return results;
    }//searchBySource
    /*Type could be PER/GPE/ORG/UKN*/
    public List<Entity> searchByType(String queryName) {
    	TermQuery query = new TermQuery(new Term("type", queryName));
        List<Entity> results = search(query);
        //Entity resultEntity = (results!=null && results.size()>0)? results.get(0) : null;
        return results;
    }//searchByType
    /*Search by infobox class/type */
    public List<Entity> searchByClass(String queryName) {
    	TermQuery query = new TermQuery(new Term("class", queryName));
        List<Entity> results = search(query);
        //Entity resultEntity = (results!=null && results.size()>0)? results.get(0) : null;
        return results;
    }//searchByClass
    /*Search by infobox values/properties*/
    public List<Entity> searchByProperties(String queryName) {
    	TermQuery query = new TermQuery(new Term("properties", queryName));
        List<Entity> results = search(query);
        //Entity resultEntity = (results!=null && results.size()>0)? results.get(0) : null;
        return results;
    }//searchByProperties
    
    public List<Entity> search(Query query) {
        if (query == null) {
            return null;
        }
        resultScoreMap = new HashMap<Entity, Double>();
        entityExplanationMap = new HashMap<Entity, String>();

        //Logger.log("        entity query is ", query.toString());
        List<Entity> resultEntities = new ArrayList<Entity>();
        try {
            TopDocs resultDocs = searcher.search(query, resultsCount);

            for (ScoreDoc scoreDoc : resultDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                Entity entity = new Entity();
                entity.setId(doc.get("id"));
                entity.setType(doc.get("type"));
                entity.setName(doc.get("name"));
                entity.setSource(doc.get("source"));
                entity.setContextText(doc.get("context") + doc.get("class") + doc.get("properties"));
                resultEntities.add(entity);
                resultScoreMap.put(entity, (double) scoreDoc.score);
                entityExplanationMap.put(entity, searcher.explain(query, scoreDoc.doc).toString());
            }
        } catch (IOException ex) {
            Logger.log("    There was a problem while searching the lucene ! ", ex.getClass().getName());
        }
        return resultEntities;
    }

    private Query getNameQuery(String queryEntity, Map<String, Double> queryWeightMap) {
        Query nameQuery = new TermQuery(new Term("name", queryEntity));
        float boost = Float.parseFloat(queryWeightMap.get(queryEntity).toString());
        nameQuery.setBoost(boost);
        return nameQuery;
    }

    private Query getClassQuery(Map<String, Double> queryWeightMap) {
        return getWeightedQuery(queryWeightMap, "class");
    }

    private Query getContextQuery(Map<String, Double> queryWeightMap) {
        return getWeightedQuery(queryWeightMap, "context");
    }

    private Query getPropertyQuery(Map<String, Double> queryWeightMap) {
        return getWeightedQuery(queryWeightMap, "properties");
    }

    private Query getWeightedQuery(Map<String, Double> queryWeightMap, String fieldName) {
        BooleanQuery query = new BooleanQuery();
        if (queryWeightMap == null) {
            return query;
        }

        for (String phrase : queryWeightMap.keySet()) {
            float boost = Float.parseFloat(queryWeightMap.get(phrase).toString());
            PhraseQuery phraseQuery = new PhraseQuery();
            phraseQuery.setBoost(boost);
            phraseQuery.setSlop(3);         // need to investigate on this value
            for (String word : phrase.split(" ")) {
//                TermQuery wordQuery = new TermQuery(new Term(fieldName, word));
//                query.setBoost(boost);
                phraseQuery.add(new Term(fieldName, word));
            }
            query.add(phraseQuery, BooleanClause.Occur.SHOULD);
        }
        return query;
    }

    public Map<Entity, Double> getResultScoreMap() {
        return resultScoreMap;
    }

    public Map<Entity, String> getEntityExplanationMap() {
        return entityExplanationMap;
    }

    public void setResultsCount(int resultsCount) {
        this.resultsCount = resultsCount;
    }
}


