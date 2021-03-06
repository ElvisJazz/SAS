package cn.edu.seu.extractor;

import cn.edu.seu.SentimentSorter;
import cn.edu.seu.utils.LexUtil;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import edu.stanford.nlp.util.Pair;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 15-3-13
 * Time: 下午4:22
 * To change this template use File | Settings | File Templates.
 */
class Node{
    public int index;
    public String block;
    ArrayList<Pair<String, Node>> nextNodeArray = new ArrayList<Pair<String, Node>>();

    public Node(int index, String block){
        this.index = index;
        this.block = block;
    }
}

public class TargetExtractor {

    // 存储抽取的名词和对应的情感词（可为动词、形容词a,名词性惯用语nl, 名词性语素ng）
    private LinkedHashMultimap<String, String> targetPairMap = LinkedHashMultimap.create();
    // 候选名词
    private Map<Integer, String> potentialNounMap = new LinkedHashMap<Integer, String>();
    // 候选情感词
    private HashMap<Integer, String> potentialSentimentMap = new LinkedHashMap<Integer, String>();
    // 副词
    private HashMap<Integer, String> adverbMap = new LinkedHashMap<Integer, String>();

    // 所有词块节点存储
    private HashMap<Integer, Node> nodeMap = new LinkedHashMap<Integer, Node>();
    // 依存关系存储   rel,head->tail
    private LinkedHashMultimap<String, Pair<Integer,Integer>> depMap = LinkedHashMultimap.create();
    // 以支配词为键值的依存关系存储
    //private HashMap<Integer, Set<Pair<Integer, String>>> headDepMap = new HashMap();
    // 抽取类型
    public enum EXTRACT_TYPE{ STF, LTP};
    private EXTRACT_TYPE type = EXTRACT_TYPE.STF;

    // 设置抽取类型
    public void setExtractType(EXTRACT_TYPE type){
        this.type = type;
    }

    // 设置潜在评价对象数组
    public void setPotentialNounMap(Map<Integer, String> potentialNounMap){
        this.potentialNounMap = potentialNounMap;
    }

    // 获取依存关系映射
    public LinkedHashMultimap<String, Pair<Integer, Integer>> getDepMap() {
        return depMap;
    }

    // 获取潜在情感词映射（包括了副词）
    public HashMap<Integer, String> getPotentialSentimentMap() {
        return potentialSentimentMap;
    }

    // 清空结果集
    public void clearResult(){
        targetPairMap.clear();
    }

    // 从分词后合并词组的已标注句子中提取目标词
    public void extractPotentialWords(String sentence){
        if(sentence.length() <= 0)
            return;
        String[] blockArray = sentence.split(" ");
        String tmp;
        Node node = new Node(0, "root");
        nodeMap.put(new Integer(0), node);
        for(int i=0; i<blockArray.length; ++i){
            tmp = blockArray[i].replaceAll("/[^\\s]*", "");
            // 是否是情感词
            if(isContainsSentiment(blockArray[i])){
                potentialSentimentMap.put(new Integer(i+1), tmp);
            }
            // 是否是名词对象
            else if(isContainsTargetObject(blockArray[i])){
                potentialNounMap.put(new Integer(i+1), tmp);
            }
            // 副词
            if(blockArray[i].contains("/d")){
                adverbMap.put(new Integer(i+1), tmp);
            }

            // 初始化节点
            node = new Node(i+1, tmp);
            nodeMap.put(new Integer(i+1), node);
        }
    }

