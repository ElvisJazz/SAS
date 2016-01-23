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
    // �洢�ִ��б�λ�ã���
    private Map<Integer, String> segMap = new HashMap();
    // �洢����������ʵ���б�λ�ã�<��ע����>
    private Map<Integer, Pair<String,String>> neMap = new HashMap();
    // �洢�����������ɫ�б�
    private List<SRNode> srList = new ArrayList();
    // �洢��ȡ�����ʺͶ�Ӧ����дʣ���Ϊ���ʡ����ݴ�a,�����Թ�����nl, ����������ng��
    private HashMultimap<String, String> targetPairMap = HashMultimap.create();


    // ��ȡ�ı����Ͼ��ӵ�����ʵ�塢�����ϵ�������ɫ
    public void readContentCorpusSentence(String segSentence, String depSentence, String srSentence) throws Exception {
        // ��¼����������������������������ʵ��
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
                    // ����A0��A1
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


    //  ��ʼ���������Ͼ��ӵ�����ʵ�塢�����ϵ�������ɫ
    public void initTopicInfo(){

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
            word = segMap.get(i);
            if((code=SentimentSorter.getSentimentWordType(word)) != 0){
                list.add(new Pair<>(code, word));
                return true;
            }
        }
        list.add(new Pair<>(0, ""));
        return false;
    }
    // Ѱ��ATT��ص�������ϵ start-end����: 0-(N-1)
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
                // ����
                s = i;
                e = i;
            } else{
                e = i;
            }
        }
        return resultMap;
    }

    // ��ĳһ�ɷ��г��Ǳ�����۶���
    public List<String> getPotentialTarget(String block, int start, int end, boolean considerComposite){
        List list = new ArrayList<String>();
        if(block.startsWith("��") && block.endsWith("��"))
            list.add(block.substring(1, block.length()-1));
        else if(considerComposite){
            // ���Ǹ���A0-A1�г�ȡ��Ǳ�����۶���
        }else{
            // ATT��Ѱ������֧�䲿��
            HashMap<Integer, Integer> rm = getATTDepMap(start, end);
            String target = "";
            for(Map.Entry<Integer, Integer> entry : rm.entrySet()) {
                for(int i=entry.getKey(); i<=entry.getValue(); ++i)
                    target += segMap.get(i);
                list.add(target);
                target = "";
            }
            // ������
        }

        return null;
    }

    // A0-A1��ȡ����
    public void extractByA0A1(){
        for(SRNode node : srList){
            // Ѱ�����۴�
            int opinionCode;
            List<Pair<Integer, String>> opinion = new ArrayList<>();
            if(hasOpinionWord(node.predication.first, node.predication.first, opinion)  ||
                    hasOpinionWord(node.adverb.first.first, node.adverb.first.second, opinion) ||
                    hasOpinionWord(node.A0.first.first, node.adverb.first.second, opinion) ||
                    hasOpinionWord(node.A1.first.first, node.adverb.first.second, opinion) ) {
                // A0A1����
                // ν�����
                if((opinionCode=opinion.get(0).first) != 0){
                    if(Math.abs(opinionCode) == 1){
                        for(String target : getPotentialTarget(node.A0.second, node.A0.first.first, node.A0.first.second, true))
                            targetPairMap.put(target, opinion.get(0).second);
                    }else{
                        for(String target : getPotentialTarget(node.A1.second, node.A1.first.first, node.A1.first.second, true))
                            targetPairMap.put(target, opinion.get(0).second);
                    }
                }
                // �������
                else if((opinionCode=opinion.get(1).first) != 0){
                    for(String target : getPotentialTarget(node.A1.second, node.A1.first.first, node.A1.first.second, true))
                        targetPairMap.put(target, opinion.get(1).second);
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
