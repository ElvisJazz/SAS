import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 12-7-19
 * Time: ����11:00
 * To change this template use File | Settings | File Templates.
 */
public class CorpusPreHandler {
     // Ԥ��������ԭʼ���ϣ����ɴ��۴�����
    public void handleAllOriginalCorpus(String readDir, String outputDir, boolean isAlignFile, boolean isEvaluation){
        File[] fileArray = (new File(readDir)).listFiles();
        for(int i=0; i<fileArray.length; ++i){
            readXmlFile(fileArray[i].getAbsolutePath(), fileArray[i].getName(), outputDir, isAlignFile, isEvaluation);
        }
    }

    // ��ȡ������xml�ļ�
    public void readXmlFile(String readFilePath, String fileName, String outputDir, boolean isAlignFile, boolean isEvaluation) {
        // д�ļ�����
        Writer labelWriter = null;
        Writer sentenceWriter = null;
        try{
            // ���ö�ȡ����
            Element weiboElement, sentenceElement;
            String weiboId=null, sentenceId=null, opinionated=null, sentence=null, topic=null, hashtag=null;
            Iterator sentenceIterator, hashtagIterator;
            int startTagIndex=0, endTagIndex=0, tmpIndex=0;


            // ��ȡ�ļ�
            SAXReader reader = new SAXReader();
            Document document = reader.read(new File(readFilePath));

            // ��ȡ���ڵ�
            Element root = document.getRootElement();
            topic = root.attributeValue("title");
            System.out.println("��ǰ�����ĵ���" + fileName);
            if(topic == null){
                int index = fileName.indexOf('.');
                if(index != -1)
                    topic = fileName.substring(0, index);
                else
                    topic = fileName;
            }

            // �ļ��������
            File file0 = new File(outputDir);

            if(!file0.exists()) {
                if(!file0.mkdirs())
                    throw new Exception("�����ļ�Ŀ¼ʧ�ܣ�");
            }
            String fileName0 = file0.getPath()+"_label//";
            File file = new File(fileName0);

            if(!file.exists() && !isEvaluation) {
                if(!file.mkdirs())
                    throw new Exception("�����ļ�Ŀ¼ʧ�ܣ�");
            }
            if(!isEvaluation)
                labelWriter = new OutputStreamWriter(new FileOutputStream(file.getPath()+"//"+topic+"_label", false), "GBK");
            sentenceWriter = new OutputStreamWriter(new FileOutputStream(outputDir+"//"+topic+"_sentence", false), "GBK");
            // ��ȡhead�ڵ�
            Iterator weiboIterator= root.elementIterator("weibo");
            while(weiboIterator.hasNext()){
                weiboElement = (Element) weiboIterator.next();
                weiboId = weiboElement.attributeValue("id");
                sentenceIterator = weiboElement.elementIterator("sentence");
                hashtagIterator = weiboElement.elementIterator("hashtag");
                if(hashtagIterator.hasNext())
                    hashtag = ((Element) hashtagIterator.next()).getText();
                else
                    hashtag = "";

                while(sentenceIterator.hasNext()){
                    sentenceElement = (Element) sentenceIterator.next();
                    opinionated = sentenceElement.attributeValue("opinionated");
                    if(isAlignFile || "Y".equals(opinionated)){
                        sentenceId = sentenceElement.attributeValue("id");
                        sentence = sentenceElement.getText();

                        //����Ǵ����������϶��룬���ҵ�������д���ʼλ�úͼ���
                        if(isEvaluation){
                            int i = 1;
                            while(sentenceElement.attributeValue("target_begin_"+i) != null){
                                sentenceWriter.write("["+sentenceElement.attributeValue("target_begin_"+i)+","+
                                        sentenceElement.attributeValue("target_end_"+i)+","+sentenceElement.attributeValue("target_polarity_"+i)+"]");
                                ++i;
                            }
                        }else{
                            // ��������е�hashtag
                            startTagIndex = sentence.indexOf('#');
                            while(startTagIndex != -1) {
                                endTagIndex = sentence.indexOf('#', startTagIndex+1);
                                if(endTagIndex != -1){
                                    Pattern p = Pattern.compile("#");
                                    Matcher m = p.matcher(sentence);
                                    sentence = m.replaceFirst("��");
                                    m = p.matcher(sentence);
                                    sentence = m.replaceFirst("��");
                                } else{
                                    break;
                                }
                                startTagIndex = sentence.indexOf('#');
                            }
                            sentence = sentence.replaceAll("#", "");
                            // ��������е�(@)
                            sentence = sentence.replaceAll("[\\(��][^\n]*@[^\n]*[\\)��]", "");
                            sentence = sentence.replaceAll("��[^\n]*@[^\n]*��", "");

                            // ��������е�http�������м�ĳɷ�
                            sentence = sentence.replaceAll("[^\\s]*:http.*http[^\\s]*","");
                            sentence = sentence.replaceAll("http.*http[^\\s]*","");
                            sentence = sentence.replaceAll("[^\\s]*:http[^\\s]*","");
                            sentence = sentence.replaceAll("http[^\\s]*","");
                        }

                        // ������ļ�
                        if(!isEvaluation){
                            if(isAlignFile){
                                sentenceWriter.write(sentence);
                            }
                            else {
                                sentenceWriter.write(sentence+"\n");
                                //labelWriter.write(weiboId+" "+sentenceId+" "+hashtag+"\n");
                                labelWriter.write(weiboId+"\n");
                            }
                        }else{
                            sentenceWriter.write("\n");
                        }
                    }
                }
                if(!isEvaluation && isAlignFile){
                    labelWriter.write(weiboId+"\n");
                    sentenceWriter.write("\n");
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        finally {
            try {
                if(labelWriter != null)
                    labelWriter.close();
                if(sentenceWriter != null)
                    sentenceWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
