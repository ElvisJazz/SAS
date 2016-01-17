import com.google.common.collect.HashMultimap;
import wordSimilarity.WordSimilarity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 15-3-15
 * Time: 下午11:39
 * To change this template use File | Settings | File Templates.
 */
public class SentimentSorter {
    // 种子情感词积极+消极
    private final String[] POSITIVE_SENTIMENTS = {"积极", "正确", "快乐", "伟大", "纯洁", "促进"};//
    private final String[] NEGATIVE_SENTIMENTS = {"消极", "错误", "悲伤", "渺小", "肮脏", "阻碍"};//

    // 正面情感词词典
    private Set<String> posSentimentDic = new HashSet<String>();
    // 负面情感词词典
    private Set<String> negSentimentDic = new HashSet<String>();

    // 相似度计算独享
    private WordSimilarity ws = new WordSimilarity();

    public void init(String posDicPath, String negDicPath){
        FileReader posReader=null, negReader=null;
        BufferedReader posBufferReader=null, negBufferReader=null;
        try{
            File posFile = new File(posDicPath);
            File negFile = new File(negDicPath);
            posReader = new FileReader(posFile);
            negReader = new FileReader(negFile);
            posBufferReader = new BufferedReader(posReader);
            negBufferReader = new BufferedReader(negReader);
            // 读取正面情感词词典
            String pos = posBufferReader.readLine();
            while(pos != null){
                pos = pos.substring(0,pos.length()-1);
                posSentimentDic.add(pos);
                pos = posBufferReader.readLine();
            }
            // 读取负面情感词词典
            String neg = negBufferReader.readLine();
            while(neg != null){
                neg = neg.substring(0,neg.length()-1);
                negSentimentDic.add(neg);
                neg = negBufferReader.readLine();
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try{
                if(posReader != null)
                    posReader.close();
                if(negReader != null)
                    negReader.close();
                if(posBufferReader != null)
                    posBufferReader.close();
                if(negBufferReader != null)
                    negBufferReader.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }
    // 分类指定目录下的文件
    public void sortAll(String readDir, String outputDir) {

        File file = new File(outputDir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                System.out.println("创建目录失败！");
                return;
            }
        }

        File[] readFileArray = (new File(readDir)).listFiles();

        for (int i = 0; i < readFileArray.length; ++i) {
            sort(readFileArray[i].getAbsolutePath(), outputDir + "//" + readFileArray[i].getName(), readFileArray[i].getName());
        }
    }

    // 分类单个文件
    public void sort(String readFilePath, String outputFilePath, String fileName) {
        FileReader fileReader = null;
        FileWriter fileWriter = null;
        BufferedReader bufferedReader = null;
        String sentence = null;
        try {
            File file = new File(readFilePath);
            fileReader = new FileReader(file);
            fileWriter = new FileWriter(outputFilePath);
            bufferedReader = new BufferedReader(fileReader);
            sentence = bufferedReader.readLine();
            while (sentence != null) {
                // 存储所有名词-情感词对
                HashMultimap<String, String> nounSentimentMap = readNounSentiment(sentence);
                // 分析当前句子
                for (String noun : nounSentimentMap.keySet()) {
                    String outputTag = compute(nounSentimentMap.get(noun));
                    if (!"OTHER".equals(outputTag))
                        fileWriter.write("[" + noun + ", " + outputTag + "]");
                }
                fileWriter.write("\n");
                // 获取下一个句子
                sentence = bufferedReader.readLine();
            }
            System.out.println(fileName + "情感相似度分析完成！");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileReader != null) {
                    fileReader.close();
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
        String noun = "", sentiment = "";
        while (index1 != -1 && index2 != -1 && index3 != -1 && index1 < index2 && index2 < index3) {
            noun = sentence.substring(index1 + 1, index2);
            sentiment = sentence.substring(index2 + 2, index3);

            // 处理情感词
            if ('的' == sentiment.charAt(sentiment.length() - 1)) {
                sentiment = sentiment.substring(0, sentiment.length() - 1);
            }
            nounSentenceMap.put(noun, sentiment);
            index1 = sentence.indexOf("[", index3);
            index2 = sentence.indexOf(", ", index1);
            index3 = sentence.indexOf(']', index2);

        }
        // 处理root情况，如果root对应情感词有其他名词对应，则取消root对应情况
        Set<String> sentimentSet = nounSentenceMap.get("#");
        Set<String> valueSet = null;
        Set<String> delSet = new HashSet<String>();
        boolean isOut = false;
        for (String sentiment1 : sentimentSet) {
            for (String key : nounSentenceMap.keySet()) {
                if (!"#".equals(key)) {
                    valueSet = nounSentenceMap.get(key);
                    for(String sen : valueSet) {
                        // 移除原来的root情感对
                        if(sentiment1.equals(sen)){
                            delSet.add(sen);
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
    public String compute(Set<String> wordSet) {
        int posScore = 0, negScore = 0;
        int maxPosScore = 0, maxNegScore = 0;
        double tmpPositiveScore, tmpNegativeScore;
        boolean isNegReverse = false;

        for (String word : wordSet) {
           /* // 处理新闻用词
            if("来自".equals(word))
                continue;
*/

            // 处理反转情况 ,形如： (-)高兴
            if(word.contains("(-)")){
                isNegReverse = true;
                word = word.substring(3);
            }
            if(word.length() > 2 && word.contains("的")){
                //System.out.println(word);
                int index = word.indexOf("的");
                if(index == word.length()-1)
                    word = word.substring(0, index);
                else
                    word = word.substring(index+1);
            }

            for (int i = 0; i < POSITIVE_SENTIMENTS.length; ++i) {
                tmpPositiveScore = ws.simWord(word, POSITIVE_SENTIMENTS[i]);
                tmpNegativeScore = ws.simWord(word, NEGATIVE_SENTIMENTS[i]);

                if (tmpPositiveScore < 0.06 && tmpNegativeScore < 0.06){

                    continue;
                }

                if (tmpPositiveScore > tmpNegativeScore) {
                    if(isNegReverse){
                        negScore++;
                    }else{
                        posScore++;
                    }
                } else if (tmpNegativeScore > tmpPositiveScore) {
                    if(isNegReverse){
                        posScore++;
                    }else{
                        negScore++;
                    }
                }
            }
            if(posScore == negScore){
                // 从情感词典中匹配
                if(posSentimentDic.contains(word)) {
                    if(isNegReverse){
                        negScore++;
                    }else{
                        posScore++;
                    }
                }else if(negSentimentDic.contains(word)){
                    if(isNegReverse){
                        posScore++;
                    }else{
                        negScore++;
                    }
                }
            }

            maxPosScore = posScore > maxPosScore ? posScore : maxPosScore;
            maxNegScore = negScore > maxNegScore ? negScore : maxNegScore;

            isNegReverse = false;
            posScore = 0;
            negScore = 0;
        }
        if (maxPosScore > maxNegScore)
            return "POS";
        else if (maxPosScore < maxNegScore)
            return "NEG";
        else
            return "POS";
    }
}
