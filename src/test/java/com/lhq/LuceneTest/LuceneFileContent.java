package com.lhq.LuceneTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
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

/**
 * @desection 对文件内容进行检索 结论：
 *            因此总结来说，在使用Lucene时，我们只遵循一个原则，只使用最新的版本，如果变更版本，需要重建索引。在老的版本你无法使用新的版本号，在新的版本中，
 *            你不要使用老的版本号。Lucene版本号主要作用是Lucene自己对索引的某些特性做的一些向下兼容，如果你想使用一个较老的版本，某些特性还是可以使用的，只是新的版本不再提供这些特性。
 * 
 * @author lhq
 *
 */
public class LuceneFileContent {

	public static void main(String[] args) {
		String sourcePath = "E:\\lucene\\document";
		String indexPath = "E:\\lucene\\Index";
		String key_words = "阿斯蒂芬";

		try {
			LuceneFileContent.textFileIndexer(sourcePath, indexPath);
			LuceneFileContent.queryKeyWords(indexPath, key_words);
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (InvalidTokenOffsetsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param sourceFile 需要添加到索引中的路径
	 * @param indexFile  存放索引路径
	 * @throws IOException
	 */
	public static void textFileIndexer(String sourceFile, String indexFile) throws IOException {

		long startTime = new Date().getTime();

		File sourceDir = new File(sourceFile);
		File indexDir = new File(indexFile);
		// 创建directory对象，也就是索引存放的位置
		Directory directory = FSDirectory.open(indexDir.toPath());
		Analyzer analyzer = new StandardAnalyzer();
//		Analyzer analyzer = new IKAnalyzer();// 使用IK中文分词器  IK分词器所依赖的lucene相关组件的版本为4.7.2
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setOpenMode(OpenMode.CREATE);
		IndexWriter indexWriter = new IndexWriter(directory, config);

		File[] textFiles = sourceDir.listFiles();
		for (int i = 0; i < textFiles.length; i++) {
//			if (textFiles[i].isFile() && textFiles[i].getName().endsWith(".txt")) {
			System.out.println("File--->" + textFiles[i].getCanonicalPath() + " 正在被索引.....");
			String str_temp = fileReaderAll(textFiles[i].getCanonicalPath(), "UTF-8");
			System.out.println("文件内容：" + str_temp);

			FieldType fieldType = new FieldType();
//				fieldType.setIndexed(false);//set 是否索引
			fieldType.setStored(true);// set 是否存储
			fieldType.setTokenized(true);// set 是否分类

			Document document = new Document();
			document.add(new Field("path", textFiles[i].getCanonicalPath(), fieldType));
			document.add(new TextField("fileContent", str_temp, Store.YES));

			indexWriter.addDocument(document);
//			}
		}

		indexWriter.close();

		long endTime = new Date().getTime();
		System.out.println("一共花费了" + (endTime - startTime) + "毫秒将" + sourceDir.getPath() + "中的文件增加到索引里面去.....");
	}

	private static String fileReaderAll(String filename, String charset) throws IOException {
		BufferedReader buffer_read = new BufferedReader(new InputStreamReader(new FileInputStream(filename), charset));
		String line = new String();
		String temp = new String();

		while ((line = buffer_read.readLine()) != null) {
			temp += line;
		}

		buffer_read.close();
		return temp;
	}

	/**
	 * @param indexFile 索引所在的路径
	 * @param keyWords  需要检索的关键字
	 * @throws IOException
	 * @throws ParseException
	 * @throws InvalidTokenOffsetsException
	 */
	private static void queryKeyWords(String indexFile, String keyWords)
			throws IOException, ParseException, InvalidTokenOffsetsException {
		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexFile).toPath()));
		IndexSearcher indexSearch = new IndexSearcher(reader);
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser queryParser = new QueryParser("fileContent", analyzer);
		Query query = queryParser.parse(keyWords);

		QueryScorer scorer = new QueryScorer(query);
		Fragmenter fragmenter = new SimpleSpanFragmenter(scorer);
		SimpleHTMLFormatter simpleHTMLFormatter = new SimpleHTMLFormatter("<b><font color='#ff00d2'>", "</font></b>");
		Highlighter highlighter = new Highlighter(simpleHTMLFormatter, scorer);
		highlighter.setTextFragmenter(fragmenter);

		TopDocs hits = indexSearch.search(query, 10);

		if (indexSearch != null) {
			TopDocs docs = indexSearch.search(query, 10); // 返回最多为10条记录
			ScoreDoc[] scoreDocs = docs.scoreDocs;
			if (scoreDocs.length > 0) {
				System.out.println("关键字：" + keyWords + "，在  " + indexFile + "中，一共检索到" + scoreDocs.length + "个...");
			}
		}

		for (ScoreDoc scoreDoc : hits.scoreDocs) {
			Document doc = indexSearch.doc(scoreDoc.doc);
			String desc = doc.get("fileContent");
			if (desc != null) {
				TokenStream tokenStream = analyzer.tokenStream("fileContent", new StringReader(desc));
				/**
				 * getBestFragment方法用于输出摘要（即权重大的内容）
				 */
				System.out.println(highlighter.getBestFragment(tokenStream, desc));
			}
		}

		indexSearch.getIndexReader().close();
	}
}
