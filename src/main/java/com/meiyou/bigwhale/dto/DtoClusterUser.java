package com.meiyou.bigwhale.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoClusterUser extends AbstractPageDto {

    private Integer id;
    private Integer clusterId;
    private Integer userId;
    private String queue;
    private String user;


    private List<Integer> clusterIds;

    @Override
    public String validate() {
        if (clusterId == null && (clusterIds == null || clusterIds.isEmpty())) {
            return "集群不能为空";
        }
        if (userId == null) {
            return "平台用户不能为空";
        }
        if (StringUtils.isBlank(queue)) {
            return "队列不能为空";
        }
        return null;
    }

}
