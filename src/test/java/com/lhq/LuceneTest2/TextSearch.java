package com.lhq.LuceneTest2;

import java.io.File;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.junit.Test;

/**
 * 查询主要有两种,一种是Query的子类查询,一种是语法解析查询,这里先说子类查询:
 * 可以先写一个获取search的公共方法,以及遍历打印搜索结果的方法,之后就可以对各种查询进行方便测试了:
 * 
 * @author lhq
 *
 */

public class TextSearch {
	
	String indexPath = "E:/lucene/Index";

	// 获取IndexWriter
	public IndexWriter getIndexWriter() throws Exception {
		File indexrepository_file = new File(indexPath);
		Path path = indexrepository_file.toPath();
		Directory directory = null;
		directory = FSDirectory.open(path);
		Analyzer analyzer = new StandardAnalyzer();// 使用IK
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		return new IndexWriter(directory, config);
	}

	// 获取IndexSearch
	public IndexSearcher getIndexSearcher() throws Exception {
		return new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(indexPath).toPath())));
	}

	// 执行查询并打印结果
	public void printResult(IndexSearcher indexSearcher, Query query, Integer num) throws Exception {
		// 使用排序
		Sort sort = new Sort();
		SortField f = new SortField("fileSize", Type.LONG, true); // 按照fileSize字段排序，true表示降序
		sort.setSort(f);
		// 多个条件排序
		// Sort sort = new Sort();
		// SortField f1 = new SortField("createdate", SortField.DOC, true);
		// SortField f2 = new SortField("bookname", SortFiedl.INT, false);
		// sort.setSort(new SortField[] { f1, f2 });
		// 高亮显示start
		// 算分
		QueryScorer scorer = new QueryScorer(query);
		// 显示得分高的片段
		Fragmenter fragmenter = new SimpleSpanFragmenter(scorer);
		// 设置标签内部关键字的颜色
		// 第一个参数：标签的前半部分；第二个参数：标签的后半部分。
		SimpleHTMLFormatter simpleHTMLFormatter = new SimpleHTMLFormatter("<b><font color='red'>", "</font></b>");
		// 第一个参数是对查到的结果进行实例化；第二个是片段得分（显示得分高的片段，即摘要）
		Highlighter highlighter = new Highlighter(simpleHTMLFormatter, scorer);
		// 设置片段
		highlighter.setTextFragmenter(fragmenter);
		// 高亮显示end
		TopDocs topDocs = indexSearcher.search(query, num, sort);
		ScoreDoc[] scoreDocs = topDocs.scoreDocs;// 文档id数组
		for (ScoreDoc scoreDoc : scoreDocs) {
			// 根据id获取文档
			Document doc = indexSearcher.doc(scoreDoc.doc);
			String name = doc.get("fileName");
			if (name != null) {
				// 把全部得分高的摘要给显示出来
				// 第一个参数是对哪个参数进行设置；第二个是以流的方式读入
				TokenStream tokenStream = new StandardAnalyzer().tokenStream("fileName", new StringReader(name));
				// 获取最高的片段
				System.out.println("高亮文档名: " + highlighter.getBestFragment(tokenStream, name));
			}
			// 获取结果,没有存储的是null,比如内容
			System.out.println("文档名: " + doc.get("fileName"));
			System.out.println("文档路径: " + doc.get("filePath"));
			System.out.println("文档大小: " + doc.get("fileSize"));
			System.out.println("文档内容: " + doc.get("fileContent"));
			System.out.println("-------------------");
		}
	}

	/**---------------------------------- begin Query子查询 --------------------------------------*/
	
	// 查询
	@Test
	public void testSelect01() throws Exception {
		IndexSearcher indexSearcher = getIndexSearcher();
		// 数字范围查询, 两边都是闭区间 43< index <103
		// 新版本中数值都使用Point进行查询,原理的Numic被废弃
		// Query query=LongPoint.newRangeQuery("fileSize", 43L, 103L);
		// 数值精确匹配,只会查找参数里的数值索引 index in param
		List<Long> list = new ArrayList<Long>();
		list.add(43L);
		list.add(103L);
		Query query2 = LongPoint.newSetQuery("fileSize", 43L, 100L);// 不定参数
		Query query1 = LongPoint.newSetQuery("fileSize", list); // 集合参数
		printResult(indexSearcher, query1, 10);
		// 关闭reader
		indexSearcher.getIndexReader().close();
	}

	/**
	 * TermRangeQuery是用于字符串范围查询的，既然涉及到范围必然需要字符串比较大小， 字符串比较大小其实比较的是ASC码值，即ASC码范围查询。
	 * 一般对于英文来说，进行ASC码范围查询还有那么一点意义， 中文汉字进行ASC码值比较没什么太大意义，所以这个TermRangeQuery了解一下就行
	 */
	@Test
	public void testSelect02() throws Exception {
		String lowerTermString = "即可";// 范围的下端的文字,后面boolean为真,对应值为闭区间
		String upperTermString = "博文";// 范围的上限内的文本,后面boolean为真,对应值为闭区间
		IndexSearcher indexSearcher = getIndexSearcher();
		// lucene 使用 BytesRef 在索引中表示utf-8编码的字符,此类含有偏移量_长度以及byte数组,可使用utf8toString
		// API转换字符串
		Query query = new TermRangeQuery("fileName", new BytesRef(lowerTermString), new BytesRef(upperTermString), true, true);
		printResult(indexSearcher, query, 10);
		// 关闭reader
		indexSearcher.getIndexReader().close();
	}

	/**
	 * BooleanQuery也是实际开发过程中经常使用的一种Query。
	 * 它其实是一个组合的Query，在使用时可以把各种Query对象添加进去并标明它们之间的逻辑关系。
	 * 所有的Query都可以通过booleanQUery组合起来 BooleanQuery本身来讲是一个布尔子句的容器，它提供了专门的API方法往其中添加子句，
	 * 并标明它们之间的关系
	 */
	@Test
	public void testSelect03() throws Exception {
		IndexSearcher indexSearcher = getIndexSearcher();
		// 组合条件
		Query query1 = new TermQuery(new Term("fileName", "歌曲"));
		Query query2 = new TermQuery(new Term("fileContent", "美国"));
		// 相当于一个包装类，将 Query 设置 Boost 值 ，然后包装起来。
		// 再通过复合查询语句，可以突出 Query 的优先级
		BoostQuery query = new BoostQuery(query2, 2f);
		// 创建BooleanQuery.Builder
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		// 添加逻辑
		/**
		 * 1．MUST和MUST：取得两个查询子句的交集。 and
		 * 2．MUST和MUST_NOT：表示查询结果中不能包含MUST_NOT所对应得查询子句的检索结果。
		 * 3．SHOULD与MUST_NOT：连用时，功能同MUST和MUST_NOT。
		 * 4．SHOULD与MUST连用时，结果为MUST子句的检索结果,但是SHOULD可影响排序。
		 * 5．SHOULD与SHOULD：表示“或”关系，最终检索结果为所有检索子句的并集。 6．MUST_NOT和MUST_NOT：无意义，检索无结果。
		 */
		builder.add(query1, Occur.SHOULD);// 文件名不包含词语,但是内容必须包含姚振
		builder.add(query, Occur.SHOULD);
		// build query
		BooleanQuery booleanQuery = builder.build();
		printResult(indexSearcher, booleanQuery, 10);
		// 关闭reader
		indexSearcher.getIndexReader().close();
	}

	@Test
	public void testSelect() throws Exception {
		IndexSearcher indexSearcher = getIndexSearcher();
		// 查询所有
		Query queryAll = new MatchAllDocsQuery();
		printResult(indexSearcher, queryAll, 10);
		// 关闭reader
		indexSearcher.getIndexReader().close();
	}

	@Test
	public void test05() throws Exception {
		IndexSearcher indexSearcher = getIndexSearcher();
		// 查询文件名以新开头的索引 前缀匹配查询
		Query query = new PrefixQuery(new Term("fileName", "new"));
		System.out.println(query);
		printResult(indexSearcher, query, 10);
		// 关闭reader
		indexSearcher.getIndexReader().close();
	}

	/**
	 * PhraseQuery，是指通过短语来检索，比如我想查“姚振 牛逼”这个短语， 那么如果待匹配的document的指定项里包含了"姚振 牛逼"这个短语，
	 * 这个document就算匹配成功。可如果待匹配的句子里包含的是“姚振 真他妈 牛逼”， 那么就无法匹配成功了，如果也想让这个匹配，就需要设定slop，
	 * 先给出slop的概念：slop是指两个项的位置之间允许的最大间隔距离
	 */
	@Test
	public void test06() throws Exception {
		IndexSearcher indexSearcher = getIndexSearcher();
		org.apache.lucene.search.PhraseQuery.Builder build = new PhraseQuery.Builder();
		build.add(new Term("fileContent", "静安"));
		build.add(new Term("fileContent", "理解"));
		// 设置slop,即最大相隔多远,即多少个文字的距离,
		build.setSlop(6);// 表示如果这两个词语相隔6个字以下的位置就匹配
		PhraseQuery phraseQuery = build.build();
		printResult(indexSearcher, phraseQuery, 10);
		// 关闭reader
		indexSearcher.getIndexReader().close();
	}

	@Test
	public void test07() throws Exception {
		IndexSearcher indexSearcher = getIndexSearcher();
		// FuzzyQuery是一种模糊查询，它可以简单地识别两个相近的词语
		Query query = new FuzzyQuery(new Term("fileContent", "牛逼"));
		printResult(indexSearcher, query, 10);
		// 关闭reader
		indexSearcher.getIndexReader().close();
	}

	@Test
	public void test09() throws Exception {
		IndexSearcher indexSearcher = getIndexSearcher();
		// Lucene也提供了通配符的查询，这就是WildcardQuery。
		// 通配符“?”代表1个字符，而“*”则代表0至多个字符。
		Query query = new WildcardQuery(new Term("fileName", "?new")); // 名字以词语结尾
		Query query1 = new WildcardQuery(new Term("fileName", "辛*")); // 名字以新开头
		Query query2 = new WildcardQuery(new Term("fileName", "辛XXXXX")); // 名字以新开头
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		builder.add(query2, Occur.MUST);
		BooleanQuery booleanQuery = builder.build();
		printResult(indexSearcher, booleanQuery, 10);
		// 关闭reader
		indexSearcher.getIndexReader().close();
	}
	
	/**---------------------------------- end Query子查询 --------------------------------------*/

	/** ------------------------- begin QueryParser条件解析查询 ---------------------- */
	@Test
	public void test08() throws Exception {
		/**
		 * 解析查询表达式 QueryParser实际上就是一个解析用户输入的工具，可以通过扫描用户输入的字符串，生成Query对象
		 */
		IndexSearcher indexSearcher = getIndexSearcher();
		Query query = new TermQuery(new Term("fileName", "词语"));
		// 参数: 默认域 分词解析器
		QueryParser queryParser = new QueryParser("fileContent", new StandardAnalyzer());
		// 解析 ,如果不指定域,使用默认域 使用语法书写
		Query parse = queryParser.parse("侯征 姚振 何毅");
		Query parse1 = queryParser.parse("fileName:侯征 姚振 何毅");// 指定域
		Query parse2 = queryParser.parse("fileName:侯*");// 匹配
		Query parse3 = queryParser.parse("+fileName:游戏 fileName:新词语");// 匹配
		
		printResult(indexSearcher, parse3, 10);
		// 关闭reader
		indexSearcher.getIndexReader().close();
	}

	@Test
	public void test10() throws Exception {
		/**
		 * 解析查询表达式 MultiFieldQueryParser支持多默认域
		 */
		IndexSearcher indexSearcher = getIndexSearcher();
		// 指定多默认域数组
		String[] arr = new String[] { "fileName", "fileContent" };
		// 搜索时设置权重
		Map<String, Float> boosts = new HashMap<String, Float>();
		boosts.put("fileContent", 10.0f);// 权重默认是1, 文件名字符合条件的排序在前面
		MultiFieldQueryParser queryParser = new MultiFieldQueryParser(arr, new StandardAnalyzer(), boosts);// 指定搜索权重
		// 解析 ,如果不指定域,使用默认域 使用语法书写
		Query parse = queryParser.parse("游戏");// 查询所有默认域里有姚振的文档
		printResult(indexSearcher, parse, 10);
		// 关闭reader
		indexSearcher.getIndexReader().close();
	}

	/** ------------------------- end QueryParser条件解析查询 ---------------------- */

	/** --------------------------------- begin 索引维护 --------------------------------*/
	//按条件删除
	@Test
	public void testDelete() throws Exception{
	    IndexWriter indexWriter = getIndexWriter();
	    Query query=new TermQuery(new Term("fileContent","老阴逼"));
	    indexWriter.deleteDocuments(query);//可传入多个参数
	    indexWriter.close();
	}
	//全删除
	@Test
	public void testDeleteAll() throws Exception{
	    IndexWriter indexWriter = getIndexWriter();
	    indexWriter.deleteAll();
	    //强制删除,不会恢复
	    //indexWriter.forceMergeDeletes();
	    indexWriter.close();
	}
	//更新
	@Test
	public void testUpdate() throws Exception{
	    IndexWriter indexWriter = getIndexWriter();
	    Document doc = new Document();
	    doc.add(new TextField("","",Store.YES));
	    // .... 修改的内容
	    indexWriter.updateDocument(new Term("fileName","词语"), doc);
	    indexWriter.close();
	}
	 //恢复,从回收站恢复
	@Test
	public void testDELETE() throws Exception{
	    IndexWriter indexWriter = getIndexWriter();
	    indexWriter.rollback();
	}
	
	/** --------------------------------- end 索引维护 --------------------------------*/
	
}

