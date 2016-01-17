import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 15-3-11
 * Time: ����9:47
 * To change this template use File | Settings | File Templates.
 */
public class PhraseProducer {
    // ����������Ե���
    public final int  N_TYPE = 1;
    // ���� ��
    public final int  DE_TYPE = 2;
    // ���ݴ�
    public final int ADJ_TYPE = 3;
    // ������
    public final int MQ_TYPE = 4;

    // ���������ϲ��ļ�
    public void produceAll(String readDir, String outputPosDir, String outputNoPosDir) {
        File[] fileArray = (new File(readDir)).listFiles();

        File file1 = new File(outputPosDir);
        if(!file1.exists()) {
            if(!file1.mkdirs()){
                System.out.println("����Ŀ¼ʧ�ܣ�");
                return;
            }
        }
        File file2 = new File(outputNoPosDir);
        if(!file2.exists()) {
            if(!file2.mkdirs()){
                System.out.println("����Ŀ¼ʧ�ܣ�");
                return;
            }
        }

        for(int i=0; i<fileArray.length; ++i){
            produce(fileArray[i].getAbsolutePath(), fileArray[i].getName(), outputPosDir, outputNoPosDir);
        }
    }
    // �������ļ��еĶ���ϲ�
    public void produce(String filePath, String fileName, String outputPosDir, String outputNoPosDir){
        FileReader reader = null;
        FileWriter posWriter = null;
        FileWriter noPosWriter = null;
        try {
            // ��ȡ�ļ�����
            File file = new File(filePath);
            reader = new FileReader(file);
            int length = (int)file.length();
            char[] content = new char[length];
            reader.read(content);
            String contentStr = String.valueOf(content);
            StringBuffer contentBuffer = new StringBuffer();

            // �����������ʶ���
           String[] lineArray = contentStr.split("\n");
            // ����ÿ��
            for(int j=0; j<lineArray.length-1; ++j){
                if(lineArray[j].length() > 0){
                    contentBuffer.append(produceLine(lineArray[j]));
                    contentBuffer.append('\n');
                }
            }

            System.out.println(fileName+"����ϲ���ɣ�");
            // ���
            File outputPosFile = new File(outputPosDir+"//"+fileName);
            File outputNoPosFile = new File(outputNoPosDir+"//"+fileName);
            posWriter = new FileWriter(outputPosFile);
            noPosWriter = new FileWriter(outputNoPosFile);
            posWriter.write(contentBuffer.toString());

            // ȥ����ǩ
            String tmpStr = String.valueOf(contentBuffer);
            tmpStr = tmpStr.replaceAll("/[^\\s]*", "");
            noPosWriter.write(tmpStr);

        } catch (Exception e) {
            e.printStackTrace();
        }  finally {
            try{
                // ��������
                noPosWriter.close();
                posWriter.close();
                reader.close();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    // ���ɴ�������
    public StringBuffer produceLine(String line){
        StringBuffer tmpLine = new StringBuffer();
        int lastEndIndex = line.lastIndexOf('��');
        int length = line.length();
        String[] blockArray = line.split(" ");
        // ����ÿ���ո�����Ĵʿ�
        for(int i=0; i<blockArray.length; ++i){
            // �滻�����Ŷ���
            if(blockArray[i].length() <= 0)
                continue;
            if(blockArray[i].charAt(0)=='��'){
                StringBuffer copyLine = tmpLine;
                if(lastEndIndex != -1) {
                    while(i<blockArray.length && blockArray[i].length()>0 && blockArray[i].charAt(0)!='��'){
                        blockArray[i] = blockArray[i].replaceAll("/[^\\s]*", "");
                        tmpLine.append(blockArray[i]);
                        ++i;
                    }
                    // �������ġ�
                    if(blockArray[i].length()>0 && blockArray[i].charAt(0)=='��')
                        tmpLine.append("��/n");
                    else
                        tmpLine = copyLine;
                }
            }
            // �滻���ʶ���
            else if(containSpecialWords(blockArray[i], N_TYPE)){
                while(i<blockArray.length-1 && (containSpecialWords(blockArray[i+1],N_TYPE) || containSpecialWords(blockArray[i+1],DE_TYPE))){
                    blockArray[i] = blockArray[i].replaceAll("/[^\\s]*", "");
                    tmpLine.append(blockArray[i]);
                    ++i;
                }
                // ������������
                if(i > 0) {
                    blockArray[i] = blockArray[i].replaceAll("/[^\\s]*", "/n");
                }
                tmpLine.append(blockArray[i]).append(' ');
            }
            // �滻�������ʵ����ʶ���
            else if(containSpecialWords(blockArray[i], MQ_TYPE)){
                while(i<blockArray.length-1 && (containSpecialWords(blockArray[i+1],MQ_TYPE) || containSpecialWords(blockArray[i+1],N_TYPE) || containSpecialWords(blockArray[i+1],DE_TYPE))){
                    blockArray[i] = blockArray[i].replaceAll("/[^\\s]*", "");
                    tmpLine.append(blockArray[i]);
                    ++i;
                }
                // ������������
                if(i > 0) {
                    blockArray[i] = blockArray[i].replaceAll("/[^\\s]*", "/n");
                }
                tmpLine.append(blockArray[i]).append(' ');
            }
            // �������ݴ�+de
            else if(containSpecialWords(blockArray[i], ADJ_TYPE)){
                while(i<blockArray.length-1 && (containSpecialWords(blockArray[i+1],ADJ_TYPE) || containSpecialWords(blockArray[i+1],DE_TYPE))){
                    blockArray[i] = blockArray[i].replaceAll("/[^\\s]*", "");
                    tmpLine.append(blockArray[i]);
                    ++i;
                }
                // �����������ݴ�
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
            return str.contains("/n") || str.contains("/vn")|| str.contains("/r"); //���ʣ������ʣ�����
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
