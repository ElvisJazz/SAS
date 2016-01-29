package cn.edu.seu;

import cn.edu.seu.extractor.CorpusExtractor;

import java.io.IOException;
import java.util.Scanner;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 12-7-19
 * Time: 下午1:13
 * To change this template use File | Settings | File Templates.
 */
public class test {
    public static void main(String[] args) throws IOException {
        // 语料预处理，生成纯句子+纯id标注
       CorpusPreHandler cph = new CorpusPreHandler();

        System.out.println("输入：1.预处理标注语料; 2.预处理评测语料对齐; 3.预处理评估语料对齐; 4~:下一步");
        Scanner cin = new Scanner(System.in);
        while(cin.hasNext()){
            int a = cin.nextInt();
            if(a == 1) {
                cph.handleAllOriginalCorpus("corpus//1_originalCorpus", "corpus//2_preprocessCorpus", "corpus//2_preprocessTopicCorpus", false, false);  // 预处理标注语料
                //break;
            }
            else  if(a == 2){
                cph.handleAllOriginalCorpus("corpus//result", "corpus//8_alignCorpus", null, true, false);  // 预处理评测语料对齐
                //break;
            }
            else if(a == 3){
                cph.handleAllOriginalCorpus("corpus//evaluate", "corpus//10_evaluationCorpus", null, false, true);  // 预处理评估语料对齐
                //break;
            }
            else
                break;
            System.out.println("输入：1.预处理标注语料; 2.预处理评测语料对齐; 3.预处理评估语料对齐; 4~:下一步");
        }



        System.out.println("输入0跳过，1继续下一步：分词");
        int a = cin.nextInt();

        // 分词操作
        if(a == 1) {
            CorpusSegmenter seg = new CorpusSegmenter();
            if(!seg.init())
                return;
            // 批量生成带pos和不带pos的分词
            seg.batchSegment("corpus//2_preprocessCorpus", "corpus//3_segmentCorpus_pos", true);
            seg.batchSegment("corpus//2_preprocessCorpus", "corpus//3_segmentCorpus_noPos", false);
            seg.destroy();
        }

        System.out.println("输入0跳过，1继续下一步：短语合并");
        a = cin.nextInt();

        // 短语合并
        if(a == 1) {
            PhraseProducer pp = new PhraseProducer();
            pp.produceAll("corpus//3_segmentCorpus_pos", "corpus//4_phraseSegmentCorpus_pos", "corpus//4_phraseSegmentCorpus_noPos");
        }

        System.out.println("输入0跳过，1继续下一步：句法分析");
        a = cin.nextInt();

        // 依存句法分析
        if(a == 1) {
            DependencyParser parser = new DependencyParser();
            parser.parseAll("corpus//4_phraseSegmentCorpus_noPos", "corpus//5_dependencyCorpus");
        }

        System.out.println("输入0跳过，1继续下一步：潜在对象和情感词抽取");
        a = cin.nextInt();

        // 潜在对象和情感词抽取
        if(a == 1) {
            CorpusExtractor corpusExtractor = new CorpusExtractor();
            corpusExtractor.extractorAll("corpus//4_phraseSegmentCorpus_pos", "corpus//5_dependencyCorpus", "corpus//6_targetPairCorpus");
        }

        System.out.println("输入0跳过，1继续下一步：情感相似度计算");
        a = cin.nextInt();

        // 情感相似度计算
        if(a == 1) {
            SentimentSorter sentimentSorter = new SentimentSorter();
            sentimentSorter.init("corpus//dic//posOpionionDic.txt", "corpus//dic//negOpionionDic.txt","corpus//dic//posEmotionDic.txt", "corpus//dic//negEmotionDic.txt");
            sentimentSorter.sortAll("corpus//6_targetPairCorpus", "corpus//3_ltp_segmentCorpus_pos", "corpus//7_nounSentimentPairCorpus");
        }
        // 对齐操作
        Aligner aligner = new Aligner();
        aligner.alignAllResult("corpus//8_alignCorpus", "corpus//7_nounSentimentPairCorpus", "corpus//8_alignCorpus_label", "corpus//2_preprocessCorpus_label", "corpus//9_testResult");


        System.out.println("输入0跳过，1继续下一步：评估计算");
        a = cin.nextInt();
        // 评估计算
        if(a == 1) {
            Evaluator evaluator = new Evaluator();
            // 评价对象+情感评估
            evaluator.evaluateAll("corpus//10_evaluationCorpus", "corpus//9_testResult", true);
            evaluator.getEvaluationResult("corpus//result_sentiment.txt");
            // 评价对象评估
            evaluator = new Evaluator();
            evaluator.evaluateAll("corpus//10_evaluationCorpus", "corpus//9_testResult", false);
            evaluator.getEvaluationResult("corpus//result_target.txt");
        }
    }
}
