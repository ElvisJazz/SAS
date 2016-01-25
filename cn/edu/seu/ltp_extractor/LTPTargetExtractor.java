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
 * Time: ����4:22
 * To change this template use File | Settings | File Templates.
 */

public class LTPTargetExtractor {
    // �����ɫ�ڵ���
    class SRNode{
        // ν�� beg, seq
        public Pair<Integer,String> predication = null;
        // ���� beg,seq
        public Pair<Pair<Integer,Integer>,String> adverb = null;
        // ʩ���� beg,seq
        public Pair<Pair<Integer,Integer>,String> A0 = null;
        // ������ beg,seq
        public Pair<Pair<Integer,Integer>,String> A1 = null;
        // ��������� beg,seq
        public Pair<Pair<Integer,Integer>,String> A2 = null;
        // ����ɷ� beg,end
        public int beg;
        public int end;
    }

    // ��������
    public String[] specialNoun = {"nh", "ni", "nl", "ns", "nz"};
    // ������ؾ��ӻ���
    private String segTopicSentence;
    private String depTopicSentence;
    private String srTopicSentence;

    // ���������ϵ��ȡ����
    private TargetExtractor depTargetExtractor;
    // ���������ϵ��ȡ����
    private TargetExtractor topicDepTargetExtractor;

    // �洢�����б�λ�ã�<��ע����>
    private Map<Integer, Pair<String,String>> segMap;
    // ����洢�����б�λ�ã�<��ע����>
    private Map<Integer, Pair<String,String>> topicSegMap;

    // �洢�����������ɫ�б�:�Ƿ�����������ɫ�ڵ�
    private List<SRNode> srList;
    // �洢�����������ɫ�б�:�Ƿ�����������ɫ�ڵ�
    private List<SRNode> topicSrList;

    // �洢��ȡ�����ʺͶ�Ӧ����дʣ���Ϊ���ʡ����ݴ�a,�����Թ�����nl, ����������ng��
    private HashMultimap<String, String> targetPairMap = HashMultimap.create();
    private Pair<String, String> lastTargetPair = new Pair<>();

    // �������������Ϣ�Ƿ��Ѿ���ʼ��
    boolean isInit = false;
    // ��ǰ�Ƿ��ڲ�����������
    boolean isHandleTopic = false;

    // ��ȡ�ı����Ͼ��ӵ�����ʵ�塢�����ϵ�������ɫ
    public void readContentCorpusSentence(String segSentence, String depSentence, String srSentence) throws Exception {
        // ��¼���Ա�ע����Ϣ
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
        // ��¼�����ϵ
        depTargetExtractor = new TargetExtractor();
        depTargetExtractor.setExtractType(TargetExtractor.EXTRACT_TYPE.LTP);
        depTargetExtractor.extractPotentialWords(segSentence);
        depTargetExtractor.readRelation(depSentence);
        // ��¼�����ɫ
        srList = new ArrayList();
        readSR(srSentence, false);
    }

    // ��ȡ�������Ͼ��ӵ�����ʵ�塢�����ϵ�������ɫ
    public void readTopicCorpusSentence(String segTopicSentence, String depTopicSentence, String srTopicSentence){
        this.segTopicSentence = segTopicSentence;
        this.depTopicSentence = depTopicSentence;
        this.srTopicSentence = srTopicSentence;
    }

    // ��ȡ�����ɫ
    public void readSR(String srSentence, boolean isCorpus){
        String[] srs = srSentence.split(";");
        int index1, index2;
        int minBeg=Integer.MAX_VALUE, maxEnd=Integer.MIN_VALUE;
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
                minBeg = (minBeg<beg)? minBeg : beg;
                maxEnd = (maxEnd>end)? maxEnd : end;
                // �����һ��typeΪ���ʻ�A0��A1������һ��Ҳ��ͬ���ı�ע����ѡ����һ��ϲ�
                if(lastType.equals(type)) {
                    // ����
                    if(type.equals("ADV")){
                        if(SentimentSorter.getSentimentWordType(seq) != 0){
                            if(node.adverb != null){
                                node.adverb.first.first = beg;
                                node.adverb.first.second = end;
                                node.adverb.second = seq;
                             }
                        }
                    }
                    // ����A0��A1��A2
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
            minBeg=Integer.MAX_VALUE;
            maxEnd=Integer.MIN_VALUE;
            if(isCorpus)
                topicSrList.add(node);
            else
                srList.add(node);
        }
    }


