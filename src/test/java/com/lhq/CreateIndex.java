package com.lhq;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class CreateIndex {
	
	public static void main(String[] args) {
		try {
			createIndex();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void createIndex() throws IOException {

		long startTime = new Date().getTime();

		File sourceDir = new File("E:/lucene/document");
		File indexDir = new File("E:/lucene/Index");
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

//				FieldType fieldType = new FieldType();
//				fieldType.setIndexed(false);//set 是否索引
//		        fieldType.setStored(true);//set 是否存储
//		        fieldType.setTokenized(true);//set 是否分类
				
				Document document = new Document();
//				document.add(new Field("path", textFiles[i].getCanonicalPath(), fieldType));
//				document.add(new Field("body", str_temp, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS));

				
				/**
				 * 新版本中使用了Int/Long/DoublePoint来表示数值型字段,但是默认不存储,不排序,也不支持加权
				 * 创建索引加权值在6.6版本后就已经废除了,并给了搜索时设置的新query,这个后面查询时再说
				 * 如果存储需要用StoredField写相同的字段,排序还要再使用NumericDocValuesField写相同的排序,
				 * 如下的fileSize,添加long值索引,存储并添加排序支持
				 */

				// 文件名称
				String file_name = textFiles[i].getName();
				Field fileNameField = new TextField("fileName", file_name, Store.YES);
				// 文件大小
				// 大小,数字类型使用point添加到索引中,同时如果需要存储,由于没有Stroe,所以需要再创建一个StoredField进行存储
				// 即 IntPoint,DoublePoint等
				Long file_size = FileUtils.sizeOf(textFiles[i]);
				Field fileSizeField = new LongPoint("fileSize", file_size);
				Field storedField = new StoredField("fileSize", file_size);

				// 同时添加排序支持
				Field storeFile = new NumericDocValuesField("fileSize", textFiles[i].length());

				// 文件路径
				String file_path = textFiles[i].getPath();
				Field filePathField = new StoredField("filePath", file_path);

				// 文件内容
				String file_content = FileUtils.readFileToString(textFiles[i]);
				Field fileContentField = new TextField("fileContent", file_content, Store.YES);
				
				
				document.add(fileNameField);
				document.add(fileSizeField);
				document.add(storedField);
				document.add(storeFile);
				document.add(filePathField);
				document.add(fileContentField);
				
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

}
