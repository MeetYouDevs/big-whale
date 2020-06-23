package com.meiyouframework.bigwhale.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoClusterUser extends AbstractPageDto {

    private String id;
    private String uid;
    private String queue;
    private String user;
    private String clusterId;
    private Integer status;

    @Override
    public String validate() {
        if (StringUtils.isBlank(uid)) {
            return "用户不能为空";
        }
        if (StringUtils.isBlank(queue)) {
            return "集群队列不能为空";
        }
        if (StringUtils.isBlank(clusterId)) {
            return "集群不能为空";
        }
        if (status == null) {
            return "状态不能为空";
        }
        return null;
    }
}
