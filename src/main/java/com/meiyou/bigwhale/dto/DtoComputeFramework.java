package com.meiyou.bigwhale.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;


/**
 * @author Suxy
 * @date 2020/2/20
 * @description file description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoComputeFramework extends AbstractPageDto {

    private Integer id;
    private String type;
    private String version;
    private String command;
    private Integer orders;

    @Override
    public String validate() {
        if (StringUtils.isBlank(type)) {
            return "类型不能为空";
        }
        if (StringUtils.isBlank(version)) {
            return "版本不能为空";
        }
        if (StringUtils.isBlank(command)) {
            return "命令不能为空";
        }
        if (orders == null) {
            return "排序不能为空";
        }
        return null;
    }

}
