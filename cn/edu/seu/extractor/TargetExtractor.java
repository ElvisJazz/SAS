package cn.edu.seu.extractor;

import com.google.common.collect.HashMultimap;
import edu.stanford.nlp.util.Pair;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 15-3-13
 * Time: ����4:22
 * To change this template use File | Settings | File Templates.
 */
class Node{
    public int index;
    ArrayList<Pair<String, Node>> nextNodeArray = new ArrayList<Pair<String, Node>>();

    public Node(int index){
        this.index = index;
    }
}

public class TargetExtractor {
    // �����Թ�����,����������,������,������,�����ﶯ�ʣ��ڶ��ʣ�,�����Թ�����,����������,���ݴ�
    public final static String[] OPINION_SET_FOR_STF = {"/nl", "/ng", "/a"};
    public final static String[] OPINION_SET_FOR_LTP = {"/a", "/d", "/v"};
    // ��������дʺ�����
    public final static String[] BLACK_OPINION_SET = {"/vshi", "/vyou", "/vf", "/vx"};
    // ����Ŀ��������ϵ����
    public final static String[] BASIC_TARGET_REL_SET_FOR_STF = {"root", "dep", "subj", "mod", "comp", "nn", "conj"};
    public final static String[] BASIC_TARGET_REL_SET_FOR_LTP = {"root", "dep", "subj", "mod", "comp", "nn", "conj"};
    // �񶨴�
    public final static String[] NOT_SET = {"��", "û", "��", "��"};
    // �񶨴ʰ�����
    public final static String[] NOT_WHITE_SET = {"����", "���ò�", "����", "�ǳ�",  "�޷�", "����","û׼"};
    // �洢��ȡ�����ʺͶ�Ӧ����дʣ���Ϊ���ʡ����ݴ�a,�����Թ�����nl, ����������ng��
    private HashMultimap<String, String> targetPairMap = HashMultimap.create();
    // ��ѡ����
    private HashMap<Integer, String> potentialNounMap = new HashMap<Integer, String>();
    // ��ѡ��д�
    private HashMap<Integer, String> potentialSentimentMap = new HashMap<Integer, String>();
    // ����
    private HashMap<Integer, String> adverbMap = new HashMap<Integer, String>();

    // ���дʿ�ڵ�洢
    private HashMap<Integer, Node> nodeMap = new HashMap<Integer, Node>();
    // �����ϵ�洢   rel,head->tail
    private HashMultimap<String, Pair<Integer,Integer>> depMap = HashMultimap.create();
    // ��֧���Ϊ��ֵ�������ϵ�洢
    //private HashMap<Integer, Set<Pair<Integer, String>>> headDepMap = new HashMap();
    // ��ȡ����
    public enum EXTRACT_TYPE{ STF, LTP};
    private EXTRACT_TYPE type = EXTRACT_TYPE.STF;

    // ���ó�ȡ����
    public void setExtractType(EXTRACT_TYPE type){
        this.type = type;
    }

    // ����Ǳ�����۶�������
    public void setPotentialNounMap(HashMap<Integer, String> potentialNounMap){
        this.potentialNounMap = potentialNounMap;
    }

    public HashMultimap<String, Pair<Integer, Integer>> getDepMap() {
        return depMap;
    }

    // �ӷִʺ�ϲ�������ѱ�ע��������ȡĿ���
    public void extractPotentialWords(String sentence){
        if(sentence.length() <= 0)
            return;
        String[] blockArray = sentence.split(" ");
        Node node = new Node(0);
        nodeMap.put(new Integer(0), node);
        for(int i=0; i<blockArray.length; ++i){
            // ��ʼ���ڵ�
            node = new Node(i+1);
            nodeMap.put(new Integer(i+1), node);
            // �Ƿ�����д�
            if(isContainsSentiment(blockArray[i])){
                blockArray[i] = blockArray[i].replaceAll("/[^\\s]*", "");
                potentialSentimentMap.put(new Integer(i+1), blockArray[i]);
            }
            // �Ƿ������ʶ���
            else if(isContainsTargetObject(blockArray[i])){
                blockArray[i] = blockArray[i].replaceAll("/[^\\s]*", "");
                potentialNounMap.put(new Integer(i+1), blockArray[i]);
            }
            // ����
            else if(blockArray[i].contains("/d")){
                blockArray[i] = blockArray[i].replaceAll("/[^\\s]*", "");
                adverbMap.put(new Integer(i+1), blockArray[i]);
            }
        }
    }

