import edu.hit.ir.ltp4j.Parser;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.trees.international.pennchinese.ChineseTreebankLanguagePack;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 15-3-10
 * Time: ����7:10
 * To change this template use File | Settings | File Templates.
 */
public class DependencyParser {
    public boolean useLTPDep = false;

    private LexicalizedParser lp;

    // ��ʼ��
    public boolean init(){
        if(useLTPDep){
            if(Parser.create("ltp_data/parser.model")<0) {
                System.err.println("load failed");
                return false;
            }
        } else{
            String[] options = { "-maxLength", "400", "-MAX_ITEMS","2000000"};
            lp = LexicalizedParser.loadModel("dependencyModel//xinhuaFactored.ser.gz", options);
        }
        return true;
    }

    // ����
    public void destroy(){
        if(useLTPDep)
            Parser.release();
    }

    // ��������
    public void parseAll( String readDir, String outputDir){
        File[] fileArray = (new File(readDir)).listFiles();
        File file = new File(outputDir);
        if(!file.exists()) {
            if(!file.mkdirs()){
                System.out.println("����Ŀ¼ʧ�ܣ�");
                return;
            }
        }

        for(int i=0; i<fileArray.length; ++i){
            parse(fileArray[i].getAbsolutePath(), fileArray[i].getName(),outputDir);
        }
    }

    // �����ļ�����
    public void parse(String filePath, String fileName, String outputDir){
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
            if(useLTPDep){
                List<String> words = new ArrayList<String>();
                List<String> tags = new ArrayList<String>();
                List<Integer> heads = new ArrayList<Integer>();
                List<String> deprels = new ArrayList<String>();
                int size = 0, num = 0;
                int head = 0;
                StringBuffer buf;
                while((line=br.readLine()) != null) {
                    ++num;
                    String[] trunks = line.split(" ");
                    words.clear();
                    tags.clear();
                    heads.clear();
                    deprels.clear();
                    for(String t : trunks){
                        int index = t.lastIndexOf('/');
                        words.add(t.substring(0, index));
                        tags.add(t.substring(index+1));
                    }
                    size = Parser.parse(words,tags,heads,deprels);
                    writer.write("[");
                    for(int i = 0; i<size; i++) {
                        head = heads.get(i);
                        buf = new StringBuffer();
                        buf.append(deprels.get(i));
                        buf.append('(');
                        if(head == 0)
                            buf.append("ROOT");
                        else
                            buf.append(words.get(head-1));
                        buf.append('-');
                        buf.append(String.valueOf(head));
                        buf.append(", ");
                        buf.append(words.get(i));
                        buf.append('-');
                        buf.append(String.valueOf(i+1));
                        buf.append(')');
                        writer.write(buf.toString());
                        if(i == size-1) {
                            writer.write("]\n");
                        }
                        else{
                            writer.write(", ");
                        }
                    }
                    System.out.print(String.valueOf(num) + "\r");
                }
            } else{
                TreebankLanguagePack tlp = new ChineseTreebankLanguagePack(); // a PennTreebankLanguagePack for English
                GrammaticalStructureFactory gsf = null;
                if (tlp.supportsGrammaticalStructures()) {
                    gsf = tlp.grammaticalStructureFactory();
                }
                int i = 0;
                while((line=br.readLine()) != null)
                {
                    ++i;
                    Tree parse = lp.parse(line);
                    GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
                    List tdl = gs.typedDependenciesCCprocessed();
                    System.out.print(String.valueOf(i) + "\r");
                    writer.write(tdl.toString()+"\n");
                }
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

    public static void main(String args[]){
        DependencyParser parser = new DependencyParser();
        String[] options = { "-maxLength", "400", "-MAX_ITEMS","2000000"};
        LexicalizedParser lp = LexicalizedParser.loadModel("dependencyModel//xinhuaFactored.ser.gz", options);

        TreebankLanguagePack tlp = new ChineseTreebankLanguagePack(); // a PennTreebankLanguagePack for English
        GrammaticalStructureFactory gsf = null;
        if (tlp.supportsGrammaticalStructures()) {
            gsf = tlp.grammaticalStructureFactory();
        }
        int i = 0;
        String line = "����Ժ ���� ���ǿ ���� �Ϻ� ����� ʱ ��� �� ֧�� �Ϻ� ���� ̽�� �»��� ��";
        Tree parse = lp.parse(line);
        GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
        List tdl = gs.typedDependenciesCCprocessed();
        System.out.println(tdl.toString());
    }
}
