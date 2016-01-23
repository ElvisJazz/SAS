package cn.edu.seu;

import edu.hit.ir.ltp4j.NER;
import edu.hit.ir.ltp4j.Parser;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.international.pennchinese.ChineseTreebankLanguagePack;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 16-1-20
 * Time: 上午11:56
 * To change this template use File | Settings | File Templates.
 */
public class NamedEntityRecognizer {
    public boolean init(){
        if(NER.create("ltp_data/ner.model") < 0) {
            System.err.println("load failed");
            return false;
        }
        return true;
    }

    // 清理
    public void destroy(){
        NER.release();
    }

    // 批量分析
    public void parseAll( String posDir, String outputDir){
        File[] fileArray = (new File(posDir)).listFiles();
        File file = new File(outputDir);
        if(!file.exists()) {
            if(!file.mkdirs()){
                System.out.println("创建目录失败！");
                return;
            }
        }

        for(int i=0; i<fileArray.length; ++i){
            parse(fileArray[i].getAbsolutePath(), fileArray[i].getName(),outputDir);
        }
    }

    // 单个文件分析
    public void parse(String filePath, String fileName, String outputDir){
        FileReader reader = null;
        FileWriter writer = null;
        BufferedReader br = null;
        try{
            // 文件输出变量
            File file = new File(outputDir+"//"+fileName);
            /*if(file.exists()) {
                System.out.println(fileName+"已存在！");
                return;
            }*/
            writer = new FileWriter(file);
            reader = new FileReader(new File(filePath));
            br = new BufferedReader(reader);
            String line;
            // 文件命名实体识别
            List<String> words = new ArrayList<String>();
            List<String> tags = new ArrayList<String>();
            List<String> ners = new ArrayList<String>();
            int size = 0, num = 0;
            while((line=br.readLine()) != null) {
                ++num;
                line = line.trim();
                if(line.equals(""))
                    continue;
                String[] trunks = line.split(" ");
                words.clear();
                tags.clear();
                ners.clear();
                for(String t : trunks){
                   int index = t.lastIndexOf('/');
                    words.add(t.substring(0, index));
                    tags.add(t.substring(index+1));
                }
                size = NER.recognize(words, tags, ners);
                for(int i = 0; i<size; i++) {
                    writer.write(words.get(i)+"/"+ners.get(i));
                    if(i != size-1) {
                        writer.write(" ");
                    }
                }
                writer.write("\n");
                System.out.print(String.valueOf(num) + "\r");
            }

            System.out.println(fileName+"命名实体识别完成");
        }catch (Exception e){
            e.printStackTrace();
        }
        finally {
            // 关闭写文件
            try {
                if(writer != null)
                    writer.close();
                if(reader != null)
                    reader.close();
                if(br != null)
                    br.close();
                Runtime.getRuntime().gc();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
