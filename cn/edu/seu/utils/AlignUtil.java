package cn.edu.seu.utils;

import cn.edu.seu.CorpusSegmenter;
import cn.edu.seu.DependencyParser;
import cn.edu.seu.ltp_extractor.LTPTargetExtractor;
import com.google.common.collect.HashMultimap;
import edu.stanford.nlp.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 16-2-11
 * Time: 上午11:45
 * To change this template use File | Settings | File Templates.
 */
public class AlignUtil {
    public static CorpusSegmenter segmenter = new CorpusSegmenter();
    public static DependencyParser dependencyParser = new DependencyParser();

    public static void init(){
        segmenter.useLTPPos = segmenter.useLTPSeg = true;
        segmenter.init("corpus//dic//scoreFilterDic.txt");
        dependencyParser.useLTPDep = true;
        dependencyParser.init();
    }

    public static void destroy(){
        segmenter.destroy();
        dependencyParser.destroy();
    }

    public static String getNeighborTarget(String sentence, int index, int length){
        List<String> words = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        List<Pair<String, String>> list = null;
        int i, ii;
        boolean isLastPart = false;
        String tmpStr;
        Map<Integer, Pair<String, String>> segMap = new HashMap<>();
        tmpStr = sentence.substring(0, index);
        while(true){
            if("".equals(tmpStr)) {
                tmpStr = sentence.substring(index+length);
                if("".equals(tmpStr))
                    break;
                isLastPart = true;
                continue;
            }
            String segSentence = segmenter.segmentSentenceUseLTP(sentence);
            String[] trunks = segSentence.split(" ");
            words.clear();
            tags.clear();
            segMap.clear();
            ii = 0;
            for(String t : trunks){
                i = t.lastIndexOf('/');
                words.add(t.substring(0, i));
                tags.add(t.substring(i+1));
                segMap.put(ii, new Pair<>(tags.get(ii),words.get(ii)));
                ii++;
            }
            HashMultimap<String, Pair<Integer,Integer>> depMap = dependencyParser.getDependencyUseLTP(words, tags);
            Pair<Pair<Integer, Integer>,String> pair = new Pair<>(new Pair<>(0, words.size()-1), sentence);
            list = LTPTargetExtractor.getPotentialTargetAndOpinion(pair, segMap, depMap, null, false);
            if(isLastPart || !list.get(0).first.equals(sentence))
                break;
            else{
                sentence = sentence.substring(index+length);
                isLastPart = true;
            }
        }
        if(list != null)
            return list.get(0).first;
        return null;
    }

}
