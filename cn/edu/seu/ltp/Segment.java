package cn.edu.seu.ltp;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 16-1-18
 * Time: 下午4:09
 * To change this template use File | Settings | File Templates.
 */
import java.util.ArrayList;
import java.util.List;
import edu.hit.ir.ltp4j.*;

public class Segment {
    public static void main(String[] args) {
        SRL.create("ltp_data/srl");
        ArrayList<String> words = new ArrayList<String>();
        words.add("一把手");
        words.add("亲自");
        words.add("过问");
        words.add("。");
        ArrayList<String> tags = new ArrayList<String>();
        tags.add("n");
        tags.add("d");
        tags.add("v");
        tags.add("wp");
        ArrayList<String> ners = new ArrayList<String>();
        ners.add("O");
        ners.add("O");
        ners.add("O");
        ners.add("O");
        ArrayList<Integer> heads = new ArrayList<Integer>();
        heads.add(2);
        heads.add(2);
        heads.add(-1);
        heads.add(2);
        ArrayList<String> deprels = new ArrayList<String>();
        deprels.add("SBV");
        deprels.add("ADV");
        deprels.add("HED");
        deprels.add("WP");
        List<Pair<Integer, List<Pair<String, Pair<Integer, Integer>>>>> srls = new ArrayList<Pair<Integer, List<Pair<String, Pair<Integer, Integer>>>>>();
        SRL.srl(words, tags, ners, heads, deprels, srls);
        for (int i = 0; i < srls.size(); ++i) {
            System.out.println(srls.get(i).first + ":");
            for (int j = 0; j < srls.get(i).second.size(); ++j) {
                System.out.println("   tpye = "+ srls.get(i).second.get(j).first + " beg = "+ srls.get(i).second.get(j).second.first + " end = "+ srls.get(i).second.get(j).second.second);
            }
        }
        SRL.release();
    }

}