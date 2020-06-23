package com.meiyouframework.bigwhale.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Suxy
 * @date 2019/10/24
 * @description file description
 */
public class LoginUser extends User {

    private final String id;
    private final boolean root;
    private Map<String, String> resources = new HashMap<>();

    public LoginUser(String id, boolean root, String username, String password, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.id = id;
        this.root = root;
    }

    public LoginUser(String id,  boolean root, String username, String password, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.id = id;
        this.root = root;
    }

    public String getId() {
        return id;
    }

    public boolean isRoot() {
        return root;
    }

    public Map<String, String> getResources() {
        return resources;
    }

    public void setResources(Map<String, String> resources) {
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
