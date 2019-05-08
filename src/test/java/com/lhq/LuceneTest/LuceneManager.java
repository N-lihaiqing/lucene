package com.lhq.LuceneTest;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.Test;
import org.wltea.analyzer.lucene.IKAnalyzer;

/**
 * 
 * @author: SHF
 * @date: 2018年3月7日 上午10:49:20
 * @Description:索引库的管理 增加 入门程序 查询 入门程序 修改 删除
 */
public class LuceneManager {
	// *************获取IndexWriter对象*************
	public IndexWriter getIndexWriter() throws IOException {
		// 2 创建一个indexWriter对象.
		// 2.1 指定索引库的存放位置 Directory对象
		// 2.2 指定一个分析器,对内容进行分析
		Directory directory = FSDirectory.open(new File("E:\\lucene\\Index").toPath());
//			Analyzer analyzer=new StandardAnalyzer();//官方推荐的分析器
		Analyzer analyzer = new IKAnalyzer();// 使用IK中文分词器
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		IndexWriter indexWriter = new IndexWriter(directory, config);
		return indexWriter;
	}

	// *************删除所有*************
	@Test
	public void testDeleteAll() throws IOException {
		IndexWriter indexWriter = getIndexWriter();
		indexWriter.deleteAll();
		indexWriter.close();
	}

	// *************根据条件删除*************
	@Test
	public void testDeleteQuery() throws IOException {
		IndexWriter indexWriter = getIndexWriter();
		Query query = new TermQuery(new Term("fileName", "mysql"));
		indexWriter.deleteDocuments(query);
		indexWriter.close();
	}

	// *************修改*************
	@Test
	public void testUpdate() throws Exception {
		IndexWriter indexWriter = getIndexWriter();
		Document doc = new Document();
		doc.add(new TextField("fileName", "fileName修改测试", Store.YES));
		doc.add(new TextField("fileContent", "fileContent修改测试", Store.YES));
//		indexWriter.updateDocument(new Term("fileName", "mysql"), doc, new IKAnalyzer());
		indexWriter.updateDocument(new Term("fileName", "mysql"), doc);
		indexWriter.close();
	}

	// *************查询所有 根据范围查询*************
	@Test
	public void testMarchAllDocsQuery() throws Exception {
		// 1 创建一个Directory对象，也就是存放索引的位置
		Directory directory = FSDirectory.open(new File("E:\\lucene\\Index").toPath());
		// 2 创建一个indexReader对象，需要指定Directory对象
		IndexReader indexReader = DirectoryReader.open(directory);
		// 3 创建一个indexsearcher对象，需要指定IndexReader对象
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);

		// 4 创建一个TermQuery对象，指定查询的域和关键字。
//			Query query=new MatchAllDocsQuery();//查询所有
//			Query query=NumericRangeQuery.newLongRange("fileSize", 0l, 100l, true, true);//根据范围查询
//			Query query=NumericRangeQuery.newDoubleRange("fileSize", 0.0, 15075.0, true, true);

		/**
		 * *************组合查询*************
		 */
		
		Query query1 = new TermQuery(new Term("fileName", "mysql"));
		Query query2 = new TermQuery(new Term("fileName", "查询"));
//		query.add(query1, Occur.MUST); //Occur.MUST：必须满足此条件，相当于and  Occur.SHOULD：应该满足，但是不满足也可以，相当于or  Occur.MUST_NOT：必须不满足。相当于not
//		query.add(query2, Occur.MUST);
		/*
		 * 7.1中,Occur.MUST等全都放到了BooleanClause中,所以,Occur.MUST等变成了BooleanClause.Occur.
		 * MUST等 所以在lucene中,组合查询的使用方法: new BooleanQuery.Builder().add(query1,BooleanClause.Occur.MUST).add(query2,BooleanClause.Occur.MUST).build();
		 */
		Query query = new BooleanQuery.Builder().add(query1, BooleanClause.Occur.MUST).add(query2, BooleanClause.Occur.MUST).build();
		System.out.println(query);
		// 5 执行查询
		TopDocs topDocs = indexSearcher.search(query, 10);
		// 6 返回查询结果，遍历查询结果并输出
		ScoreDoc[] scoreDocs = topDocs.scoreDocs;
		for (ScoreDoc scoreDoc : scoreDocs) {
			int doc = scoreDoc.doc;
			Document document = indexSearcher.doc(doc);
			String fileName = document.get("fileName");
			System.out.println(fileName);
			String fileContent = document.get("fileContent");
			System.out.println(fileContent);
			String fileSize = document.get("fileSize");
			System.out.println(fileSize);
			String filePath = document.get("filePath");
			System.out.println(filePath);
			System.out.println("--------------------------");
		}
		// 7 关闭IndexReader对象
		indexReader.close();
	}

	// *************条件解释的对象查询*************
	@Test
	public void testQueryParser() throws Exception {
		// 1 创建一个Directory对象，也就是存放索引的位置
		Directory directory = FSDirectory.open(new File("E:\\lucene\\Index").toPath());
		// 2 创建一个indexReader对象，需要指定Directory对象
		IndexReader indexReader = DirectoryReader.open(directory);
		// 3 创建一个indexsearcher对象，需要指定IndexReader对象
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);

		// 4 创建一个TermQuery对象，指定查询的域和关键字。
		/**
		 * 条件解释
		 */
		QueryParser queryParser = new QueryParser("fileName", new IKAnalyzer());
		// *:* 域：值
		Query query = queryParser.parse("+fileName:mysql +fileName:查询");
		System.out.println(query);
		// 5 执行查询
		TopDocs topDocs = indexSearcher.search(query, 10);
		// 6 返回查询结果，遍历查询结果并输出
		ScoreDoc[] scoreDocs = topDocs.scoreDocs;
		for (ScoreDoc scoreDoc : scoreDocs) {
			int doc = scoreDoc.doc;
			Document document = indexSearcher.doc(doc);
			String fileName = document.get("fileName");
			System.out.println(fileName);
			String fileContent = document.get("fileContent");
			System.out.println(fileContent);
			String fileSize = document.get("fileSize");
			System.out.println(fileSize);
			String filePath = document.get("filePath");
			System.out.println(filePath);
			System.out.println("--------------------------");
		}
		// 7 关闭IndexReader对象
		indexReader.close();
	}
}
