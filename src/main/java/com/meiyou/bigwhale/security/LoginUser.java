package com.meiyou.bigwhale.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Suxy
 * @date 2019/10/24
 * @description file description
 */
public class LoginUser extends User {

    public static final PasswordEncoder PASSWORD_ENCODER = new StandardPasswordEncoder();

    private final Integer id;
    private final boolean root;
    private Map<String, List<String>> resources = new HashMap<>();

    public LoginUser(String username, String password, Collection<? extends GrantedAuthority> authorities, Integer id, boolean root) {
        super(username, password, authorities);
        this.id = id;
        this.root = root;
    }

    public LoginUser(String username, String password, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities, Integer id, boolean root) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.id = id;
        this.root = root;
    }

    public Integer getId() {
        return id;
    }

    public boolean isRoot() {
        return root;
    }

    public Map<String, List<String>> getResources() {
        return resources;
    }

    public void setResources(Map<String, List<String>> resources) {
        this.resources = resources;
    }

    public boolean check(String code) {
        if (root) {
            return true;
        }
        for (String s : resources.keySet()) {
            if (s.equals(code)) {
                return true;
            }
        }
        return false;
    }

}
