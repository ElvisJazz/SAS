package cn.edu.seu;

import cn.edu.seu.utils.PunctuationUtil;
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

    // 读取核心过滤规则词典
    public static void readDic(String dicPath){
        // 情感句块过滤词典
        Map<String, Set> setMap = new HashMap<>();
        setMap.put(AXB, AXBDic);
        setMap.put(STA, statementDic);
        String tmp;
        String[] line;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(dicPath)),"GBK"));
            while((tmp=reader.readLine()) != null){
                if("".equals(tmp))
                    continue;
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
        }finally {
            if(reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    // 过滤
    public static String filter(String segSentence){
        segSentence = " " + segSentence + " ";
        String result = filterByAXBRule(segSentence);
        result = filterByStatementRule(result);
        return result.trim();
    }

    // 过滤"就...而言，据...报道"之类的句块
    private static String filterByAXBRule(String segSentence){
        int index1, index2, index3;
        boolean hasPunctuation;
        for(Pair<String,String> pair : AXBDic){
            index1 = segSentence.indexOf(" "+pair.first+"/");
            index2 = segSentence.indexOf(" "+pair.second+"/", index1+1);
            index3 = segSentence.indexOf(" ", index2+1);
            if(index1!=-1 && index2!=-1 && index3!=-1){
                hasPunctuation = false;
                for(int i=index1; i<index2; i++){
                    if(PunctuationUtil.END_PUNCTUATION.contains(""+segSentence.charAt(i))){
                        hasPunctuation = true;
                        break;
                    }
                }
                if(!hasPunctuation){
                    if(index3<segSentence.length()-1 && PunctuationUtil.END_PUNCTUATION.contains(""+segSentence.charAt(index3+1)))
                        index3 = segSentence.indexOf(" ", index3+1);

                    index2 = index3+1;
                    segSentence = segSentence.replace(segSentence.substring(index1, index2), "");
                }
            }
        }
        return segSentence;
    }

    // 过滤"说，认为，主张"之类的陈述性句块
    private static String filterByStatementRule(String segSentence){
        int index1, index2, index3, lastIndex;
        char c;
        for(String sta : statementDic){
            lastIndex = 0;
            index2 = segSentence.indexOf(" "+sta+"/");
            index1 = segSentence.lastIndexOf("/", index2 - 1);
            // 往前寻找连续的名词，且在标点后或开头处
            index3 = index1;
            while(index3 != -1){
                if((c=segSentence.charAt(index3+1))!='n' && c!='r')
                    break;
                lastIndex = index3;
                index3 = segSentence.lastIndexOf("/", index3-1);
            }
            if((index3==-1 || segSentence.charAt(index3+1)=='w') && index1!=-1 && index2!=-1 && ((c=segSentence.charAt(index1+1))=='n' || c=='r')){
                index1 = segSentence.lastIndexOf(" ", lastIndex);
                if(index3!=-1 && PunctuationUtil.PUNCTUATION.contains(""+segSentence.charAt(index3-1)))
                    segSentence = segSentence.substring(0, index3-1)+"。/wp "+segSentence.substring(index3+4);
                index3 = segSentence.indexOf(" ", index2+1);
                if(index3<segSentence.length()-1 && PunctuationUtil.STATEMENT_PUNCTUATION.contains(""+segSentence.charAt(index3+1)))
                    index3 = segSentence.indexOf(" ", index3+1);

                index2 = index3+1;
                //System.err.println(segSentence);
                segSentence = segSentence.replace(segSentence.substring(index1+1, index2), "");
                //System.err.println(segSentence);
            }
        }
        return segSentence;
    }

    public static void main(String[] args){
        String s = "就/p ,/w 我/r 推测/v ，/wp 你/r 还是/v 个/q 孩子/n 啊/u ！/wp 汪峰/nh 分析/v 我/r 不/d 会/v 永远/d  上/v  不/d  了/v  头条/n  ！/wp";
        CoreSentenceFilter.readDic("corpus//dic//scoreFilterDic.txt");
        s = CoreSentenceFilter.filter(s);
        System.out.println(s);
    }
}
