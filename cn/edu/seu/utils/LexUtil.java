package cn.edu.seu.utils;

/**
 * Created with IntelliJ IDEA.
 * User: Jazz
 * Date: 16-2-13
 * Time: 下午12:47
 * To change this template use File | Settings | File Templates.
 */
public class LexUtil {
    // 特殊名词
    public static String SPECIAL_NOUN = " nh ni nl ns nz n ";
    // 人称名词
    public static String HUMAN_NOUN = " nh ";
    // 特别处理的人称代词
    public static String HUMAN_PRONOUN  = " 你 妳 他 她 你们 妳们 他们 她们 ";

    // 名词性惯用语,名词性语素,副动词,名动词,不及物动词（内动词）,动词性惯用语,动词性语素,形容词
    public final static String[] OPINION_SET_FOR_STF = {"/nl", "/ng", "/a"};
    public final static String[] OPINION_SET_FOR_LTP = {"/a", "/d", "/v", "/n", "/i"};
    // 动词性情感词黑名单
    public final static String[] BLACK_OPINION_SET = {"/vshi", "/vyou", "/vf", "/vx"};
    // 基本目标依赖关系类型
    public final static String[] BASIC_TARGET_REL_SET_FOR_STF = {"root", "dep", "subj", "mod", "comp", "nn"};
    public final static String[] BASIC_TARGET_REL_SET_FOR_LTP = {"HED", "ATT", "SBV", "VOB", "FOB", "POB", "IOB", "ADV", "CMP"};
    // 含宾语成分的关系
    public final static String[] OBJECT_REL_SET_FOR_LTP = {"VOB", "FOB", "POB", "IOB"};
    // 根关系
    public final static String ROOT_FOR_STF = "root";
    public final static String ROOT_FOR_LTP = "HED";
    // adv
    public final static String ADV_DEP_FOR_STF = "advmod";
    public final static String ADV_DEP_FOR_LTP = "ADV";
    // coo
    public final static String COO_DEP_FOR_STF = "conj";
    public final static String COO_DEP_FOR_LTP = "COO";
    // 反转关系
    public final static String NEG_FOR_STF = "neg";
    // 副词消弱标记
    public final static String[] WEAK_ADV = {}; // 只是 “仅仅，不过，而已”
    // 否定词
    public final static String[] NOT_SET = {"不", "非", "无","没", "不多", "不大", "不太", "否认", "无法", "毫无", "缺乏", "减轻", "减缓", "减慢", "减少", "缓解","缓减","缓轻","遏制","阻止","不可","不能","不得", "没有", "没什么"};
    // 否定词白名单
    public final static String[] NOT_WHITE_SET = {"不论", "不得不", "不过", "不可不", "非常",  "无非", "无论","没准","不少"};
    // A0作为评价对象的关联性普通动词
    public final static String RELATE_COMMON_VERB = " 是 让 使 使得 有 作为 ";
    // 情感词词性
    public final static String SENTIMENT_TAG = " a b d n i v u z m ws ";
}
