package com.meiyouframework.bigwhale.dto;

import com.meiyouframework.bigwhale.common.Constant;
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

    private static final Pattern SPARK_NAME_PATTERN = Pattern.compile("--name ([\\w-.,]+)");
    private static final Pattern FLINK_NAME_PATTERN = Pattern.compile("-ynm ([\\w-.,]+)");
    private static final Pattern SPARK_QUEUE_PATTERN = Pattern.compile("--queue ([\\w-.,]+)");
    private static final Pattern FLINK_QUEUE_PATTERN = Pattern.compile("-yqu ([\\w-.,]+)");

    private String id;
    private String name;
    private String description;
    private Integer type;
    private Integer timeout;
    private String script;
    private String input;
    private String output;
    private String agentId;
    private String clusterId;
    private String uid;
    private Date createTime;
    private Date updateTime;

    private String user;
    private String queue;
    private String app;

    /**
     * 资源展示字段
     */
    private Integer totalMemory;
    private Integer totalCores;

    /**
     * 模糊搜字段
     * @return
     */
    private String text;

    @Override
    public String validate() {
        if (StringUtils.isBlank(name)) {
            return "名称不能为空";
        }
        if (timeout == null) {
            return "运行超时时间不能为空";
        }
        if (type == null) {
            return "类型不能为空";
        }
        //检查集群或代理参数
        if (type != Constant.SCRIPT_TYPE_SHELL_BATCH) {
            if (StringUtils.isBlank(clusterId)) {
                return "集群不能为空";
            }
            agentId = null;
        } else {
            if (StringUtils.isBlank(agentId)) {
                return "代理不能为空";
            }
            clusterId = null;
        }
        if (StringUtils.isBlank(script)) {
            return "脚本不能为空";
        }
        script = script.trim().replaceAll(" ", " ");
        //提取app的值
        app = null;
        if (type != Constant.SCRIPT_TYPE_SHELL_BATCH) {
            if (type == Constant.SCRIPT_TYPE_SPARK_STREAMING || type == Constant.SCRIPT_TYPE_SPARK_BATCH) {
                Matcher matcher = SPARK_NAME_PATTERN.matcher(script);
                if (matcher.find()) {
                    app = matcher.group(1);
                }
                matcher = SPARK_QUEUE_PATTERN.matcher(script);
                if (matcher.find()) {
                    queue = matcher.group(1);
                }
            }
            if (type == Constant.SCRIPT_TYPE_FLINK_STREAMING || type == Constant.SCRIPT_TYPE_FLINK_BATCH) {
                Matcher matcher = FLINK_NAME_PATTERN.matcher(script);
                if (matcher.find()) {
                    app = matcher.group(1);
                }
                matcher = FLINK_QUEUE_PATTERN.matcher(script);
                if (matcher.find()) {
                    queue = matcher.group(1);
                }
            }
            if (StringUtils.isBlank(app)) {
                return "未能从参数中提取到应用名称，请检查脚本";
            }
        } else {
            app = name;
        }
        return null;
    }

    public boolean isOffline() {
        return type == Constant.SCRIPT_TYPE_SHELL_BATCH ||
                type == Constant.SCRIPT_TYPE_SPARK_BATCH ||
                type == Constant.SCRIPT_TYPE_FLINK_BATCH;
    }
}