    // 读取句子的句法分析后的关系
    public void readRelation(String depSentence) throws Exception{
        if(depSentence==null || depSentence.trim().equals(""))
            return;
        depSentence = depSentence.substring(1, depSentence.length()-1);
        String advType = type.equals(EXTRACT_TYPE.STF)? LexUtil.ADV_DEP_FOR_STF: LexUtil.ADV_DEP_FOR_LTP;
        depSentence += ", ";
        String[] blockArray = depSentence.split("\\), ");
        int index = 0, index2 = 0;
        String relation = "", word = "";
        Integer head = 0, tail = 0;
        for(String block : blockArray){
            if(block!=null && block.length() <= 3)
                continue;
            // 获取每个节点所有指针的后续节点
            try{
                // 获取关系类型
                index = block.indexOf('(');
                relation = block.substring(0, index);
                // 获取头节点
                index2 = block.indexOf(", ", index);
                index = block.lastIndexOf('-', index2);
                head = new Integer(block.substring(index+1, index2));
                // 获取尾巴节点
                index = block.lastIndexOf('-');
                tail = new Integer(block.substring(index+1));
                word = block.substring(index2+2, index);
                // 存储依存关系
                Node node = nodeMap.get(tail);
                nodeMap.get(head).nextNodeArray.add(new Pair<String, Node>(relation, node));
                depMap.put(relation, new Pair<Integer, Integer>(head,tail));
                // 附加的adv关系
                if(relation.equals(advType))
                    adverbMap.put(tail, word);
            }catch (Exception e){
                throw new Exception(e);
            }
        }
        // 处理副词标记，除了直接v-adv,a-adv还有(v-n)+(v-adv)
        String adv, sen;
        HashMultimap<String, String> indirectDepMap = HashMultimap.create();
        for(int i : potentialSentimentMap.keySet()){
            for(Pair<Integer,Integer> pair : depMap.get(advType)){
                if(pair.first == i) {
                    sen = potentialSentimentMap.get(i);
                    adv = adverbMap.get(pair.second);
                    if(adv != null){
                        indirectDepMap.put(sen, adv);
                        for(Pair<String, Node> nodePair : nodeMap.get(pair.first).nextNodeArray){
                            if(!nodePair.first.equals(advType) && potentialSentimentMap.containsKey(nodePair.second.index))
                                indirectDepMap.put(potentialSentimentMap.get(nodePair.second.index), adv);
                        }
                    }
                }
            }
        }
        boolean hasWeakAdv;
        for(int i : potentialSentimentMap.keySet()){
            sen = potentialSentimentMap.get(i);
            String senStr = sen+"(";
            hasWeakAdv = false;
            for(String adverb : indirectDepMap.get(sen)) {
                if(adverb != null){
                    senStr += (adverb+" ");
                    for(String wAdv : LexUtil.WEAK_ADV){
                        if(wAdv.equals(adverb))
                            hasWeakAdv = true;
                    }
                }
            }
            if(hasWeakAdv)
                senStr += "){w}";
            else
                senStr += "){n}";

            potentialSentimentMap.put(i, senStr);
        }
    }

    // Root规则抽取 ：情感词与主题关联
    public void extractByRootRule(){
        String rootStr = (type.equals(EXTRACT_TYPE.LTP))? LexUtil.ROOT_FOR_LTP : LexUtil.ROOT_FOR_STF;
        Set<Pair<Integer, Integer>> set = depMap.get(rootStr);
        Iterator<Pair<Integer,Integer>> iterator = set.iterator();
        if(iterator.hasNext()){
            Pair<Integer,Integer> pair = iterator.next();
            // 判断获取的pair中是否存在潜在情感词
            if(potentialSentimentMap.containsKey(pair.second)){
                // 获取和该词存在依存关系的词
                HashMap<Integer, String> depWordMap = getDepPairMap(pair.second);
                for(Integer index : depWordMap.keySet()){
                    // 关联的词是否属于潜在评价对象
                    if(!potentialNounMap.containsKey(index)){
                        targetPairMap.put("#", potentialSentimentMap.get(pair.second));
                    }
                }
            }
            // 是否间接存在情感词
           HashMap<Integer, String> depWordMap = getDepPairMap(pair.second);
           for(Integer index : depWordMap.keySet()){
               // 关联的词是否属于潜在评价对象
               if(potentialSentimentMap.containsKey(index)){
                   targetPairMap.put("#", potentialSentimentMap.get(index));
               }
           }
        }
    }

    //  直接规则抽取 ：情感词与对象直接关联
    void extractByDirectRule(){
        for(Map.Entry entry : depMap.entries()){
            // 获取关系类型
            String rel = (String)entry.getKey();
            Integer index1 = 0, index2 = 0;
            // 关系是否属于情感关系类型subj, obj, mod, ccomp
            if(isContainsTargetRel(rel, true)){
                index1 = ((Pair<Integer, Integer>)entry.getValue()).first;
                index2 = ((Pair<Integer, Integer>)entry.getValue()).second;

                // 第一个词是情感词，并且第二个词是潜在名词对象
                if(potentialSentimentMap.containsKey(index1) && potentialNounMap.containsKey(index2)){
                    targetPairMap.put(potentialNounMap.get(index2), potentialSentimentMap.get(index1));
                }
                // 第二个词是情感词，并且第一个词是潜在名词对象
                else if(potentialSentimentMap.containsKey(index2) && potentialNounMap.containsKey(index1)){
                    // 若评价词是情感词后指，则忽略该评价对象
                    if(Math.abs(SentimentSorter.getSentimentWordType(nodeMap.get(index2).block)) == 2)
                        continue;
                    targetPairMap.put(potentialNounMap.get(index1), potentialSentimentMap.get(index2));
                }
            }
        }
    }

