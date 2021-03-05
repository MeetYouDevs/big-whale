package com.meiyou.bigwhale.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoCluster extends AbstractPageDto {

    private Integer id;
    private String name;
    private String yarnUrl;
    private String fsDefaultFs;
    private String fsWebhdfs;
    private String fsUser;
    private String fsDir;
    private Boolean defaultFileCluster;
    private Boolean flinkProxyUserEnabled;
    private String streamBlackNodeList;
    private String batchBlackNodeList;

    @Override
    public String validate() {
        if (StringUtils.isBlank(name)) {
            return "名称不能为空";
        }
        if (StringUtils.isBlank(yarnUrl)) {
            return "yarn管理地址不能为空";
        }
        if (StringUtils.isBlank(fsDefaultFs)) {
            return "fs.defaultFS不能为空";
        }
        if (StringUtils.isBlank(fsWebhdfs)) {
            return "fs.webhdfs不能为空";
        }
        if (StringUtils.isBlank(fsUser)) {
            return "操作用户不能为空";
        }
        if (StringUtils.isBlank(fsDir)) {
            return "程序包存储目录不能为空";
        }
        if ("/".equals(fsDir)) {
            return "不能使用根目录作为存储目录";
        }
        //处理存储路径
        if (!fsDir.startsWith("/")) {
            fsDir = "/" + fsDir;
        }
        if (fsDir.endsWith("/")) {
            fsDir = fsDir.substring(0, fsDir.length() - 1);
        }
        return null;
    }
}
