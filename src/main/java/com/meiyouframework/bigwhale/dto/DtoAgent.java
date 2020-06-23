package com.meiyouframework.bigwhale.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoAgent extends AbstractPageDto {

    private String id;
    private String host;
    private String mac;
    private String ip;
    private Integer status;
    private Integer version;
    private Date createTime;
    private Date lastConnTime;
    private Integer socketPort;
    private String clusterId;

    @Override
    public String validate() {
        if (StringUtils.isBlank(ip)) {
            return "IP不能为空";
        }
        if (StringUtils.isBlank(host)) {
            return "HOST不能为空";
        }
        if (StringUtils.isBlank(mac)) {
            return "MAC不能为空";
        }
        return null;
    }

}
