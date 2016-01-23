package cn.edu.seu.ltp_extractor;

import com.google.common.collect.HashMultimap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 15-3-13
 * Time: ����4:37
 * To change this template use File | Settings | File Templates.
 */
public class LTPCorpusExtractor {
    public void extractorAll(String segDir, String depDir, String srDir,
                             String segTopicDir, String depTopicDir, String srTopicDir,
                             String outputDir){
        File file = new File(outputDir);
        if(!file.exists()) {
            if(!file.mkdirs()){
                System.out.println("����Ŀ¼ʧ�ܣ�");
                return;
            }
        }

        File[] readSegFileArray = (new File(segDir)).listFiles();
        File[] readDepFileArray = (new File(depDir)).listFiles();
        File[] readSrFileArray = (new File(srDir)).listFiles();
        File[] readSegTopicFileArray = (new File(segTopicDir)).listFiles();
        File[] readDepTopicFileArray = (new File(depTopicDir)).listFiles();
        File[] readSrTopicFileArray = (new File(srTopicDir)).listFiles();
        // ���������Եȣ��򷵻�
        if(readSegFileArray.length !=  readDepFileArray.length || readDepFileArray.length !=  readSrFileArray.length
                || readSegTopicFileArray.length !=  readDepTopicFileArray.length || readDepTopicFileArray.length !=  readSrTopicFileArray.length) {
            System.out.println("����ʵ�塢�����ϵ�������ɫ�ļ���Ŀ��ƥ�䣡");
            return;
        }

        for(int i=0; i<readSegFileArray.length; ++i){
            extractorFile(readSegFileArray[i].getAbsolutePath(), readDepFileArray[i].getAbsolutePath(), readSrFileArray[i].getAbsolutePath(),
                    readSegTopicFileArray[i].getAbsolutePath(), readDepTopicFileArray[i].getAbsolutePath(), readSrTopicFileArray[i].getAbsolutePath(),
                    outputDir+"//"+readSegFileArray[i].getName(),readSegFileArray[i].getName());
        }
    }

    public void extractorFile(String segPath, String depPath, String srPath,
                              String segTopicPath, String depTopicPath, String srTopicPath,
                              String outputFilePath, String fileName){
        FileWriter writer = null;
        BufferedReader bufferSegReader = null, bufferDepReader = null, bufferSrReader = null;
        BufferedReader bufferSegTopicReader = null, bufferDepTopicReader = null, bufferSrTopicReader = null;
        try{
            writer = new FileWriter(new File(outputFilePath));
            bufferSegReader = new BufferedReader(new FileReader(new File(segPath)));
            bufferDepReader = new BufferedReader(new FileReader(new File(depPath)));
            bufferSrReader = new BufferedReader(new FileReader(new File(srPath)));
            bufferSegTopicReader = new BufferedReader(new FileReader(new File(segTopicPath)));
            bufferDepTopicReader = new BufferedReader(new FileReader(new File(depTopicPath)));
            bufferSrTopicReader = new BufferedReader(new FileReader(new File(srTopicPath)));

            String segSentence="", depSentence="", srSentence="";
            String segTopicSentence="", depTopicSentence="", srTopicSentence="";
            int i = 0;
            HashMap<Integer, HashMultimap<String, String>> outputMap = new HashMap();
            while((segSentence=bufferSegReader.readLine())!=null && (depSentence = bufferDepReader.readLine())!=null && (srSentence = bufferSrReader.readLine())!=null
                    && (segTopicSentence=bufferSegTopicReader.readLine())!=null && (depTopicSentence = bufferDepTopicReader.readLine())!=null && (srTopicSentence = bufferSrTopicReader.readLine())!=null){
                // �ֱ��ȡ�ִʣ�����������ʵ��ʶ�𣩡������ϵ�������ɫ�ļ������Ŀ��Ԫ�飨���۶�����дʣ�
                LTPTargetExtractor extractor = new LTPTargetExtractor();
                extractor.readContentCorpusSentence(segSentence, depSentence, srSentence);
                extractor.readTopicCorpusSentence(segTopicSentence, depTopicSentence, srTopicSentence);
                outputMap.put(i, extractor.extract());
                i++;
            }
            // �������-��дʹ�ϵ�ԣ��Կո����
            HashMultimap<String, String> subMap = null;
            for(HashMap.Entry entry : outputMap.entrySet()){
                subMap = (HashMultimap<String, String>)entry.getValue();
                // ���ÿ��Ĺ�ϵ��
                for(HashMap.Entry entry1 : subMap.entries()){
                    writer.write("["+entry1.getKey()+", "+entry1.getValue()+"]");
                }
                writer.write("\n");
            }
            System.out.println(fileName+"Ǳ�ڶ������дʳ�ȡ��ɣ�");
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
