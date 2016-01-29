package cn.edu.seu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 15-3-16
 * Time: 下午9:42
 * To change this template use File | Settings | File Templates.
 */
public class Evaluator {
    // 存储精确评价下提交结果中正确的样例数
    private ArrayList<Integer> seriousCorrectSet = new ArrayList<Integer>();
    // 存储宽松评价下提交结果中正确的覆盖率
    private ArrayList<Double> softCorrectForPSet = new ArrayList<Double>();
    private ArrayList<Double> softCorrectForRSet = new ArrayList<Double>();
    //提交结果中总的样例数；评测文本中所有样例数
    private ArrayList<Integer> allPostSet = new ArrayList<Integer>();
    private ArrayList<Integer> allSystemSet = new ArrayList<Integer>();

    // 统计各个文本中错误感情数
    private ArrayList<Integer> wrongSentimentSet = new ArrayList<Integer>();

    // 精确和宽松评价下提交结果中正确的样例数临时变量
    private int seriousCount = 0;
    private double softCountForP = 0.0;
    private double softCountForR = 0.0;
    private int wrongSentimentCount = 0;

    // 评估所有文件
    public void evaluateAll(String evaluateCorpusDir, String resultCorpusDir, boolean isSentiment){
        File[] evaluateCorpusArray = (new File(evaluateCorpusDir)).listFiles();
        File[] resultCorpusArray = (new File(resultCorpusDir)).listFiles();
        if(evaluateCorpusArray.length != resultCorpusArray.length){
            System.out.println("对齐目录文件数目不匹配！");
            return;
        }

        for(int i=0; i<resultCorpusArray.length; ++i){
            evaluate(evaluateCorpusArray[i].getAbsolutePath(), resultCorpusArray[i].getAbsolutePath(), isSentiment);
            System.out.println(resultCorpusArray[i].getName()+"评测完成！");
        }

    }

