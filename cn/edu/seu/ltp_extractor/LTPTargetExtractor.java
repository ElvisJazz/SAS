package cn.edu.seu.ltp_extractor;

import cn.edu.seu.utils.PunctuationUtil;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.TreeMultimap;
import edu.stanford.nlp.util.Pair;
import cn.edu.seu.extractor.TargetExtractor;
import cn.edu.seu.SentimentSorter;

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

    // 特殊名词
    public String specialNoun = "nh ni nl ns nz ";
    // 主题相关句子缓存
    private String segTopicSentence;
    private String depTopicSentence;
    private String srTopicSentence;

    // 正文依存关系抽取对象
    private TargetExtractor depTargetExtractor;
    // 主题依存关系抽取对象
    private TargetExtractor topicDepTargetExtractor;

    // 存储词性列表：位置，<标注，词>
    private Map<Integer, Pair<String,String>> segMap;
    // 主题存储词性列表：位置，<标注，词>
    private Map<Integer, Pair<String,String>> topicSegMap;

    // 存储正文中语义角色列表:是否处理过，语义角色节点
    private List<SRNode> srList;
    // 存储主题中语义角色列表:是否处理过，语义角色节点
    private List<SRNode> topicSrList;

    // 存储抽取的名词和对应的情感词（可为动词、形容词a,名词性惯用语nl, 名词性语素ng）
    private HashMultimap<String,String> resultTargetPairMap = HashMultimap.create();
    private HashMultimap<String,String> targetPairMap = HashMultimap.create();
    private Pair<String, String> lastTargetPair = new Pair<>();

    // 主题语料相关信息是否已经初始化
    private boolean isInit = false;
    // 当前是否在操作主题语料
    private boolean isHandleTopic = false;

    // 当前已分析过的A0,A1结构范围
    private List<Pair<Integer,Integer>> analyseRangeList = new ArrayList<>();

    // 读取文本语料句子的命名实体、依存关系、语义角色
    public void readContentCorpusSentence(String segSentence, String depSentence, String srSentence) throws Exception {
        // 记录词性标注等信息
        segMap = new HashMap();
        String[] segs = segSentence.split(" ");
        int index;
        for(int i=0; i<segs.length; ++i){
            if(segs[i].equals(""))
                continue;
            index = segs[i].lastIndexOf('/');
            if(index != -1)
                segMap.put(i, new Pair<String, String>(segs[i].substring(index+1), segs[i].substring(0,index)));
        }
        // 记录依存关系
        depTargetExtractor = new TargetExtractor();
        depTargetExtractor.setExtractType(TargetExtractor.EXTRACT_TYPE.LTP);
        depTargetExtractor.extractPotentialWords(segSentence);
        depTargetExtractor.readRelation(depSentence);
        // 记录语义角色
        srList = new ArrayList();
        readSR(srSentence, false);
    }

    // 读取主题语料句子的命名实体、依存关系、语义角色
    public void readTopicCorpusSentence(String segTopicSentence, String depTopicSentence, String srTopicSentence){
        this.segTopicSentence = segTopicSentence;
        this.depTopicSentence = depTopicSentence;
        this.srTopicSentence = srTopicSentence;
    }

    // 读取语义角色
    public void readSR(String srSentence, boolean isCorpus){
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
            String type="", seq="", lastType="";
            int beg=0, end, lastEnd=0;
            while(i<seqs.length){
                type = seqs[i].replaceAll("type=", "");
                beg =  Integer.valueOf(seqs[i+1].replaceAll("beg=", ""));
                end =  Integer.valueOf(seqs[i+2].replaceAll("end=", ""));
                seq =  seqs[i+3].replaceAll("seq=", "");
                minBeg = (minBeg<beg)? minBeg : beg;
                maxEnd = (maxEnd>end)? maxEnd : end;
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
                }else if(type.equals("A0")){
                    node.A0 = new Pair<>(new Pair<>(beg,end),seq);
                }else if(type.equals("A1")){
                    node.A1 = new Pair<>(new Pair<>(beg,end), seq);
                }else if(type.equals("A2")){
                    node.A2 = new Pair<>(new Pair<>(beg,end), seq);
                }else if(type.equals("ADV")){
                    node.adverb = new Pair<>(new Pair<>(beg,end), seq);
                }
                lastEnd = end;
                i += 4;
            }
            node.beg = minBeg;
            node.end = maxEnd;
            if(isCorpus)
                topicSrList.add(node);
            else
                srList.add(node);
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
                topicSegMap.put(i, new Pair<String, String>(segs[i].substring(index+1), segs[i].substring(0,index)));
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

    // 填充评价对象和评价词map
    public boolean putTargetAndOpinion(String target, String opinion, String adverb, String verb){
        if(target==null || target.trim().equals("") || opinion==null || opinion.trim().equals("") || target.equals(opinion))
            return false;
        char startWord = target.charAt(0);
        char endWord = target.charAt(target.length()-1);
        if(startWord!='《' && PunctuationUtil.PUNCTUATION.contains(""+startWord))
            target = target.substring(1);
        if(target.length()!=0 && endWord!='》' && PunctuationUtil.PUNCTUATION.contains(""+endWord))
            target = target.substring(0, target.length()-1);

        if(isReverse(adverb, verb))
            opinion = "(-)"+opinion;
        targetPairMap.put(target, opinion);
        lastTargetPair.first = target;
        lastTargetPair.second = opinion;
        return true;
    }

    // 情感词反转
    public boolean isReverse(String adverb, String verb){
        boolean advWhite = false, vWhite = false;
        boolean flag = false;
        // 否定词白名单
        for(String whiteWord : TargetExtractor.NOT_WHITE_SET){
            if(adverb.contains(whiteWord))
                advWhite = true;
            if(verb.contains(whiteWord))
                vWhite = true;
        }

        // 否定词
        for(String notWord : TargetExtractor.NOT_SET){
            if(!advWhite && notWord.equals(adverb))
                flag = !flag;
            if(!vWhite && notWord.equals(verb))
                flag = !flag;
        }
        return flag;
    }

    // 分析并提取出目标元组，输出
    public HashMultimap<String, String> extract(){
        extractByA0A1();
        mark(0);
        extractByATT();
        extractByNE();
        mark(1);
        return resultTargetPairMap;
    }

    // 打标 markNum: 0,取自语义分析 1,取自依存分析
    public void mark(int markNum){
        for(Map.Entry<String,String> entry : targetPairMap.entries()){
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
    public Pair<Integer,Integer> getCombinationBlock(int start, int end){
        int _start = -1, _end = -1; // 书名号开始结束index
        int rStart = -1, rEnd = -1;
        int reStart = -1, reEnd = -1;
        for(int i=0; i<segMap.size(); ++i){
            if(segMap.get(i).equals("《") && _start==-1)
                _start = i+1;
            else if(segMap.get(i).equals("》") && start!=-1){
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
    public List<Pair<Integer, Integer>> getATTDepRangeList(int start, int end){
        Set<Pair<Integer, Integer>> ATTDepSet;
        if(isHandleTopic)
            ATTDepSet = topicDepTargetExtractor.getDepMap().get("ATT");
        else
            ATTDepSet = depTargetExtractor.getDepMap().get("ATT");

        SortedSet<Float> ATTNodeSet = new TreeSet<Float>();
        List<Pair<Integer, Integer>> resultList = new ArrayList<>();
        float _start=-1, _end=-1;
        int min, max;
        for(Pair<Integer, Integer> p : ATTDepSet){
            if(p.first>=start+1 && p.first<=end+1 && p.second>=start+1 && p.second<=end+1){
                min = Math.min(p.first-1, p.second-1);
                max = Math.max(p.first-1, p.second-1);
                for(int i=min; i<max; ++i){
                    ATTNodeSet.add((float)i);
                    ATTNodeSet.add((float)(i+0.5));
                }
                ATTNodeSet.add((float)max);
            }
        }
        // 遍历节点，计算覆盖范围
        for(float i : ATTNodeSet){
            if(i-_end >= 1.0){
                if(_end != -1.0)
                    resultList.add(getCombinationBlock((int)_start, (int)_end));
                _start = _end = i;
            }else{
                if(_end == -1.0)
                    _start = i;
                _end = i;
            }
        }
        if(_start!=-1.0 && _end!=-1.0)
            resultList.add(getCombinationBlock((int)_start, (int)_end));
        return resultList;
    }

    // 从话题中寻找评价对象
    public String getTopicTarget(){
        if(!isInit){
            try{
                initTopicInfo();
            }catch (Exception e){
                System.err.println("主题语料相关信息初始化失败！");
                e.printStackTrace();
                return "";
            }
        }
        if(topicSegMap.size() > 0){
            isHandleTopic = true;
            Pair<Pair<Integer,Integer>,String> pair = new Pair<>(new Pair<>(0,topicSegMap.size()-1), segTopicSentence.replaceAll("/[a-z ]*", ""));
            List<Pair<String,String>> list = getPotentialTargetAndOpinion(pair);
            String result = list.size()>0 ? list.get(0).first : "";
            isHandleTopic = false;
            return result;
        }
        return "";
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
    public String getReferenceWord(int index){
        String noun;
        if(index>0 && !(noun=getContinurousNoun(index-1)).equals(""))
            return noun;
        int size = targetPairMap.size();
        if(size > 0){
            return lastTargetPair.first;
        }
        return getTopicTarget();
    }

    // 从指定范围内在分词序列中寻找指定词的下标
    public int findIndexFromSeg(Map<Integer, Pair<String,String>> map, int start, String word) {
        for(int i=start; i<map.size(); ++i){
            if(map.get(i).second.equals(word))
                return i;
        }
        return -1;
    }

    // 从某一成分中抽出潜在评价对象和评价词
    public List<Pair<String,String>> getPotentialTargetAndOpinion(Pair<Pair<Integer,Integer>,String> nodeAPair){
        String block = "";
        Map<Integer, Pair<String,String>> tmpSegMap;
        if(isHandleTopic)
            tmpSegMap = topicSegMap;
        else
            tmpSegMap = segMap;

        int start=0, end=0;
        if(nodeAPair != null){
            block = nodeAPair.second;
            start = nodeAPair.first.first;
            end = nodeAPair.first.second;
        }
        List list = new ArrayList<Pair<String,String>>();
        // 代词或隐性A0或A1指代搜寻
        if(!isHandleTopic && block=="" || (block.length()==1 && (tmpSegMap.get(start).first.equals("r") || tmpSegMap.get(start).first.equals("m"))))
            list.add(new Pair<String, String>(getReferenceWord(start), ""));
        // 考虑是否被《》包裹
        if(block.startsWith("《") && block.endsWith("》"))
            list.add(new Pair<String, String>(block.substring(1, block.length()-1),""));
        else{
            // ATT部分包括adj
            List<Pair<Integer, Integer>> targetRangeList = getATTDepRangeList(start, end);
            String target = "", opinion = "";
            String tmp1, tmp2;
            for(Pair<Integer,Integer> pair : targetRangeList){
                for(int i=pair.first; i<pair.second; ++i){
                    tmp1 = tmpSegMap.get(i).first;
                    tmp2 = tmpSegMap.get(i+1).first;
                    if((tmp1.equals("a") || SentimentSorter.getSentimentWordType(tmpSegMap.get(i).second)!=0) && tmp2.equals("u") && i+1<pair.second){
                        pair.first = i+2;
                        opinion = tmp1;
                    }
                    else if(tmp1.equals("u") && (tmp2.equals("a") || SentimentSorter.getSentimentWordType(tmpSegMap.get(i+1).second)!=0) && i-1>=pair.first){
                        pair.second = i-1;
                        opinion = tmp2;
                    }
                }
                for(int j=pair.first; j<=pair.second; ++j)
                    target += tmpSegMap.get(j).second;

                if(!target.equals("")){
                    list.add(new Pair<String,String>(target,opinion));
                    target = "";
                    opinion = "";
                }
            }
            // 含《》且不在之前的ATT中
            List<Pair<Integer,Integer>> bookQuotationList = new ArrayList<>();
            int index1 =  findIndexFromSeg(tmpSegMap, start, "《"); //block.indexOf("《");
            int index2;
            while(index1!=-1 && index1>=start && index1<=end){
                index2 = findIndexFromSeg(tmpSegMap, index1+1, "》"); //block.indexOf("》", index1+1);
                if(index2 != -1){
                    if(targetRangeList.size() > 0){
                        for(Pair<Integer, Integer> pair : targetRangeList){
                            if(index2<pair.first || index1>pair.second){
                                for(int i=index1; i<=index2; ++i){
                                    target += tmpSegMap.get(i).second;
                                }
                                bookQuotationList.add(new Pair<>(index1+1, index2-1));
                                list.add(new Pair<>(target,""));
                                target = "";
                            }
                        }
                    }else{
                        for(int i=index1; i<=index2; ++i){
                            target += tmpSegMap.get(i).second;
                        }
                        bookQuotationList.add(new Pair<>(index1+1, index2-1));
                        list.add(new Pair<>(target,""));
                        target = "";
                    }
                }else{
                    break;
                }
                index1 = findIndexFromSeg(tmpSegMap, index2+1, "《"); //block.indexOf("》", index2+1);
            }
            // 寻找不在ATT且不在《》中的命名实体
            List<Pair<Integer,String>> nrList = new ArrayList<>();
            boolean step = false;
            for(int i=start; i<=end; ++i){
                if(specialNoun.contains(tmpSegMap.get(i).first+" ")){
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
                        for(Pair<Integer,Integer> pair : targetRangeList){
                            if(i<pair.first || i>pair.second){
                                nrList.add(new Pair<>(i,tmpSegMap.get(i).second));
                            }
                        }
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
                        list.add(new Pair<>(lastPair.second, ""));
                    else
                        lastPair = new Pair<>();
                    lastPair.first = pair.first;
                    lastPair.second = pair.second;
                }
            }
            if(lastPair != null)
                list.add(new Pair<>(lastPair.second, ""));
        }
        if(list.size() == 0 && !tmpSegMap.get(end).first.equals("a"))
            list.add(new Pair<>(block, ""));
        return list;
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
        // 判断A0和A1的副词、谓词是否是情感词，若不是且A0和A1中含有复合A0A1结构，则跳出
        if(!preHasOpinion && !advHasOpinion && index<srList.size()-1 && hasA0OrA1(node, srList.get(index+1)))
            return;
        // A0或A1或A2是否有情感词
        boolean A0HasOpinion = (node.A0!=null && hasOpinionWord(node.A0.first.first, node.A0.first.second, opinion0));
        boolean A1hasOpinion = (node.A1!=null && hasOpinionWord(node.A1.first.first, node.A1.first.second, opinion1));
        boolean A2HasOpinion = (node.A2!=null && hasOpinionWord(node.A2.first.first, node.A2.first.second, opinion0));
        // 是否插入评价对象和评价词
        boolean putOne = false;
        // A0,A1至少要存在一个
        if((node.A0!=null || node.A1!=null) && (preHasOpinion || advHasOpinion || A0HasOpinion || A1hasOpinion || A2HasOpinion)) {
            // 获取副词
            String adv = node.adverb!=null? node.adverb.second : "";
            // 谓词情感
            if((opinionCode=opinion.get(0).first) !=  0){
                if(Math.abs(opinionCode) == 1){
                    for(Pair<String,String> pair : getPotentialTargetAndOpinion(node.A0))
                        putOne = putTargetAndOpinion(pair.first, opinion.get(0).second, adv, node.predication.second);
                }else{
                    for(Pair<String,String> pair : getPotentialTargetAndOpinion(node.A1))
                        putOne = putTargetAndOpinion(pair.first, opinion.get(0).second, adv, node.predication.second);
                }
            }
            // 副词情感
            else if(node.adverb!=null && opinion.get(1).first != 0){
                List<Pair<String,String>> potentialPairList = getPotentialTargetAndOpinion(node.A0);
                if(node.A0==null && node.A1!=null && node.predication.first>node.A1.first.first)
                    potentialPairList = getPotentialTargetAndOpinion(node.A1);
                for(Pair<String,String> pair : potentialPairList)
                    putOne = putTargetAndOpinion(pair.first, opinion.get(1).second, adv, node.predication.second);
            }
            else{
                List<Pair<String,String>> potentialPairList = getPotentialTargetAndOpinion(node.A0);
                int num = 0;
                // 有A0A1或无A0有A1：A0中寻找评价对象，优先A1中寻找评价词，其次是自身
                while(node.A1!= null && !putOne){
                    if(node.predication.first > node.A1.first.first)
                        potentialPairList = getPotentialTargetAndOpinion(node.A1);
                    if(opinion1.get(0).first != 0) {
                        for(Pair<String,String> pair : potentialPairList)
                            putOne = putTargetAndOpinion(pair.first, opinion1.get(0).second, adv, node.predication.second);
                    }else{
                        for(Pair<String,String> pair : potentialPairList)
                            putOne = putTargetAndOpinion(pair.first, pair.second, adv, node.predication.second);
                    }
                    if(putOne || num==1)
                        break;
                    else{
                        potentialPairList.clear();
                        potentialPairList = getPotentialTargetAndOpinion(node.A1);
                        num++;
                    }
                }
                // 有A0A1或无A1有A0：若有A1则从A1中寻找评价对象，优先A0中寻找评价词，其次是自身；若无A1则从A0中寻找评价对象和评价词
                if(node.A0 != null){
                    if(node.A1 != null) {
                        potentialPairList.addAll(getPotentialTargetAndOpinion(node.A1));
                    }
                    if(opinion0.get(0).first != 0) {
                        for(Pair<String,String> pair : potentialPairList)
                            putOne = putTargetAndOpinion(pair.first, opinion0.get(0).second, adv, node.predication.second);
                    }else{
                        for(Pair<String,String> pair : potentialPairList)
                            putOne = putTargetAndOpinion(pair.first, pair.second, adv, node.predication.second);
                    }
                }
            }
            //if(putOne)
                analyseRangeList.add(new Pair<>(node.beg, node.end));
        }
    }

    // A0-A1抽取规则
    public void extractByA0A1(){
        for(int i=0; i<srList.size(); ++i){
            SRNode node = srList.get(i);
            extractNodeByA0A1(node, i);
        }
    }

    // NR抽取规则
    public void extractByNE(){
        // 利用依存关系抽取出与独立的命名实体直接关联的评价词
        HashMap<Integer, String> potentialNounMap = new HashMap<>();
        Boolean inRange = false;
        String lastEntity = "", entity = "";
        for(int i=0; i<segMap.size(); ++i){
            if(specialNoun.contains(segMap.get(i).first+" ")){
                inRange = false;
                for(Pair<Integer,Integer> rangePair : analyseRangeList){
                    if(i>=rangePair.first && i<=rangePair.second){
                        inRange = true;
                        break;
                    }
                }
                if(!inRange) {
                    if("".equals(lastEntity)){
                        lastEntity = segMap.get(i).second;
                    }else{
                        lastEntity += segMap.get(i).second;
                    }
                    continue;
                }
            }
            if(!"".equals(lastEntity)){
                potentialNounMap.put(i, lastEntity);
                lastEntity = "";
            }
        }
        depTargetExtractor.clearResult();
        depTargetExtractor.setPotentialNounMap(potentialNounMap);
        targetPairMap.putAll(depTargetExtractor.extract());
    }

    // ATT抽取规则
    public void extractByATT(){
        // 获取所有ATT关系
        List<Pair<Integer,Integer>> attList = getATTDepRangeList(0, segMap.size()-1);
        // 利用依存关系抽取出与ATT短语直接关联的评价词
        HashMap<Integer, String> potentialNounMap = new HashMap<>();
        Boolean inRange;
        String tmp1, tmp2, phrase="";
        for(Pair<Integer,Integer> attPair : attList){
            inRange = false;
            for(Pair<Integer,Integer> rangePair : analyseRangeList){
                if(attPair.first>=rangePair.first && attPair.first<=rangePair.second){
                    inRange = true;
                    break;
                }
            }
            if(!inRange) {
                // 处理n+de+a, a+de+n等情况
                boolean isStep = false;
                for(int i=attPair.first; i<attPair.second; ++i){
                    tmp1 = segMap.get(i).first;
                    tmp2 = segMap.get(i+1).first;
                    if((tmp1.equals("a") || SentimentSorter.getSentimentWordType(segMap.get(i).second)!=0) && tmp2.equals("u") && i+1<attPair.second){
                        i++;
                        phrase = "";
                        continue;
                    }
                    else if(tmp1.equals("u") && (tmp2.equals("a") || SentimentSorter.getSentimentWordType(segMap.get(i+1).second)!=0) && i-1>=attPair.first){
                        isStep = true;
                        break;
                    }
                    phrase += segMap.get(i).second;
                }
                if(!isStep && !segMap.get(attPair.second).first.equals("a"))
                    phrase += segMap.get(attPair.second).second;
                potentialNounMap.put(attPair.second+1, phrase);
                phrase = "";
            }
        }
        depTargetExtractor.clearResult();
        depTargetExtractor.setPotentialNounMap(potentialNounMap);
        targetPairMap.putAll(depTargetExtractor.extract());
    }

    public static void main(String args[]){
        String s = "动产/v 登记/v 条例/n 明年/nt 出台/v ,/wp 或/c 为/p 开征/v 房产税/n 做/v 铺垫/n";
        s = s.replaceAll("/[a-z ]*", "");
        System.out.println(s);

    }
}
