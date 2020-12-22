package com.meiyou.bigwhale.common.pojo;

/**
 * @author Suxy
 * @date 2019/11/7
 * @description file description
 */
public class BackpressureInfo {

    /**
     * 实际ratio * 100，方便监控任务处理
     */
    public final int ratio;
    public final String nextVertex;

    public BackpressureInfo(int ratio, String nextVertex) {
        this.ratio = ratio;
        this.nextVertex = nextVertex;
    }

}