    // 评估单个文件
    public void evaluate(String evaluateFilePath, String resultFilePath, boolean isSentiment){
        FileReader evaluateFileReader = null;
        FileReader resultFileReader = null;
        BufferedReader evaluateBufferReader = null;
        BufferedReader resultBufferReader = null;
        try{
            File evaluateFile = new File(evaluateFilePath);
            File resultFile = new File(resultFilePath);
            evaluateFileReader = new FileReader(evaluateFile);
            resultFileReader = new FileReader(resultFile);

            //计算评测文本中所有样例数
            int length = (int)evaluateFile.length();
            char[] content = new char[length];
            evaluateFileReader.read(content);
            String evaluateStr = String.valueOf(content);
            allSystemSet.add(new Integer(count(evaluateStr, "[")));
            // 计算提交结果中总的样例数
            length = (int)resultFile.length();
            char[] content1 = new char[length];
            resultFileReader.read(content1);
            String resultStr = String.valueOf(content1);
            allPostSet.add(new Integer(count(resultStr, "[")));

            // 计算待评估语料与评测语料每一行的提交结果中正确的样例数
            evaluateBufferReader = new BufferedReader(evaluateFileReader);
            resultBufferReader = new BufferedReader(resultFileReader);
            String postLine[]  = resultStr.split("\n");
            String systemLine[] = evaluateStr.split("\n");
            for(int i=0; i<postLine.length; ++i){
                computeCorrect(postLine[i], systemLine[i], isSentiment);
            }
            seriousCorrectSet.add(new Integer(seriousCount));
            softCorrectForPSet.add(new Double(softCountForP));
            softCorrectForRSet.add(new Double(softCountForR));
            wrongSentimentSet.add(wrongSentimentCount);
            seriousCount = 0;
            softCountForP = 0;
            softCountForR = 0;
            wrongSentimentCount = 0;

        } catch (Exception e){
            e.printStackTrace();
        } finally {
            try{
                if(resultFileReader != null){
                    resultFileReader.close();
                }
                if(evaluateFileReader != null){
                    evaluateFileReader.close();
                }
                if(resultBufferReader != null){
                    resultBufferReader.close();
                }
                if(evaluateBufferReader != null){
                    evaluateBufferReader.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    // 统计字符串中指定字符串出现的次数
    public int count(String str, String target){
        int count = 0;
        int index = str.indexOf(target);
        while(index != -1){
            ++count;
            index = str.indexOf(target, index+1);
        }

        return count;
    }

    // 计算精确和宽松标准下样例的准确情况
    public void computeCorrect(String postLine, String systemLine, boolean isSentiment){
        int postIndex1 = 0, postIndex2 = 0;
        int systemIndex1 = 0, systemIndex2 = 0;
        String startPostIndex = null, endPostIndex = null;
        String startSystemIndex = null, endSystemIndex = null;
        String postSentiment = null, systemSentiment = null;
        double matchLength = 0.0, allLengthForP = 0.0, allLengthForR = 0.0;
        Integer startPostIndexValue, endPostIndexValue,startSystemIndexValue,endSystemIndexValue;
        // 分别获取提交和系统行中的起始位置和情感词
       while(true){
           systemIndex1 = systemLine.indexOf('[', systemIndex2);
           if(systemIndex1 == -1)
               return;
           systemIndex2 = systemLine.indexOf(',', systemIndex1);
           if(systemIndex2 == -1)
               return;
           startSystemIndex = systemLine.substring(systemIndex1+1, systemIndex2);
           systemIndex1 = systemLine.indexOf(',', systemIndex2+1);
           if(systemIndex1 == -1)
               return;
           endSystemIndex = systemLine.substring(systemIndex2+1, systemIndex1);
           systemIndex2 = systemLine.indexOf(']', systemIndex1+1);
           if(systemIndex2 == -1)
               return;
           systemSentiment = systemLine.substring(systemIndex1+1, systemIndex2);

           startSystemIndexValue = Integer.valueOf(startSystemIndex);
           endSystemIndexValue = Integer.valueOf(endSystemIndex);

           if(startSystemIndex!=null && endSystemIndex!=null && systemSentiment!=null){
               while(true){
                   postIndex1 = postLine.indexOf('[', postIndex2);
                   if(postIndex1 == -1)
                       return;
                   postIndex2 = postLine.indexOf(',', postIndex1);
                   if(postIndex2 == -1)
                       return;
                   startPostIndex = postLine.substring(postIndex1+1, postIndex2);
                   postIndex1 = postLine.indexOf(',', postIndex2+1);
                   if(postIndex1 == -1)
                       return;
                   endPostIndex = postLine.substring(postIndex2+1, postIndex1);
                   postIndex2 = postLine.indexOf(']', postIndex1+1);
                   if(postIndex2 == -1)
                       return;
                   postSentiment = postLine.substring(postIndex1+1, postIndex2);

                   // 提交部分已遍历完
                   if(startPostIndex==null || endPostIndex==null || postSentiment==null)
                       return;

                   // 判断标注部分是否与系统当前的标注部分有重叠
                   startPostIndexValue = Integer.valueOf(startPostIndex);
                   endPostIndexValue = Integer.valueOf(endPostIndex);

                   if((startPostIndexValue>=startSystemIndexValue && startPostIndexValue<=endSystemIndexValue) ||
                           (endPostIndexValue>=startSystemIndexValue && endPostIndexValue<=endSystemIndexValue)){
                       if(!postSentiment.equals(systemSentiment)){
                           ++wrongSentimentCount;
                           if(isSentiment)
                                break;
                       }
                       // 精确匹配
                       if(startPostIndexValue.equals(startSystemIndexValue) && endPostIndexValue.equals(endSystemIndexValue)) {
                           ++seriousCount;
                           ++softCountForP;
                           ++softCountForR;
                           break;
                       }
                       // 覆盖率匹配计算
                       else{
                           if(startPostIndexValue>=startSystemIndexValue){
                               if(endPostIndexValue > endSystemIndexValue)
                                    matchLength = endSystemIndexValue - startPostIndexValue+1;
                               else
                                   matchLength = endPostIndexValue - startPostIndexValue+1;
                           }else{
                               if(startPostIndexValue < startSystemIndexValue)
                                   matchLength = endPostIndexValue - startSystemIndexValue+1;
                               else
                                   matchLength = endPostIndexValue - startPostIndexValue+1;
                           }
                           allLengthForP = endPostIndexValue - startPostIndexValue+1;
                           allLengthForR = endSystemIndexValue - startSystemIndexValue+1;

                           assert matchLength >= 0: "匹配长度应大于等于0！";
                           assert allLengthForP > 0: "标注数据元素长度应大于0！";
                           assert allLengthForR > 0: "评估数据元素长度应大于0！";

                           softCountForP += (matchLength / allLengthForP);
                           softCountForR += (matchLength / allLengthForR);
                           break;
                        }
                   } else if(startPostIndexValue < startSystemIndexValue){
                       continue;
                   } else{
                       break;
                   }
               }
           } else{
               break;
           }

       }

    }

    public void getEvaluationResult(String evaluationResultFilePath){
        File file = new File(evaluationResultFilePath);
        if(!file.getParentFile().exists()) {
            if(!file.getParentFile().mkdirs()){
                System.out.println("创建目录失败！");
                return;
            }
        }
        FileWriter writer = null;
        DecimalFormat df = new DecimalFormat("#.####");

        try{
            ArrayList<Double> seriousCorrectRateList = new ArrayList<Double>();
            ArrayList<Double> seriousRecallRateList = new ArrayList<Double>();
            ArrayList<Double> softCorrectRateList = new ArrayList<Double>();
            ArrayList<Double> softRecallRateList = new ArrayList<Double>();

            ArrayList<Double> seriousFRateList = new ArrayList<Double>();
            ArrayList<Double> softFRateList = new ArrayList<Double>();

            double tmpP = 0.0, tmpR = 0.0, tmpF = 0.0;
            int macroSeriousPN = 0;
            double macroSoftPN = 0, macroSoftRN = 0;
            int allSystemInstance = 0, allPostInstance = 0;
            double microSeriousP = 0.0, microSeriousR = 0.0, microSeriousF = 0.0;
            double microSoftP = 0.0, microSoftR = 0.0, microSoftF = 0.0;
            int fileNum = seriousCorrectSet.size();

            writer= new FileWriter(file);
            // 输出每个文件的精确评价下的正确样例数、宽松评价下的正确样例数、识别的样例数、系统的样例数
            writer.write("【序号】\t精确正确样例数\t宽松正确样例数\t识别的样例数\t系统的样例数\t感情错误的样例数\n");
            for(int i=0; i<fileNum; ++i){
                writer.write(i+"\t"+String.valueOf(seriousCorrectSet.get(i))+"\t"+df.format(softCorrectForPSet.get(i))+"\t"+String.valueOf(allPostSet.get(i))+"\t"+String.valueOf(allSystemSet.get(i))+"\t"+String.valueOf(wrongSentimentSet.get(i))+"\r\n");
            }
            writer.write("\r\n\r\n");
            // 输出每个文件的精确评价下的准确率、宽松评价下的准确率、精确评价下的召回率、宽松评价下的召回率、精确评价下的F值、宽松评价下的F值
            writer.write("【序号】\t精确准确率\t精确召回率\t精确F值\t宽松准确率\t宽松召回率\t宽松F值\r\n");
            for(int i=0; i<fileNum; ++i){
                macroSeriousPN += seriousCorrectSet.get(i);
                macroSoftPN += softCorrectForPSet.get(i);
                macroSoftRN += softCorrectForRSet.get(i);
                allPostInstance += allPostSet.get(i);
                allSystemInstance += allSystemSet.get(i);

                tmpP = ((double)seriousCorrectSet.get(i))/((double)allPostSet.get(i));
                tmpR = ((double)seriousCorrectSet.get(i))/((double)allSystemSet.get(i));
                tmpF = 2*tmpP*tmpR/(tmpP+tmpR);
                microSeriousP += tmpP;
                microSeriousR += tmpR;
                microSeriousF += tmpF;
                seriousCorrectRateList.add(tmpP);
                seriousRecallRateList.add(tmpR);
                seriousFRateList.add(tmpF);
                writer.write(i + "\t" + df.format(tmpP) + "\t" + df.format(tmpR) + "\t" + df.format(tmpF));

                tmpP = (softCorrectForPSet.get(i))/((double)allPostSet.get(i));
                tmpR = (softCorrectForRSet.get(i))/((double)allSystemSet.get(i));
                tmpF = 2*tmpP*tmpR/(tmpP+tmpR);
                microSoftP += tmpP;
                microSoftR += tmpR;
                microSoftF += tmpF;
                softCorrectRateList.add(tmpP);
                softRecallRateList.add(tmpR);
                softFRateList.add(tmpF);
                writer.write("\t"+df.format(tmpP)+"\t"+df.format(tmpR)+"\t"+df.format(tmpF)+"\r\n");
            }
            writer.write("\r\n\r\n");
            // 输出精确评价下的准确率、精确评价下的召回率、精确评价下的F值
            writer.write("精确标准\t准确率\t召回率\tF值\r\n");
            tmpP = (double)macroSeriousPN / allPostInstance;
            tmpR = (double)macroSeriousPN / allSystemInstance;
            tmpF = (double)2*tmpP*tmpR/(tmpP+tmpR);
            writer.write("微平均\t"+df.format(tmpP)+"\t"+df.format(tmpR)+"\t"+df.format(tmpF)+"\r\n");
            microSeriousP /= fileNum;
            microSeriousR /= fileNum;
            microSeriousF /= fileNum;
            writer.write("宏平均\t"+df.format(microSeriousP)+"\t"+df.format(microSeriousR)+"\t"+df.format(microSeriousF)+"\r\n");

            // 输出宽松评价下的准确率、宽松评价下的召回率、宽松评价下的F值
            writer.write("宽松标准\t准确率\t召回率\tF值\r\n");
            tmpP = macroSoftPN / allPostInstance;
            tmpR = macroSoftRN / allSystemInstance;
            tmpF = (double)2*tmpP*tmpR/(tmpP+tmpR);
            writer.write("微平均\t"+df.format(tmpP)+"\t"+df.format(tmpR)+"\t"+df.format(tmpF)+"\r\n");
            microSoftP /= fileNum;
            microSoftR /= fileNum;
            microSoftF /= fileNum;
            writer.write("宏平均\t"+df.format(microSoftP)+"\t"+df.format(microSoftR)+"\t"+df.format(microSoftF)+"\r\n");
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try{
                if(writer != null)
                    writer.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }
}

