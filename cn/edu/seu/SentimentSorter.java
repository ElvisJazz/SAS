package cn.edu.seu;

import com.google.common.collect.HashMultimap;
import cn.edu.seu.wordSimilarity.WordSimilarity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 15-3-15
 * Time: ����11:39
 * To change this template use File | Settings | File Templates.
 */
public class SentimentSorter {
    public static final String POS = "POS";
    public static final String NEG = "NEG";
    public static final String OTHER = "OTHER";
    // ������дʻ���+����
    private final String[] POSITIVE_SENTIMENTS = {"����", "��ȷ", "����", "ΰ��", "����", "�ٽ�"};//
    private final String[] NEGATIVE_SENTIMENTS = {"����", "����", "����", "��С", "����", "�谭"};//

    // �������۴ʴʵ�
    private static Set<String> posOpinionDic = new HashSet<String>();
    // �������۴ʵ�
    private static Set<String> negOpinionDic = new HashSet<String>();
    // �������ʴʵ�
    private static Set<String> posEmotionDic = new HashSet<String>();
    // �������ʵ�
    private static Set<String> negEmotionDic = new HashSet<String>();

    // ���ƶȼ������
    private WordSimilarity ws = new WordSimilarity();

    public static void init(String posDicPath, String negDicPath, String posEmotionDicPath, String negEmotionDicPath){
        Vector<BufferedReader> bufVec = new Vector<>();
        Vector<Set> setVec = new Vector<>();
        try{
            bufVec.add(new BufferedReader(new FileReader(new File(posDicPath))));
            bufVec.add(new BufferedReader(new FileReader(new File(negDicPath))));
            bufVec.add(new BufferedReader(new FileReader(new File(posEmotionDicPath))));
            bufVec.add(new BufferedReader(new FileReader(new File(negEmotionDicPath))));
            setVec.add(posOpinionDic);
            setVec.add(negOpinionDic);
            setVec.add(posEmotionDic);
            setVec.add(negEmotionDic);
            String tmp;
            // ��ȡ�ʵ�
            for(int i=0; i<4; ++i){
                while((tmp=bufVec.get(i).readLine()) != null){
                    tmp = tmp.substring(0,tmp.length()-1);
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
    // ����ָ��Ŀ¼�µ��ļ�
    public void sortAll(String readDir, String outputDir) {

        File file = new File(outputDir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                System.out.println("����Ŀ¼ʧ�ܣ�");
                return;
            }
        }

        File[] readFileArray = (new File(readDir)).listFiles();

        for (int i = 0; i < readFileArray.length; ++i) {
            sort(readFileArray[i].getAbsolutePath(), outputDir + "//" + readFileArray[i].getName(), readFileArray[i].getName());
        }
    }

    // ���൥���ļ�
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
                // �洢��������-��дʶ�
                HashMultimap<String, String> nounSentimentMap = readNounSentiment(sentence);
                // ������ǰ����
                for (String noun : nounSentimentMap.keySet()) {
                    String outputTag = compute(nounSentimentMap.get(noun));
                    if (!OTHER.equals(outputTag))
                        fileWriter.write("[" + noun + ", " + outputTag + "]");
                }
                fileWriter.write("\n");
                // ��ȡ��һ������
                sentence = bufferedReader.readLine();
            }
            System.out.println(fileName + "������ƶȷ�����ɣ�");
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

    // ��ȡһ������������-��дʶ�
    public HashMultimap<String, String> readNounSentiment(String sentence) {
        HashMultimap<String, String> nounSentenceMap = HashMultimap.create();
        int index1 = 0;
        int index2 = sentence.indexOf(", ", index1);
        int index3 = sentence.indexOf(']', index2);
        String noun = "", sentiment = "";
        while (index1 != -1 && index2 != -1 && index3 != -1 && index1 < index2 && index2 < index3) {
            noun = sentence.substring(index1 + 1, index2);
            sentiment = sentence.substring(index2 + 2, index3);

            // ������д�
            if ('��' == sentiment.charAt(sentiment.length() - 1)) {
                sentiment = sentiment.substring(0, sentiment.length() - 1);
            }
            nounSentenceMap.put(noun, sentiment);
            index1 = sentence.indexOf("[", index3);
            index2 = sentence.indexOf(", ", index1);
            index3 = sentence.indexOf(']', index2);

        }
        // ����root��������root��Ӧ��д����������ʶ�Ӧ����ȡ��root��Ӧ���
        Set<String> sentimentSet = nounSentenceMap.get("#");
        Set<String> valueSet = null;
        Set<String> delSet = new HashSet<String>();
        boolean isOut = false;
        for (String sentiment1 : sentimentSet) {
            for (String key : nounSentenceMap.keySet()) {
                if (!"#".equals(key)) {
                    valueSet = nounSentenceMap.get(key);
                    for(String sen : valueSet) {
                        // �Ƴ�ԭ����root��ж�
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
        // ɾ��Ԫ��
        for (String delSentiment : delSet) {
            nounSentenceMap.remove("#", delSentiment);
        }

        return nounSentenceMap;
    }

    // ���������Ӵ�������ƶ�ֵ,��������POS������NEG,����OTHER
    public String compute(Set<String> wordSet) {
        int posScore = 0, negScore = 0;
        int maxPosScore = 0, maxNegScore = 0;
        double tmpPositiveScore, tmpNegativeScore;
        boolean isNegReverse = false;

        for (String word : wordSet) {
            // ����ת��� ,���磺 (-)����
            if(word.contains("(-)")){
                isNegReverse = true;
                word = word.substring(3);
            }
            if(word.length() > 2 && word.contains("��")){
                //System.out.println(word);
                int index = word.indexOf("��");
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
                // ����дʵ���ƥ��
                if(posOpinionDic.contains(word) || posEmotionDic.contains(word)) {
                    if(isNegReverse){
                        negScore++;
                    }else{
                        posScore++;
                    }
                }else if(negOpinionDic.contains(word) || negEmotionDic.contains(word)){
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
            return POS;
        else if (maxPosScore < maxNegScore)
            return NEG;
        else
            return OTHER;
    }

    // ��ȡ��д����0��������дʻ�۵�ʣ�+-1���۵�ʣ�+-2����д�
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
