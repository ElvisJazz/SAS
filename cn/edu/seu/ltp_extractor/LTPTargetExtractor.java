package cn.edu.seu.ltp_extractor;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.TreeMultimap;
import edu.stanford.nlp.util.Pair;
import cn.edu.seu.extractor.TargetExtractor;
import cn.edu.seu.SentimentSorter;

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
    public String[] specialNoun = {"/nh", "/ni", "/nl", "/ns", "/nz"};
    // ������ؾ��ӻ���
    private String segTopicSentence;
    private String depTopicSentence;
    private String srTopicSentence;
    // ���������ϵ��ȡ����
    private TargetExtractor depTargetExtractor;
    // ���������ϵ��ȡ����
    private TargetExtractor topicDepTargetExtractor;
    // �洢�����б�λ�ã�<��ע����>
    private Map<Integer, Pair<String,String>> segMap = new HashMap();
    // �洢�����������ɫ�б�
    private List<SRNode> srList = new ArrayList();
    // �洢��ȡ�����ʺͶ�Ӧ����дʣ���Ϊ���ʡ����ݴ�a,�����Թ�����nl, ����������ng��
    private HashMultimap<String, String> targetPairMap = HashMultimap.create();
    private Pair<String, String> lastTargetPair = new Pair<>();


    // ��ȡ�ı����Ͼ��ӵ�����ʵ�塢�����ϵ�������ɫ
    public void readContentCorpusSentence(String segSentence, String depSentence, String srSentence) throws Exception {
        // ��¼���Ա�ע����Ϣ
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
        readSR(srSentence);
    }

    // ��ȡ�������Ͼ��ӵ�����ʵ�塢�����ϵ�������ɫ
    public void readTopicCorpusSentence(String segTopicSentence, String depTopicSentence, String srTopicSentence){
        this.segTopicSentence = segTopicSentence;
        this.depTopicSentence = depTopicSentence;
        this.srTopicSentence = srTopicSentence;
    }

    // ��ȡ�����ɫ
    public void readSR(String srSentence){
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
            srList.add(node);
        }
    }


    //  ��ʼ���������Ͼ��ӵ�����ʵ�塢�����ϵ�������ɫ
    public void initTopicInfo(){

    }

    // ������۶�������۴�map
    public void putTargetAndOpinion(String target, String opinion){
        targetPairMap.put(target, opinion);
        lastTargetPair.first = target;
        lastTargetPair.second = opinion;
    }

    // ��������ȡ��Ŀ��Ԫ�飬���
    public HashMultimap<String, String> extract(){

        return null;
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
        Set<Pair<Integer, Integer>> ATTDepSet = depTargetExtractor.getDepMap().get("ATT");
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
        return "";
    }

    // ָ������������
    public String getReferenceWord(){
        int size = targetPairMap.size();
        if(size > 0){
            return lastTargetPair.first;
        }
        return getTopicTarget();
    }

    // ��ĳһ�ɷ��г��Ǳ�����۶�������۴�
    public List<Pair<String,String>> getPotentialTargetAndOpionion(String block, int start, int end, boolean considerComposite, boolean isAType){
        List list = new ArrayList<Pair<String,String>>();
        // �����Ƿ񱻡�������
        if(block.startsWith("��") && block.endsWith("��"))
            list.add(new Pair<String, String>(block.substring(1, block.length()-1),""));
        // ���Ǹ���A0-A1�г�ȡ��Ǳ�����۶���
        else if(considerComposite){

        }else{
            // ATT���ְ���adj
            List<Pair<Integer, Integer>> ATTList = getATTDepRangeList(start, end);
            String target = "", opinion = "";
            String tmp1, tmp2;
            for(Pair<Integer,Integer> pair : ATTList){
                for(int i=pair.first; i<pair.second; ++i){
                    tmp1 = segMap.get(i).first;
                    tmp2 = segMap.get(i+1).first;
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
                    target += segMap.get(j);

                list.add(new Pair<String,String>(target,opinion));
                target = "";
                opinion = "";
            }

            // ������
        }

        return list;
    }

    // A0-A1��ȡ����
    public void extractByA0A1(){
        for(SRNode node : srList){
            // �����������ɷְ����ڡ����У��򲻽���
            if(isInBookQuotation(node.beg, node.end))
                continue;
            // Ѱ�����۴�
            int opinionCode;
            List<Pair<Integer, String>> opinion = new ArrayList<>();
            if(hasOpinionWord(node.predication.first, node.predication.first, opinion)  ||
                    node.adverb!=null && hasOpinionWord(node.adverb.first.first, node.adverb.first.second, opinion) ||
                    node.A0!=null && hasOpinionWord(node.A0.first.first, node.A0.first.second, opinion) ||
                    node.A1!=null && hasOpinionWord(node.A1.first.first, node.A1.first.second, opinion) ||
                    node.A2!=null && hasOpinionWord(node.A2.first.first, node.A2.first.second, opinion) ) {
                // A0A1����
                // ν�����
                if((opinionCode=opinion.get(0).first) != 0){
                    if(Math.abs(opinionCode) == 1){
                        for(Pair<String,String> pair : getPotentialTargetAndOpionion(node.A0.second, node.A0.first.first, node.A0.first.second, true, true))
                            putTargetAndOpinion(pair.first, opinion.get(0).second);
                    }else{
                        for(Pair<String,String> pair : getPotentialTargetAndOpionion(node.A1.second, node.A1.first.first, node.A1.first.second, true, true))
                            putTargetAndOpinion(pair.first, opinion.get(0).second);
                    }
                }
                // �������
                else if((opinionCode=opinion.get(1).first) != 0){
                    for(Pair<String,String> pair : getPotentialTargetAndOpionion(node.A1.second, node.A1.first.first, node.A1.first.second, true, true))
                        putTargetAndOpinion(pair.first, opinion.get(1).second);
                }
                //
            }
        }

    }

    // NR��ȡ����
    public void extractByNR(){

    }

    // ATT��ȡ����
    public void extractByATT(){

    }

    // DO������дʳ�ȡ����
    public void extractByDO(){

    }



    public static void main(String args[]){
        String s = "2(û): type=ADV beg=0 end=0 seq=���� type=ADV beg=1 end=1 seq=ֻ�� type=A1 beg=3 end=3 seq=����;6(��): type=A0 beg=0 end=5 seq=����ֻ��û���ӵ���;9(��): type=A1 beg=10 end=10 seq=����;14(Ҫ): type=ADV beg=13 end=13 seq=Ҳ;15(����): type=TMP beg=8 end=8 seq=���� type=A1 beg=9 end=12 seq=�з��ӵİ��� type=ADV beg=13 end=13 seq=Ҳ type=A1 beg=16 end=18 seq=��˰����;22(��): type=A1 beg=23 end=23 seq=����;24(��): type=PRP beg=21 end=23 seq=Ϊ���� type=A1 beg=25 end=26 seq=��Ѫ��Ǯ;29(��): type=A0 beg=28 end=28 seq=����;32(��ʼ): type=ADV beg=31 end=31 seq=��;33(��˰): type=ADV beg=31 end=31 seq=��;";
        LTPTargetExtractor ex = new LTPTargetExtractor();
        ex.readSR(s);

    }
}
