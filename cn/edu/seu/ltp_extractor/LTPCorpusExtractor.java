package cn.edu.seu.ltp_extractor;

import com.google.common.collect.HashMultimap;

import java.io.*;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 15-3-13
 * Time: 下午4:37
 * To change this template use File | Settings | File Templates.
 */
public class LTPCorpusExtractor {
    public void extractorAll(String segDir, String depDir, String srDir,
                             String segTopicDir, String depTopicDir, String srTopicDir,
                             String alignOriginalDir, String outputDir){
        File file = new File(outputDir);
        if(!file.exists()) {
            if(!file.mkdirs()){
                System.out.println("创建目录失败！");
                return;
            }
        }
        LTPTargetExtractor.init();

        File[] readSegFileArray = (new File(segDir)).listFiles();
        File[] readDepFileArray = (new File(depDir)).listFiles();
        File[] readSrFileArray = (new File(srDir)).listFiles();
        File[] readSegTopicFileArray = (new File(segTopicDir)).listFiles();
        File[] readDepTopicFileArray = (new File(depTopicDir)).listFiles();
        File[] readSrTopicFileArray = (new File(srTopicDir)).listFiles();
        File[] alignOriginalFileArray = (new File(alignOriginalDir)).listFiles();
        // 若数量不对等，则返回
        if(readSegFileArray.length !=  readDepFileArray.length || readDepFileArray.length !=  readSrFileArray.length
                || readSegTopicFileArray.length !=  readDepTopicFileArray.length || readDepTopicFileArray.length !=  readSrTopicFileArray.length
                || readDepTopicFileArray.length !=  alignOriginalFileArray.length) {
            System.out.println("命名实体、依存关系和语义角色文件数目不匹配！");
            return;
        }

        for(int i=0; i<readSegFileArray.length; ++i){
            extractorFile(readSegFileArray[i].getAbsolutePath(), readDepFileArray[i].getAbsolutePath(), readSrFileArray[i].getAbsolutePath(),
                    readSegTopicFileArray[i].getAbsolutePath(), readDepTopicFileArray[i].getAbsolutePath(), readSrTopicFileArray[i].getAbsolutePath(),
                    alignOriginalFileArray[i].getAbsolutePath(), outputDir+"//"+readSegFileArray[i].getName(),readSegFileArray[i].getName());
        }

        LTPTargetExtractor.destroy();
    }

    public void extractorFile(String segPath, String depPath, String srPath,
                              String segTopicPath, String depTopicPath, String srTopicPath,
                              String alignOriginalFilePath, String outputFilePath, String fileName){
        BufferedWriter writer = null;
        BufferedReader bufferSegReader = null, bufferDepReader = null, bufferSrReader = null, bufferOriginalReader = null;
        BufferedReader bufferSegTopicReader = null, bufferDepTopicReader = null, bufferSrTopicReader = null;
        try{
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputFilePath)), "UTF-8"));

            bufferSegReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(segPath)), "UTF-8"));
            bufferDepReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(depPath)), "UTF-8"));
            bufferSrReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(srPath)), "UTF-8"));
            bufferSegTopicReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(segTopicPath)), "UTF-8"));
            bufferDepTopicReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(depTopicPath)), "UTF-8"));
            bufferSrTopicReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(srTopicPath)), "UTF-8"));
            bufferOriginalReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(alignOriginalFilePath)), "UTF-8"));

            String segSentence="", depSentence="", srSentence="";
            String segTopicSentence="", depTopicSentence="", srTopicSentence="", originalSentence="";
            int i = 0;
            HashMap<Integer, HashMultimap<String, String>> outputMap = new HashMap();
            while((segSentence=bufferSegReader.readLine())!=null && (depSentence = bufferDepReader.readLine())!=null && (srSentence = bufferSrReader.readLine())!=null && (originalSentence=bufferOriginalReader.readLine())!=null
                    && (segTopicSentence=bufferSegTopicReader.readLine())!=null && (depTopicSentence = bufferDepTopicReader.readLine())!=null && (srTopicSentence = bufferSrTopicReader.readLine())!=null){
                // 分别读取分词（包括了命名实体识别）、依存关系、语义角色文件，输出目标元组（评价对象，情感词）
                LTPTargetExtractor extractor = new LTPTargetExtractor();
                extractor.readTopicCorpusSentence(segTopicSentence, depTopicSentence, srTopicSentence);
                extractor.readContentCorpusSentence(segSentence, depSentence, srSentence, originalSentence);
                //System.out.println(segSentence);
                outputMap.put(i, extractor.extract());
                i++;
            }
            // 输出名词-情感词关系对，以空格隔开
            HashMultimap<String, String> subMap = null;
            String opinion, mark;
            int index1, index2;
            for(HashMap.Entry entry : outputMap.entrySet()){
                subMap = (HashMultimap<String, String>)entry.getValue();
                // 输出每句的关系对
                for(HashMap.Entry<String,String> entry1 : subMap.entries()){
                    opinion = entry1.getValue();
                    index1 = opinion.lastIndexOf("-%%");
                    if(index1!=-1){
                        index2 = opinion.lastIndexOf("%%-", index1);
                        mark = opinion.substring(index2+3, index1);
                        opinion = opinion.substring(0, index2);
                        writer.write("["+entry1.getKey()+", "+opinion+"]("+mark+")");
                    }
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
                if(bufferSrReader != null)
                    bufferSrReader.close();
                if(bufferSegReader != null)
                    bufferSegReader.close();
                if(bufferDepReader != null)
                    bufferDepReader.close();
                if(bufferSrTopicReader != null)
                    bufferSrTopicReader.close();
                if(bufferSegTopicReader != null)
                    bufferSegTopicReader.close();
                if(bufferDepTopicReader != null)
                    bufferDepTopicReader.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
