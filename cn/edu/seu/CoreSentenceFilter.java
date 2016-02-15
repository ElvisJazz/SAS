package cn.edu.seu;

import edu.stanford.nlp.util.Pair;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 16-2-16
 * Time: 上午12:01
 * To change this template use File | Settings | File Templates.
 */
public class CoreSentenceFilter {
    // 句式集合
    public static Set<Pair<String,String>> AXBDic = new HashSet<>();
    public static Set<String> statementDic = new HashSet<>();
    public static Set<String> contrastDic = new HashSet<>();

    // 类型
    public final static String AXB = "axb";
    public final static String STA = "sta";
    public final static String CON = "con";

    // 读取核心过滤规则词典
    public void readDic(String dicPath){
        // 情感句块过滤词典
        Map<String, Set> setMap = new HashMap<>();
        setMap.put(AXB, AXBDic);
        setMap.put(STA, statementDic);
        setMap.put(CON, contrastDic);
        String tmp;
        String[] line;
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(dicPath)),"GBK"));
            while((tmp=reader.readLine()) != null){
                line = tmp.trim().split(" ");
                if(AXB.equals(line[0])){
                    setMap.get(AXB).add(new Pair<>(line[1], line[2]));
                }else{
                    setMap.get(line[0]).add(line[1]);
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 过滤"就...而言，据...报道"之类的句块
    public static String filterByAXBRule(String sentence){

        return sentence;
    }

    // 过滤"说，认为，主张"之类的陈述性句块
    public static String filterByStatementRule(String sentence){

        return sentence;
    }

    // 转折句式，根据情况保留转折成分
    public static String filterByContrastRule(String sentence){

        return sentence;
    }
}
