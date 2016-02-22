package cn.edu.seu;

import com.google.common.collect.HashMultimap;
import cn.edu.seu.wordSimilarity.WordSimilarity;

import java.io.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 15-3-15
 * Time: 下午11:39
 * To change this template use File | Settings | File Templates.
 */
public class SentimentSorter {
    public static final String POS = "POS";
    public static final String NEG = "NEG";
    public static final String OTHER = "OTHER";

    // 种子情感词积极+消极
    private final static String[] POSITIVE_SENTIMENTS = {"积极", "正确", "快乐", "伟大", "纯洁", "促进"};//
    private final static String[] NEGATIVE_SENTIMENTS = {"消极", "错误", "悲伤", "渺小", "肮脏", "阻碍"};//

    // 正面评价词词典
    private static Set<String> posOpinionDic = new HashSet<>();
    // 负面评价词典
    private static Set<String> negOpinionDic = new HashSet<>();
    // 正面感情词词典
    private static Set<String> posEmotionDic = new HashSet<>();
    // 负面感情词典
    private static Set<String> negEmotionDic = new HashSet<>();

    // 相似度计算独享
    public static WordSimilarity WS = new WordSimilarity();

    public static void init(String posDicPath, String negDicPath, String posEmotionDicPath, String negEmotionDicPath){
        Vector<BufferedReader> bufVec = new Vector<>();
        Vector<Set> setVec = new Vector<>();
        try{
            bufVec.add(new BufferedReader(new InputStreamReader(new FileInputStream(new File(posDicPath)),"GBK")));
            bufVec.add(new BufferedReader(new InputStreamReader(new FileInputStream(new File(negDicPath)),"GBK")));
            bufVec.add(new BufferedReader(new InputStreamReader(new FileInputStream(new File(posEmotionDicPath)),"GBK")));
            bufVec.add(new BufferedReader(new InputStreamReader(new FileInputStream(new File(negEmotionDicPath)),"GBK")));
            setVec.add(posOpinionDic);
            setVec.add(negOpinionDic);
            setVec.add(posEmotionDic);
            setVec.add(negEmotionDic);
            String tmp;
            // 读取词典
            for(int i=0; i<4; ++i){
                while((tmp=bufVec.get(i).readLine()) != null){
                    tmp = tmp.trim();
                    setVec.get(i).add(tmp);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try{
                for(int i=0; i<4; ++i)
                    bufVec.get(i).close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }
    // 分类指定目录下的文件
    public void sortAll(String readDir, String segPosDir, String outputDir) {

        File file = new File(outputDir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                System.out.println("创建目录失败！");
                return;
            }
        }

        File[] readFileArray = (new File(readDir)).listFiles();
        File[] segFileArray = (new File(segPosDir)).listFiles();
        if(readFileArray.length != segFileArray.length){
            System.out.println("目录文件数目不匹配！");
            return;
        }

        for (int i = 0; i < readFileArray.length; ++i) {
            sort(readFileArray[i].getAbsolutePath(), segFileArray[i].getAbsolutePath(), outputDir + "//" + readFileArray[i].getName(), readFileArray[i].getName());
        }
    }

    // 分类单个文件
    public void sort(String readFilePath, String segFilePath, String outputFilePath, String fileName) {
        FileWriter fileWriter = null;
        BufferedReader bufferedReader = null;
        BufferedReader bufferedSegReader = null;
        String sentence, segSentence;
        try {
            fileWriter = new FileWriter(outputFilePath);
            bufferedReader = new BufferedReader(new FileReader(new File(readFilePath)));
            bufferedSegReader = new BufferedReader(new FileReader(new File(segFilePath)));
            sentence = bufferedReader.readLine();
            segSentence = bufferedSegReader.readLine();
            while (sentence != null) {
                // 存储所有名词-情感词对
                HashMultimap<String, String> nounSentimentMap = readNounSentiment(sentence);
                // 分析当前句子
                for (String noun : nounSentimentMap.keySet()) {
                    String outputTag = compute(noun, nounSentimentMap.get(noun), segSentence);
                    if (!OTHER.equals(outputTag))
                        fileWriter.write("[" + noun + ", " + outputTag + "]");
                }
                fileWriter.write("\n");
                // 获取下一个句子
                sentence = bufferedReader.readLine();
                segSentence = bufferedSegReader.readLine();
            }
            System.out.println(fileName + "情感相似度分析完成！");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferedSegReader != null) {
                    bufferedSegReader.close();
                }
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (fileWriter != null) {
                    fileWriter.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 读取一句中所有名词-情感词对
    public HashMultimap<String, String> readNounSentiment(String sentence) {
        HashMultimap<String, String> nounSentenceMap = HashMultimap.create();
        int index1 = 0;
        int index2 = sentence.indexOf(", ", index1);
        int index3 = sentence.indexOf(']', index2);
        int index4 = sentence.lastIndexOf("(", index3);
        int index5 = sentence.indexOf("{", index4);
        String noun, sentiment;
        HashMap<String, String[]> advMap = new HashMap<>();
        while (index1 != -1 && index2 != -1 && index3 != -1 && index4 != -1 && index5!=-1 && index1 < index2 && index2 < index3 && index4 < index3) {
            noun = sentence.substring(index1 + 1, index2);
            sentiment = sentence.substring(index5, index5+3)+sentence.substring(index2 + 2, index4);
            advMap.put(sentiment, sentence.substring(index4+1, index3-1).split(" "));

            // 处理情感词
            if ('的' == sentiment.charAt(sentiment.length() - 1)) {
                sentiment = sentiment.substring(0, sentiment.length() - 1);
            }
            nounSentenceMap.put(noun, sentiment);
            index1 = sentence.indexOf("[", index3);
            index2 = sentence.indexOf(", ", index1);
            index3 = sentence.indexOf(']', index2);
            index4 = sentence.lastIndexOf("(", index3);
            index5 = sentence.indexOf("{", index4);
        }
        // 处理root情况，如果root对应情感词有其他名词对应，则取消root对应情况
        Set<String> sentimentSet = nounSentenceMap.get("#");
        Set<String> valueSet;
        Set<String> delSet = new HashSet<String>();
        boolean isOut = false;
        for (String sentiment1 : sentimentSet) {
            for (String key : nounSentenceMap.keySet()) {
                if (!"#".equals(key)) {
                    valueSet = nounSentenceMap.get(key);
                    for(String sen : valueSet) {
                        // 移除原来的root情感对(包括副词)
                        if(sentiment1.equals(sen)){
                            delSet.add(sen);
                            // adv
                            String[] array = advMap.get(sen);
                            if(array != null){
                                for(String adv : array)
                                    delSet.add(adv);
                            }
                            isOut = true;
                            break;
                        }
                    }
                    if(isOut){
                        isOut = false;
                        break;
                    }
                }
            }
        }
        // 删除元素
        for (String delSentiment : delSet) {
            nounSentenceMap.remove("#", delSentiment);
        }

        return nounSentenceMap;
    }

    // 计算与种子词情感相似度值,返回正满POS，负面NEG,其他OTHER
    public static String compute(String target, Set<String> wordSet, String segSentence) {
        double posScore = 0, negScore = 0;
        double maxPosScore = 0, maxNegScore = 0;
        double tmpPositiveScore, tmpNegativeScore;
        boolean isNegReverse = false;
        double rate, baseScore, score;
        String oSentence = segSentence.replaceAll("/\\S*\\s*", "");
        int index = oSentence.indexOf(target);
        index = index==-1? 0 : index;
        int sIndex;
        for (String word : wordSet) {
            // 计算基础分，由距离和词性决定
            sIndex = oSentence.indexOf(word);
            if(sIndex == index)
                sIndex = index - 1;
            if(segSentence.contains(word+"/d"))
                baseScore = 0.7;//*Math.log(oSentence.length()/Math.abs(index-sIndex));
            else
                baseScore = 1.0;//*Math.log(oSentence.length()/Math.abs(index-sIndex));
            // 计算分值系数
            if(word.startsWith("{w}"))
                rate = 0.5;
            else
                rate = 1.0;
            score = baseScore*rate;

            // 处理反转情况 ,形如： (-)高兴
            word = word.substring(3);
            if(word.contains("(-)")){
                isNegReverse = true;
                word = word.substring(3);
            }
            // 问号反转
            index = segSentence.indexOf(word);
            if(index != -1){
                index = segSentence.indexOf("/w", index);
                if(index!=-1 && (segSentence.charAt(index-1)=='？' || segSentence.charAt(index-1)=='?')){
                    if(isNegReverse)
                        isNegReverse = false;
                    else
                        isNegReverse = true;
                }
            }
            // 处理形容词含“的”的情况
            if(word.length() > 2 && word.contains("的")){
                //System.out.println(word);
                index = word.indexOf("的");
                if(index == word.length()-1)
                    word = word.substring(0, index);
                else
                    word = word.substring(index+1);
            }

            // 相似度计算
           /* for (int i = 0; i < POSITIVE_SENTIMENTS.length; ++i) {
                tmpPositiveScore = WS.simWord(word, POSITIVE_SENTIMENTS[i]);
                tmpNegativeScore = WS.simWord(word, NEGATIVE_SENTIMENTS[i]);

                if (tmpPositiveScore < 0.06 && tmpNegativeScore < 0.06){
                    continue;
                }

                if (tmpPositiveScore > tmpNegativeScore) {
                    if(isNegReverse){
                        negScore += score;
                    }else{
                        posScore += score;
                    }
                } else if (tmpNegativeScore > tmpPositiveScore) {
                    if(isNegReverse){
                        posScore += score;
                    }else{
                        negScore += score;
                    }
                }
            }*/
            if(posScore == negScore){
                // 从情感词典中匹配
                if(posOpinionDic.contains(word) || posEmotionDic.contains(word)) {
                    if(isNegReverse){
                        negScore += score;
                    }else{
                        posScore += score;
                    }
                }else if(negOpinionDic.contains(word) || negEmotionDic.contains(word)){
                    if(isNegReverse){
                        posScore += score;
                    }else{
                        negScore += score;
                    }
                }
            }

            maxPosScore = posScore > maxPosScore ? posScore : maxPosScore;
            maxNegScore = negScore > maxNegScore ? negScore : maxNegScore;

            isNegReverse = false;
            posScore = 0.0;
            negScore = 0.0;
        }
        if (maxPosScore > maxNegScore)
            return POS;
        else if (maxPosScore < maxNegScore)
            return NEG;
        else
            return OTHER;
    }

    // 获取情感词类别：0：不是情感词或观点词，+-1：观点词，+-2：情感词
    public static int getSentimentWordType(String word){
        if(posOpinionDic.contains(word))
            return 1;
        else if(negOpinionDic.contains(word))
            return -1;
        else if(posEmotionDic.contains(word))
            return 2;
        else if(negEmotionDic.contains(word))
            return -2;

        return 0;
    }
}
