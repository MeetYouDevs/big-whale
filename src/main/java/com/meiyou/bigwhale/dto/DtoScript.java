package com.meiyou.bigwhale.dto;

import com.meiyou.bigwhale.common.Constant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoScript extends AbstractPageDto {

    private static final Pattern SPARK_QUEUE_PATTERN = Pattern.compile("--queue ([\\w-.,]+)");
    private static final Pattern FLINK_QUEUE_PATTERN = Pattern.compile("-yqu ([\\w-.,]+)");
    private static final Pattern SPARK_NAME_PATTERN = Pattern.compile("--name ([\\w-.,]+)");
    private static final Pattern FLINK_NAME_PATTERN = Pattern.compile("-ynm ([\\w-.,]+)");

    private Integer id;
    private String name;
    private String description;
    private String type;
    private Integer monitorId;
    /**
     * 列表查询字段
     */
    private Boolean monitorEnabled;
    private Integer scheduleId;
    /**
     * 拓扑节点ID
     */
    private String scheduleTopNodeId;
    private Integer clusterId;
    private Integer agentId;
    private Integer timeout;
    private String content;
    private String input;
    private String output;
    private Date createTime;
    private Integer createBy;
    private Date updateTime;
    private Integer updateBy;

    /**
     * yarn应用属性
     */
    private String user;
    private String queue;
    private String app;


    /**
     * 资源展示字段
     */
    private Integer totalMemory;
    private Integer totalCores;

    private DtoMonitor monitor;

    /**
     * 模糊搜字段
     * @return
     */
    private String text;

    @Override
    public String validate() {
        if (StringUtils.isBlank(name)) {
            return "脚本名称不能为空";
        }
        if (type == null) {
            return "脚本类型不能为空";
        }
        if (timeout == null) {
            return "脚本超时不能为空";
        }
        //检查集群或代理参数
        if (isYarn()) {
            if (clusterId == null) {
                return "脚本集群不能为空";
            }
            agentId = null;
        } else {
            if (agentId == null) {
                return "脚本代理不能为空";
            }
            clusterId = null;
        }
        if (StringUtils.isBlank(content)) {
            return "脚本代码不能为空";
        }
        content = content.trim().replaceAll(" ", " ");
        //提取app的值
        app = null;
        if (isYarn()) {
            String [] arr = extractQueueAndApp();
            queue = arr[0];
            app = arr[1];
            if (StringUtils.isBlank(app)) {
                return "脚本【" + name + "】未能从参数中提取到应用名称，请检查代码";
            }
        }
        if (monitor != null) {
            return monitor.validate();
        }
        return null;
    }

    public boolean isBatch() {
        return !Constant.ScriptType.SPARK_STREAM.equals(type) &&
                !Constant.ScriptType.FLINK_STREAM.equals(type);
    }

    public boolean isYarn() {
        return Constant.ScriptType.SPARK_BATCH.equals(type) ||
                Constant.ScriptType.SPARK_STREAM.equals(type) ||
                Constant.ScriptType.FLINK_BATCH.equals(type) ||
                Constant.ScriptType.FLINK_STREAM.equals(type);
    }

    /**
     * [queue, app]
     * @return
     */
    private String [] extractQueueAndApp() {
        String queue = null;
        String app = null;
        if (Constant.ScriptType.SPARK_BATCH.equals(type) || Constant.ScriptType.SPARK_STREAM.equals(type)) {
            Matcher matcher = SPARK_QUEUE_PATTERN.matcher(content);
            if (matcher.find()) {
                queue = matcher.group(1);
            }
            matcher = SPARK_NAME_PATTERN.matcher(content);
            if (matcher.find()) {
                app = matcher.group(1);
            }
        }
        if (Constant.ScriptType.FLINK_BATCH.equals(type) || Constant.ScriptType.FLINK_STREAM.equals(type)) {
            Matcher matcher = FLINK_QUEUE_PATTERN.matcher(content);
            if (matcher.find()) {
                queue = matcher.group(1);
            }
            matcher = FLINK_NAME_PATTERN.matcher(content);
            if (matcher.find()) {
                app = matcher.group(1);
            }
        }
        return new String[]{queue, app};
    }

}
