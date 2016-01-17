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
 * Time: ����7:10
 * To change this template use File | Settings | File Templates.
 */
public class DependencyParser {
    // ��������
    public void parseAll(LexicalizedParser lp, String readDir, String outputDir){
        File[] fileArray = (new File(readDir)).listFiles();
        File file = new File(outputDir);
        if(!file.exists()) {
            if(!file.mkdirs()){
                System.out.println("����Ŀ¼ʧ�ܣ�");
                return;
            }
        }

        for(int i=0; i<fileArray.length; ++i){
            parse(lp, fileArray[i].getAbsolutePath(), fileArray[i].getName(),outputDir);
        }
    }

    // �����ļ�����
    public void parse(LexicalizedParser lp, String filePath, String fileName, String outputDir){
       /* String[] arg2 = {"-maxLength", "140", "-encoding", "gbk", "-outputFormat", "typedDependenciesCollapsed", "source//xinhuaFactored.ser.gz",
                filePath, "-writeOutputFiles", "-outputFilesDirectory ", outputDir};
        LexicalizedParser.main(arg2);   */
        FileReader reader = null;
        FileWriter writer = null;
        BufferedReader br = null;
        try{
            // �ļ��������
            File file = new File(outputDir+"//"+fileName);
            if(file.exists()) {
                System.out.println(fileName+"�Ѵ��ڣ�");
                return;
            }
            writer = new FileWriter(file);
            reader = new FileReader(new File(filePath));
            br = new BufferedReader(reader);
            String line;
            // �ļ��䷨����
            TreebankLanguagePack tlp = new ChineseTreebankLanguagePack(); // a PennTreebankLanguagePack for English
            GrammaticalStructureFactory gsf = null;
            if (tlp.supportsGrammaticalStructures()) {
                gsf = tlp.grammaticalStructureFactory();
            }
            int i = 0;
            while((line=br.readLine()) != null)
            {
                ++i;
                //line = "�� ���ܵ� �� ���ܵ� �� �� ���� �� �Ϻ� �� ���� �� ���� �� �� �� һ �� ̰������ �� �� �� ���� ˵ �� ��";
                Tree parse = lp.parse(line);
                GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
                List tdl = gs.typedDependenciesCCprocessed();
                //System.out.println(tdl.toString());
                System.out.print(String.valueOf(i) + "\r");
                writer.write(tdl.toString()+"\n");
            }
            System.out.println(fileName+"�䷨����������");
        }catch (Exception e){
            e.printStackTrace();
        }
        finally {
            // �ر�д�ļ�
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
