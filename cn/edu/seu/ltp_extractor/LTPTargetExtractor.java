package cn.edu.seu.ltp_extractor;

import cn.edu.seu.CorpusSegmenter;
import cn.edu.seu.utils.AlignUtil;
import cn.edu.seu.utils.LexUtil;
import cn.edu.seu.utils.PunctuationUtil;
import cn.edu.seu.wordSimilarity.WordSimilarity;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import edu.stanford.nlp.util.CollectionUtils;
import edu.stanford.nlp.util.Pair;
import cn.edu.seu.extractor.TargetExtractor;
import cn.edu.seu.SentimentSorter;

import java.io.*;
import java.security.KeyPair;
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
        // 转折
        public String dis = null;
        // 施事者 beg,seq
        public Pair<Pair<Integer,Integer>,String> A0 = null;
        // 受事者 beg,seq
        public Pair<Pair<Integer,Integer>,String> A1 = null;
        // 间接受事者 beg,seq
        public Pair<Pair<Integer,Integer>,String> A2 = null;
        // 整体成分 beg,end
        public int beg;
        public int end;
    }
/*
    public static CorpusSegmenter segmenter = new CorpusSegmenter();*/
    // 主题相关句子缓存
    private String segTopicSentence;
    private String depTopicSentence;
    private String srTopicSentence;

    // 指代寻找所需参量
    // 当前处理块的长度
    private int length;
    // 当前处理块的起始
    private int start;
    // 对齐所需整段微博句子
    private String originalSentence;
    // 第一个实体
    private String firstTarget = "";
    // 当前分析句子
    private String currentSentence;
    private String currentSegSentence;
    private int currentSegSentenceOffset;
    private Map<Integer, String> originalNHMap = new LinkedHashMap<>();
    private Set<String> currentHNSet = new HashSet();

    // 正文依存关系抽取对象
    private TargetExtractor depTargetExtractor;
    // 主题依存关系抽取对象
    private TargetExtractor topicDepTargetExtractor;

    // 存储词性列表：位置，<标注，词>
    private Map<Integer, Pair<String,String>> segMap;
    // 主题存储词性列表：位置，<标注，词>
    private Map<Integer, Pair<String,String>> topicSegMap;

    // 当前处理语义节点
    private SRNode curNode;
    private SRNode lastNode = null;
    private Pair<Integer,Integer> range;
    // 和转折词关联的评价对象
    private Map<String, String> contrastMap = new HashMap<>();
    // 存储正文中语义角色列表:是否处理过，语义角色节点
    private List<SRNode> srList;
    // 存储主题中语义角色列表:是否处理过，语义角色节点
    private List<SRNode> topicSrList;
    // 存储全句分句主语
    private Map<Pair<Integer,Integer>, String> allSubjectMap;

    // 存储抽取的名词和对应的情感词（可为动词、形容词a,名词性惯用语nl, 名词性语素ng）
    private LinkedHashMultimap<String,String> resultTargetPairMap = LinkedHashMultimap.create();
    private LinkedHashMultimap<String,String> targetPairMap = LinkedHashMultimap.create();
    private Pair<String, String> lastTargetPair = new Pair<>("", "");

    // 主题语料相关信息是否已经初始化
    private boolean isInit = false;

    // 当前已分析过的A0,A1结构范围
    private List<Pair<Integer,Integer>> analyseRangeList = new ArrayList<>();
    // 当前主题
    private String topic = null;
    // 情感句块过滤词典
    private static Set<String> sentimentFilterDic = new HashSet<>();
    // 正面语义名词性词典
    private static Set<String> sentimentNounDic = new HashSet<>();
    // 转折词词典
    private static Set<String> contrastDic = new HashSet<>();

    // 同一条微博的上一句的第一个评价对象
    private static String lastSentenceTarget = "";
    // 上一条微博
    private static String lastSentenceWeibo = "";

    // 获取潜在评价对象和评价词函数的状态
    private static Boolean hasChanged = false;

    // 初始化
    public static void initDic(String filterDicPath, String sentimentNounDicPath, String contrastDicPath){
        try {
            // 情感句块过滤词典
            String tmp;
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filterDicPath)),"GBK"));
            while((tmp=reader.readLine()) != null){
                tmp = tmp.substring(0,tmp.length()).trim();
                sentimentFilterDic.add(tmp);
            }
            reader.close();
            // 正面语义名词性词典
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(sentimentNounDicPath)),"GBK"));
            while((tmp=reader.readLine()) != null){
                tmp = tmp.trim();
                sentimentNounDic.add(tmp);
            }
            reader.close();
            // 转折词典
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(contrastDicPath)),"GBK"));
            while((tmp=reader.readLine()) != null){
                tmp = tmp.trim();
                contrastDic.add(tmp);
            }
            reader.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 初始化
    public static void init(){
       /* segmenter.useLTPSeg = segmenter.useLTPPos = true;
        segmenter.init("corpus//dic//scoreFilterDic.txt");*/
        AlignUtil.init();
    }

    public static void destroy(){
        /*segmenter.destroy();*/
        AlignUtil.destroy();
    }

    // 读取文本语料句子的命名实体、依存关系、语义角色
    public void readContentCorpusSentence(String segSentence, String depSentence, String srSentence, String originalSentence) throws Exception {
        this.originalSentence = originalSentence;
        currentSegSentence = segSentence;
        currentSentence = "";
        // 记录词性标注等信息
        String tmp;
        length = 0;
        start = 0;
        segMap = new HashMap();
        String[] segs = segSentence.split(" ");
        int index;
        for(int i=0; i<segs.length; ++i){
            if(segs[i].equals(""))
                continue;
            index = segs[i].lastIndexOf('/');
            if(index != -1){
                tmp = segs[i].substring(0, index);
                segMap.put(i, new Pair<>(segs[i].substring(index+1), tmp));
                currentSentence += tmp;/*
                length += tmp.length();*/
            }
        }
        // 记录依存关系
        depTargetExtractor = new TargetExtractor();
        depTargetExtractor.setExtractType(TargetExtractor.EXTRACT_TYPE.LTP);
        depTargetExtractor.extractPotentialWords(segSentence);
        depTargetExtractor.readRelation(depSentence);
        // 记录语义角色
        srList = new ArrayList();
        readSR(srSentence, false);
        // 读取全句主语
        //readAllSubject();
        // 读取所有人名
        readAllNH();
    }

    // 读取主题语料句子的命名实体、依存关系、语义角色
    public void readTopicCorpusSentence(String segTopicSentence, String depTopicSentence, String srTopicSentence){
        this.segTopicSentence = segTopicSentence;
        this.depTopicSentence = depTopicSentence;
        this.srTopicSentence = srTopicSentence;
    }

    // 获取全句的每个子句的主语
    public void readAllSubject(){
        allSubjectMap = new LinkedHashMap<>();
        int index = 0;
        String target;
        Pair<Integer,Integer> pair = depTargetExtractor.getDepMap().get("HED").iterator().next();
        for(SRNode node : srList){
            if(node.predication.first+1 == pair.second){
                if(node.A0 != null && index<srList.size()-1 && !hasA0OrA1(node, srList.get(index+1))){
                    if(Math.abs(SentimentSorter.getSentimentWordType(node.predication.second)) == 2){
                        if(node.A1 != null)
                            target = getPotentialTargetAndOpinion(node.A1, segMap, depTargetExtractor.getDepMap(), this, false, false).get(0).first;
                        else
                            target = getTopicTarget();
                    }
                    else
                        target = getPotentialTargetAndOpinion(node.A0, segMap, depTargetExtractor.getDepMap(), this, true, false).get(0).first;
                    allSubjectMap.put(new Pair<>(node.beg, node.end), target);
                }else if(node.A1!=null && index<srList.size()-1 && !hasA0OrA1(node, srList.get(index+1))){
                    if(Math.abs(SentimentSorter.getSentimentWordType(node.predication.second)) == 1) {
                        if(node.A0 != null)
                            target = getPotentialTargetAndOpinion(node.A0, segMap, depTargetExtractor.getDepMap(), this, true, false).get(0).first;
                        else
                            target = getTopicTarget();
                    }
                    else
                        target = getPotentialTargetAndOpinion(node.A1, segMap, depTargetExtractor.getDepMap(), this, false, false).get(0).first;
                    allSubjectMap.put(new Pair<>(node.beg, node.end), target);
                }
            }
            index++;
        }
    }

    // 读取所有人名
    public void readAllNH(){
        String segSentence = AlignUtil.segmenter.segmentSentence(originalSentence, true);//AlignUtil.segmenter.segmentSentenceUseLTP(originalSentence);
        int offset = originalSentence.indexOf(currentSentence);
        offset = offset==-1? 0 : offset;
        int count = 0;
        currentSegSentenceOffset = 0;
        String[] trunks = segSentence.split(" ");
        int i = 0, ii = 0;
        String tag, word;
        for(String t : trunks){
            i = t.lastIndexOf('/');
            if(i == -1){
                //System.err.println("分词未找到/:"+originalSentence);
                continue;
            }
            tag = t.substring(i + 1);
            word = t.substring(0, i);
            if(LexUtil.HUMAN_NOUN_STF.contains(" "+tag+" ")) {
                originalNHMap.put(ii, word);
                if(count > offset)
                    currentHNSet.add(word);
            }
            ii++;
            count += i;
            if(count <= offset)
                currentSegSentenceOffset = ii;
        }
    }

    // 读取语义角色
    public void readSR(String srSentence, boolean isCorpus){
        String alpha = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String[] srs = srSentence.split(";");
        int index1, index2;
        int minBeg, maxEnd;
        for(String sr : srs){
            if(sr.equals(""))
                continue;
            SRNode node = new SRNode();
            index1 = sr.indexOf('(');
            index2 = sr.indexOf(')', index1+1);
            if(index1==-1 || index2==-1)
                continue;
            node.predication = new Pair<>(Integer.valueOf(sr.substring(0, index1)), sr.substring(index1+1, index2));
            minBeg = maxEnd = Integer.valueOf(sr.substring(0, index1));
            sr = sr.substring(index2+3);
            String[] seqs = sr.split(" ");
            int i = 0;
            String type, seq, lastSeq="", lastType="", space="";
            int beg, end, lastEnd=0;
            boolean isRepeatA = false;
            boolean isTargetTag = true;
            while(i<seqs.length){
                type = seqs[i].replaceAll("type=", "");
                beg =  Integer.valueOf(seqs[i + 1].replaceAll("beg=", ""));
                end =  Integer.valueOf(seqs[i+2].replaceAll("end=", ""));
                seq =  seqs[i+3].replaceAll("seq=", "");
                // 如果上一次type为副词或A0或A1，且这一次也是同样的标注，则选择其一或合并
                /*if(lastType.equals(type)) {
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
                    // 连续A0或A1或A2
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
                        } else if(type.equals("A2")){
                            if(node.A2 != null){
                                node.A2.first.second = end;
                                node.A2.second += seq;
                            }
                        }
                    }
                }else*/ if(type.equals("A0")){
                    node.A0 = new Pair<>(new Pair<>(beg,end),seq);
                }else if(type.equals("A1")){
                    node.A1 = new Pair<>(new Pair<>(beg,end), seq);
                }else if(type.equals("A2")){
                    node.A1 = new Pair<>(new Pair<>(beg,end), seq);
                }/*else if(type.equals("A3")){
                    node.A0 = new Pair<>(new Pair<>(beg,end), seq);
                }*/else if(type.equals("ADV")){
                    node.adverb = new Pair<>(new Pair<>(beg,end), seq);
                }else if(type.equals("DIS")){
                    node.dis = seq;
                }else{
                    isTargetTag = false;
                }

                if(isTargetTag){
                    minBeg = (minBeg<beg)? minBeg : beg;
                    maxEnd = (maxEnd>end)? maxEnd : end;
                }
                //lastEnd = end;
                if(lastType.equals(type) && (type.equals("A0")))
                    isRepeatA = true;
                lastType = type;
                isTargetTag = true;
                //lastSeq = seq;
                i += 4;
            }
            node.beg = minBeg;
            node.end = maxEnd;
            if(!isRepeatA) {
                if(isCorpus)
                    topicSrList.add(node);
                else
                    srList.add(node);
            }
        }
    }


    //  初始化主题语料句子的命名实体、依存关系、语义角色
    public void initTopicInfo()  throws Exception {
        // 记录词性标注等信息
        topicSegMap = new HashMap();
        String[] segs = segTopicSentence.split(" ");
        int index;
        for(int i=0; i<segs.length; ++i){
            if(segs[i].equals(""))
                continue;
            index = segs[i].lastIndexOf('/');
            if(index != -1)
                topicSegMap.put(i, new Pair<>(segs[i].substring(index+1), segs[i].substring(0,index)));
        }
        // 记录依存关系
        topicDepTargetExtractor = new TargetExtractor();
        topicDepTargetExtractor.setExtractType(TargetExtractor.EXTRACT_TYPE.LTP);
        topicDepTargetExtractor.extractPotentialWords(segTopicSentence);
        topicDepTargetExtractor.readRelation(depTopicSentence);
        // 记录语义角色
        topicSrList = new ArrayList();
        readSR(srTopicSentence, true);
    }
    // 去掉字符串首尾标点符号
    public String filterBEPunctuation(String word){
        while(word.length()>0 && PunctuationUtil.PUNCTUATION.contains(""+word.charAt(0))){
            word = word.substring(1);
        }
        while(word.length()>0 && PunctuationUtil.PUNCTUATION.contains(""+word.charAt(word.length()-1))){
            word = word.substring(0, word.length()-1);
        }
        return word;
    }

    // 检测并保留评价对象和评价词有效部分
    public boolean checkValid(Pair<String,String> pair){
        String target = pair.first;
        String opinion = pair.second;
        if(target==null || opinion==null || target.equals("") || opinion.equals(""))
            return false;
        target = filterBEPunctuation(target.trim());
        String oOpinion = opinion.trim();
        int index = opinion.lastIndexOf('(');
        if(index != -1)
            opinion = oOpinion.substring(0, index);
        index = opinion.indexOf(')');
        if(index != -1)
            opinion = opinion.substring(index+1);
        if(target.length()<2 || opinion.equals("") || SentimentSorter.getSentimentWordType(opinion)==0
                || SentimentSorter.getSentimentWordType(target)!=0 || target.endsWith(opinion))
            return false;
        pair.first = target;
        pair.second = oOpinion;
        return true;
    }

    // 填充评价对象和评价词map, fromSelf:是否情感词取自同一语义成分
    public boolean putTargetAndOpinion(String target, String opinion, Pair<Integer,Integer> range, String verb, boolean fromSelf){
        Pair<String,String> pair0 = new Pair<>(target, opinion);
        if(!checkValid(pair0))
            return false;

        target = pair0.first;
        opinion = pair0.second;
        // 过滤无关评价对象
        for(Pair<String,String> pair : segMap.values()){
            if(pair.second.equals(target)){
                if(pair.first.equals("d") || pair.first.equals("v") || pair.first.equals("r") || pair.first.equals("i"))
                    return false;
            }
        }
        String originalOpinion = opinion;
        char startWord = target.charAt(0);
        char endWord = target.charAt(target.length()-1);
        if(startWord!='《' && startWord!='"' && PunctuationUtil.PUNCTUATION.contains(""+startWord))
            target = target.substring(1);
        if(target.length()!=0 && endWord!='》' && endWord!='"' && endWord!=')' && PunctuationUtil.PUNCTUATION.contains(""+endWord))
            target = target.substring(0, target.length()-1);
        // 获取范围内所有的副词
        Set<String> adverbs = new HashSet<>();
        boolean hasWeakAdv = false;
        if(!fromSelf){
            // 范围内副词方式
            for(int i=range.first; i<=range.second; ++i){
                if(segMap.get(i).first.equals("d"))
                    adverbs.add(segMap.get(i).second);
            }
            // 添加副词
            opinion += "(";
            for(String adverb : adverbs) {
                opinion += (adverb+" ");
                for(String wAdv : LexUtil.WEAK_ADV){
                    if(wAdv.equals(adverb))
                        hasWeakAdv = true;
                }
            }
            if(hasWeakAdv)
                opinion += "){w}";
            else
                opinion += "){n}";
        }else{
            // 从依存关系中获取副词方式
            int advIndex=-1, vIndex=-1;
            for(int index=0; index<segMap.size(); ++index){
                if(segMap.get(index).second.equals(opinion))
                    advIndex = index;
                if(segMap.get(index).second.equals(verb))
                    vIndex = index;
                if(advIndex!=-1 && vIndex!=-1)
                    break;
            }

            HashMap<Integer, String> potentialSentimentMap = depTargetExtractor.getPotentialSentimentMap();
            if(advIndex != vIndex){
                String vOpinion = potentialSentimentMap.get(vIndex+1);
                if(vOpinion != null){
                    vIndex = vOpinion.lastIndexOf("(");
                    for(String adverb : vOpinion.substring(vIndex+1, vOpinion.length()-1).split(" ")){
                        int index = findIndexFromSeg(segMap, range.first, adverb, false);
                        if(index != -1)
                            adverbs.add(adverb);
                    }
                }
            }

            opinion = potentialSentimentMap.get(advIndex+1);
            if(opinion == null){
                assert false: "impossible!";
                return false;
            }
            advIndex = opinion.lastIndexOf("(");
            for(String adverb : opinion.substring(advIndex+1, opinion.length()-1).split(" "))
            {
                adverbs.add(adverb);
            }
        }

        if(isReverse(originalOpinion, adverbs, verb))
            opinion = "(-)"+opinion;
        /*if(opinion.contains("其实"))            {
            System.out.println(originalSentence);
            System.out.println(target+" "+opinion);
        }*/
        targetPairMap.put(target, opinion);
        lastTargetPair.first = target;
        lastTargetPair.second = opinion;
        return true;
    }

    // 情感词反转
    public boolean isReverse(String opinion, Set<String> adverbs, String verb){
        List<Boolean> advWhite = new ArrayList<Boolean>();
        boolean vWhite = false;
        boolean flag = false;
        boolean tFlag = false;
        // 处理副词
        // 否定词白名单
        for(String adverb : adverbs) {
            if(adverb.equals(opinion)){
                advWhite.add(false);
                continue;
            }
            for(String whiteWord : LexUtil.NOT_WHITE_SET){
                if(adverb.contains(whiteWord)){
                    tFlag = true;
                    break;
                }
            }
            if(tFlag){
                advWhite.add(true);
            } else{
                advWhite.add(false);
            }
        }
        tFlag = false;
        // 否定词
        int i = 0;
        for(String adverb : adverbs) {
            if(adverb.equals(opinion)) {
                i++;
                continue;
            }
            for(String notWord : LexUtil.NOT_SET){
                if(!advWhite.get(i) && notWord.equals(adverb)){
                    flag = !flag;
                    break;
                }
            }
            i++;
        }
        // 处理动词
        if(verb.equals(opinion))
            return flag;
        // 否定词白名单
        for(String whiteWord : LexUtil.NOT_WHITE_SET){
            if(verb.contains(whiteWord)){
                vWhite = true;
                break;
            }
        }
        // 否定词
        for(String notWord : LexUtil.NOT_SET){
            if(!vWhite && notWord.equals(verb)){
                flag = !flag;
                break;
            }
        }
        return flag;
    }

    // 打标 markNum: 0,取自语义分析 1,取自依存分析
    public void mark(int markNum){
        for(Map.Entry<String,String> entry : targetPairMap.entries()){
            if("".equals(firstTarget))
                firstTarget = entry.getKey();
            resultTargetPairMap.put(entry.getKey(), entry.getValue().concat("%%-"+markNum+"-%%"));
        }
        targetPairMap.clear();
    }

    // 某一范围内是否含有评价词
    public boolean hasOpinionWord(int beg, int end, List list){
        String word;
        int code;
        for(int i=beg; i<=end; ++i){
            word = segMap.get(i).second;
            if((code=SentimentSorter.getSentimentWordType(word)) != 0){
                list.add(new Pair<>(code, word));
                return true;
            }
        }
        list.add(new Pair<>(0, ""));
        return false;
    }

    // 是否在《》中
    public boolean isInBookQuotation(int start, int end){
        int start1=-1,end1=-1, start2=-1, end2=-1;
        for(int i=1; i<segMap.size(); ++i){
            if(start-i>0 && segMap.get(start-i).equals("《"))
                start1 = start-i;
            else if(start-i>0 && segMap.get(start-i).equals("》"))
                end1 = start-i;
            else if(end+i<segMap.size() && segMap.get(end+i).equals("《"))
                start2 = end+i;
            else if(end+i<segMap.size() && segMap.get(end+i).equals("》"))
                end2 = end+i;
        }
        if(start1>end1 && end2<start2 && end2!=-1)
            return true;
        return false;
    }

    // 判断书名号成分是否和指定词块重叠，返回原词块或重叠合并部分
    public static Pair<Integer,Integer> getCombinationBlock(Map<Integer, Pair<String,String>> segMap, int start, int end){
        int _start = -1, _end = -1; // 书名号开始结束index
        int rStart = -1, rEnd = -1;
        int reStart = -1, reEnd = -1;
        for(int i=0; i<segMap.size(); ++i){
            if(segMap.get(i).second.equals("《") && _start==-1)
                _start = i+1;
            else if(segMap.get(i).second.equals("》") && start!=-1){
                _end = i-1;
                // 判断
                if(_start>=start && _start<=end){
                    rStart = start;
                    if(_end > end)
                        rEnd = _end;
                    else
                        rEnd = end;
                }
                if(_end>=start && _end<=end){
                    rEnd = end;
                    if(_start < start)
                        rStart = _start;
                    else
                        rStart = start;
                }
                if(_start<start && _end>end){
                    rStart = _start;
                    rEnd = _end;
                }
                if(reStart == -1)
                    reStart = rStart;
                reEnd = rEnd;
            }
        }
        if(reStart==-1 || reEnd==-1)
            return new Pair<>(start, end);
        return new Pair<>(reStart, reEnd);
    }

    // 寻找ATT相关的依赖关系覆盖范围 start-end部分: 0-(N-1)
    public static List<Pair<Integer, Integer>> getATTDepRangeList(int start, int end,  Map<Integer, Pair<String,String>> segMap, Set<Pair<Integer, Integer>> ATTDepSet){
        SortedSet<Float> ATTNodeSet = new TreeSet<>();
        List<Pair<Integer, Integer>> resultList = new ArrayList<>();
        float _start=-1, _end=-1;
        int min, max;
        for(Pair<Integer, Integer> p : ATTDepSet){
            min = Math.min(p.first-1, p.second-1);
            max = Math.max(p.first-1, p.second-1);
           /* if(segMap.get(p.first-1).first.equals("nd"))
                continue;*/
            for(int i=min; i<max; ++i){
                ATTNodeSet.add((float)i);
                ATTNodeSet.add((float)(i+0.5));
            }
            ATTNodeSet.add((float)max);
        }
        // 遍历节点，计算覆盖范围
        for(float i : ATTNodeSet){
            if(i-_end >= 1.0){
                if(_end != -1.0 && ((_start>=start && _start<=end) || (_end>=start && _end<=end)) && !isInvalidATT(segMap,(int)_start,(int)_end))
                    resultList.add(getCombinationBlock(segMap, (int)_start, (int)_end));
                _start = _end = i;
            }else{
                if(_end == -1.0)
                    _start = i;
                _end = i;
            }
        }
        if(_start!=-1.0 && _end!=-1.0 && ((_start>=start && _start<=end) || (_end>=start && _end<=end)) && !isInvalidATT(segMap,(int)_start,(int)_end))
            resultList.add(getCombinationBlock(segMap, (int)_start, (int)_end));

        return resultList;
    }

    // 是否是无关的ATT部分
    public static boolean isInvalidATT(Map<Integer, Pair<String,String>> segMap, int start, int end){
        /*if(segMap.get(end).first.equals("nt") || (end>=1 && segMap.get(end-1).first.equals("nt") && segMap.get(end).first.equals("nd"))) {
            for(int i=start; i<=end; i++)
                System.err.print(segMap.get(i).second);
            System.err.println("");
            return true;
        }*/
        return false;
    }

    // 从话题中寻找评价对象
    public String getTopicTarget(){
        if(!isInit){
            try{
                initTopicInfo();
            }catch (Exception e){
                System.err.println("主题语料相关信息初始化失败！");
                e.printStackTrace();
                topic = "";
                return "";
            }
        }
        if(topicSegMap.size() > 0){
            Pair<Pair<Integer,Integer>,String> pair = new Pair<>(new Pair<>(0,topicSegMap.size()-1), segTopicSentence.replaceAll("/[a-z ]*", ""));
            List<Pair<String,String>> list = getPotentialTargetAndOpinion(pair, topicSegMap, topicDepTargetExtractor.getDepMap(), null, false, true);
            String result = list.size()>0 ? list.get(0).first : "";
            topic = result;
            return result;
        }
        topic = "";
        return "";
    }

    // 获取最近的人名
    public String getNeighborNH(int index){
        String lastHN = "";
        index = currentSegSentenceOffset+index;
        int findIndex = -1;
        String firstHN = "";
        for(int i : originalNHMap.keySet()){
            findIndex = i;
            if(i>index && !"".equals(lastHN))
                break;
            lastHN = originalNHMap.get(i);
            if("".equals(firstHN))
                firstHN = lastHN;
        }
        if(findIndex!=-1 && findIndex<currentSegSentenceOffset)
            lastHN = firstHN;
        return lastHN;
    }

    // 获得连续名词
    public String getContinurousNoun(int index){
        String s = "";
        for(int i=index; i>=0; i--){
            if(segMap.get(i).first.startsWith("n"))
                s = segMap.get(i).second + s;
            else
                break;
        }
        for(int i=index; i<segMap.size(); i++){
            if(segMap.get(i).first.startsWith("n"))
                s += segMap.get(i).second;
            else
                break;
        }
        return s;
    }

    // 指代及隐性搜索，寻找上一个
    public static String getReferenceWord(int index, LTPTargetExtractor extractor, boolean isUseLastTarget){
        String lastSubject = "";
        // 本句代词邻近搜索
        if(index>0 && !(lastSubject=extractor.getContinurousNoun(index - 1)).equals(""))
            return lastSubject;
        // 从本句的本段中找
        if("".equals(lastSubject)){
            int startIndex = 0;
            index = index>0? index : extractor.curNode.beg;
            int i = index-1;
            String tmp = "";
            for(; i>0; i--){
                if(extractor.segMap.get(i).first.equals("wp"))
                    break;
                tmp = extractor.segMap.get(i).second+tmp;
            }
            startIndex = i;
            if(startIndex<=index-1 && !"".equals(tmp)){
                Pair<Pair<Integer,Integer>,String> nodeAPair = new Pair<>(new Pair<>(startIndex+1, index-1), tmp);
                lastSubject = getPotentialTargetAndOpinion(nodeAPair, extractor.segMap, extractor.depTargetExtractor.getDepMap(), extractor, false, false).get(0).first;
                if(!hasChanged)
                    lastSubject = "";
            }
        }
        // 获取邻近句子的主语，先从上文开始，再从主题，最后下文
        // 从前文获取
        /*if("".equals(lastSubject)){
            Iterator<Pair<Integer,Integer>> iterator = extractor.allSubjectMap.keySet().iterator();
            Pair<Integer,Integer> pair;
            if(iterator.hasNext()){
                pair = iterator.next();
                *//*if(index>=pair.first && index<=pair.second)
                    break;*//*
                lastSubject = extractor.allSubjectMap.get(pair);
                if(!lastSubject.equals("")){
                    System.out.println(extractor.currentSentence);
                    System.out.println(lastSubject);
                }
            }
        }*/

        // 从主题获取
        if(isUseLastTarget && "".equals(lastSubject))
            lastSubject = extractor.getTopicTarget();
        // 获取上一个评价对象
        if(isUseLastTarget && "".equals(lastSubject) && extractor.targetPairMap.size() > 0)
            lastSubject = extractor.lastTargetPair.first;

        // 从上一句获取第一个评价对象
        /*if(isUseLastTarget && "".equals(lastSubject) && lastSentenceWeibo.equals(extractor.originalSentence))
            lastSubject = lastSentenceTarget;*/

        // 从下文获取
       /* if("".equals(lastSubject) && iterator.hasNext()){
            pair = iterator.next();
            lastSubject = allSubjectMap.get(pair);
        }*/
       /*if("".equals(lastSubject)){
            String result  = AlignUtil.getNeighborTarget(extractor.originalSentence, extractor.start, extractor.length);
            if(result != null)
                lastSubject = result;
       }*/
        return lastSubject;
    }

    // 从指定范围内在分词序列中寻找指定词的下标
    public static int findIndexFromSeg(Map<Integer, Pair<String,String>> map, int start, String word, boolean isChar) {
        if(isChar){
            int sum = 0;
            String tmp;
            for(int i=0; i<map.size(); ++i){
                tmp = map.get(i).second;
                if(i>=start && tmp.equals(word))
                    return sum;
                sum += tmp.length();
            }
        }else{
            for(int i=start; i<map.size(); ++i){
                if(map.get(i).second.equals(word))
                    return i;
            }
        }
        return -1;
    }

    // 是否是修饰词
    public static boolean isAdj(String word){
        if(word.equals("a") || word.equals("i") || word.equals("b"))
            return true;
        return false;
    }

    // 从某一成分中抽出潜在评价对象和评价词
    public static List<Pair<String,String>> getPotentialTargetAndOpinion(Pair<Pair<Integer,Integer>,String> nodeAPair,
            Map<Integer, Pair<String,String>> tmpSegMap, LinkedHashMultimap<String, Pair<Integer,Integer>> depMap, LTPTargetExtractor extractor, boolean isA0, boolean canBeEmpty){
        boolean isFindSBV = false;
        hasChanged = true;
        // 若A0为空，优先寻找谓词最近的SBV主语部分
        if(isA0 && nodeAPair==null && extractor!=null){
            for(Pair<Integer,Integer> pair : extractor.depTargetExtractor.getDepMap().get("SBV")){
                if(pair.first == extractor.curNode.predication.first+1){
                    nodeAPair = new Pair<>(new Pair<>(pair.second-1, pair.second-1), tmpSegMap.get(pair.second-1).second);
                    isFindSBV = true;
                    break;
                }
            }
            if(extractor.curNode.A1 != null){
                extractor.start = findIndexFromSeg(tmpSegMap, extractor.curNode.A1.first.first, tmpSegMap.get(extractor.curNode.A1.first.first).second, true);
                extractor.length = findIndexFromSeg(tmpSegMap, extractor.curNode.A1.first.second, tmpSegMap.get(extractor.curNode.A1.first.second).second, true) - extractor.start;
            }else{
                extractor.start = 0;
                extractor.length = 0;
            }
        }

        String block = "";
        boolean findMQ = false;
        int start=0, end=0;
        if(nodeAPair != null){
            block = nodeAPair.second;
            start = nodeAPair.first.first;
            end = nodeAPair.first.second;
        }
        List<Pair<String,String>> list = new ArrayList<>();
        List<Integer> indexList = new ArrayList<>();
        /*// 一个名词都没有，则跳过
        boolean hasNoNoun = true;
        for(int i=start; i<=end; ++i){
            if(segMap.get(i).first.contains("n"))
                hasNoNoun = false;
        }
        if(hasNoNoun)
            return list;*/
        // 助词对象缺失
        if(tmpSegMap.get(end).first.equals("u") && end<tmpSegMap.size()-1){
            block += tmpSegMap.get(end+1).second;
            end++;
        }
        // 代词或隐性A0或A1指代搜寻
        if(extractor!=null && block.length()<=2 && LexUtil.HUMAN_PRONOUN.contains(" "+block+" "))
            list.add(new Pair<>(extractor.getNeighborNH(start), ""));
        if(/*(list.size()==0 || list.get(0).first.equals("")) && */extractor!=null && (block=="" || (block.length()==1 && (tmpSegMap.get(start).first.equals("r") || tmpSegMap.get(start).first.equals("m"))))){
            String target = getReferenceWord(start, extractor, true);
            if(!"".equals(target))
                list.add(new Pair<>(target, ""));
        }
        // 考虑是否被《》包裹
        if(block.startsWith("《") && block.endsWith("》"))
            list.add(new Pair<>(block.substring(1, block.length() - 1),""));
        else if(list.size() == 0){
            if(extractor != null && extractor.curNode != null && isA0)
                end = end<=0? extractor.curNode.predication.first : end;/*
            else if(!isA0)
                end = end==-1? extractor.curNode.predication.first : end;*/
            // ATT部分包括adj
            List<Pair<Integer, Integer>> targetRangeList = getATTDepRangeList(start, end, tmpSegMap, depMap.get("ATT"));
            String target = "", opinion = "";
            String tmp1, tmp2, lastTmp="";
            boolean onlyHasMQ;
            boolean isStep;
            int j;
            Pair<Boolean, Pair<String,Integer>> result;
            for(Pair<Integer,Integer> pair0 : targetRangeList){
                Pair<Integer,Integer> pair = new Pair<>(pair0.first, pair0.second);
                onlyHasMQ = true;
                isStep = false;
                target = "";
                opinion = "";

                if(pair.first == pair.second && tmpSegMap.get(pair.first).first.equals("r"))
                    continue;
                // 整段人名处理
                /*result = getHN(tmpSegMap, pair, extractor);
                j = result.second.second;
                target = result.second.first;
                // 处理结尾是时间名词
                if(result.first){
                    onlyHasMQ = false;
                    isStep = true;
                    if(!tmpSegMap.get(pair.second).first.equals("nd") && j==pair.second && tmpSegMap.get(j).first.startsWith("n")){
                        target += tmpSegMap.get(j).second;
                    }
                }else*/{

                    for(int i=pair.first; i<pair.second; ++i){
                        tmp1 = tmpSegMap.get(i).first;
                        tmp2 = tmpSegMap.get(i+1).first;

                        if(!tmp1.equals("m") && !tmp1.equals("q") && !tmp1.equals("nt") && !tmp1.equals("nd"))
                            onlyHasMQ = false;

                        if((isAdj(tmp1) || (tmp1.equals("v") && isAdj(lastTmp)) || (!tmp1.equals("n") && SentimentSorter.getSentimentWordType(tmpSegMap.get(i).second)!=0)) && tmp2.equals("u") && i+1<pair.second){
                            pair.first = i+2;
                            opinion = tmpSegMap.get(i).second;
                        }else if(((tmp1.equals("u") && (isAdj(tmp2) || SentimentSorter.getSentimentWordType(tmpSegMap.get(i+1).second)!=0)) || (isAdj(tmp1) && tmp2.equals("v") && lastTmp.equals("u"))) && i-1>=pair.first){
                            if(!tmp2.equals("n"))
                                pair.second = i-1;
                            opinion = tmpSegMap.get(i+1).second;
                        }
                        lastTmp = tmp1;
                    }
                }
                // 过滤
                /*if("m".equals(tmpSegMap.get(pair.first).first)*//* && pair.first+1<tmpSegMap.size() && tmpSegMap.get(pair.first+1).first.equals("q")*//*){
                    //findMQ = true;
                    for(int ii=pair.first; ii<=pair.second; ii++)
                        System.out.print("-1"+tmpSegMap.get(ii).second);
                    System.out.println();
                    isStep = true;
                    //continue;
                }*/
                // 处理获取的ATT
                if(!isStep){
                    // 处理人名抽取
                    result = getHN(tmpSegMap, pair, extractor);
                    j = result.second.second;
                    target = result.second.first;
                    // 处理结尾是时间名词
                    if(!tmpSegMap.get(pair.second).first.equals("nd") && (!result.first || (j==pair.second && tmpSegMap.get(j).first.startsWith("n"))))
                        target += tmpSegMap.get(j).second;
                    // 添加已分析范围
                    if(extractor!=null && isFindSBV)
                        extractor.range.first = extractor.range.first > pair.first? pair.first : extractor.range.first;
                }
                if(!target.equals("") && !onlyHasMQ){
                    list.add(new Pair<>(target,opinion));
                    indexList.add(pair.second);
                    break;
                }
            }
            // 含《》且不在之前的ATT中
            List<Pair<Integer,Integer>> bookQuotationList = new ArrayList<>();
            int index1 =  findIndexFromSeg(tmpSegMap, start, "《", false); //block.indexOf("《");
            int index2;
            while(index1!=-1 && index1>=start && index1<=end){
                index2 = findIndexFromSeg(tmpSegMap, index1+1, "》", false); //block.indexOf("》", index1+1);
                if(index2 != -1){
                    if(targetRangeList.size() > 0){
                        for(Pair<Integer, Integer> pair : targetRangeList){
                            if(index2<pair.first || index1>pair.second){
                                for(int i=index1; i<=index2; ++i){
                                    target += tmpSegMap.get(i).second;
                                }
                                bookQuotationList.add(new Pair<>(index1+1, index2-1));
                                list.add(new Pair<>(target,""));
                                indexList.add(index2);
                                target = "";
                            }
                        }
                    }else{
                        for(int i=index1; i<=index2; ++i){
                            target += tmpSegMap.get(i).second;
                        }
                        bookQuotationList.add(new Pair<>(index1+1, index2-1));
                        list.add(new Pair<>(target,""));
                        indexList.add(index2);
                        target = "";
                    }
                }else{
                    break;
                }
                index1 = findIndexFromSeg(tmpSegMap, index2+1, "《", false); //block.indexOf("》", index2+1);
            }
            // 寻找不在ATT且不在《》中的命名实体
            List<Pair<Integer,String>> nrList = new ArrayList<>();
            boolean step = false;
            for(int i=start; i<=end; ++i){
                if(LexUtil.SPECIAL_NOUN_LTP.contains(" "+tmpSegMap.get(i).first+" ")){
                    // 判断不在《》中
                    for(Pair<Integer,Integer> bPair : bookQuotationList){
                        if(i>=bPair.first && i<=bPair.second){
                            step = true;
                            break;
                        }
                    }
                    if(step){
                        step = false;
                        break;
                    }
                    // 判断不在ATT中
                    if(targetRangeList.size() > 0){
                        boolean inRange = false;
                        for(Pair<Integer,Integer> pair : targetRangeList){
                            if(i>=pair.first && i<=pair.second){
                                inRange = true;
                                break;
                            }
                        }
                        if(!inRange)
                            nrList.add(new Pair<>(i,tmpSegMap.get(i).second));
                    }else{
                        nrList.add(new Pair<>(i,tmpSegMap.get(i).second));
                    }
                }
            }
            // 合并临近的实体
            Pair<Integer,String> lastPair = null;
            for(Pair<Integer,String> pair : nrList){
                if(lastPair!=null && pair.first-lastPair.first==1){
                    lastPair.first += 1;
                    lastPair.second += pair.second;
                }else{
                    if(lastPair != null)
                        addWithoutRedundantKey(list, targetRangeList, lastPair);
                    else
                        lastPair = new Pair<>();
                    lastPair.first = pair.first;
                    lastPair.second = pair.second;
                }
            }
            if(lastPair != null){
                addWithoutRedundantKey(list, targetRangeList, lastPair);
                indexList.add(lastPair.first);
            }
        }
        if(list.size() == 0){
            if((end!=-1 && isAdj(tmpSegMap.get(end).first)) || findMQ || canBeEmpty)
                block = "";
            if(!"".equals(block))
                hasChanged = false;
            list.add(new Pair<>(block, ""));
        }else if("".equals(block)){
            int min = indexList.size()-1;
            int index = 0;
            for(int i=0; i<indexList.size(); ++i){
                if(indexList.get(i) < min){
                    min = indexList.get(i);
                    index = i;
                }
            }
            Pair<String,String> pair = list.get(index);
            list = new ArrayList<>();
            list.add(pair);
        }
        // 添加位置信息
        /*if(!isHandleTopic){
            for(Pair<String,String> pair : list){
                pair.first += ("("+start+")");
            }
        }*/
        return list;
    }

    // 筛选人名，返回人名+当前处理位置的下一个位置
    public static Pair<Boolean,Pair<String, Integer>> getHN(Map<Integer, Pair<String,String>> tmpSegMap, Pair<Integer,Integer> pair, LTPTargetExtractor extractor){
        // 处理人名抽取
        // 是否有人名
        boolean hasHN = false;
        String target = "";
        double sim1 = SentimentSorter.WS.simWord(tmpSegMap.get(pair.second).second, "人");
        //double sim2 = SentimentSorter.WS.simWord(tmpSegMap.get(pair.second).second, "品德");
        if(tmpSegMap.get(pair.first).first.equals("nh") && sim1>0.7/* && sim1>sim2*/)
            hasHN = true;
        else if(extractor!=null &&  sim1>0.7/* && sim1>sim2*/){
            hasHN = true;
            target = extractor.getNeighborNH(pair.second);
            if(!"".equals(target))
                return new Pair<>(hasHN, new Pair<>(target, -1));
        }
        // 生成评价对象
        int j=pair.first;
        for(; j<pair.second; ++j){
            if(hasHN){
                if(tmpSegMap.get(j).first.startsWith("n") || tmpSegMap.get(j).first.equals("u") || tmpSegMap.get(j).first.equals("c"))
                    target += tmpSegMap.get(j).second;
                else
                    break;
            }else{
                target += tmpSegMap.get(j).second;
            }
        }
        return new Pair<>(hasHN, new Pair<>(target, j));
    }

    // 无冗余添加
    public static void addWithoutRedundantKey(List<Pair<String, String>> list, List<Pair<Integer, Integer>> targetRangeList, Pair<Integer, String> key){
        // 判断是否在范围内
        /*boolean inRange = false;
        for(Pair<Integer,Integer> pair : targetRangeList){
            if((key.first>=pair.first && key.first<=pair.second))
                inRange = true;
        }
        if(inRange)*/{
            for(Pair<String,String> pair : list){
                if(pair.first.contains(key.second))
                    return;
                else if(key.second.contains(pair.first)){
                    pair.first = key.second;
                    return;
                }
            }
        }
        list.add(new Pair<>(key.second, ""));
    }

    // 判断当前语义节点中的A0和A1中是否含有其他A0或A1结构
    public boolean hasA0OrA1(SRNode currentNode, SRNode node){
        if(node.A0!=null && currentNode.A0!=null && node.A0.first.first>=currentNode.A0.first.first && node.A0.first.second<=currentNode.A0.first.second)
            return true;
        else if(node.A1!=null && currentNode.A0!=null && node.A1.first.first>=currentNode.A0.first.first && node.A1.first.second<=currentNode.A0.first.second)
            return true;
        else if(node.A0!=null && currentNode.A1!=null && node.A0.first.first>=currentNode.A1.first.first && node.A0.first.second<=currentNode.A1.first.second)
            return true;
        else if(node.A1!=null && currentNode.A1!=null && node.A1.first.first>=currentNode.A1.first.first && node.A1.first.second<=currentNode.A1.first.second)
            return true;
        return false;
    }

    // 利用A0A1规则从语义角色节点中抽取评价对象和评价词，返回抽取的评价对象列表
    public void extractNodeByA0A1(SRNode node, int index){
        // 如果整个语义成分包含在《》中，则不解析
        if(isInBookQuotation(node.beg, node.end))
            return;
        // 寻找评价词
        int opinionCode;
        // 存放谓词+副词，A0,A1中的评价词
        List<Pair<Integer, String>> opinion = new ArrayList<>();
        List<Pair<Integer, String>> opinion0 = new ArrayList<>();
        List<Pair<Integer, String>> opinion1 = new ArrayList<>();
        // 处理A0或A1中单独一个助词存在的情况：关联前面的潜在情感词
        if(node.A0!=null && node.A0.second.length()==1 && segMap.get(node.A0.first.first).first.equals("u") && node.A0.first.first>0){
            node.A0.first.first--;
            node.A0.first.second--;
            node.A0.second = segMap.get(node.A0.first.first).second;
            if(node.A0.first.first()<node.beg)
                node.beg--;
        }
        if(node.A1!=null && node.A1.second.length()==1 && segMap.get(node.A1.first.first).first.equals("u") && node.A1.first.first>0){
            node.A1.first.first--;
            node.A1.first.second--;
            node.A1.second = segMap.get(node.A1.first.first).second;
        }
        // 谓词和副词是否是情感词
        boolean preHasOpinion = hasOpinionWord(node.predication.first, node.predication.first, opinion);
        boolean advHasOpinion = (node.adverb!=null && hasOpinionWord(node.adverb.first.first, node.adverb.first.second, opinion));
        range = new Pair<>(node.beg, node.end);
        // 判断A0和A1的副词、谓词是否是情感词，若不是且A0和A1中含有复合A0A1结构，则跳出
        if(!preHasOpinion && !advHasOpinion && index<srList.size()-1 && hasA0OrA1(node, srList.get(index+1))){
            analyseRangeList.add(range);
            return;
        }
        // A0或A1或A2是否有情感词
        boolean A0HasOpinion = (node.A0!=null && hasOpinionWord(node.A0.first.first, node.A0.first.second, opinion0));
        boolean A1hasOpinion = (node.A1!=null && hasOpinionWord(node.A1.first.first, node.A1.first.second, opinion1));
        boolean A2HasOpinion = (node.A2!=null && hasOpinionWord(node.A2.first.first, node.A2.first.second, opinion0));
        // 是否插入评价对象和评价词
        boolean putOne = false;
        // A0,A1至少要存在一个
        if(preHasOpinion || ((node.A0!=null || node.A1!=null) && (advHasOpinion || A0HasOpinion || A1hasOpinion || A2HasOpinion))) {
            // 谓词情感
            // A0 A1不存在
            if(node.A0==null && node.A1==null && (lastNode==null || node.predication.first>lastNode.end)
                    && Math.abs(SentimentSorter.getSentimentWordType(node.predication.second))!=2){
                List<Pair<String,String>> list = getPotentialTargetAndOpinion(node.A0, segMap, depTargetExtractor.getDepMap(), this, true, false);
                String target;
                for(Pair<String,String> pair0 : list){
                    target =  pair0.first;
                    if(!"".equals(target)){
                        putOne |= putTargetAndOpinion(target, node.predication.second, range, node.predication.second, false);
                    }
                }
            }
            // A0 A1存在之一
            else if((opinionCode=opinion.get(0).first) !=  0){
                if(Math.abs(opinionCode)==1 && (node.A1==null || node.predication.first<node.A1.first.first)){
                    for(Pair<String,String> pair : getPotentialTargetAndOpinion(node.A0, segMap, depTargetExtractor.getDepMap(), this, true, false)) {
                        putOne |= putTargetAndOpinion(pair.first, opinion.get(0).second, range, node.predication.second, false);
                        putOne |= putTargetAndOpinion(pair.first, pair.second, range, node.predication.second, true);
                    }
                }else{
                    for(Pair<String,String> pair : getPotentialTargetAndOpinion(node.A1, segMap, depTargetExtractor.getDepMap(), this, false, false)){
                        putOne |= putTargetAndOpinion(pair.first, opinion.get(0).second, range, node.predication.second, false);
                        putOne |= putTargetAndOpinion(pair.first, pair.second, range, node.predication.second, true);
                    }
                }
            }
            // 副词情感
            else if(node.adverb!=null && opinion.get(1).first != 0){
                List<Pair<String,String>> potentialPairList = getPotentialTargetAndOpinion(node.A0, segMap, depTargetExtractor.getDepMap(), this, true, false);
                if(node.A0==null && node.A1!=null && node.predication.first>node.A1.first.first)
                    potentialPairList = getPotentialTargetAndOpinion(node.A1, segMap, depTargetExtractor.getDepMap(), this, false, false);
                for(Pair<String,String> pair : potentialPairList){
                    if(pair.second.length() > 0)
                        putOne |= putTargetAndOpinion(pair.first, pair.second, range, node.predication.second, true);
                    else
                        putOne |= putTargetAndOpinion(pair.first, opinion.get(1).second, range, node.predication.second, false);
                }
            }
            else{

                String blackNoun = "";
                List<Pair<String,String>> potentialPairList = getPotentialTargetAndOpinion(node.A0, segMap, depTargetExtractor.getDepMap(), this, true, false);
                int num = 0;
                boolean isA0Pair = true;

                // 有A0A1或无A0有A1：A0中寻找评价对象，优先A0中寻找评价词，其次是A1
                while(node.A1!= null){
                    if(node.predication.first > node.A1.first.first){
                        potentialPairList = getPotentialTargetAndOpinion(node.A1, segMap, depTargetExtractor.getDepMap(), this, false, false);
                        //isA0Pair = false;
                    }
                    for(Pair<String,String> pair : potentialPairList){
                        if(!pair.first.equals(blackNoun)){
                            if(Math.abs(SentimentSorter.getSentimentWordType(pair.second)) == 2)
                            {
                                blackNoun = pair.first;
                                break;
                            }
                        }
                    }
                    if(opinion1.get(0).first != 0){
                        // 判断是否是与人名关联的
                        Pair<Boolean, Pair<String,Integer>> hnResult = getHN(segMap, node.A1.first, this);
                        for(Pair<String,String> pair : potentialPairList){
                            if(!pair.first.equals(blackNoun)){
                                if(!hnResult.first || num==1 || pair.first.contains(hnResult.second.first))
                                    putOne |= putTargetAndOpinion(pair.first, opinion1.get(0).second, range, node.predication.second, false);
                            }
                        }
                    }else{
                        for(Pair<String,String> pair : potentialPairList){
                            if(!pair.first.equals(blackNoun)){
                                putOne |= putTargetAndOpinion(pair.first, pair.second, range, node.predication.second, true);
                            }
                        }
                    }
                    if(putOne || num==1 || (node.A0!=null && LexUtil.HUMAN_PRONOUN.contains(" "+node.A0.second+" "))
                        || LexUtil.RELATE_COMMON_VERB.contains(" "+node.predication+" "))
                        break;
                    else{
                        potentialPairList.clear();
                        potentialPairList = getPotentialTargetAndOpinion(node.A1, segMap, depTargetExtractor.getDepMap(), this, false, false);
                        num++;
                    }
                }
                // 有A0A1或无A1有A0：若有A1则从A1中寻找评价对象，优先A0中寻找评价词，其次是自身；若无A1则从A0中寻找评价对象和评价词
                potentialPairList = getPotentialTargetAndOpinion(node.A0, segMap, depTargetExtractor.getDepMap(), this, true, false);
                if(node.A0 != null){
                    if(node.A1 != null) {
                        List<Pair<String,String>> potentialPairList1 = getPotentialTargetAndOpinion(node.A1, segMap, depTargetExtractor.getDepMap(), this, false, false);
                        // 判断是否是与人名关联的
                        Pair<Boolean, Pair<String,Integer>> hnResult = getHN(segMap, node.A1.first, this);
                        for(Pair<String,String> pair : potentialPairList) {
                            if(!hnResult.first || pair.first.contains(hnResult.second.first)){
                                for(Pair<String,String> pair1 : potentialPairList1){
                                    putOne |= putTargetAndOpinion(pair.first, pair1.second, range, node.predication.second, false);
                                }
                            }
                        }

                        if(!LexUtil.HUMAN_PRONOUN.contains(" "+node.A0.second+" ") &&  !LexUtil.RELATE_COMMON_VERB.contains(" "+node.predication+" "))
                            potentialPairList.addAll(potentialPairList1);
                    }
                    for(Pair<String,String> pair : potentialPairList){
                        if(!pair.first.equals(blackNoun)){
                            if(Math.abs(SentimentSorter.getSentimentWordType(pair.second)) == 2)
                            {
                                blackNoun = pair.first;
                                break;
                            }
                        }
                    }
                    if(opinion0.get(0).first != 0) {
                        for(Pair<String,String> pair : potentialPairList) {
                            if(!pair.first.equals(blackNoun))
                                putOne |= putTargetAndOpinion(pair.first, opinion0.get(0).second, range, node.predication.second, false);
                        }
                    }else{
                        for(Pair<String,String> pair : potentialPairList){
                            if(!pair.first.equals(blackNoun))
                                putOne |= putTargetAndOpinion(pair.first, pair.second, range, node.predication.second, true);
                        }
                    }
                }
            }

            analyseRangeList.add(range);
        }else if(node.A1!=null && ((node.predication!=null && node.predication.first<node.A1.first.first
                && node.predication.second.matches(".*[\\u6709\\u6ca1\\u65e0].*")) ||
                (node.adverb!=null  && node.adverb.first.first<node.A1.first.first
                        && node.adverb.second.matches(".*[\\u6709\\u6ca1\\u65e0].*")))){
            // 什么是否对倾向性有影响
            boolean hasWhatEffect = false;
            if(node.A1.second.contains("什么") && !node.predication.second.matches(".*[\\u6ca1\\u65e0].*") &&
                    (node.adverb==null || !node.adverb.second.matches(".*[\\u6ca1\\u65e0].*"))) // "没，无"
                hasWhatEffect = true;

            for(String nounSen : sentimentNounDic){
                if(node.A1.second.contains(nounSen)){
                    List<Pair<String,String>> potentialPairList = getPotentialTargetAndOpinion(node.A0, segMap, depTargetExtractor.getDepMap(), this, true, false);
                    for(Pair<String,String> pair : potentialPairList){
                        if(hasWhatEffect)
                            putOne |= putTargetAndOpinion(pair.first, "坏", range, node.predication.second, false);
                        else
                            putOne |= putTargetAndOpinion(pair.first, "好", range, node.predication.second, false);
                    }
                    break;
                }
            }
            analyseRangeList.add(range);
        }
    }

    // A0-A1抽取规则
    public void extractByA0A1(){
        Pair<String,String> tmpPair = new Pair<>("", "");
        boolean isValid = false;
        HashMultimap<String, String> tmpMap = HashMultimap.create();
        String key;
        for(int i=0; i<srList.size(); ++i){
            SRNode node = srList.get(i);
            curNode = node;
            extractNodeByA0A1(node, i);
            lastNode = node;

            /*if(isValid){
                if(lastTargetPair.first.equals(tmpPair.first) && !tmpPair.second.equals(lastTargetPair.second)) {
                    tmpMap.put(tmpPair.first, tmpPair.second);
                }
            }
            else */if(tmpPair.first.equals(lastTargetPair.first) && !tmpPair.second.equals(lastTargetPair.second)){
                // 添加转折词关联的评价对象
                if(targetPairMap.size()>0 && contrastDic.contains(node.dis)) {
                    //isValid = true;
                    tmpMap.put(tmpPair.first, tmpPair.second);
                }
            }
            if(lastTargetPair.first.equals(tmpPair.first) && lastTargetPair.second.equals(tmpPair.second)){
                tmpPair.first = "";
                tmpPair.second = "";
            }else{
                tmpPair.first = lastTargetPair.first;
                tmpPair.second = lastTargetPair.second;
            }
        }
        /*for(Map.Entry<String, String> entry : targetPairMap.entries()){
            key = entry.getKey();
            if(!contrastMap.containsKey(key) || entry.getValue().equals(contrastMap.get(key))) {
                tmpMap.put(entry.getKey(), entry.getValue());
            }*//*else{
                System.out.println(originalSentence);
                System.out.println(contrastMap);
                System.out.println(entry.getKey());
                System.out.println(entry.getValue());
            }*//*
        }*/
        for(Map.Entry<String,String> entry : tmpMap.entries()){
            /*System.out.println(currentSentence);
            System.out.println(entry);*/
            targetPairMap.remove(entry.getKey(), entry.getValue());
        }
    }

    // 去掉范围内情感词,获取待连接情感词
    public void removeSentimentInRange(){
        Map<Integer,String> sentimentMap = depTargetExtractor.getPotentialSentimentMap();
        Set<Integer> senDelSet = new HashSet<>();
        // 去掉已在范围内的情感词
        for(Pair<Integer,Integer> rangePair : analyseRangeList){
            for(int index : sentimentMap.keySet()){
                if(index-1>=rangePair.first && index-1<=rangePair.second)
                    senDelSet.add(index);
            }
        }
        for(int index : senDelSet)
            sentimentMap.remove(index);
    }

    // NR抽取规则
    public void extractByNE(){
        // 利用依存关系抽取出与独立的命名实体直接关联的评价词
        HashMap<Integer, String> potentialNounMap = new HashMap<>();
        //removeSentimentInRange();
        Boolean inRange;
        String lastEntity = "";
        for(int i=0; i<segMap.size(); ++i){
            /*if(analyseRangeList.size() == 0 && segMap.get(i).first.equals("n")){
                if("".equals(lastEntity)){
                    lastEntity = segMap.get(i).second;
                }else{
                    lastEntity += segMap.get(i).second;
                }
            }*/
            if(LexUtil.SPECIAL_NOUN_LTP.contains(" "+segMap.get(i).first+" ")){
                inRange = false;
                //i = findIndexFromSeg(segMap, 0, pair.second, false);
                if(i != -1){
                    for(Pair<Integer,Integer> rangePair : analyseRangeList){
                        if(i>=rangePair.first && i<=rangePair.second){
                            inRange = true;
                            break;
                        }
                    }
                    if(!inRange) {
                        analyseRangeList.add(new Pair<>(i, i));
                        if("".equals(lastEntity)){
                            lastEntity = segMap.get(i).second;
                        }else{
                            lastEntity += segMap.get(i).second;
                        }
                        continue;
                    }
                }
            }
            if(!"".equals(lastEntity)){
                potentialNounMap.put(i, lastEntity);
                //System.out.println(lastEntity);
                lastEntity = "";
            }
        }
        depTargetExtractor.clearResult();
        depTargetExtractor.setPotentialNounMap(potentialNounMap);
        LinkedHashMultimap<String, String> resultMap = depTargetExtractor.extract();
        if(resultMap.containsKey("#")){
            // 先从前文获取#指代评价对象
            String opinion = resultMap.get("#").iterator().next();
            int endIndex = opinion.lastIndexOf('(');
            if(endIndex != -1)
                opinion = opinion.substring(0, endIndex);
            int wordIndex = findIndexFromSeg(segMap, 0, opinion, false);
            this.curNode = new SRNode();
            this.curNode.beg = this.curNode.end = wordIndex;
            String target = getReferenceWord(wordIndex, this, false);
            /*if("".equals(target)){
                if(topic == null){
                    getTopicTarget();
                }
                target = topic;
            }*/
            if(!"".equals(target)){
                Set<String> values = resultMap.get("#");
                resultMap.putAll(target, values);
            }
            resultMap.removeAll("#");
        }
        for(Map.Entry<String,String> entry : resultMap.entries()){
            Pair<String,String> pair0 = new Pair<>(entry.getKey(), entry.getValue());
            if(checkValid(pair0))
                targetPairMap.put(pair0.first, pair0.second);
            /*else
                System.out.println(pair0);*/
        }
    }

    // ATT抽取规则
    public void extractByATT(){
        // 获取所有ATT关系
        List<Pair<Integer,Integer>> attList = getATTDepRangeList(0, segMap.size()-1, segMap, depTargetExtractor.getDepMap().get("ATT"));
        // 利用依存关系抽取出与ATT短语直接关联的评价词
        HashMap<Integer, String> potentialNounMap = new HashMap<>();
        removeSentimentInRange();
        Boolean inRange;
        String tmp1, tmp2, phrase="", lastTmp="";

        for(Pair<Integer,Integer> attPair : attList){
            inRange = false;
            for(Pair<Integer,Integer> rangePair : analyseRangeList){
                if(attPair.first>=rangePair.first && attPair.first<=rangePair.second){
                    inRange = true;
                    break;
                }
            }
            if(!inRange) {
                analyseRangeList.add(new Pair<>(attPair.first, attPair.second));
                // 处理n+de+a, a+de+n等情况
                boolean isStep = false;
                boolean onlyHasMQ = true;
                phrase = "";

                for(int i=attPair.first; i<attPair.second; ++i){
                    onlyHasMQ = true;
                    tmp1 = segMap.get(i).first;
                    tmp2 = segMap.get(i+1).first;
                    if(!tmp1.equals("m") && !tmp1.equals("q") && !tmp1.equals("nt") && !tmp1.equals("nd"))
                        onlyHasMQ = false;

                    if((isAdj(tmp1)  || (tmp1.equals("v") && isAdj(lastTmp)) || (!tmp1.equals("n") && SentimentSorter.getSentimentWordType(segMap.get(i).second)!=0)) && tmp2.equals("u") && i+1<attPair.second){
                        i++;
                        attPair.first = i+1;
                        phrase = "";
                        continue;
                    }else if(((tmp1.equals("u") && (isAdj(tmp2) || (!tmp2.equals("n") && SentimentSorter.getSentimentWordType(segMap.get(i+1).second)!=0))) || (isAdj(tmp1) && tmp2.equals("v") && lastTmp.equals("u"))) && i-1>=attPair.first){
                        isStep = true;
                        attPair.second = i-1;
                        break;
                    }
                    //phrase += segMap.get(i).second;
                }
                // 过滤
                /*if("m".equals(segMap.get(attPair.first).first)*//* && attPair.first+1<segMap.size() && segMap.get(attPair.first+1).first.equals("q")*//*){
                    for(int ii=attPair.first; ii<=attPair.second; ii++)
                        System.out.print("-2"+segMap.get(ii).second);
                    System.out.println();
                    continue;
                }*/
                // 处理人名抽取
                Pair<Boolean, Pair<String,Integer>> result = getHN(segMap, attPair, this);
                int j = result.second.second;
                phrase = result.second.first;

                if(!result.first || (j==attPair.second && segMap.get(attPair.second).first.startsWith("n"))){
                    if(isStep)
                        phrase += segMap.get(attPair.second).second;
                    else if(!isAdj(segMap.get(attPair.second).first) && !segMap.get(attPair.second).first.equals("nd"))
                        phrase += segMap.get(attPair.second).second;
                }
                if(!"".equals(phrase) && !onlyHasMQ){
                    potentialNounMap.put(attPair.second+1, phrase);
                }
            }
        }
        depTargetExtractor.clearResult();
        depTargetExtractor.setPotentialNounMap(potentialNounMap);
        LinkedHashMultimap<String, String> resultMap = depTargetExtractor.extract();
        if(resultMap.containsKey("#")){
            // 先从前文获取#指代评价对象
            String opinion = resultMap.get("#").iterator().next();
            int endIndex = opinion.lastIndexOf('(');
            if(endIndex != -1)
                opinion = opinion.substring(0, endIndex);
            int wordIndex = findIndexFromSeg(segMap, 0, opinion, false);
            this.curNode = new SRNode();
            this.curNode.beg = this.curNode.end = wordIndex;
            String target = getReferenceWord(wordIndex, this, false);
            /*if("".equals(target)){
                if(topic == null){
                    getTopicTarget();
                }
                target = topic;
            }*/
            if(!"".equals(target)){
                Set<String> values = resultMap.get("#");
                resultMap.putAll(target, values);
            }
                resultMap.removeAll("#");
        }
        for(Map.Entry<String,String> entry : resultMap.entries()){
            Pair<String,String> pair0 = new Pair<>(entry.getKey(), entry.getValue());
            if(checkValid(pair0))
                targetPairMap.put(pair0.first, pair0.second);
        }
    }

    // 通过人称代词寻找指代
    public void extractByPronoun(){
        // 利用依存关系抽取出与独立的命名实体直接关联的评价词
        Map<Integer, String> potentialNounMap = new HashMap<>();
        Map<String, Integer> pronIndexMap = new HashMap<>();
        removeSentimentInRange();
        Boolean inRange;
        String pron = "";

        for(int i=0; i<segMap.size(); ++i){
            pron = segMap.get(i).second;
            if(LexUtil.HUMAN_PRONOUN.contains(" "+pron+" ")){
                inRange = false;
                for(Pair<Integer,Integer> rangePair : analyseRangeList){
                    if(i>=rangePair.first && i<=rangePair.second){
                        inRange = true;
                        break;
                    }
                }
                if(!inRange) {
                    analyseRangeList.add(new Pair<>(i, i));
                    potentialNounMap.put(i+1, pron);
                    if(!pronIndexMap.containsKey(pron))
                        pronIndexMap.put(pron, i);
                }
            }
        }
        depTargetExtractor.clearResult();
        depTargetExtractor.setPotentialNounMap(potentialNounMap);
        LinkedHashMultimap<String, String> resultMap = depTargetExtractor.extract();
        /*if(resultMap.containsKey("#")){
            if(topic == null){
                getTopicTarget();
            }
            if(!"".equals(topic)){
                Set<String> values = resultMap.get("#");
                resultMap.putAll(topic, values);
                resultMap.removeAll("#");
            }
        }*/
        String nh;
        LinkedHashMultimap<String, String> newResultMap = LinkedHashMultimap.create();
        for(String key : resultMap.keySet()){
            if(pronIndexMap.containsKey(key)){
                nh = getNeighborNH(pronIndexMap.get(key));
                System.out.println(originalSentence+" "+nh);
                if(!"".equals(nh))
                    newResultMap.putAll(nh, resultMap.get(key));
            }
        }
        targetPairMap.putAll(newResultMap);
    }


    // 分析并提取出目标元组，输出
    public LinkedHashMultimap<String, String> extract(){
        extractByA0A1();
        filterVirtualSentiment();
        mark(0);
        extractByATT();
        extractByNE();
        //extractByPronoun();
        filterVirtualSentiment();
        mark(1);
        // 记录本句分析信息
        lastSentenceWeibo = originalSentence;
        lastSentenceTarget = firstTarget;
        return resultTargetPairMap;
    }
    // 是否是宾语
    public boolean isObject(String word){
        LinkedHashMultimap<String, Pair<Integer, Integer>> depMap = depTargetExtractor.getDepMap();
        int index = findIndexFromSeg(segMap, 0, word, false);
        if(index == -1)
            return false;
        index++;
        for(String dep : LexUtil.OBJECT_REL_SET_FOR_LTP){
            for(Pair<Integer, Integer> pair : depMap.get(dep)){
                if(pair.second == index)
                    return true;
            }
        }
        // ATT情况
        for(Pair<Integer, Integer> pair : depMap.get("ATT")){
            if(pair.first==index || pair.second==index)
                return true;
        }
        return false;
    }

    // 过滤虚拟语态词附近的情感词
    public void filterVirtualSentiment(){
        List<Pair<Integer, Integer>> rangeList = new ArrayList<>();
        int index, startIndex, endIndex = 0;
        for(String word : sentimentFilterDic){
            while(currentSegSentence.contains(" "+word+"/") && endIndex<currentSentence.length() && (index=currentSentence.indexOf(word, endIndex)) != -1){
                if(isObject(word)){
                    endIndex = index + 1;
                    continue;
                }
                /*for(startIndex=index-1; startIndex>0; startIndex--){
                    if(PunctuationUtil.END_PUNCTUATION.contains(originalSentence.charAt(startIndex)+""))
                        break;
                }
                //*/startIndex = index;
                for(endIndex=index+1; endIndex<currentSentence.length(); endIndex++){
                    if(PunctuationUtil.END_PUNCTUATION.contains(currentSentence.charAt(endIndex)+""))
                        break;
                }
                startIndex = startIndex < 0? 0 : startIndex;
                //System.out.println(word);
                rangeList.add(new Pair<>(startIndex, endIndex));
            }
        }

        boolean inRange;
        LinkedHashMultimap<String, String> tmpTargetPairMap = LinkedHashMultimap.create();
        for(String key : targetPairMap.keySet()){
            inRange = false;
            index = currentSentence.indexOf(key);
            for(Pair<Integer,Integer> pair : rangeList){
                if(index>=pair.first && index<=pair.second){
                    inRange = true;
                    break;
                }
            }
            if(!inRange) {
                for(String sen : targetPairMap.get(key)){
                    startIndex = sen.indexOf('(');
                    String word = sen.substring(0, startIndex);
                    inRange = false;
                    index = currentSentence.indexOf(word);
                    for(Pair<Integer,Integer> pair : rangeList){
                        if(index>=pair.first && index<=pair.second){
                            inRange = true;
                            break;
                        }
                    }
                    if(!inRange)
                        tmpTargetPairMap.put(key, sen);
                    /*else{
                        System.out.println(currentSentence+"\n"+key+" "+sen);
                    }*/
                }
            }
        }
        targetPairMap = tmpTargetPairMap;
    }

    public static void main(String args[]){
        String s = "动产/v 登记/v 条例/n 明年/nt 出台/v ,/wp 或/c 为/p 开征/v 房产税/n 做/v 铺垫/n";
        s = s.replaceAll("/[a-z ]*", "");
        System.out.println(s);

    }
}
