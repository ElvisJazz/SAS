package extractor;

import com.google.common.collect.HashMultimap;
import edu.stanford.nlp.util.Pair;

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
public class CorpusExtractor {
    public void extractorAll(String readPhraseDir, String readDepDir, String outputDir){
        File file = new File(outputDir);
        if(!file.exists()) {
            if(!file.mkdirs()){
                System.out.println("����Ŀ¼ʧ�ܣ�");
                return;
            }
        }

        File[] readPhraseFileArray = (new File(readPhraseDir)).listFiles();
        File[] readDepFileArray = (new File(readDepDir)).listFiles();
        // ���������Եȣ��򷵻�
        if(readPhraseFileArray.length !=  readDepFileArray.length) {
            System.out.println("����������ϵ�ļ���Ŀ��ƥ�䣡");
            return;
        }

        for(int i=0; i<readPhraseFileArray.length; ++i){
            extractorFile(readPhraseFileArray[i].getAbsolutePath(), readDepFileArray[i].getAbsolutePath(), outputDir+"//"+readDepFileArray[i].getName(),readDepFileArray[i].getName());
        }
    }

    public void extractorFile(String readPhraseFilePath, String readDepFilePath, String outputFilePath, String fileName){
        FileReader phraseReader = null, depReader = null;
        FileWriter writer = null;
        BufferedReader bufferPhraseReader = null, bufferDepReader = null;
        try{
            File phraseFile = new File(readPhraseFilePath);
            File depFile = new File(readDepFilePath);
            File outputFile = new File(outputFilePath);
            phraseReader = new FileReader(phraseFile);
            depReader = new FileReader(depFile);
            writer = new FileWriter(outputFile);
            bufferPhraseReader = new BufferedReader(phraseReader);
            bufferDepReader = new BufferedReader(depReader);

            String phraseSentence="", depSentence="";
            int i = 0;
            HashMap<Integer, HashMultimap<String, String>> outputMap = new HashMap();
            while((phraseSentence=bufferPhraseReader.readLine())!=null && (depSentence = bufferDepReader.readLine())!=null){
                // �ֱ��ȡ������Ӻ͹�ϵ���ӣ����Ŀ��Ԫ�飨���۶�����дʣ�
                TargetExtractor extractor = new TargetExtractor();
                extractor.extractPotentialWords(phraseSentence);
                extractor.readRelation(depSentence);
                outputMap.put(new Integer(i), extractor.extract());
                i++;
                if(i==20)
                    System.out.print("");
            }
            // �������-��дʹ�ϵ�ԣ��Կո����
            Pair<Integer, Integer> outputPair = null;
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
                if(phraseReader != null)
                    phraseReader.close();
                if(depReader != null)
                    depReader.close();
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
