package cn.edu.seu;

import cn.edu.seu.utils.AlignUtil;
import cn.edu.seu.utils.LexUtil;
import cn.edu.seu.utils.PunctuationUtil;
import edu.hit.ir.ltp4j.Pair;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 12-7-19
 * Time: 上午11:00
 * To change this template use File | Settings | File Templates.
 */

public class CorpusPreHandler {

    // 预处理所有原始语料，生成待分析语料
    public void handleAllOriginalCorpus(String readDir, String outputDir, String outputTopicDir, boolean isAlignFile, boolean isEvaluation){
        File[] fileArray = (new File(readDir)).listFiles();
        for(int i=0; i<fileArray.length; ++i){
            readXmlFile(fileArray[i].getAbsolutePath(), fileArray[i].getName(), outputDir, outputTopicDir, isAlignFile, isEvaluation);
        }
    }

    // 读取并解析xml文件
    public void readXmlFile(String readFilePath, String fileName, String outputDir, String outputTopicDir, boolean isAlignFile, boolean isEvaluation) {
        // 写文件变量
        Writer labelWriter = null;
        Writer sentenceWriter = null;
        Writer topicWriter = null;
        Writer alignOffsetWriter = null;
        Writer alignOriginalWriter = null;
        int offset, YNum;

        try{
            // 设置读取变量
            Element weiboElement, sentenceElement;
            String weiboId=null, opinionated=null, sentence=null, oSentence=null, allSentence=null, topic=null, hashtag=null;
            Iterator sentenceIterator, hashtagIterator;

            // 读取文件
            SAXReader reader = new SAXReader();
            Document document = reader.read(new File(readFilePath));

            // 获取根节点
            Element root = document.getRootElement();
            topic = root.attributeValue("title");
            System.out.println("当前处理文档：" + fileName);
            if(topic == null){
                int index = fileName.indexOf('.');
                if(index != -1)
                    topic = fileName.substring(0, index);
                else
                    topic = fileName;
            }

            // 文件输出变量
            File outputFile = new File(outputDir);
            if(!outputFile.exists()) {
                if(!outputFile.mkdirs())
                    throw new Exception("创建正文输出文件目录失败！");
            }

            String labelFileName = outputFile.getPath()+"_label//";
            File labelFile = new File(labelFileName);
            File alignOffsetFile = new File(outputFile.getPath()+"_offset//");
            File alignOriginalFile = new File(outputFile.getPath()+"_original//");

            if(!labelFile.exists() && !isEvaluation) {
                if(!labelFile.mkdirs())
                    throw new Exception("创建标签文件目录失败！");
            }
            if(isAlignFile && alignOffsetFile!=null && !alignOffsetFile.exists()){
                if(!alignOffsetFile.mkdirs())
                    throw new Exception("创建对齐offset文件目录失败！");
            }
            if(isAlignFile && alignOriginalFile!=null && !alignOriginalFile.exists()){
                if(!alignOriginalFile.mkdirs())
                    throw new Exception("创建对齐original文件目录失败！");
            }

            File topicFile = null;
            if(!isAlignFile && !isEvaluation && outputTopicDir!=null){
                topicFile = new File(outputTopicDir);
                if(!topicFile.exists()) {
                    if(!topicFile.mkdirs())
                        throw new Exception("创建主题输出文件目录失败！");
                }
            }
            if(outputDir != null && topicFile != null)
                topicWriter = new OutputStreamWriter(new FileOutputStream(topicFile.getPath()+"//"+topic+"_topic", false), "UTF-8");
            if(!isEvaluation)
                labelWriter = new OutputStreamWriter(new FileOutputStream(labelFile.getPath()+"//"+topic+"_label", false), "UTF-8");
            if(isAlignFile){
                alignOffsetWriter = new OutputStreamWriter(new FileOutputStream(alignOffsetFile.getPath()+"//"+topic+"_offset", false), "UTF-8");
                alignOriginalWriter = new OutputStreamWriter(new FileOutputStream(alignOriginalFile.getPath()+"//"+topic+"_oiginal", false), "UTF-8");
            }
            sentenceWriter = new OutputStreamWriter(new FileOutputStream(outputDir+"//"+topic+"_sentence", false), "UTF-8");

            // 获取head节点
            Iterator weiboIterator= root.elementIterator("weibo");
            while(weiboIterator.hasNext()){
                offset = 0;
                YNum = 0;
                allSentence = "";
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
                    sentence = sentenceElement.getText().replaceAll("[“”＂]", "\"");
                    oSentence = sentence;
                    if("Y".equals(opinionated))
                        YNum++;
                    if(isAlignFile || "Y".equals(opinionated)){
                        //sentenceId = sentenceElement.attributeValue("id");

                        //如果是处理评测语料对齐，需找到所有情感词起始位置和极性
                        if(isEvaluation){
                            int i = 1;
                            while(sentenceElement.attributeValue("target_begin_"+i) != null){
                                sentenceWriter.write("["+sentenceElement.attributeValue("target_begin_"+i)+","+
                                        sentenceElement.attributeValue("target_end_"+i)+","+sentenceElement.attributeValue("target_polarity_"+i)+"]");
                                ++i;
                            }
                        }else{

                            // 处理句子中的(@)
                            sentence = sentence.replaceAll("[\\(（][^\n]*@[^\n]*[\\)）]", "");
                            sentence = sentence.replaceAll("（[^\n]*@[^\n]*）", "");
                            Pattern p = Pattern.compile("@\\S* ");
                            Matcher m = p.matcher(sentence);
                            while(m.find()){
                                String mStr = m.group();
                                sentence = sentence.replaceAll(mStr, mStr.substring(1, mStr.length()-1));
                            }

                            // 处理句子中的http和链接中间的成分
                            sentence = sentence.replaceAll("[^\\s]*:http.*http[^\\s]*","");
                            sentence = sentence.replaceAll("http.*http[^\\s]*","");
                            sentence = sentence.replaceAll("[^\\s]*:http[^\\s]*","");
                            sentence = sentence.replaceAll("http[^\\s]*","");

                            // 处理句子中的.
                            sentence = sentence.replaceAll("\\.","，");

                            // 过滤非否定词白名单
                            for(String whiteWord : LexUtil.FILTER_WHITE_SET){
                                sentence = sentence.replaceAll(whiteWord, "");
                            }

                            // 寻找句子中的主题
                            if(topicWriter != null){
                                Pair<String, String> result = getTopicAndContent(sentence, hashtag, false);
                                sentence = result.second;
                                // 处理主题空格前后均无标点的情况
                                hashtag = handleSpaceInSentence(result.first);
                            }
                            if(isAlignFile){
                                Pair<String, String> result = getTopicAndContent(sentence, hashtag, false);
                                sentence = result.second;
                            }

                            // 处理正文空格前后均无标点的情况
                            sentence = handleSpaceInSentence(sentence);

                            // 为情感后指词添加主语（若无）
                            if(!isAlignFile)
                                sentence = addSubject(sentence);

                            if(alignOffsetWriter != null && "Y".equals(opinionated))
                                alignOffsetWriter.write(offset+"\n");
                            offset += oSentence.length();
                            allSentence += sentence;
                        }

                        // 输出到文件
                        if(!isEvaluation){
                            if(isAlignFile){
                                sentenceWriter.write(oSentence);
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
                    else if(topicWriter != null){
                        Pair<String, String> result = getTopicAndContent(sentence, hashtag, false);
                        // 处理主题空格前后均无标点的情况
                        hashtag = handleSpaceInSentence(result.first);
                    }
                }
                if(!isEvaluation && isAlignFile){
                    labelWriter.write(weiboId+"\n");
                    sentenceWriter.write("\n");
                    if(YNum > 0){
                        for(int i=0; i<YNum; i++){
                            alignOriginalWriter.write(allSentence);
                            alignOriginalWriter.write("\n");
                        }
                    }
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
                if(alignOffsetWriter != null)
                    alignOffsetWriter.close();
                if(alignOriginalWriter != null)
                    alignOriginalWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 为情感后指词添加主语
    public String addSubject(String sentence){
        AlignUtil.init();
        String segSentence = AlignUtil.segmenter.segmentSentence(sentence, false);
        String trunks[] = segSentence.split(" ");
        StringBuilder result = new StringBuilder();
        if(Math.abs(SentimentSorter.getSentimentWordType(trunks[0])) == 2)
            result.append("我");
        result.append(trunks[0]);
        for(int i=1; i<trunks.length; i++){
            if(PunctuationUtil.PUNCTUATION.contains(trunks[i-1]) && Math.abs(SentimentSorter.getSentimentWordType(trunks[i])) == 2)
                result.append("我");
            result.append(trunks[i]);
        }
        return result.toString();
    }

    // 获取句子中的主题和正文
    /*1）《XXX》在句首（其前可含其他符号）或句末（其后可含其他符号）出现，与正文以空格分开。
      2）【XXX】在句首出现
      3）以hashtag标注的主题。*/
    public Pair<String, String> getTopicAndContent(String sentence, String hashtag, boolean isDeleteTopic){
        int startTagIndex, endTagIndex;
        String topic = "";
        // 获取当前主题
        if(sentence.startsWith("#") && sentence.endsWith("#"))
            sentence = sentence.substring(1, sentence.length()-1);
        else{
            startTagIndex = sentence.indexOf('#');
            while(startTagIndex != -1) {
                endTagIndex = sentence.indexOf('#', startTagIndex+1);
                if(endTagIndex != -1){
                    topic = sentence.substring(startTagIndex+1, endTagIndex).trim();
                    break;
                } else
                    break;
            }
            if(isDeleteTopic){
                String tmp = sentence.replaceFirst("#.*#", "").trim();
                if(!"".equals(tmp))
                    sentence = tmp;
            }

            sentence = sentence.replaceFirst("#", "").trim();
            if(sentence.contains("# "))
                sentence = sentence.replaceFirst("# ", ",").trim();
            else
                sentence = sentence.replaceFirst("#", ",").trim();
            sentence = sentence.replaceAll("#", "").trim();

        }
        topic = (topic.equals(""))? hashtag : topic;
        // 处理句首或句末的《》
        startTagIndex = sentence.indexOf('《');
        if(startTagIndex != -1) {
            endTagIndex = sentence.indexOf('》', startTagIndex+1);
            int flag = isDependentBookPunctuation(sentence, startTagIndex, endTagIndex+1);
            if(endTagIndex!=-1 && flag!=-1){
                topic = sentence.substring(startTagIndex+1, endTagIndex).trim();
                if(flag == 0)
                    sentence = sentence.substring(endTagIndex+1).trim();
                else
                    sentence = sentence.substring(0, startTagIndex).trim();
                return new Pair<String, String>(topic, sentence);
            }
        }
        // 处理句首的【XXX】
        startTagIndex = sentence.indexOf('【');
        boolean flag = true;
        StringBuilder tmpTopic = null;
        while(startTagIndex != -1) {
            // 前面可以有标点符号
            for(int i=0; i<startTagIndex; ++i){
                if(PunctuationUtil.PUNCTUATION.indexOf(sentence.charAt(i)) == -1) {
                    flag = false;
                    break;
                }
            }
            if(flag) {
                Pair<Integer, StringBuilder> result = getBlockContent(sentence, startTagIndex);
                startTagIndex = sentence.indexOf('【', result.first);
                if(result.first < sentence.length()-1)
                    sentence = sentence.substring(result.first+1);
                if(result.second.length() > 0)
                    tmpTopic = result.second;
            }else
                break;
        }
        if(tmpTopic!=null && tmpTopic.length()>0)
            return new Pair<String, String>(tmpTopic.toString(), sentence);
        // 返回句子中#XXX#分割的主题
        return new Pair<>(topic, sentence);
    }

    // 判断是否是独立的书名号部分(必须包含句子其他成分)
    public int isDependentBookPunctuation(String sentence, int start, int end) {
        boolean flag = true;
        // 是否在句首
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
        // 是否在句末
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

    // 获取方括号内的内容
    public Pair<Integer, StringBuilder> getBlockContent(String sentence, int start){
        char c;
        boolean match;
        Stack<Character> stack = new Stack<Character>();
        StringBuilder tmpTopic = new StringBuilder();
        stack.push('【');
        int i = start + 1;
        for(; i<sentence.length(); ++i){
            c=sentence.charAt(i);
            if( c!= '】'){
                stack.push(c);
            }else{
                match = true;
                tmpTopic.delete(0, tmpTopic.length());
                while(match && !stack.empty()){
                    c = stack.pop();
                    if(c != '【')
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

    // 处理句中空格前后去标点的情况
    public String handleSpaceInSentence(String sentence){
        String lastSen;
        String tmp1, tmp2;
        String alpha = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String pattern = "(.*[^\\s,，、\\.。;；:!！\\?？\\-—_=+<>（）(){}|\\/\\*&\\^'\"”“’‘\\$%@#])(\\s+)([^\\s,，、\\.。;；:!！\\?？\\-—_=+<>（）(){}|\\/\\*&\\^'\"”“’‘\\$%@#].*)";
        boolean result;
        int i;
        do{
            lastSen = sentence;
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(sentence);
            StringBuffer sb = new StringBuffer();
            StringBuffer sb0 = new StringBuffer();
            i=0;
            //使用find()方法查找第一个匹配的对象
            result = m.find();
            //使用循环将句子里所有的匹配找出并替换再将内容加到sb里
            if(result) {
                i++;
                tmp1 = m.group(1);
                tmp2 = m.group(3);
                //if(!alpha.contains(""+tmp1.charAt(tmp1.length()-1)) && !alpha.contains(""+tmp2.charAt(0)))
                sb.append(tmp1+"OOOO"+tmp2);
                /*else
                    sb.append(tmp1+" "+tmp2);*/
                m.appendReplacement(sb0, " ");
            }
            //最后调用appendTail()方法将最后一次匹配后的剩余字符串加到sb里；
            m.find();
            m.appendTail(sb);
            sentence = sb.toString();
        }while(!lastSen.equals(sentence));
        return sentence.replaceAll("OOOO", ",");
    }

    public static  void main(String args[]){
        String sentence = "nt";

        boolean is = sentence.matches("(nt)|(nd)");
        System.out.println(is);
    }
}
