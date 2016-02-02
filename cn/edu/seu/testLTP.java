package cn.edu.seu;

import cn.edu.seu.extractor.CorpusExtractor;
import cn.edu.seu.ltp_extractor.LTPCorpusExtractor;

import java.io.IOException;
import java.util.Scanner;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 12-7-19
 * Time: 下午1:13
 * To change this template use File | Settings | File Templates.
 */
public class testLTP {
    public static void main(String[] args) throws IOException {
        boolean runInAll = false;
        Scanner cin = new Scanner(System.in);
        // 语料预处理，生成纯句子+纯id标注
       /*CorpusPreHandler cph = new CorpusPreHandler();
        System.out.println("输入：1.预处理标注语料; 2.预处理评测语料对齐; 3.预处理评估语料对齐; 4~:下一步; 5：run in all" );
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
            else if(a == 5){
                runInAll = true;
                break;
            }
            else
                break;
            System.out.println("输入：1.预处理标注语料; 2.预处理评测语料对齐; 3.预处理评估语料对齐; 4~:下一步; 5：run in all");
        }

        System.out.println("输入0跳过，1继续下一步：分词");

        // 分词操作
        if(runInAll || (cin.nextInt()) == 1) {
            CorpusSegmenter seg = new CorpusSegmenter();
            seg.useLTPSeg = true;
            seg.useLTPPos = true;
            if(!seg.init())
                return;
            // 批量生成带pos的分词
            seg.batchSegment("corpus//2_preprocessCorpus", "corpus//3_ltp_segmentCorpus_noPos", false);
            seg.batchSegment("corpus//2_preprocessTopicCorpus", "corpus//3_ltp_segmentTopicCorpus_noPos", false);
            seg.batchSegment("corpus//3_ltp_segmentCorpus_noPos", "corpus//3_ltp_segmentCorpus_pos", true);
            seg.batchSegment("corpus//3_ltp_segmentTopicCorpus_noPos", "corpus//3_ltp_segmentTopicCorpus_pos", true);
            seg.destroy();
        }

        System.out.println("输入0跳过，1继续下一步：句法分析");

        // 依存句法分析
        if(runInAll || (cin.nextInt()) == 1) {
            DependencyParser parser = new DependencyParser();
            parser.useLTPDep = true;
            if(!parser.init())
                return;
            parser.parseAll("corpus//3_ltp_segmentCorpus_pos", "corpus//5_ltp_dependencyCorpus");
            parser.parseAll("corpus//3_ltp_segmentTopicCorpus_pos", "corpus//5_ltp_dependencyTopicCorpus");
            parser.destroy();
        }

        System.out.println("输入0跳过，1继续下一步：命名实体识别");

        // 命名实体识别
        if(runInAll || (cin.nextInt()) == 1) {
            NamedEntityRecognizer ner = new NamedEntityRecognizer();
            if(!ner.init())
                return;
            ner.parseAll("corpus//3_ltp_segmentCorpus_pos", "corpus//6_ltp_nerCorpus");
            ner.parseAll("corpus//3_ltp_segmentTopicCorpus_pos", "corpus//6_ltp_nerTopicCorpus");
            ner.destroy();
        }

        System.out.println("输入0跳过，1继续下一步：语义分析");

        // 语义分析
        if(runInAll || (cin.nextInt()) == 1) {
            SemanticParser parser = new SemanticParser();
            if(!parser.init())
                return;
            parser.parseAll("corpus//3_ltp_segmentCorpus_pos", "corpus//5_ltp_dependencyCorpus","corpus//6_ltp_nerCorpus", "corpus//7_ltp_semanticCorpus");
            parser.parseAll("corpus//3_ltp_segmentTopicCorpus_pos", "corpus//5_ltp_dependencyTopicCorpus","corpus//6_ltp_nerTopicCorpus", "corpus//7_ltp_semanticTopicCorpus");
            parser.destroy();
        }*/

        System.out.println("输入0跳过，1继续下一步：潜在对象和情感词抽取");

        SentimentSorter.init("corpus//dic//posOpinionDic.txt", "corpus//dic//negOpinionDic.txt","corpus//dic//posEmotionDic.txt", "corpus//dic//negEmotionDic.txt");

        // 潜在对象和情感词抽取
        if(runInAll || (cin.nextInt()) == 1) {
            LTPCorpusExtractor corpusExtractor = new LTPCorpusExtractor();
            corpusExtractor.extractorAll("corpus//3_ltp_segmentCorpus_pos", "corpus//5_ltp_dependencyCorpus", "corpus//7_ltp_semanticCorpus",
                    "corpus//3_ltp_segmentTopicCorpus_pos", "corpus//5_ltp_dependencyTopicCorpus", "corpus//7_ltp_semanticTopicCorpus", "corpus//8_ltp_targetPairCorpus" );
        }

        System.out.println("输入0跳过，1继续下一步：情感相似度计算");

        // 情感相似度计算
        if(runInAll || (cin.nextInt()) == 1) {
            SentimentSorter sentimentSorter = new SentimentSorter();
            sentimentSorter.sortAll("corpus//8_ltp_targetPairCorpus", "corpus//3_ltp_segmentCorpus_pos", "corpus//8_ltp_nounSentimentPairCorpus");
        }
        // 对齐操作
        Aligner aligner = new Aligner();
        aligner.alignAllResult("corpus//8_alignCorpus", "corpus//8_ltp_nounSentimentPairCorpus", "corpus//8_alignCorpus_label", "corpus//2_preprocessCorpus_label", "corpus//9_testResult");


        System.out.println("输入0跳过，1继续下一步：评估计算");

        // 评估计算
        if(runInAll || (cin.nextInt()) == 1) {
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
