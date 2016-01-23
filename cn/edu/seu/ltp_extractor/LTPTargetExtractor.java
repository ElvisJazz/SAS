package cn.edu.seu.ltp_extractor;

import com.google.common.collect.HashMultimap;
import edu.stanford.nlp.util.Pair;
import cn.edu.seu.extractor.TargetExtractor;
import cn.edu.seu.SentimentSorter;

import java.util.*;

/*
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 15-3-13
 * Time: 下午4:22
 * To change this template use File | Settings | File Templates.
 */

public class LTPTargetExtractor {
    // 语义角色节点类
    class SRNode{
        // 谓词 beg, seq
        public Pair<Integer,String> predication = null;
        // 副词 beg,seq
        public Pair<Pair<Integer,Integer>,String> adverb = null;
        // 施事者 beg,seq
        public Pair<Pair<Integer,Integer>,String> A0 = null;
        // 受事者 beg,seq
        public Pair<Pair<Integer,Integer>,String> A1 = null;
    }

    // 特殊名词
    public String[] specialNoun = {"/nh", "/ni", "/nl", "/ns", "/nz"};
    // 主题相关句子缓存
    private String segTopicSentence;
    private String depTopicSentence;
    private String srTopicSentence;
    // 正文依存关系抽取对象
    private TargetExtractor depTargetExtractor;
    // 主题依存关系抽取对象
    private TargetExtractor topicDepTargetExtractor;
    // 存储分词列表：位置，词
    private Map<Integer, String> segMap = new HashMap();
    // 存储正文中命名实体列表：位置，<标注，词>
    private Map<Integer, Pair<String,String>> neMap = new HashMap();
    // 存储正文中语义角色列表
    private List<SRNode> srList = new ArrayList();
    // 存储抽取的名词和对应的情感词（可为动词、形容词a,名词性惯用语nl, 名词性语素ng）
    private HashMultimap<String, String> targetPairMap = HashMultimap.create();


    // 读取文本语料句子的命名实体、依存关系、语义角色
    public void readContentCorpusSentence(String segSentence, String depSentence, String srSentence) throws Exception {
        // 记录人名、地名、机构名等特殊命名实体
        String[] segs = segSentence.split(" ");
        String seg;
        for(int i=0; i<segs.length; ++i){
            if(segs[i].equals(""))
                continue;
            seg = segs[i].replaceAll("/[^\\s]*", "");
            segMap.put(i, seg);
            for(String n : specialNoun) {
                if(segs[i].contains(n))
                    neMap.put(i, new Pair<String, String>(n, seg));
            }
        }
        // 记录依存关系
        depTargetExtractor = new TargetExtractor();
        depTargetExtractor.setExtractType(TargetExtractor.EXTRACT_TYPE.LTP);
        depTargetExtractor.extractPotentialWords(segSentence);
        depTargetExtractor.readRelation(depSentence);
        // 记录语义角色
        readSR(srSentence);
    }

    // 读取主题语料句子的命名实体、依存关系、语义角色
    public void readTopicCorpusSentence(String segTopicSentence, String depTopicSentence, String srTopicSentence){
        this.segTopicSentence = segTopicSentence;
        this.depTopicSentence = depTopicSentence;
        this.srTopicSentence = srTopicSentence;
    }

    // 读取语义角色
    public void readSR(String srSentence){
        String[] srs = srSentence.split(";");
        int index1, index2;
        for(String sr : srs){
            if(sr.equals(""))
                continue;
            SRNode node = new SRNode();
            index1 = sr.indexOf('(');
            index2 = sr.indexOf(')', index1+1);
            node.predication = new Pair<>(Integer.valueOf(sr.substring(0, index1)), sr.substring(index1+1, index2));
            sr = sr.substring(index2+3);
            String[] seqs = sr.split(" ");
            int i = 0;
            String type="", seq="", lastType="";
            int beg=0, end, lastEnd=0;
            while(i<seqs.length){
                type = seqs[i].replaceAll("type=", "");
                beg =  Integer.valueOf(seqs[i+1].replaceAll("beg=", ""));
                end =  Integer.valueOf(seqs[i+2].replaceAll("end=", ""));
                seq =  seqs[i+3].replaceAll("seq=", "");

                // 如果上一次type为副词或A0或A1，且这一次也是同样的标注，则选择其一或合并
                if(lastType.equals(type)) {
                    // 副词
                    if(type.equals("ADV")){
                        if(SentimentSorter.getSentimentWordType(seq) != 0){
                            if(node.adverb != null){
                                node.adverb.first.first = beg;
                                node.adverb.first.second = end;
                                node.adverb.second = seq;
                             }
                        }
                    }
                    // 连续A0或A1
                    else if(beg-lastEnd == 1){
                        if(type.equals("A0")){
                            if(node.A0 != null){
                                node.A0.first.second = end;
                                node.A0.second += seq;
                            }
                        }
                        else if(type.equals("A1")){
                            if(node.A1 != null){
                                node.A1.first.second = end;
                                node.A1.second += seq;
                            }
                        }
                    }
                }else if(type.equals("A0")){
                    node.A0 = new Pair<>(new Pair<>(beg,end),seq);
                }else if(type.equals("A1")){
                    node.A1 = new Pair<>(new Pair<>(beg,end), seq);
                }else if(type.equals("ADV")){
                    node.adverb = new Pair<>(new Pair<>(beg,end), seq);
                }
                lastEnd = end;
                i += 4;
            }
            srList.add(node);
        }
    }


