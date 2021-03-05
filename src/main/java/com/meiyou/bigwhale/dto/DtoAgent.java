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
public class DtoAgent extends AbstractPageDto {

    private Integer id;
    private String name;
    private String description;
    private List<String> instances;
    private Integer clusterId;

    @Override
    public String validate() {
        if (StringUtils.isBlank(name)) {
            return "名称不能为空";
        }
        if (instances == null || instances.isEmpty()) {
            return "实例不能为空";
        }
        return null;
    }

}
