import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.trees.international.pennchinese.ChineseTreebankLanguagePack;

import java.io.*;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 15-3-10
 * Time: 下午7:10
 * To change this template use File | Settings | File Templates.
 */
public class DependencyParser {
    // 批量分析
    public void parseAll(LexicalizedParser lp, String readDir, String outputDir){
        File[] fileArray = (new File(readDir)).listFiles();
        File file = new File(outputDir);
        if(!file.exists()) {
            if(!file.mkdirs()){
                System.out.println("创建目录失败！");
                return;
            }
        }

        for(int i=0; i<fileArray.length; ++i){
            parse(lp, fileArray[i].getAbsolutePath(), fileArray[i].getName(),outputDir);
        }
    }

    // 单个文件分析
    public void parse(LexicalizedParser lp, String filePath, String fileName, String outputDir){
       /* String[] arg2 = {"-maxLength", "140", "-encoding", "gbk", "-outputFormat", "typedDependenciesCollapsed", "source//xinhuaFactored.ser.gz",
                filePath, "-writeOutputFiles", "-outputFilesDirectory ", outputDir};
        LexicalizedParser.main(arg2);   */
        FileReader reader = null;
        FileWriter writer = null;
        BufferedReader br = null;
        try{
            // 文件输出变量
            File file = new File(outputDir+"//"+fileName);
            if(file.exists()) {
                System.out.println(fileName+"已存在！");
                return;
            }
            writer = new FileWriter(file);
            reader = new FileReader(new File(filePath));
            br = new BufferedReader(reader);
            String line;
            // 文件句法分析
            TreebankLanguagePack tlp = new ChineseTreebankLanguagePack(); // a PennTreebankLanguagePack for English
            GrammaticalStructureFactory gsf = null;
            if (tlp.supportsGrammaticalStructures()) {
                gsf = tlp.grammaticalStructureFactory();
            }
            int i = 0;
            while((line=br.readLine()) != null)
            {
                ++i;
                //line = "最 腐败的 、 无能的 就 是 北京 、 上海 、 深圳 和 广州 ， 都 是 一 伙 贪官污吏 ， 这 是 不用 说 的 。";
                Tree parse = lp.parse(line);
                GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
                List tdl = gs.typedDependenciesCCprocessed();
                //System.out.println(tdl.toString());
                System.out.print(String.valueOf(i) + "\r");
                writer.write(tdl.toString()+"\n");
            }
            System.out.println(fileName+"句法依存分析完成");
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
