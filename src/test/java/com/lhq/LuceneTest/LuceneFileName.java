package com.lhq.LuceneTest;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * 对文件名进行检索
 * 
 * @author lhq
 *
 */

public class LuceneFileName {

	public static void main(String[] args) throws IOException, ParseException, InvalidTokenOffsetsException {

		String keys = "new1";
		searchFileName(keys);
	}

	public static void searchFileName(String keys) throws IOException, ParseException, InvalidTokenOffsetsException {
		// 1 创建一个Directory对象，也就是存放索引的位置
		Directory directory = FSDirectory.open(new File("E:\\lucene\\Index").toPath());
		// 2 创建一个indexReader对象，需要指定Directory对象
		IndexReader indexReader = DirectoryReader.open(directory);
		// 3 创建一个indexsearcher对象，需要指定IndexReader对象
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		// 4 创建一个TermQuery对象，指定查询的域和关键字。
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser queryParser = new QueryParser("fileName", analyzer);
		
		// Lucene也提供了通配符的查询，这就是WildcardQuery。
		// 通配符“?”代表1个字符，而“*”则代表0至多个字符。
		Query query = new WildcardQuery(new Term("fileName", "new?")); // 名字以词语结尾
		Query query1 = new WildcardQuery(new Term("fileName", "辛*")); // 名字以新开头
		Query query2 = new WildcardQuery(new Term("fileName", "辛XXXXX")); // 名字以新开头
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
//		builder.add(query, Occur.SHOULD);
		builder.add(query1, Occur.SHOULD);
		builder.add(query2, Occur.SHOULD);
		BooleanQuery booleanQuery = builder.build();
		
		
		TopDocs topDocs = indexSearcher.search(booleanQuery, 10);
		
//		Query query = queryParser.parse(keys);
		// 5 执行查询
//		TopDocs topDocs = indexSearcher.search(query, 10);
		// 6 返回查询结果，遍历查询结果并输出
		ScoreDoc[] scoreDocs = topDocs.scoreDocs;
		for (ScoreDoc scoreDoc : scoreDocs) {
			int doc = scoreDoc.doc;
			Document document = indexSearcher.doc(doc);
			String fileName = document.get("fileName");
			System.out.println("fileName:   " + fileName);
			String fileContent = document.get("fileContent");
			System.out.println("fileContent:   " + fileContent);
			String fileSize = document.get("fileSize");
			System.out.println("fileSize:   " + fileSize);
			String filePath = document.get("filePath");
			System.out.println("filePath:   " + filePath);
			System.out.println("--------------------------");
		}
		
		QueryScorer scorer = new QueryScorer(query);
		Fragmenter fragmenter = new SimpleSpanFragmenter(scorer);
		SimpleHTMLFormatter simpleHTMLFormatter = new SimpleHTMLFormatter("<b><font color='#ff00d2'>", "</font></b>");
		Highlighter highlighter = new Highlighter(simpleHTMLFormatter, scorer);
		highlighter.setTextFragmenter(fragmenter);
		
		for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
			Document doc = indexSearcher.doc(scoreDoc.doc);
			String desc = doc.get("fileName");
			if (desc != null) {
				TokenStream tokenStream = analyzer.tokenStream("fileName", new StringReader(desc));
				/**
				 * getBestFragment方法用于输出摘要（即权重大的内容）
				 */
				System.out.println(highlighter.getBestFragment(tokenStream, desc));
			}
		}
		
		// 7 关闭IndexReader对象
		indexReader.close();
	}
}
