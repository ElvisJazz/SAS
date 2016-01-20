import edu.hit.ir.ltp4j.Pair;
import edu.hit.ir.ltp4j.Parser;
import edu.hit.ir.ltp4j.SRL;
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
 * Time: 上午12:47
 * To change this template use File | Settings | File Templates.
 */
public class SemanticParser {
    // 初始化
    public boolean init(){
        if(SRL.create("ltp_data/srl") < 0){
            System.err.println("load failed");
            return false;
        }
         return true;
    }

    // 清理
    public void destroy(){
        SRL.release();
    }

    // 批量分析
    public void parseAll(String segDir,String depDir, String nerDir, String outputDir){
        File[] segFileArray = (new File(segDir)).listFiles();
        File[] nerFileArray = (new File(nerDir)).listFiles();
        File[] depFileArray = (new File(depDir)).listFiles();
        File outputFile = new File(outputDir);
        if(!outputFile.exists()) {
            if(!outputFile.mkdirs()){
                System.out.println("创建语义分析结果目录失败！");
                return;
            }
        }

        for(int i=0; i<segFileArray.length; ++i){
            parse(segFileArray[i].getName(),
                    segFileArray[i].getAbsolutePath(),
                    depFileArray[i].getAbsolutePath() ,
                    nerFileArray[i].getAbsolutePath() ,
                    outputDir);
        }
    }

    // 单个文件分析
    public void parse(String fileName, String segFilePath, String depFilePath, String nerFilePath, String outputDir){
        FileReader segReader = null;
        FileReader nerReader = null;
        FileReader depReader = null;
        FileWriter writer = null;
        BufferedReader segBr = null;
        BufferedReader depBr = null;
        BufferedReader nerBr = null;
        try{
            // 文件输出变量
            File file = new File(outputDir+"//"+fileName);
            writer = new FileWriter(file);
            segReader = new FileReader(new File(segFilePath));
            nerReader = new FileReader(new File(nerFilePath));
            depReader = new FileReader(new File(depFilePath));
            segBr = new BufferedReader(segReader);
            depBr = new BufferedReader(depReader);
            nerBr = new BufferedReader(nerReader);
            String segLine, nerLine, depLine;
            // 文件语义分析
            List<String> words = new ArrayList<String>();
            List<String> tags = new ArrayList<String>();
            List<String> ners = new ArrayList<String>();
            List<Integer> heads = new ArrayList<Integer>();
            List<String> deps = new ArrayList<String>();
            int size = 0, num = 0, index = 0, index2 = 0, n = 0;
            String tmp = "";
            while((segLine=segBr.readLine()) != null) {
                ++num;
                // 读取分词和词性
                String[] trunks = segLine.split(" ");
                words.clear();
                tags.clear();
                ners.clear();
                deps.clear();
                heads.clear();
                for(String t : trunks){
                    index = t.lastIndexOf('/');
                    words.add(t.substring(0, index));
                    tags.add(t.substring(index+1));
                }
                // 读取命名实体
                nerLine = nerBr.readLine();
                String[] nerTrunks = nerLine.split(" ");
                for(String ner : nerTrunks){
                    ners.add(ner);
                }
                // 读取依存关系
                depLine = depBr.readLine();
                depLine = depLine.substring(1, depLine.length()-1);
                String[] depTrunks = depLine.split("\\), ");
                for(String dep : depTrunks){
                    index = dep.indexOf('(');
                    deps.add(dep.substring(0, index));
                    index2 = dep.indexOf(", ", index+1);
                    index = dep.lastIndexOf('-', index2);
                    n = Integer.valueOf(dep.substring(index+1, index2)) - 1;
                    heads.add(n);
                }
                // 语义分析
                List<Pair<Integer, List<Pair<String, Pair<Integer, Integer>>>>> srls = new ArrayList<Pair<Integer, List<Pair<String, Pair<Integer, Integer>>>>>();
                SRL.srl(words, tags, ners, heads, deps, srls);
                for (int i = 0; i < srls.size(); ++i) {
                    writer.write(srls.get(i).first + ":");
                    for (int j = 0; j < srls.get(i).second.size(); ++j) {
                        writer.write(" type="+ srls.get(i).second.get(j).first + " beg="+ srls.get(i).second.get(j).second.first + " end="+ srls.get(i).second.get(j).second.second);
                    }
                    writer.write(";");
                }
                writer.write("\n");
            }
            System.out.println(fileName+"语义分析完成");
        }catch (Exception e){
            e.printStackTrace();
        }
        finally {
            // 关闭写文件
            try {
                if(writer != null)
                    writer.close();
                if(segReader != null)
                    segReader.close();
                if(nerReader != null)
                    nerReader.close();
                if(depReader != null)
                    depReader.close();
                if(segBr != null)
                    segBr.close();
                if(nerBr != null)
                    nerBr.close();
                if(depBr != null)
                    depBr.close();
                Runtime.getRuntime().gc();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
