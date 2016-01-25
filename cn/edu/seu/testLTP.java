package cn.edu.seu;

import cn.edu.seu.extractor.CorpusExtractor;
import cn.edu.seu.ltp_extractor.LTPCorpusExtractor;

import java.io.IOException;
import java.util.Scanner;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 12-7-19
 * Time: ����1:13
 * To change this template use File | Settings | File Templates.
 */
public class testLTP {
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
            seg.useLTPSeg = true;
            seg.useLTPPos = true;
            if(!seg.init())
                return;
            // �������ɴ�pos�ķִ�
            seg.batchSegment("corpus//2_preprocessCorpus", "corpus//3_ltp_segmentCorpus_noPos", false);
            seg.batchSegment("corpus//2_preprocessTopicCorpus", "corpus//3_ltp_segmentTopicCorpus_noPos", false);
            seg.batchSegment("corpus//3_ltp_segmentCorpus_noPos", "corpus//3_ltp_segmentCorpus_pos", true);
            seg.batchSegment("corpus//3_ltp_segmentTopicCorpus_noPos", "corpus//3_ltp_segmentTopicCorpus_pos", true);
            seg.destroy();
        }

        System.out.println("����0������1������һ�����䷨����");
        a = cin.nextInt();

        // ����䷨����
        if(a == 1) {
            DependencyParser parser = new DependencyParser();
            parser.useLTPDep = true;
            if(!parser.init())
                return;
            parser.parseAll("corpus//3_ltp_segmentCorpus_pos", "corpus//5_ltp_dependencyCorpus");
            parser.parseAll("corpus//3_ltp_segmentTopicCorpus_pos", "corpus//5_ltp_dependencyTopicCorpus");
            parser.destroy();
        }

        System.out.println("����0������1������һ��������ʵ��ʶ��");
        a = cin.nextInt();

        // ����ʵ��ʶ��
        if(a == 1) {
            NamedEntityRecognizer ner = new NamedEntityRecognizer();
            if(!ner.init())
                return;
            ner.parseAll("corpus//3_ltp_segmentCorpus_pos", "corpus//6_ltp_nerCorpus");
            ner.parseAll("corpus//3_ltp_segmentTopicCorpus_pos", "corpus//6_ltp_nerTopicCorpus");
            ner.destroy();
        }

        System.out.println("����0������1������һ�����������");
        a = cin.nextInt();

        // �������
        if(a == 1) {
            SemanticParser parser = new SemanticParser();
            if(!parser.init())
                return;
            parser.parseAll("corpus//3_ltp_segmentCorpus_pos", "corpus//5_ltp_dependencyCorpus","corpus//6_ltp_nerCorpus", "corpus//7_ltp_SemanticCorpus");
            parser.parseAll("corpus//3_ltp_segmentTopicCorpus_pos", "corpus//5_ltp_dependencyTopicCorpus","corpus//6_ltp_nerTopicCorpus", "corpus//7_ltp_SemanticTopicCorpus");
            parser.destroy();
        }

        System.out.println("����0������1������һ����Ǳ�ڶ������дʳ�ȡ");
        a = cin.nextInt();

        // Ǳ�ڶ������дʳ�ȡ
        if(a == 1) {
            LTPCorpusExtractor corpusExtractor = new LTPCorpusExtractor();
            corpusExtractor.extractorAll("corpus//3_ltp_segmentCorpus_pos", "corpus//5_ltp_dependencyCorpus", "corpus//7_ltp_SemanticCorpus,",
                    "corpus//3_ltp_segmentTopicCorpus_pos", "corpus//5_ltp_dependencyTopicCorpus", "corpus//7_ltp_SemanticTopicCorpus,", "corpus//8_ltp_TargetPairCorpus" );
        }

        System.out.println("����0������1������һ����������ƶȼ���");
        a = cin.nextInt();

        // ������ƶȼ���
        if(a == 1) {
            SentimentSorter sentimentSorter = new SentimentSorter();
            SentimentSorter.init("corpus//dic//posOpinionDic.txt", "corpus//dic//negOpinionDic.txt","corpus//dic//posEmotionDic.txt", "corpus//dic//negEmotionDic.txt");
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
