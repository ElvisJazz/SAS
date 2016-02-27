package cn.edu.seu.extractor;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;

import java.io.*;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 15-3-13
 * Time: 下午4:37
 * To change this template use File | Settings | File Templates.
 */
public class CorpusExtractor {
    public void extractorAll(String readPhraseDir, String readDepDir, String outputDir){
        File file = new File(outputDir);
        if(!file.exists()) {
            if(!file.mkdirs()){
                System.out.println("创建目录失败！");
                return;
            }
        }

        File[] readPhraseFileArray = (new File(readPhraseDir)).listFiles();
        File[] readDepFileArray = (new File(readDepDir)).listFiles();
        // 若数量不对等，则返回
        if(readPhraseFileArray.length !=  readDepFileArray.length) {
            System.out.println("词组和依存关系文件数目不匹配！");
            return;
        }

        for(int i=0; i<readPhraseFileArray.length; ++i){
            extractorFile(readPhraseFileArray[i].getAbsolutePath(), readDepFileArray[i].getAbsolutePath(), outputDir+"//"+readDepFileArray[i].getName(),readDepFileArray[i].getName());
        }
    }

    public void extractorFile(String readPhraseFilePath, String readDepFilePath, String outputFilePath, String fileName){
        BufferedWriter writer = null;
        BufferedReader bufferPhraseReader = null, bufferDepReader = null;
        try{
            File phraseFile = new File(readPhraseFilePath);
            File depFile = new File(readDepFilePath);
            File outputFile = new File(outputFilePath);
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
            bufferPhraseReader = new BufferedReader(new InputStreamReader(new FileInputStream(phraseFile), "UTF-8"));
            bufferDepReader = new BufferedReader(new InputStreamReader(new FileInputStream(depFile), "UTF-8"));

            String phraseSentence="", depSentence="";
            int i = 0;
            HashMap<Integer, LinkedHashMultimap<String, String>> outputMap = new HashMap();
            while((phraseSentence=bufferPhraseReader.readLine())!=null && (depSentence = bufferDepReader.readLine())!=null){
                // 分别读取词组句子和关系句子，输出目标元组（评价对象，情感词）
                TargetExtractor extractor = new TargetExtractor();
                extractor.extractPotentialWords(phraseSentence);
                extractor.readRelation(depSentence);
                outputMap.put(i, extractor.extract());
                i++;
            }
            // 输出名词-情感词关系对，以空格隔开
            HashMultimap<String, String> subMap = null;
            for(HashMap.Entry entry : outputMap.entrySet()){
                subMap = (HashMultimap<String, String>)entry.getValue();
                // 输出每句的关系对
                for(HashMap.Entry entry1 : subMap.entries()){
                    writer.write("["+entry1.getKey()+", "+entry1.getValue()+"]");
                }
                writer.write("\n");
            }
            System.out.println(fileName+"潜在对象和情感词抽取完成！");
        }catch (Exception e){
            e.printStackTrace();
        }
        finally {
            try{
                if(writer != null)
                    writer.close();
                if(bufferPhraseReader != null)
                    bufferPhraseReader.close();
                if(bufferDepReader != null)
                    bufferDepReader.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