    // ��ȡ���ӵľ䷨������Ĺ�ϵ
    public void readRelation(String depSentence) throws Exception{
        depSentence = depSentence.substring(1, depSentence.length()-1);
        depSentence += ", ";
        String[] blockArray = depSentence.split("\\), ");
        int index = 0, index2 = 0;
        String relation = "";
        Integer head = 0, tail = 0;
        for(String block : blockArray){
            if(block!=null && block.length() <= 3)
                continue;
            // ��ȡÿ���ڵ�����ָ��ĺ����ڵ�
            try{
                // ��ȡ��ϵ����
                index = block.indexOf('(');
                relation = block.substring(0, index);
                // ��ȡͷ�ڵ�
                index2 = block.indexOf(", ", index);
                index = block.lastIndexOf('-', index2);
                head = new Integer(block.substring(index+1, index2));
                // ��ȡβ�ͽڵ�
                index = block.lastIndexOf('-');
                tail = new Integer(block.substring(index+1));
                // �洢�����ϵ
                Node node = nodeMap.get(tail);
                nodeMap.get(head).nextNodeArray.add(new Pair<String, Node>(relation, node));
                depMap.put(relation, new Pair<Integer, Integer>(head,tail));
            }catch (Exception e){
                throw new Exception(e);
            }
        }
    }

    // Root�����ȡ ����д����������
    public void extractByRootRule(){
        Set<Pair<Integer, Integer>> set = depMap.get("root");
        Iterator<Pair<Integer,Integer>> iterator = set.iterator();
        if(iterator.hasNext()){
            Pair<Integer,Integer> pair = iterator.next();
            // �жϻ�ȡ��pair���Ƿ����Ǳ����д�
            if(potentialSentimentMap.containsKey(pair.second)){
                // ��ȡ�͸ôʴ��������ϵ�Ĵ�
                HashMap<Integer, String> depWordMap = getDepPairMap(pair.second);
                for(Integer index : depWordMap.keySet()){
                    // �����Ĵ��Ƿ�����Ǳ�����۶���
                    if(!potentialNounMap.containsKey(index)){
                        targetPairMap.put("#", potentialSentimentMap.get(pair.second));
                    }
                }
            }
            // �Ƿ��Ӵ�����д�
           HashMap<Integer, String> depWordMap = getDepPairMap(pair.second);
           for(Integer index : depWordMap.keySet()){
               // �����Ĵ��Ƿ�����Ǳ�����۶���
               if(potentialSentimentMap.containsKey(index)){
                   targetPairMap.put("#", potentialSentimentMap.get(index));
               }
           }
        }
    }

    //  ֱ�ӹ����ȡ ����д������ֱ�ӹ���
    void extractByDirectRule(){
        for(Map.Entry entry : depMap.entries()){
            // ��ȡ��ϵ����
            String rel = (String)entry.getKey();
            Integer index1 = 0, index2 = 0;
            // ��ϵ�Ƿ�������й�ϵ����subj, obj, mod, ccomp
            if(isContainsTargetRel(rel, false)){
                index1 = ((Pair<Integer, Integer>)entry.getValue()).first;
                index2 = ((Pair<Integer, Integer>)entry.getValue()).second;

                // ��һ��������дʣ����ҵڶ�������Ǳ�����ʶ���
                if(potentialSentimentMap.containsKey(index1) && potentialNounMap.containsKey(index2)){
                    targetPairMap.put(potentialNounMap.get(index2), potentialSentimentMap.get(index1));
                }
                // �ڶ���������дʣ����ҵ�һ������Ǳ�����ʶ���
                else if(potentialSentimentMap.containsKey(index2) && potentialNounMap.containsKey(index1)){
                    targetPairMap.put(potentialNounMap.get(index1), potentialSentimentMap.get(index2));
                }
            }
        }
    }

