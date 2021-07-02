package com.meiyou.bigwhale.util;

import com.meiyou.bigwhale.common.Constant.YarnResourcemanagerScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yangjie
 */
public class YarnUtil {

    private YarnUtil() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(YarnUtil.class);

    public static String getQueueName(String yarnSchedulerType, String queue) {
        if (YarnResourcemanagerScheduler.FAIR_SCHEDULER.equals(yarnSchedulerType) &&
                queue != null && !"root".equals(queue) && !queue.startsWith("root.")) {
            return "root." + queue;
        }
        return queue;
    }
}