    //  初始化主题语料句子的命名实体、依存关系、语义角色
    public void initTopicInfo(){

    }

    // 分析并提取出目标元组，输出
    public HashMultimap<String, String> extract(){

        return null;
    }

    // 某一范围内是否含有评价词
    public boolean hasOpinionWord(int beg, int end, List list){
        String word;
        int code;
        for(int i=beg; i<=end; ++i){
            word = segMap.get(i);
            if((code=SentimentSorter.getSentimentWordType(word)) != 0){
                list.add(new Pair<>(code, word));
                return true;
            }
        }
        list.add(new Pair<>(0, ""));
        return false;
    }
    // 寻找ATT相关的依赖关系 start-end部分: 0-(N-1)
    public HashMap<Integer, Integer> getATTDepMap(int start, int end){
        Set<Pair<Integer, Integer>> ATTDepSet = depTargetExtractor.getDepMap().get("ATT");
        SortedSet<Integer> ATTNodeSet = new TreeSet<Integer>();
        HashMap<Integer, Integer> resultMap = new HashMap<>();
        for(Pair<Integer, Integer> p : ATTDepSet){
            if(p.first>=start+1 && p.first<=end+1 && p.second>=start+1 && p.second<=end+1){
                ATTNodeSet.add(p.first-1);
                ATTNodeSet.add(p.second-1);
            }
        }
        int s = 0, e = -2;
        for(Integer i : ATTNodeSet){
            if(i - e > 1){
                if(e != -2)
                    resultMap.put(s, e);
                // 更新
                s = i;
                e = i;
            } else{
                e = i;
            }
        }
        return resultMap;
    }

    // 从某一成分中抽出潜在评价对象
    public List<String> getPotentialTarget(String block, int start, int end, boolean considerComposite){
        List list = new ArrayList<String>();
        if(block.startsWith("《") && block.endsWith("》"))
            list.add(block.substring(1, block.length()-1));
        else if(considerComposite){
            // 考虑复合A0-A1中抽取出潜在评价对象
        }else{
            // ATT中寻找连续支配部分
            HashMap<Integer, Integer> rm = getATTDepMap(start, end);
            String target = "";
            for(Map.Entry<Integer, Integer> entry : rm.entrySet()) {
                for(int i=entry.getKey(); i<=entry.getValue(); ++i)
                    target += segMap.get(i);
                list.add(target);
                target = "";
            }
            // 含《》
        }

        return null;
    }

    // A0-A1抽取规则
    public void extractByA0A1(){
        for(SRNode node : srList){
            // 寻找评价词
            int opinionCode;
            List<Pair<Integer, String>> opinion = new ArrayList<>();
            if(hasOpinionWord(node.predication.first, node.predication.first, opinion)  ||
                    hasOpinionWord(node.adverb.first.first, node.adverb.first.second, opinion) ||
                    hasOpinionWord(node.A0.first.first, node.adverb.first.second, opinion) ||
                    hasOpinionWord(node.A1.first.first, node.adverb.first.second, opinion) ) {
                // A0A1均有
                // 谓词情感
                if((opinionCode=opinion.get(0).first) != 0){
                    if(Math.abs(opinionCode) == 1){
                        for(String target : getPotentialTarget(node.A0.second, node.A0.first.first, node.A0.first.second, true))
                            targetPairMap.put(target, opinion.get(0).second);
                    }else{
                        for(String target : getPotentialTarget(node.A1.second, node.A1.first.first, node.A1.first.second, true))
                            targetPairMap.put(target, opinion.get(0).second);
                    }
                }
                // 副词情感
                else if((opinionCode=opinion.get(1).first) != 0){
                    for(String target : getPotentialTarget(node.A1.second, node.A1.first.first, node.A1.first.second, true))
                        targetPairMap.put(target, opinion.get(1).second);
                }
                //
            }
        }

    }

    // NR抽取规则
    public void extractByNR(){

    }

    // ATT抽取规则
    public void extractByATT(){

    }

    // DO单独情感词抽取规则
    public void extractByDO(){

    }



    public static void main(String args[]){
        String s = "2(没): type=ADV beg=0 end=0 seq=本来 type=ADV beg=1 end=1 seq=只是 type=A1 beg=3 end=3 seq=房子;6(愁): type=A0 beg=0 end=5 seq=本来只是没房子的人;9(有): type=A1 beg=10 end=10 seq=房子;14(要): type=ADV beg=13 end=13 seq=也;15(增加): type=TMP beg=8 end=8 seq=现在 type=A1 beg=9 end=12 seq=有房子的百姓 type=ADV beg=13 end=13 seq=也 type=A1 beg=16 end=18 seq=交税负担;22(买): type=A1 beg=23 end=23 seq=房子;24(花): type=PRP beg=21 end=23 seq=为买房子 type=A1 beg=25 end=26 seq=光血汗钱;29(多): type=A0 beg=28 end=28 seq=房子;32(开始): type=ADV beg=31 end=31 seq=又;33(收税): type=ADV beg=31 end=31 seq=又;";
        LTPTargetExtractor ex = new LTPTargetExtractor();
        ex.readSR(s);

    }
}