    //  ��ʼ���������Ͼ��ӵ�����ʵ�塢�����ϵ�������ɫ
    public void initTopicInfo()  throws Exception {
        // ��¼���Ա�ע����Ϣ
        topicSegMap = new HashMap();
        String[] segs = segTopicSentence.split(" ");
        int index;
        for(int i=0; i<segs.length; ++i){
            if(segs[i].equals(""))
                continue;
            index = segs[i].lastIndexOf('/');
            if(index != -1)
                segMap.put(i, new Pair<String, String>(segs[i].substring(index+1), segs[i].substring(0,index)));
        }
        // ��¼�����ϵ
        topicDepTargetExtractor = new TargetExtractor();
        topicDepTargetExtractor.setExtractType(TargetExtractor.EXTRACT_TYPE.LTP);
        topicDepTargetExtractor.extractPotentialWords(segTopicSentence);
        topicDepTargetExtractor.readRelation(depTopicSentence);
        // ��¼�����ɫ
        topicSrList = new ArrayList();
        readSR(srTopicSentence, true);
    }

    // ������۶�������۴�map
    public void putTargetAndOpinion(String target, String opinion){
        String startWord = ""+target.charAt(0);
        String endWord = ""+target.charAt(target.length()-1);
        if(PunctuationUtil.PUNCTUATION.contains(startWord) && startWord!="��")
            target = target.substring(1);
        if(PunctuationUtil.PUNCTUATION.contains(endWord) && endWord!="��")
            target = target.substring(0, target.length()-1);
        targetPairMap.put(target, opinion);
        lastTargetPair.first = target;
        lastTargetPair.second = opinion;
    }

    // ��������ȡ��Ŀ��Ԫ�飬���
    public HashMultimap<String, String> extract(){
        extractByA0A1();
        extractByATT();
        extractByNE();
        extractByDO();
        return targetPairMap ;
    }

    // ĳһ��Χ���Ƿ������۴�
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

    // �Ƿ��ڡ�����
    public boolean isInBookQuotation(int start, int end){
        int start1=-1,end1=-1, start2=-1, end2=-1;
        for(int i=1; i<segMap.size(); ++i){
            if(start-i>0 && segMap.get(start-i).equals("��"))
                start1 = start-i;
            else if(start-i>0 && segMap.get(start-i).equals("��"))
                end1 = start-i;
            else if(end+i<segMap.size() && segMap.get(end+i).equals("��"))
                start2 = end+i;
            else if(end+i<segMap.size() && segMap.get(end+i).equals("��"))
                end2 = end+i;
        }
        if(start1>end1 && end2<start2 && end2!=-1)
            return true;
        return false;
    }

