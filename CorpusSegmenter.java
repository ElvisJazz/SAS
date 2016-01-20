import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Native;
import edu.hit.ir.ltp4j.Postagger;
import edu.hit.ir.ltp4j.Segmentor;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 15-3-10
 * Time: 下午3:58
 * To change this template use File | Settings | File Templates.
 */
public class CorpusSegmenter {
    public boolean useLTPSeg = false;
    public boolean useLTPPos = false;

    // 定义接口CLibrary，继承自com.sun.jna.Library
    public interface CLibrary extends Library{
        CLibrary Instance = (CLibrary) Native.loadLibrary("NLPIR", CLibrary.class);
        // 初始化函数声明
        public int NLPIR_Init(byte[] sDataPath, int encoding, byte[] sLicenceCode);
        //执行分词函数声明
        public String NLPIR_ParagraphProcess(String sSrc, int bPOSTagged);
        //提取关键词函数声明
        public String NLPIR_GetKeyWords(String sLine, int nMaxKeyLimit, boolean bWeightOut);
        //退出函数声明
        public void NLPIR_Exit();
    }

    //  初始化
    public boolean init(){
        if(useLTPSeg){
            if(Segmentor.create("ltp_data/cws.model") < 0){
                System.err.println("load failed");
                return false;
            }
        }

        if(useLTPPos){
            if(Postagger.create("ltp_data/pos.model") < 0) {
                System.err.println("load failed");
                return false;
            }
        }

        if(!useLTPSeg || !useLTPPos){
            try{
                String argu = "";
                String system_charset = "GBK";
                int charset_type = 1;
                int init_flag = 0;
                init_flag = CLibrary.Instance.NLPIR_Init(argu.getBytes(system_charset), charset_type, "0".getBytes(system_charset));

                if (0 == init_flag) {
                    System.err.println("初始化失败！");
                    return false;
                }
            }catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    // 销毁
    public void destroy(){
        if(useLTPSeg)
            Segmentor.release();
        if(useLTPPos)
            Postagger.release();
        if(!useLTPSeg || !useLTPPos)
            CLibrary.Instance.NLPIR_Exit();
    }

    // 批量分词
    public void batchSegment(String readDir, String outputDir, boolean isPos){
        File[] fileArray = (new File(readDir)).listFiles();
        for(int i=0; i<fileArray.length; ++i){
            segment(fileArray[i].getAbsolutePath(), isPos, outputDir);
        }
    }

    // 分词
    public void segment(String fileName, boolean isPos, String outputDir){
        Writer writer = null;
        FileReader reader = null;
        try {
            // 读取待分词文本
            File file = new File(fileName);
            reader = new FileReader(file);
            int length = (int)file.length();
            char[] charBuffer = new char[length];
            reader.read(charBuffer);
            String sInput = String.valueOf(charBuffer);

            // 输出分词结果
            File outputFile = null;
            StringBuilder outputFileName = new StringBuilder(file.getName());
            if(isPos){
                outputFile = new File(outputDir+"//"+outputFileName);
            }
            else {
                outputFile = new File(outputDir+"//"+outputFileName);
            }

            // 创建输出文件
            if(!outputFile.getParentFile().exists()){
                if(!outputFile.getParentFile().mkdirs())
                    throw new Exception("创建分词输出目录失败！");
            }
            writer = new OutputStreamWriter(new FileOutputStream(outputFile.getPath(), false), "GBK");
            String nativeBytes = "";
            if(useLTPSeg && !isPos){
                List<String> words = new ArrayList<String>();
                int size = 0, index = 0;
                String[] sentences = sInput.split("\n");
                for(String s : sentences){
                    if(index == sentences.length-1)
                        break;
                    index++;
                    size = Segmentor.segment(s, words);
                    for(int i = 0; i<size; i++) {
                        nativeBytes += words.get(i);
                        if(i == size-1) {
                            nativeBytes += '\n';
                        } else{
                            nativeBytes += ' ';
                        }
                    }
                    words.clear();
                }
            }else if(useLTPPos && isPos){
                List<String> words;
                List<String> postags = new ArrayList<String>();
                int size = 0, index = 0;
                String[] sentences = sInput.split("\n");
                for(String s : sentences){
                    if(index == sentences.length-1)
                        break;
                    index++;
                    postags.clear();
                    words = Arrays.asList(s.split(" "));
                    size = Postagger.postag(words, postags);
                    for(int i = 0; i < size; i++) {
                        nativeBytes += (words.get(i)+"/"+postags.get(i));
                        if(i == size-1) {
                            nativeBytes += '\n';
                        } else{
                            nativeBytes += ' ';
                        }
                    }
                }
            }else{
                if(isPos)
                    nativeBytes = CLibrary.Instance.NLPIR_ParagraphProcess(sInput, 1);
                else
                    nativeBytes = CLibrary.Instance.NLPIR_ParagraphProcess(sInput, 0);
            }
            writer.write(nativeBytes);

            System.out.println(outputFileName + "分词完成！");

        }catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            // 关闭写文件
            try {
                if(writer != null)
                    writer.close();
                if(reader != null)
                    reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 分词
    public String segmentSentence(String sentence, boolean isPos){
        try {
                String argu = "";
                String system_charset = "GBK";
                int charset_type = 1;
                int init_flag = 0;
                init_flag = CLibrary.Instance.NLPIR_Init(argu.getBytes(system_charset), charset_type, "0".getBytes(system_charset));

                if (0 == init_flag) {
                    System.err.println("初始化失败！");
                    return null;
                }

                String nativeBytes = null;
                if(isPos)
                    nativeBytes = CLibrary.Instance.NLPIR_ParagraphProcess(sentence, 1);
                else
                    nativeBytes = CLibrary.Instance.NLPIR_ParagraphProcess(sentence, 0);

               return nativeBytes;
        }catch (Exception e) {
              e.printStackTrace();
        }
        return null;
    }
}