    // ��ӹ����ȡ:1) H->T,H->O 2) O->H->T  3) T->H->O
    public void extractByIndirectRule(){
        // 1) H->T,H->O
        for(Node node : nodeMap.values()){
            Integer nounIndex = 0, sentimentIndex = 0;
            if(node.nextNodeArray.size() >= 2){
                for(Pair<String, Node> pair : node.nextNodeArray){
                    // ��ȡ��д�
                    if(isContainsTargetRel(pair.first, true) && potentialSentimentMap.containsKey(pair.second.index)){
                        sentimentIndex = pair.second.index;
                        // ��ȡ����
                        for(Pair<String, Node> pair1 : node.nextNodeArray){
                            if(isContainsTargetRel(pair1.first, true) && potentialNounMap.containsKey(pair1.second.index)){
                                nounIndex = pair1.second.index;
                                targetPairMap.put(potentialNounMap.get(nounIndex), potentialSentimentMap.get(sentimentIndex));
                            }
                        }
                    }
                }
            }
        }

        // ���� 2) 3)
        for(Node node : nodeMap.values()){
            // ���ʣ���д����
            Integer nounIndex = 0, sentimentIndex = 0;
            boolean isFirstWordOpinion = false;
            // ��ǰ�ڵ��Ƿ�����д�
            if(potentialSentimentMap.containsKey(node.index)){
                isFirstWordOpinion = true;
                sentimentIndex = new Integer(node.index);
            }
            // ��ǰ�ڵ��Ƿ�������
            else if(potentialNounMap.containsKey(node.index)){
                isFirstWordOpinion = false;
                nounIndex = new Integer(node.index);
            }else{
                continue;
            }
            // ������ǰ�ڵ�����к����ڵ�
            for(Pair<String, Node> pair : node.nextNodeArray){
                // �Ƿ��ҵ�Ŀ��������ϵ
                if(isContainsTargetRel(pair.first, true)) {
                    // ���������ڵ�����к����ڵ�
                    for(Pair<String, Node> pair1 : pair.second.nextNodeArray){
                        // �Ƿ�����ڵ��ҵ�Ŀ��������ϵ
                        if(isContainsTargetRel(pair1.first, true)) {
                            if(isFirstWordOpinion && potentialNounMap.containsKey(pair1.second.index)) {
                                nounIndex =  new Integer(pair1.second.index);
                                targetPairMap.put(potentialNounMap.get(nounIndex), potentialSentimentMap.get(sentimentIndex));
                            } else if(!isFirstWordOpinion && potentialSentimentMap.containsKey(pair1.second.index)) {
                                sentimentIndex =  new Integer(pair1.second.index);
                                targetPairMap.put(potentialNounMap.get(nounIndex), potentialSentimentMap.get(sentimentIndex));
                            }
                        }
                    }
                }
            }
        }
    }