    // �ж������ųɷ��Ƿ��ָ���ʿ��ص�������ԭ�ʿ���ص��ϲ�����
    public Pair<Integer,Integer> getCombinationBlock(int start, int end){
        int _start = -1, _end = -1; // �����ſ�ʼ����index
        int rStart = -1, rEnd = -1;
        int reStart = -1, reEnd = -1;
        for(int i=0; i<segMap.size(); ++i){
            if(segMap.get(i).equals("��") && _start==-1)
                _start = i;
            else if(segMap.get(i).equals("��") && start!=-1){
                _end = i;
                // �ж�
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

    // Ѱ��ATT��ص�������ϵ���Ƿ�Χ start-end����: 0-(N-1)
    public List<Pair<Integer, Integer>> getATTDepRangeList(int start, int end){
        Set<Pair<Integer, Integer>> ATTDepSet;
        if(isHandleTopic)
            ATTDepSet = topicDepTargetExtractor.getDepMap().get("ATT");
        else
            ATTDepSet = depTargetExtractor.getDepMap().get("ATT");

        SortedSet<Integer> ATTNodeSet = new TreeSet<Integer>();
        List<Pair<Integer, Integer>> resultList = new ArrayList<>();
        int _start=-1, _end=-1;
        int min, max;
        for(Pair<Integer, Integer> p : ATTDepSet){
            if(p.first>=start+1 && p.first<=end+1 && p.second>=start+1 && p.second<=end+1){
                min = Math.min(p.first-1, p.second-1);
                max = Math.max(p.first-1, p.second-1);
                for(int i=min; i<=max; ++i)
                    ATTNodeSet.add(i);
            }
        }
        // �����ڵ㣬���㸲�Ƿ�Χ
        for(int i : ATTNodeSet){
            if(i-end > 1){
                if(_end != -1)
                    resultList.add(getCombinationBlock(_start, _end));
                _start = _end = i;
            }else{
                if(_end == -1)
                    _start = i;
                _end = i;
            }
        }
        return resultList;
    }

    // �ӻ�����Ѱ�����۶���
    public String getTopicTarget(){
        if(!isInit){
            try{
                initTopicInfo();
            }catch (Exception e){
                System.err.println("�������������Ϣ��ʼ��ʧ�ܣ�");
                e.printStackTrace();
                return "";
            }
        }
        isHandleTopic = true;
        Pair<Pair<Integer,Integer>,String> pair = new Pair<>(new Pair<>(0,topicSegMap.size()-1), segTopicSentence.replaceAll("/[a-z ]*", ""));
        String result = getPotentialTargetAndOpionion(pair).get(0).first;
        isHandleTopic = false;
        return result;
    }

    // ָ��������������Ѱ����һ��
    public String getReferenceWord(){
        int size = targetPairMap.size();
        if(size > 0){
            return lastTargetPair.first;
        }
        return getTopicTarget();
    }

    // ��ĳһ�ɷ��г��Ǳ�����۶�������۴�
    public List<Pair<String,String>> getPotentialTargetAndOpionion(Pair<Pair<Integer,Integer>,String> nodeAPair){
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
        // ���ʻ�����A0��A1ָ����Ѱ
        if(!isHandleTopic && block=="" || (block.length()==1 && tmpSegMap.get(start).first.equals("r")))
            list.add(new Pair<String, String>(getReferenceWord(), ""));
        // �����Ƿ񱻡�������
        if(block.startsWith("��") && block.endsWith("��"))
            list.add(new Pair<String, String>(block,""));
        else{
            // ATT���ְ���adj
            List<Pair<Integer, Integer>> targetRangeList = getATTDepRangeList(start, end);
            String target = "", opinion = "";
            String tmp1, tmp2;
            for(Pair<Integer,Integer> pair : targetRangeList){
                for(int i=pair.first; i<pair.second; ++i){
                    tmp1 = tmpSegMap.get(i).first;
                    tmp2 = tmpSegMap.get(i+1).first;
                    if(tmp1.equals("a") && tmp2.equals("u") && i+1<pair.second){
                        pair.first = i+2;
                        opinion = tmp1;
                    }
                    else if(tmp1.equals("u") && tmp2.equals("a") && i-1>=pair.first){
                        pair.second = i-1;
                        opinion = tmp2;
                    }
                }
                for(int j=pair.first; j<=pair.second; ++j)
                    target += tmpSegMap.get(j);

                if(!target.equals("")){
                    list.add(new Pair<String,String>(target,opinion));
                    target = "";
                    opinion = "";
                }
            }
            // �������Ҳ���֮ǰ��ATT��
            int index1 = block.indexOf("��");
            int index2;
            while(index1 != -1){
                index2 = block.indexOf("��", index1+1);
                if(index2 != -1){
                    for(Pair<Integer, Integer> pair : targetRangeList){
                        if(index2<pair.first || index1>pair.second){
                            for(int i=index1; i<=index2; ++i){
                                target += tmpSegMap.get(i).second;
                            }
                            list.add(new Pair<>(target,""));
                            target = "";
                        }
                    }
                }else{
                    break;
                }
                index1 = block.indexOf("��", index2+1);
            }
            // Ѱ�Ҳ���ATT�Ҳ��ڡ����е�����ʵ��
            List<Pair<Integer,String>> nrList = new ArrayList<>();
            for(int i=start; i<=end; ++i){
                for(String noun : specialNoun){
                    if(tmpSegMap.get(i).first.equals(noun)){
                        for(Pair<Integer,Integer> pair : targetRangeList){
                            if(i>=pair.first && i<=pair.second){
                                nrList.add(new Pair<>(i,tmpSegMap.get(i).second));
                            }
                        }
                    }
                }
            }
            // �ϲ��ٽ���ʵ��
            Pair<Integer,String> lastPair = null;
            for(Pair<Integer,String> pair : nrList){
                if(lastPair!=null && pair.first-lastPair.first==1){
                    lastPair.first += 1;
                    lastPair.second += pair.second;
                }else{
                    if(lastPair != null)
                        list.add(new Pair<>(lastPair.second, ""));
                    lastPair.first = pair.first;
                    lastPair.second = pair.second;
                }
            }
        }
        if(list.size() == 0)
            list.add(block);
        return list;
    }

    // �жϵ�ǰ����ڵ��е�A0��A1���Ƿ�������A0��A1�ṹ
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

    // ����A0A1����������ɫ�ڵ��г�ȡ���۶�������۴ʣ����س�ȡ�����۶����б�
    public void extractNodeByA0A1(SRNode node, int index){
        // �����������ɷְ����ڡ����У��򲻽���
        if(isInBookQuotation(node.beg, node.end))
            return;
        // Ѱ�����۴�
        int opinionCode;
        // ���ν��+���ʣ�A0,A1�е����۴�
        List<Pair<Integer, String>> opinion = new ArrayList<>();
        List<Pair<Integer, String>> opinion0 = new ArrayList<>();
        List<Pair<Integer, String>> opinion1 = new ArrayList<>();
        // ����A0��A1�е���һ�����ʴ��ڵ����������ǰ���Ǳ����д�
        if(node.A0!=null && node.A0.second.length()==1 && segMap.get(node.A0.first.first).first.equals("u") && node.A0.first.first>0){
            node.A0.first.first--;
            node.A0.first.second--;
            node.A0.second = segMap.get(node.A0.first.first).second;
        }
        if(node.A1!=null && node.A1.second.length()==1 && segMap.get(node.A1.first.first).first.equals("u") && node.A1.first.first>0){
            node.A1.first.first--;
            node.A1.first.second--;
            node.A1.second = segMap.get(node.A1.first.first).second;
        }
        // ν�ʺ͸����Ƿ�����д�
        boolean preHasOpinion = hasOpinionWord(node.predication.first, node.predication.first, opinion);
        boolean advHasOpinion = (node.adverb!=null && hasOpinionWord(node.adverb.first.first, node.adverb.first.second, opinion));
        // �ж�A0��A1�ĸ��ʡ�ν���Ƿ�����дʣ���������A0��A1�к��и���A0A1�ṹ��������
        if(!preHasOpinion && !advHasOpinion && index<srList.size()-1 && hasA0OrA1(node, srList.get(index+1)))
            return;
        // A0,A1����Ҫ����һ��
        if((node.A0!=null || node.A1!=null) && (preHasOpinion || advHasOpinion ||
                (node.A0!=null && hasOpinionWord(node.A0.first.first, node.A0.first.second, opinion0)) ||
                (node.A1!=null && hasOpinionWord(node.A1.first.first, node.A1.first.second, opinion1)) ||
                (node.A2!=null && hasOpinionWord(node.A2.first.first, node.A2.first.second, opinion0)))) {

            // ν�����
            if((opinionCode=opinion.get(0).first) !=  0){
                if(Math.abs(opinionCode) == 1){
                    for(Pair<String,String> pair : getPotentialTargetAndOpionion(node.A0))
                        putTargetAndOpinion(pair.first, opinion.get(0).second);
                }else{
                    for(Pair<String,String> pair : getPotentialTargetAndOpionion(node.A1))
                        putTargetAndOpinion(pair.first, opinion.get(0).second);
                }
            }
            // �������
            else if(node.adverb!=null && opinion.get(1).first != 0){
                for(Pair<String,String> pair : getPotentialTargetAndOpionion(node.A0))
                    putTargetAndOpinion(pair.first, opinion.get(1).second);
            }
            else{
                List<Pair<String,String>> potentialPairList;
                // ��A0A1����A0��A1��A0��Ѱ�����۶�������A1��Ѱ�����۴ʣ����������
                if(node.A1 != null){
                    potentialPairList = getPotentialTargetAndOpionion(node.A0);
                    if(opinion1.get(0).first != 0) {
                        for(Pair<String,String> pair : potentialPairList)
                            putTargetAndOpinion(pair.first, opinion1.get(0).second);
                    }else{
                        for(Pair<String,String> pair : potentialPairList)
                            putTargetAndOpinion(pair.first, pair.second);
                    }
                }
                // ��A0A1����A1��A0������A1���A1��Ѱ�����۶�������A0��Ѱ�����۴ʣ��������������A1���A0��Ѱ�����۶�������۴�
                if(node.A0 != null){
                    if(node.A1 != null)
                        potentialPairList = getPotentialTargetAndOpionion(node.A1);
                    else
                        potentialPairList = getPotentialTargetAndOpionion(node.A0);
                    if(opinion0.get(0).first != 0) {
                        for(Pair<String,String> pair : potentialPairList)
                            putTargetAndOpinion(pair.first, opinion0.get(0).second);
                    }else{
                        for(Pair<String,String> pair : potentialPairList)
                            putTargetAndOpinion(pair.first, pair.second);
                    }
                }
            }
        }
    }

    // A0-A1��ȡ����
    public void extractByA0A1(){
        for(int i=0; i<srList.size(); ++i){
            SRNode node = srList.get(i);
            extractNodeByA0A1(node, i);
        }
    }

    // NR��ȡ����
    public void extractByNE(){

    }

    // ATT��ȡ����
    public void extractByATT(){

    }

    // DO������дʳ�ȡ����
    public void extractByDO(){

    }

    public static void main(String args[]){
        String s = "����/v �Ǽ�/v ����/n ����/nt ��̨/v ,/wp ��/c Ϊ/p ����/v ����˰/n ��/v �̵�/n";
        s = s.replaceAll("/[a-z ]*", "");
        System.out.println(s);

    }
}
