package com.meiyouframework.bigwhale.entity.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * @author Suxy
 * @date 2019/9/25
 * @description file description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auth_user")
public class User {

    @Id
    @GenericGenerator(name = "idGenerator", strategy = "uuid")
    @GeneratedValue(generator = "idGenerator")
    private String id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String password;
    private Boolean enabled;
    private Boolean root;
    private Date createTime;
    private Date updateTime;
    @Transient
    private List<String> roles;

}