    // �񶨷�ת
    public void negReverse(){
        // ���滻����
        Set<Pair<String,String>> replaceSet = new HashSet<Pair<String, String>>();
        // �ٽ��ʷ�ת
        boolean isWhiteWord = false; // �Ƿ��ڰ�������
        String tmpW = null;
        for(Integer key : adverbMap.keySet()){
            String adverb = adverbMap.get(key);
            // ����Ƿ��ڰ�������
            for(String whiteWord : NOT_WHITE_SET){
                if(whiteWord.equals(adverb)){
                    isWhiteWord = true;
                    break;
                }
            }

            if(isWhiteWord){
                isWhiteWord = false;
                continue;
            }
            // ����Ƿ��Ƿ񶨴�
            else{
                for(String notWord : NOT_SET){
                    if(adverb.contains(notWord)){
                        // Ѱ���Ƿ��ٽ�������д�
                        for(int i=1; i<4; ++i){
                            if(potentialSentimentMap.containsKey(key-i)){
                                tmpW = potentialSentimentMap.get(key-i);
                                break;
                            }
                            else if(potentialSentimentMap.containsKey(key+i)){
                                tmpW = potentialSentimentMap.get(key+i);
                                break;
                            }
                        }

                        if(tmpW != null){
                            for(String noun : targetPairMap.keySet()){
                                for(String tmpSentiment : targetPairMap.get(noun)) {
                                    if(tmpSentiment.equals(tmpW)){
                                        replaceSet.add(new Pair<String,String>(noun, tmpW));
                                    }
                                }
                            }
                            tmpW = null;
                        }
                    }
                }
            }
        }
        // �����ϵ��ת
        Set<Pair<Integer,Integer>> negSet = depMap.get("neg");
        String sentimentStr = null;
        Iterator iterator = negSet.iterator();
        while(iterator.hasNext()){
            sentimentStr = potentialSentimentMap.get(((Pair<Integer, Integer>)iterator.next()).first);
            for(String key : targetPairMap.keySet()){
                for(String tmpSentiment : targetPairMap.get(key)) {
                    if(tmpSentiment.equals(sentimentStr)){
                        replaceSet.add(new Pair<String,String>(key, tmpSentiment));
                    }
                }
            }
        }
        for(Pair<String,String> pair : replaceSet){
            targetPairMap.remove(pair.first, pair.second);
            targetPairMap.put(pair.first, "(-)"+pair.second);
        }
    }

    // ��������ȡ��Ŀ��Ԫ�飬���
    public HashMultimap<String, String> extract(){
        extractByRootRule();
        extractByDirectRule();
        extractByIndirectRule();
        negReverse();
        return targetPairMap;
    }

    // �Ƿ����Ǳ����д�
    public boolean isContainsSentiment(String word){
        String[] set;
        if(type == EXTRACT_TYPE.STF)
            set = OPINION_SET_FOR_STF;
        else
            set = OPINION_SET_FOR_LTP;

        for(String label : set){
            if(word.contains(label))
                return true;
        }
        if(word.contains("/v")){
            if(type == EXTRACT_TYPE.STF){
                for(String label : BLACK_OPINION_SET){
                    if(word.contains(label))
                        return false;
                }
            }
            return true;
        }
        return false;
    }

    // �Ƿ����Ǳ�����۶���
    public boolean isContainsTargetObject(String word){
        if(type == EXTRACT_TYPE.STF)
            return word.contains("/n") && !word.contains("/nl") && !word.contains("/ng");
        else
            return false;
    }

    // �Ƿ������Ŀ�����������ϵ
    public boolean isContainsTargetRel(String word, boolean extendLabel){
        String extendLabelStr;
        String[] set;
        if(type == EXTRACT_TYPE.STF) {
            extendLabelStr = "conj";
            set = BASIC_TARGET_REL_SET_FOR_STF;
        }else {
            extendLabelStr = "COO";
            set = BASIC_TARGET_REL_SET_FOR_LTP;
        }
        if(extendLabel && word.contains(extendLabelStr))
            return true;
        for(String label : set){
            if(word.contains(label))
                return true;
        }
        return false;
    }

    // ��ȡ��Ŀ��ʴ��������ϵ�Ĵ��Լ���ϵ
    public HashMap<Integer, String> getDepPairMap(Integer targetIndex){
        HashMap<Integer, String> depWordMap = new HashMap<Integer, String>();
        for(Map.Entry entry : depMap.entries()){
            Pair<Integer, Integer> relPair = (Pair<Integer, Integer>) entry.getValue();
            if(targetIndex.equals(relPair.first)){
                depWordMap.put(relPair.second, (String)entry.getKey());
            } else if(targetIndex.equals(relPair.second)){
                depWordMap.put(relPair.first, (String)entry.getKey());
            }
        }
        return depWordMap;
    }
}
