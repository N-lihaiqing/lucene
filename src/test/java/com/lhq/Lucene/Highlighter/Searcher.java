package com.lhq.Lucene.Highlighter;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Searcher {
    
    private IndexSearcher searcher;
    private IndexReader reader;
    
    public Searcher(String indexDir) throws IOException {
        Directory dir=FSDirectory.open(Paths.get(indexDir));
        this.reader=DirectoryReader.open(dir);
        this.searcher=new IndexSearcher(this.reader);
    }
    
    public void search(String key) throws ParseException, IOException, InvalidTokenOffsetsException {
    
        SmartChineseAnalyzer analyzer=new SmartChineseAnalyzer();
        QueryParser parser=new QueryParser("desc",analyzer);
        Query query=parser.parse(key);
        
        long start=System.currentTimeMillis();
        TopDocs hits=searcher.search(query, 10);
        long end=System.currentTimeMillis();
        System.out.println("匹配 "+key+" ，总共花费"+(end-start)+"毫秒"+"查询到"+hits.totalHits+"个记录");
        
        QueryScorer scorer=new QueryScorer(query);
        Fragmenter fragmenter=new SimpleSpanFragmenter(scorer);
        SimpleHTMLFormatter simpleHTMLFormatter=new SimpleHTMLFormatter("<b><font color='#004bf9'>","</font></b>");
        Highlighter highlighter=new Highlighter(simpleHTMLFormatter, scorer);
        highlighter.setTextFragmenter(fragmenter);
        for(ScoreDoc scoreDoc:hits.scoreDocs){
            Document doc=searcher.doc(scoreDoc.doc);
            System.out.println(doc.get("city"));
            String desc=doc.get("desc");
            if(desc!=null){
                TokenStream tokenStream=analyzer.tokenStream("desc", new StringReader(desc));
                /**
                 * getBestFragment方法用于输出摘要（即权重大的内容）
                 */
                System.out.println(highlighter.getBestFragment(tokenStream, desc));
            }
        }
        reader.close();
    }
    
    
    public static void main(String[] args) throws InvalidTokenOffsetsException {
        try {
            new Searcher("E:\\lucene\\Index").search("城市");
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
