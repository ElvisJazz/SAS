package cn.edu.seu;

import cn.edu.seu.utils.AlignUtil;
import edu.stanford.nlp.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 15-3-16
 * Time: 下午10:31
 * To change this template use File | Settings | File Templates.
 */
public class Aligner {
    // 对情感输出结果进行格式调整输出
    public void alignAllResult(String alignCorpusDir, String objectCorpusDir, String alignLabelCorpusDir, String objectLabelCorpusDir,
                               String alignOffsetDir, String alignOriginalDir, String preDir, String outputDir){
        //AlignUtil.init();
        File file = new File(outputDir);
        if(!file.exists()) {
            if(!file.mkdirs()){
                System.out.println("创建目录失败！");
                return;
            }
        }

        File[] alignCorpusArray = (new File(alignCorpusDir)).listFiles();
        File[] objectCorpusArray = (new File(objectCorpusDir)).listFiles();
        File[] alignLabelCorpusArray = (new File(alignLabelCorpusDir)).listFiles();
        File[] objectLabelCorpusArray = (new File(objectLabelCorpusDir)).listFiles();
        File[] alignOffsetCorpusArray = (new File(alignOffsetDir)).listFiles();
        File[] alignOriginalArray = (new File(alignOriginalDir)).listFiles();
        File[] preArray = (new File(preDir)).listFiles();

        if(alignLabelCorpusArray.length!=objectLabelCorpusArray.length){
            System.out.println("对其目录文件数目不匹配！");
            return;
        }

        for(int i=0; i<objectLabelCorpusArray.length; ++i){
            alignResultFile(alignCorpusArray[i].getAbsolutePath(), objectCorpusArray[i].getAbsolutePath(),
                    alignLabelCorpusArray[i].getAbsolutePath(),objectLabelCorpusArray[i].getAbsolutePath(),
                    alignOffsetCorpusArray[i].getAbsolutePath(), alignOriginalArray[i].getAbsolutePath(),preArray[i].getAbsolutePath(),
                    outputDir + "//" + objectCorpusArray[i].getName(), objectCorpusArray[i].getName());
        }
        //AlignUtil.destroy();
    }
    // 对齐结果文件
    public void alignResultFile(String alignFilePath, String objectFilePath,String alignLabelFilePath, String objectLabelFilePath,
                                String alignOffsetFilePath, String alignOriginalFilePath, String preFilePath, String outputFilePath, String fileName){
        FileReader alignLabelFileReader = null;
        FileReader objectLabelFileReader = null;
        BufferedReader alignFileBufferReader = null;
        BufferedReader objectFileBufferReader = null;
        BufferedReader alignOffsetFileBufferReader = null;
        BufferedReader alignOriginalFileBufferReader = null;
        BufferedReader preFileBufferReader = null;
        FileWriter writer = null;
        try{
            // 初始化读写文件变量
            alignFileBufferReader = new BufferedReader(new FileReader(new File(alignFilePath)));
            objectFileBufferReader = new BufferedReader(new FileReader(new File(objectFilePath)));
            alignOffsetFileBufferReader = new BufferedReader(new FileReader(new File(alignOffsetFilePath)));
            alignOriginalFileBufferReader = new BufferedReader(new FileReader(new File(alignOriginalFilePath)));
            preFileBufferReader = new BufferedReader(new FileReader(new File(preFilePath)));
            File alignLabelFile = new File(alignLabelFilePath);
            File objectLabelFile = new File(objectLabelFilePath);
            alignLabelFileReader = new FileReader(alignLabelFile);
            objectLabelFileReader = new FileReader(objectLabelFile);
            writer = new FileWriter(new File(outputFilePath));

            // 读取对齐文微博号
            int length = (int)alignLabelFile.length();
            char[] alignLabelBuffer = new char[length];
            alignLabelFileReader.read(alignLabelBuffer);
            String alignLabelStr = String.valueOf(alignLabelBuffer);
            String[] alignLabelArray = alignLabelStr.split("\n");
            // 读取结果文件微博号
            length = (int)objectLabelFile.length();
            char[] objectLabelBuffer = new char[length];
            objectLabelFileReader.read(objectLabelBuffer);
            String objectLabelStr = String.valueOf(objectLabelBuffer);
            String[] objectLabelArray = objectLabelStr.split("\n");

            // 存储临时结果
            Map<Integer, Pair<Integer, String>> tmpResultMap = new TreeMap<Integer, Pair<Integer, String>>();
            int index1 = 0, index2 = 0, index3 = 0;
            int startIndex = 0, endIndex = 0;
            String tmpWord = "", tmpLabel = "";
            String objectSentence = "", objectLabel = "", alignSentence = "", alignLabel = "", originalSentence = "", preSentence = "";
            int alignOffset;
            int i = 0, j = 0;
            // 根据对齐文件对结果进行对齐并输出
            while(true){
                // 读取每行
                objectSentence = objectFileBufferReader.readLine();
                if(i >= objectLabelArray.length)
                    break;
                objectLabel = objectLabelArray[i++];
                while(!alignLabel.equals(objectLabel) && j<alignLabelArray.length){
                    alignLabel = alignLabelArray[j];
                    alignSentence = alignFileBufferReader.readLine();
                    ++j;
                }

                if(alignSentence==null || objectSentence==null)
                    break;

                alignOffset = Integer.parseInt(alignOffsetFileBufferReader.readLine());
                originalSentence  = alignOriginalFileBufferReader.readLine();
                preSentence = preFileBufferReader.readLine();

                if("".equals(objectSentence)){
                    writer.write("\n");
                    continue;
                }
                // 循环读取句子中的情感词对
                while(true){
                    index1 = objectSentence.indexOf("[", index3);
                    index2 = objectSentence.indexOf(", ", index1);
                    index3 = objectSentence.indexOf("]", index2);
                    if(index1!=-1 && index2!=-1 && index3!=-1){
                        tmpWord = objectSentence.substring(index1+1, index2);
                        tmpLabel = objectSentence.substring(index2+2, index3);
                        // 分词处理
                        CorpusSegmenter segmenter = new CorpusSegmenter();
                        String segSentence = segmenter.segmentSentence(alignSentence, true);
                        // 处理无评价对象的情况
                        /*if("#".equals(tmpWord)){
                            tmpWord = getTargetObjectFormNeighborhood(segSentence);
                            //tmpWord = AlignUtil.getNeighborTarget(originalSentence, alignOffset, preSentence.length());
                            if(tmpWord == null)
                                continue;
                        }*/
                        // 处理偏移问题
                        startIndex = alignSentence.indexOf(tmpWord, alignOffset);
                        if(startIndex == -1)
                            startIndex = alignSentence.lastIndexOf(tmpWord, alignOffset);
                        endIndex = startIndex + tmpWord.length() - 1;
                        if(startIndex != -1)
                            tmpResultMap.put(startIndex, new Pair<>(endIndex, tmpLabel));
                    }else{
                        for(Integer num1 : tmpResultMap.keySet()) {
                            writer.write("["+String.valueOf(num1)+","+String.valueOf(tmpResultMap.get(num1).first)+","+tmpResultMap.get(num1).second+"]");
                        }
                        writer.write("\n");
                        tmpResultMap.clear();
                        index3 = 0;
                        break;
                    }
                }
            }
            System.out.println(fileName + "对齐完成！");
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            try{
                if(alignFileBufferReader!=null)
                    alignFileBufferReader.close();
                if(objectFileBufferReader!=null)
                    objectFileBufferReader.close();
                if(alignLabelFileReader!=null)
                    alignLabelFileReader.close();
                if(objectLabelFileReader!=null)
                    objectLabelFileReader.close();
                /*alignLabelFileBufferReader.close();
                objectLabelFileBufferReader.close();*/
                if(writer != null)
                writer.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    // 从上下文语句中获取评价对象
    public String getTargetObjectFormNeighborhood(String segSentence){
        String word = null;
        // 将短语合并
        PhraseProducer producer = new PhraseProducer();
        StringBuffer buffer = producer.produceLine(segSentence);
        // 寻找第一个名词或名词短语
        int index = buffer.indexOf("/n");
        int index0 = -1;
        if(index != -1){
            index0 = buffer.lastIndexOf(" ", index);
            if(index0 != -1)
                word = buffer.substring(index0+1, index);
            else
                word = buffer.substring(0, index);
        }
        // 处理《》
        if(word.startsWith("《") && word.endsWith("》"))
            word = word.substring(1, word.length()-1);
        return word;
    }
}
