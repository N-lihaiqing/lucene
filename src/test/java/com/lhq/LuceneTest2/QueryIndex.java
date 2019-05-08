package com.lhq.LuceneTest2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class QueryIndex {
	
	public static void queryIndex() {
		try {
		    //1 创建Directory对象,索引存放位置
		    File indexrepository_file = new File("D:\\others\\lucene\\index");
		    Path path = indexrepository_file.toPath();
		    Directory directory = FSDirectory.open(path);
		    //2 创建IndexReader
		    IndexReader indexReader = DirectoryReader.open(directory);
		    //3 创建IndexSearch对象
		    IndexSearcher indexSearch=new IndexSearcher(indexReader);
		    //4 创建TermQuery对象,指定查询的域和关键字
		    Query query=new TermQuery(new Term("fileContent","姚振"));
		    //5 查询
		    TopDocs topDocs = indexSearch.search(query, 5);//前5个
		    //6 遍历结果
		    ScoreDoc[] scoreDocs = topDocs.scoreDocs;//文档id数组
		    for (ScoreDoc scoreDoc : scoreDocs) {
		        //根据id获取文档
		        Document doc = indexSearch.doc(scoreDoc.doc);
		        //获取结果,没有存储的是null,比如内容
		        System.out.println("文档名: "+doc.get("fileName"));
		        System.out.println("文档路径: "+doc.get("filePath"));
		        System.out.println("文档大小: "+doc.get("fileSize"));
		        System.out.println("文档内容: "+doc.get("fileContent"));
		        System.out.println("-------------------");
		    }
		    //7 关闭reader
		    indexReader.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	}

}
