package cn.edu.seu.ltp;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 16-1-18
 * Time: 锟斤拷锟斤拷4:09
 * To change this template use File | Settings | File Templates.
 */
import java.util.ArrayList;
import java.util.List;
import edu.hit.ir.ltp4j.*;

public class Segment {
    public static void main(String[] args) {
        if(Parser.create("../../../ltp_data/parser.model")<0) {
            System.err.println("load failed");
            return;
        }
        List<String> words = new ArrayList<String>();
        List<String> tags = new ArrayList<String>();
        words.add("一把手");tags.add("n");
        words.add("亲自");tags.add("d");
        words.add("过问");tags.add("v");
        words.add("。");tags.add("wp");
        List<Integer> heads = new ArrayList<Integer>();
        List<String> deprels = new ArrayList<String>();

        int size = Parser.parse(words,tags,heads,deprels);

        for(int i = 0;i<size;i++) {
            System.out.print(heads.get(i)+":"+deprels.get(i));
            if(i==size-1) {
                System.out.println();
            }
            else{
                System.out.print("        ");
            }
        }

        Parser.release();
    }
}