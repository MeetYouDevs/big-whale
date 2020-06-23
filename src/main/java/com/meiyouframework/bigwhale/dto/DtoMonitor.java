package com.meiyouframework.bigwhale.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoMonitor extends AbstractPageDto {

    private String id;
    private Integer type;
    private Integer status;
    private String cron;
    private String uid;
    private String scriptId;
    private Integer waitingBatches;
    private Boolean autoRestart;
    private Boolean exAutoRestart;
    private Boolean sendMail;
    private List<String> dingdingHooks;
    private Date createTime;
    private Date updateTime;
    private Date executeTime;

    @Override
    public String validate() {
        if (StringUtils.isBlank(cron)) {
            return "cron表达式不能为空";
        }
        if (type == null) {
            return "类型不能为空";
        }
        if (StringUtils.isBlank(scriptId)) {
            return "脚本不能为空";
        }
        return null;
    }
}