    // 间接规则抽取:1) H->T,H->O 2) O->H->T  3) T->H->O
    public void extractByIndirectRule(){
        // 1) H->T,H->O
        for(Node node : nodeMap.values()){
            Integer nounIndex = 0, sentimentIndex = 0;
            if(node.nextNodeArray.size() >= 2){
                for(Pair<String, Node> pair : node.nextNodeArray){
                    // 获取情感词
                    if(isContainsTargetRel(pair.first, true) && potentialSentimentMap.containsKey(pair.second.index)){
                        sentimentIndex = pair.second.index;
                        // 获取名词
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

        // 规则 2) 3)
        for(Node node : nodeMap.values()){
            // 名词，情感词序号
            Integer nounIndex = 0, sentimentIndex = 0;
            boolean isFirstWordOpinion = false;
            // 当前节点是否是情感词
            if(potentialSentimentMap.containsKey(node.index)){
                isFirstWordOpinion = true;
                sentimentIndex = new Integer(node.index);
            }
            // 当前节点是否是名词
            else if(potentialNounMap.containsKey(node.index)){
                isFirstWordOpinion = false;
                nounIndex = new Integer(node.index);
            }else{
                continue;
            }
            // 遍历当前节点的所有后续节点
            for(Pair<String, Node> pair : node.nextNodeArray){
                // 是否找到目标依赖关系
                if(isContainsTargetRel(pair.first, true)) {
                    // 遍历后续节点的所有后续节点
                    for(Pair<String, Node> pair1 : pair.second.nextNodeArray){
                        // 是否后续节点找到目标依赖关系
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

    // 四节点间接关系
    // 间接规则抽取:1) H1->T,H1->H2->O 2) H1->O,H1->H2->T 3) O->H1->H2->T  4) T->H1->H2->O
    public void extractByIndirectExRule(){
        // 1) 2)规则
        for(Node node : nodeMap.values()){
            Integer nounIndex = 0, sentimentIndex = 0;
            boolean isFirstWordOpinion = false;
            if(node.nextNodeArray.size() >= 2){
                for(Pair<String, Node> pair : node.nextNodeArray){
                    // 获取情感词或名词
                    if(isContainsTargetRel(pair.first, true)){
                        if(potentialSentimentMap.containsKey(pair.second.index)){
                            sentimentIndex = pair.second.index;
                            isFirstWordOpinion = true;
                        }else if(potentialNounMap.containsKey(pair.second.index)){
                            nounIndex = pair.second.index;
                            isFirstWordOpinion = false;
                        }else{
                            continue;
                        }
                        // 获取名词或情感词
                        for(Pair<String, Node> pair0 : node.nextNodeArray){
                            if(isContainsTargetRel(pair0.first, true)){
                                for(Pair<String, Node> pair1 : pair0.second.nextNodeArray){
                                    if(pair1.first.equals("CMP")){
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
            }
        }

        // 规则 2) 3)
        for(Node node : nodeMap.values()){
            // 名词，情感词序号
            Integer nounIndex = 0, sentimentIndex = 0;
            boolean isFirstWordOpinion = false;
            // 当前节点是否是情感词
            if(potentialSentimentMap.containsKey(node.index)){
                isFirstWordOpinion = true;
                sentimentIndex = new Integer(node.index);
            }
            // 当前节点是否是名词
            else if(potentialNounMap.containsKey(node.index)){
                isFirstWordOpinion = false;
                nounIndex = new Integer(node.index);
            }else{
                continue;
            }
            // 遍历当前节点的所有后续节点
            for(Pair<String, Node> pair : node.nextNodeArray){
                // 是否找到目标依赖关系
                if(isContainsTargetRel(pair.first, true)) {
                    for(Pair<String, Node> pair0 : pair.second.nextNodeArray){
                        // 遍历后续节点的所有后续节点
                        if(isContainsTargetRel(pair0.first, true)) {
                            // 遍历后续节点的所有后续节点
                            for(Pair<String, Node> pair1 : pair0.second.nextNodeArray){
                                // 是否后续节点找到目标依赖关系
                                if(pair1.first.equals("CMP")) {
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
        }
    }

    // 否定反转
    public void negReverse(){
        // 待替换容器
        Set<Pair<String,String>> replaceSet = new HashSet<Pair<String, String>>();
        // 临近词反转
        boolean isWhiteWord = false; // 是否在白名单中
        boolean flag = false;
        String sentiment;
        for(String noun : targetPairMap.keySet()){
            for(String opinion : targetPairMap.get(noun)){
                int index1 = opinion.indexOf('(');
                int index2 = opinion.indexOf(')', index1);
                sentiment = opinion.substring(0, index1);
                for(String adverb : opinion.substring(index1+1, index2).split(" ")){
                    if(adverb.equals(sentiment))
                        continue;
                    // 检测是否在白名单中
                    for(String whiteWord : LexUtil.NOT_WHITE_SET){
                        if(whiteWord.equals(adverb)){
                            isWhiteWord = true;
                            break;
                        }
                    }

                    if(isWhiteWord){
                        isWhiteWord = false;
                        continue;
                    }
                    // 检测是否是否定词
                    else{
                        for(String notWord : LexUtil.NOT_SET){
                            if(adverb.equals(notWord)){
                                flag = !flag;
                            }
                        }
                    }
                }
                if(flag) {
                    replaceSet.add(new Pair<>(noun, opinion));
                    flag = false;
                }
            }
        }
        // 依存关系反转
        if(type.equals(EXTRACT_TYPE.STF)){
            Set<Pair<Integer,Integer>> negSet = depMap.get(LexUtil.NEG_FOR_STF);
            String sentimentStr;
            Iterator iterator = negSet.iterator();
            while(iterator.hasNext()){
                sentimentStr = potentialSentimentMap.get(((Pair<Integer, Integer>)iterator.next()).first);
                for(String key : targetPairMap.keySet()){
                    for(String tmpSentiment : targetPairMap.get(key)) {
                        if(tmpSentiment.equals(sentimentStr)){
                            replaceSet.add(new Pair(key, tmpSentiment));
                        }
                    }
                }
            }
        }

        for(Pair<String,String> pair : replaceSet){
            targetPairMap.remove(pair.first, pair.second);
            targetPairMap.put(pair.first, "(-)"+pair.second);
        }
    }

    // 并列评价对象搜索
    public void cooRule(){
        String coo = type.equals(EXTRACT_TYPE.LTP)? LexUtil.COO_DEP_FOR_LTP : LexUtil.COO_DEP_FOR_STF;
        String target;
        for(Pair<Integer,Integer> pair : depMap.get(coo)){
            if(potentialNounMap.containsKey(pair.first) && targetPairMap.containsKey(target = potentialNounMap.get(pair.first))){
                targetPairMap.putAll(nodeMap.get(pair.second).block, targetPairMap.get(target));
            }else if(potentialNounMap.containsKey(pair.second) && targetPairMap.containsKey(target = potentialNounMap.get(pair.second))){
                targetPairMap.putAll(nodeMap.get(pair.first).block, targetPairMap.get(target));
            }
        }
    }

    // 分析并提取出目标元组，输出
    public LinkedHashMultimap<String, String> extract(){
        extractByRootRule();
        extractByDirectRule();
        extractByIndirectRule();
        extractByIndirectExRule();
        negReverse();
        //cooRule();

        return targetPairMap;
    }

    // 是否包含潜在情感词
    public boolean isContainsSentiment(String word){
        String[] set;
        if(type == EXTRACT_TYPE.STF)
            set = LexUtil.OPINION_SET_FOR_STF;
        else
            set = LexUtil.OPINION_SET_FOR_LTP;

        for(String label : set){
            if(word.contains(label))
                return true;
        }
        if(word.contains("/v")){
            if(type == EXTRACT_TYPE.STF){
                for(String label : LexUtil.BLACK_OPINION_SET){
                    if(word.contains(label))
                        return false;
                }
            }
            return true;
        }else if(SentimentSorter.getSentimentWordType(word.replaceAll("/[^\\s]*", "")) != 0)
            return true;
        return false;
        /*Set<String> set = new HashSet<>();
        set.add(word);
        return SentimentSorter.compute(set).equals("OTHER");*/
    }

    // 是否包含潜在评价对象
    public boolean isContainsTargetObject(String word){
        if(type == EXTRACT_TYPE.STF)
            return word.contains("/n") && !word.contains("/nl") && !word.contains("/ng");
        else
            return false;
    }

    // 是否包含在目标基本依赖关系
    public boolean isContainsTargetRel(String word, boolean extendLabel){
        String extendLabelStr;
        String[] set;
        if(type == EXTRACT_TYPE.STF) {
            extendLabelStr = LexUtil.COO_DEP_FOR_STF;
            set = LexUtil.BASIC_TARGET_REL_SET_FOR_STF;
        }else {
            extendLabelStr = LexUtil.COO_DEP_FOR_LTP;
            set = LexUtil.BASIC_TARGET_REL_SET_FOR_LTP;
        }
        if(extendLabel && word.contains(extendLabelStr))
            return true;
        for(String label : set){
            if(word.contains(label))
                return true;
        }
        return false;
    }

    // 获取和目标词存在依存关系的词以及关系
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
