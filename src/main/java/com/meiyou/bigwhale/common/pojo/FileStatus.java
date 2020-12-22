package com.meiyou.bigwhale.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Suxy
 * @date 2020/3/12
 * @description file description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileStatus implements Serializable {

    private String id;
    private String name;
    private String path;
    private long length;
    private boolean isdir;
    private Date modification_time;
    private String permission;
    private String owner;
    private String group;

}
