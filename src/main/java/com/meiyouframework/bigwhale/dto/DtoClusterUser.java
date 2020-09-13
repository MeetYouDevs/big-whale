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
    private String clusterId;
    private String queue;
    private String user;

    @Override
    public String validate() {
        if (StringUtils.isBlank(uid)) {
            return "平台用户不能为空";
        }
        if (StringUtils.isBlank(clusterId)) {
            return "集群不能为空";
        }
        if (StringUtils.isBlank(queue)) {
            return "队列不能为空";
        }
        return null;
    }

}
