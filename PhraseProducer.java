import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 15-3-11
 * Time: 下午9:47
 * To change this template use File | Settings | File Templates.
 */
public class PhraseProducer {
    // 名册短语特性单词
    public final int  N_TYPE = 1;
    // 助词 的
    public final int  DE_TYPE = 2;
    // 形容词
    public final int ADJ_TYPE = 3;
    // 数量词
    public final int MQ_TYPE = 4;

    // 批处理短语合并文件
    public void produceAll(String readDir, String outputPosDir, String outputNoPosDir) {
        File[] fileArray = (new File(readDir)).listFiles();

        File file1 = new File(outputPosDir);
        if(!file1.exists()) {
            if(!file1.mkdirs()){
                System.out.println("创建目录失败！");
                return;
            }
        }
        File file2 = new File(outputNoPosDir);
        if(!file2.exists()) {
            if(!file2.mkdirs()){
                System.out.println("创建目录失败！");
                return;
            }
        }

        for(int i=0; i<fileArray.length; ++i){
            produce(fileArray[i].getAbsolutePath(), fileArray[i].getName(), outputPosDir, outputNoPosDir);
        }
    }
    // 将单个文件中的短语合并
    public void produce(String filePath, String fileName, String outputPosDir, String outputNoPosDir){
        FileReader reader = null;
        FileWriter posWriter = null;
        FileWriter noPosWriter = null;
        try {
            // 读取文件内容
            File file = new File(filePath);
            reader = new FileReader(file);
            int length = (int)file.length();
            char[] content = new char[length];
            reader.read(content);
            String contentStr = String.valueOf(content);
            StringBuffer contentBuffer = new StringBuffer();

            // 遍历处理名词短语
           String[] lineArray = contentStr.split("\n");
            // 遍历每行
            for(int j=0; j<lineArray.length-1; ++j){
                if(lineArray[j].length() > 0){
                    contentBuffer.append(produceLine(lineArray[j]));
                    contentBuffer.append('\n');
                }
            }

            System.out.println(fileName+"词组合并完成！");
            // 输出
            File outputPosFile = new File(outputPosDir+"//"+fileName);
            File outputNoPosFile = new File(outputNoPosDir+"//"+fileName);
            posWriter = new FileWriter(outputPosFile);
            noPosWriter = new FileWriter(outputNoPosFile);
            posWriter.write(contentBuffer.toString());

            // 去掉标签
            String tmpStr = String.valueOf(contentBuffer);
            tmpStr = tmpStr.replaceAll("/[^\\s]*", "");
            noPosWriter.write(tmpStr);

        } catch (Exception e) {
            e.printStackTrace();
        }  finally {
            try{
                // 结束工作
                noPosWriter.close();
                posWriter.close();
                reader.close();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    // 生成处理后的行
    public StringBuffer produceLine(String line){
        StringBuffer tmpLine = new StringBuffer();
        int lastEndIndex = line.lastIndexOf('》');
        int length = line.length();
        String[] blockArray = line.split(" ");
        // 遍历每个空格隔开的词块
        for(int i=0; i<blockArray.length; ++i){
            // 替换书名号短语
            if(blockArray[i].length() <= 0)
                continue;
            if(blockArray[i].charAt(0)=='《'){
                StringBuffer copyLine = tmpLine;
                if(lastEndIndex != -1) {
                    while(i<blockArray.length && blockArray[i].length()>0 && blockArray[i].charAt(0)!='》'){
                        blockArray[i] = blockArray[i].replaceAll("/[^\\s]*", "");
                        tmpLine.append(blockArray[i]);
                        ++i;
                    }
                    // 加上最后的》
                    if(blockArray[i].length()>0 && blockArray[i].charAt(0)=='》')
                        tmpLine.append("》/n");
                    else
                        tmpLine = copyLine;
                }
            }
            // 替换名词短语
            else if(containSpecialWords(blockArray[i], N_TYPE)){
                while(i<blockArray.length-1 && (containSpecialWords(blockArray[i+1],N_TYPE) || containSpecialWords(blockArray[i+1],DE_TYPE))){
                    blockArray[i] = blockArray[i].replaceAll("/[^\\s]*", "");
                    tmpLine.append(blockArray[i]);
                    ++i;
                }
                // 加上最后的名词
                if(i > 0) {
                    blockArray[i] = blockArray[i].replaceAll("/[^\\s]*", "/n");
                }
                tmpLine.append(blockArray[i]).append(' ');
            }
            // 替换含数量词的名词短语
            else if(containSpecialWords(blockArray[i], MQ_TYPE)){
                while(i<blockArray.length-1 && (containSpecialWords(blockArray[i+1],MQ_TYPE) || containSpecialWords(blockArray[i+1],N_TYPE) || containSpecialWords(blockArray[i+1],DE_TYPE))){
                    blockArray[i] = blockArray[i].replaceAll("/[^\\s]*", "");
                    tmpLine.append(blockArray[i]);
                    ++i;
                }
                // 加上最后的名词
                if(i > 0) {
                    blockArray[i] = blockArray[i].replaceAll("/[^\\s]*", "/n");
                }
                tmpLine.append(blockArray[i]).append(' ');
            }
            // 处理形容词+de
            else if(containSpecialWords(blockArray[i], ADJ_TYPE)){
                while(i<blockArray.length-1 && (containSpecialWords(blockArray[i+1],ADJ_TYPE) || containSpecialWords(blockArray[i+1],DE_TYPE))){
                    blockArray[i] = blockArray[i].replaceAll("/[^\\s]*", "");
                    tmpLine.append(blockArray[i]);
                    ++i;
                }
                // 加上最后的形容词
                blockArray[i] = blockArray[i].replaceAll("/[^\\s]*", "/a");
                tmpLine.append(blockArray[i]).append(' ');
            }
            else{
                tmpLine.append(blockArray[i]).append(' ');
            }
        }
        return tmpLine;
    }

    public boolean containSpecialWords(String str, int type){
        if(type == N_TYPE){
            return str.contains("/n") || str.contains("/vn")|| str.contains("/r"); //名词，名动词，代词
        }else if(type == DE_TYPE){
            return str.contains("/ude1");
        }else if(type == ADJ_TYPE){
            return str.contains("/a");
        }else if(type == MQ_TYPE){
            return str.contains("/m") || str.contains("/q");
        }

        return false;
    }
}
