package cn.edu.seu;

import cn.edu.seu.utils.PunctuationUtil;
import edu.hit.ir.ltp4j.Pair;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.util.Iterator;
import java.util.Stack;
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
    // Ԥ��������ԭʼ���ϣ����ɴ���������
    public void handleAllOriginalCorpus(String readDir, String outputDir, String outputTopicDir, boolean isAlignFile, boolean isEvaluation){
        File[] fileArray = (new File(readDir)).listFiles();
        for(int i=0; i<fileArray.length; ++i){
            readXmlFile(fileArray[i].getAbsolutePath(), fileArray[i].getName(), outputDir, outputTopicDir, isAlignFile, isEvaluation);
        }
    }

    // ��ȡ������xml�ļ�
    public void readXmlFile(String readFilePath, String fileName, String outputDir, String outputTopicDir, boolean isAlignFile, boolean isEvaluation) {
        // д�ļ�����
        Writer labelWriter = null;
        Writer sentenceWriter = null;
        Writer topicWriter = null;
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
            File outputFile = new File(outputDir);
            if(!outputFile.exists()) {
                if(!outputFile.mkdirs())
                    throw new Exception("������������ļ�Ŀ¼ʧ�ܣ�");
            }

            String labelFileName = outputFile.getPath()+"_label//";
            File labelFile = new File(labelFileName);
            if(!labelFile.exists() && !isEvaluation) {
                if(!labelFile.mkdirs())
                    throw new Exception("������ǩ�ļ�Ŀ¼ʧ�ܣ�");
            }

            File topicFile = null;
            if(!isAlignFile && !isEvaluation && outputTopicDir!=null){
                topicFile = new File(outputTopicDir);
                if(!topicFile.exists()) {
                    if(!topicFile.mkdirs())
                        throw new Exception("������������ļ�Ŀ¼ʧ�ܣ�");
                }
            }
            if(outputDir != null && topicFile != null)
                topicWriter = new OutputStreamWriter(new FileOutputStream(topicFile.getPath()+"//"+topic+"_topic", false), "GBK");
            if(!isEvaluation)
                labelWriter = new OutputStreamWriter(new FileOutputStream(labelFile.getPath()+"//"+topic+"_label", false), "GBK");
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
                            // ��������е�(@)
                            sentence = sentence.replaceAll("[\\(��][^\n]*@[^\n]*[\\)��]", "");
                            sentence = sentence.replaceAll("��[^\n]*@[^\n]*��", "");

                            // ��������е�http�������м�ĳɷ�
                            sentence = sentence.replaceAll("[^\\s]*:http.*http[^\\s]*","");
                            sentence = sentence.replaceAll("http.*http[^\\s]*","");
                            sentence = sentence.replaceAll("[^\\s]*:http[^\\s]*","");
                            sentence = sentence.replaceAll("http[^\\s]*","");

                            // Ѱ�Ҿ����е�����
                            if(topicWriter != null){
                                Pair<String, String> result = getTopicAndContent(sentence, hashtag);
                                sentence = result.second;
                                // ��������ո�ǰ����ޱ������
                                hashtag = handleSpaceInSentence(result.first);
                            }

                            // �������Ŀո�ǰ����ޱ������
                            sentence = handleSpaceInSentence(sentence);
                        }

                        // ������ļ�
                        if(!isEvaluation){
                            if(isAlignFile){
                                sentenceWriter.write(sentence);
                            }
                            else {
                                topicWriter.write(hashtag+"\n");
                                sentenceWriter.write(sentence+"\n");
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
                if(topicWriter != null)
                    topicWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // ��ȡ�����е����������
    /*1����XXX���ھ��ף���ǰ�ɺ��������ţ����ĩ�����ɺ��������ţ����֣��������Կո�ֿ���
      2����XXX���ھ��׳���
      3����hashtag��ע�����⡣*/
    public Pair<String, String> getTopicAndContent(String sentence, String hashtag){
        int startTagIndex, endTagIndex;
        String topic = "";
        // ��ȡ��ǰ����
        if(sentence.startsWith("#") && sentence.endsWith("#"))
            sentence = sentence.substring(1, sentence.length()-1);
        else{
            startTagIndex = sentence.indexOf('#');
            while(startTagIndex != -1) {
                endTagIndex = sentence.indexOf('#', startTagIndex+1);
                if(endTagIndex != -1){
                    topic = sentence.substring(startTagIndex+1, endTagIndex).trim();
                    startTagIndex = sentence.indexOf('#', endTagIndex+1);
                } else
                    break;
            }
            sentence = sentence.replaceAll("#.*#", "").trim();
        }
        topic = (topic=="")? hashtag : topic;
        // ������׻��ĩ�ġ���
        startTagIndex = sentence.indexOf('��');
        if(startTagIndex != -1) {
            endTagIndex = sentence.indexOf('��', startTagIndex+1);
            int flag = isDependentBookPunctuation(sentence, startTagIndex, endTagIndex+1);
            if(endTagIndex!=-1 && flag!=-1){
                topic = sentence.substring(startTagIndex, endTagIndex+1).trim();
                if(flag == 0)
                    sentence = sentence.substring(endTagIndex+1).trim();
                else
                    sentence = sentence.substring(0, startTagIndex).trim();
                return new Pair<String, String>(topic, sentence);
            }
        }
        // ������׵ġ�XXX��
        startTagIndex = sentence.indexOf('��');
        boolean flag = true;
        StringBuilder tmpTopic = null;
        while(startTagIndex != -1) {
            // ǰ������б�����
            for(int i=0; i<startTagIndex; ++i){
                if(PunctuationUtil.PUNCTUATION.indexOf(sentence.charAt(i)) == -1) {
                    flag = false;
                    break;
                }
            }
            if(flag) {
                Pair<Integer, StringBuilder> result = getBlockContent(sentence, startTagIndex);
                startTagIndex = sentence.indexOf('��', result.first);
                if(result.first < sentence.length()-1)
                    sentence = sentence.substring(result.first+1);
                if(result.second.length() > 0)
                    tmpTopic = result.second;
            }else
                break;
        }
        if(tmpTopic!=null && tmpTopic.length()>0)
            return new Pair<String, String>(tmpTopic.toString(), sentence);
        // ���ؾ�����#XXX#�ָ������
        return new Pair<String, String>(topic, sentence);
    }

    // �ж��Ƿ��Ƕ����������Ų���(����������������ɷ�)
    public int isDependentBookPunctuation(String sentence, int start, int end) {
        boolean flag = true;
        // �Ƿ��ھ���
        if(end<sentence.length() && sentence.charAt(end)==' ') {
            for(int i=0; i<start; ++i){
                if(PunctuationUtil.PUNCTUATION.indexOf(sentence.charAt(i)) == -1) {
                    flag = false;
                    break;
                }
            }
            if(flag)
                return 0;
        }
        // �Ƿ��ھ�ĩ
        if(start>0 && sentence.charAt(start-1)==' ') {
            for(int i=end; i<sentence.length(); ++i){
                if(PunctuationUtil.PUNCTUATION.indexOf(sentence.charAt(i)) == -1) {
                    flag = false;
                    break;
                }
            }
            if(flag)
                return 1;
        }
        return -1;
    }

    // ��ȡ�������ڵ�����
    public Pair<Integer, StringBuilder> getBlockContent(String sentence, int start){
        char c;
        boolean match;
        Stack<Character> stack = new Stack<Character>();
        StringBuilder tmpTopic = new StringBuilder();
        stack.push('��');
        int i = start + 1;
        for(; i<sentence.length(); ++i){
            c=sentence.charAt(i);
            if( c!= '��'){
                stack.push(c);
            }else{
                match = true;
                tmpTopic.delete(0, tmpTopic.length());
                while(match && !stack.empty()){
                    c = stack.pop();
                    if(c != '��')
                        tmpTopic.insert(0, c);
                    else{
                        match = false;
                    }
                }
                if(stack.empty())
                    break;
            }
        }
        return new Pair<Integer, StringBuilder>(i, tmpTopic);
    }

    // ������пո�ǰ��ȥ�������
    public String handleSpaceInSentence(String sentence){
        String lastSen;
        do{
            lastSen = sentence;
            String pattern = "(.*[^\\s,����\\.��;��:!��\\?��\\-��_=+<>����(){}|\\/\\*&\\^'\"��������\\$%@#])(\\s+)([^\\s,����\\.��;��:!��\\?��\\-��_=+<>����(){}|\\/\\*&\\^'\"��������\\$%@#].*)";
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(sentence);
            StringBuffer sb = new StringBuffer();
            StringBuffer sb0 = new StringBuffer();
            int i=0;
            //ʹ��find()�������ҵ�һ��ƥ��Ķ���
            boolean result = m.find();
            //ʹ��ѭ�������������е�ƥ���ҳ����滻�ٽ����ݼӵ�sb��
            if(result) {
                i++;
                sb.append(m.group(1)+"OOOO"+m.group(3));
                m.appendReplacement(sb0, " ");
            }
            //������appendTail()���������һ��ƥ����ʣ���ַ����ӵ�sb�
            m.find();
            m.appendTail(sb);
            sentence = sb.toString();
        }while(!lastSen.equals(sentence));
        return sentence.replaceAll("OOOO", ",");
    }

    public static  void main(String args[]){
        CorpusPreHandler p = new CorpusPreHandler();
       Pair<String,String> a = p.getTopicAndContent("������Ц���������������˾��Ψһԭ����Ƕ������ܣ�","");
        System.out.println(a);
    }
}
