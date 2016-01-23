package cn.edu.seu;

import cn.edu.seu.extractor.CorpusExtractor;

import java.io.IOException;
import java.util.Scanner;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 12-7-19
 * Time: ����1:13
 * To change this template use File | Settings | File Templates.
 */
public class test {
    public static void main(String[] args) throws IOException {
        // ����Ԥ�������ɴ�����+��id��ע
       CorpusPreHandler cph = new CorpusPreHandler();

        System.out.println("���룺1.Ԥ�����ע����; 2.Ԥ�����������϶���; 3.Ԥ�����������϶���; 4~:��һ��");
        Scanner cin = new Scanner(System.in);
        while(cin.hasNext()){
            int a = cin.nextInt();
            if(a == 1) {
                cph.handleAllOriginalCorpus("corpus//1_originalCorpus", "corpus//2_preprocessCorpus", "corpus//2_preprocessTopicCorpus", false, false);  // Ԥ�����ע����
                //break;
            }
            else  if(a == 2){
                cph.handleAllOriginalCorpus("corpus//result", "corpus//8_alignCorpus", null, true, false);  // Ԥ�����������϶���
                //break;
            }
            else if(a == 3){
                cph.handleAllOriginalCorpus("corpus//evaluate", "corpus//10_evaluationCorpus", null, false, true);  // Ԥ�����������϶���
                //break;
            }
            else
                break;
            System.out.println("���룺1.Ԥ�����ע����; 2.Ԥ�����������϶���; 3.Ԥ�����������϶���; 4~:��һ��");
        }



        System.out.println("����0������1������һ�����ִ�");
        int a = cin.nextInt();

        // �ִʲ���
        if(a == 1) {
            CorpusSegmenter seg = new CorpusSegmenter();
            if(!seg.init())
                return;
            // �������ɴ�pos�Ͳ���pos�ķִ�
            seg.batchSegment("corpus//2_preprocessCorpus", "corpus//3_segmentCorpus_pos", true);
            seg.batchSegment("corpus//2_preprocessCorpus", "corpus//3_segmentCorpus_noPos", false);
            seg.destroy();
        }

        System.out.println("����0������1������һ��������ϲ�");
        a = cin.nextInt();

        // ����ϲ�
        if(a == 1) {
            PhraseProducer pp = new PhraseProducer();
            pp.produceAll("corpus//3_segmentCorpus_pos", "corpus//4_phraseSegmentCorpus_pos", "corpus//4_phraseSegmentCorpus_noPos");
        }

        System.out.println("����0������1������һ�����䷨����");
        a = cin.nextInt();

        // ����䷨����
        if(a == 1) {
            DependencyParser parser = new DependencyParser();
            parser.parseAll("corpus//4_phraseSegmentCorpus_noPos", "corpus//5_dependencyCorpus");
        }

        System.out.println("����0������1������һ����Ǳ�ڶ������дʳ�ȡ");
        a = cin.nextInt();

        // Ǳ�ڶ������дʳ�ȡ
        if(a == 1) {
            CorpusExtractor corpusExtractor = new CorpusExtractor();
            corpusExtractor.extractorAll("corpus//4_phraseSegmentCorpus_pos", "corpus//5_dependencyCorpus", "corpus//6_targetPairCorpus");
        }

        System.out.println("����0������1������һ����������ƶȼ���");
        a = cin.nextInt();

        // ������ƶȼ���
        if(a == 1) {
            SentimentSorter sentimentSorter = new SentimentSorter();
            sentimentSorter.init("corpus//dic//posOpionionDic.txt", "corpus//dic//negOpionionDic.txt","corpus//dic//posEmotionDic.txt", "corpus//dic//negEmotionDic.txt");
            sentimentSorter.sortAll("corpus//6_targetPairCorpus", "corpus//7_nounSentimentPairCorpus");
        }
        // �������
        Aligner aligner = new Aligner();
        aligner.alignAllResult("corpus//8_alignCorpus", "corpus//7_nounSentimentPairCorpus", "corpus//8_alignCorpus_label", "corpus//2_preprocessCorpus_label", "corpus//9_testResult");


        System.out.println("����0������1������һ������������");
        a = cin.nextInt();
        // ��������
        if(a == 1) {
            Evaluator evaluator = new Evaluator();
            // ���۶���+�������
            evaluator.evaluateAll("corpus//10_evaluationCorpus", "corpus//9_testResult", true);
            evaluator.getEvaluationResult("corpus//result_sentiment.txt");
            // ���۶�������
            evaluator = new Evaluator();
            evaluator.evaluateAll("corpus//10_evaluationCorpus", "corpus//9_testResult", false);
            evaluator.getEvaluationResult("corpus//result_target.txt");
        }
    }
}
