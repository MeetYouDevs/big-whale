package com.meiyouframework.bigwhale.security;

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.util.AntPathMatcher;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author Suxy
 * @date 2019/10/22
 * @description file description
 */
@EnableWebSecurity
public class WebSecurityConfigurerAdaptor extends WebSecurityConfigurerAdapter {

    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    private final String[] authPath = new String[]{"/auth/**", "/admin/**", "/api/**"};

    @Resource
    private JdbcTemplate jdbcTemplate;


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();
        http.headers().frameOptions().sameOrigin();
        //访问控制
        http.authorizeRequests().antMatchers(authPath).access("@webSecurityConfigurerAdaptor.hasPermission(request, authentication)");
        http.authorizeRequests().antMatchers("/**").authenticated();
        //登录
        http.formLogin().loginPage("/login.html").permitAll()
                .successHandler(authenticationSuccessHandler())
                .failureHandler(authenticationFailureHandler());
        //授权
        http.exceptionHandling().accessDeniedHandler(accessDeniedHandler());
        //退出
        http.logout().logoutUrl("/logout.html").permitAll().invalidateHttpSession(true);
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers(
                "/*.ico",
                "/libs/**",
                "/css/**",
                "/js/**",
                "/img/**",
                "/openapi/**"
        );
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        LoginUserDetailsService userDetailsService = new LoginUserDetailsService();
        userDetailsService.setJdbcTemplate(jdbcTemplate);
        userDetailsService.setUsersByUsernameQuery("select username,password,enabled,id,root from auth_user where username = ?");
        userDetailsService.setAuthoritiesByUsernameQuery("select username,role from auth_user_role where username = ?");
        auth.userDetailsService(userDetailsService).passwordEncoder(new StandardPasswordEncoder());
    }

    public boolean hasPermission(HttpServletRequest request, Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUser) {
            if (((LoginUser) principal).isRoot()) {
                return true;
            }
            for (String url : ((LoginUser) principal).getResources().values()) {
                if (url.contains(",")) {
                    for (String part : url.split(",")) {
                        if (antPathMatcher.match(part, request.getRequestURI())) {
                            return true;
                        }
                    }
                } else {
                    if (antPathMatcher.match(url, request.getRequestURI())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private AuthenticationSuccessHandler authenticationSuccessHandler() {
        final String defRoleResourceByRoleCode = "select rr.resource, re.url from auth_role_resource rr, auth_resource re where rr.resource = re.code and rr.role in (%s)";
        return (httpServletRequest, httpServletResponse, authentication) -> {
            LoginUser principal = (LoginUser) authentication.getPrincipal();
            //非超级管理员则加载资源
            if (!principal.isRoot()) {
                //加载用户资源
                StringBuilder roles = new StringBuilder();
                principal.getAuthorities().forEach(grantedAuthority -> roles.append("\'").append(grantedAuthority.getAuthority()).append("\'").append(","));
                String sql = String.format(defRoleResourceByRoleCode, roles.substring(0, roles.length() - 1));
                List<Map<String, Object>> roleResourcesList = jdbcTemplate.queryForList(sql);
                Map<String, String> roleResourcesMap = new HashMap<>();
                String contextPath = httpServletRequest.getContextPath();
                roleResourcesList.forEach(item -> {
                    String resource = item.get("resource") != null ? (String) item.get("resource") : "";
                    String url = "";
                    if (item.get("url") != null) {
                        url = StringUtils.join(Stream.of(item.get("url").toString().split(",")).map(u -> contextPath + u).toArray(), ",");
                    }
                    roleResourcesMap.put(resource, url);
                });
                principal.setResources(roleResourcesMap);
            }
            httpServletRequest.getSession().setAttribute("user", principal);
            httpServletResponse.setContentType("application/json;charset=UTF-8");
            httpServletResponse.getWriter().write("{\"code\": 0, \"msg\": \"" + principal.getUsername() + "\"}");
        };
    }

    private AuthenticationFailureHandler authenticationFailureHandler() {
        return (httpServletRequest, httpServletResponse, e) -> {
            httpServletResponse.setContentType("application/json;charset=UTF-8");
            if (e instanceof BadCredentialsException) {
                httpServletResponse.getWriter().write("{\"code\": -1, \"msg\": \"账号或密码错误\"}");
            } else {
                httpServletResponse.getWriter().write("{\"code\": -1, \"msg\": \"账号状态异常，请联系管理员\"}");
            }
        };
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (httpServletRequest, httpServletResponse, e) -> {
            httpServletResponse.setContentType("application/json;charset=UTF-8");
            httpServletResponse.getWriter().write("{\"code\": -1, \"msg\": \"无权访问\"}");
        };
    }

    class LoginUserDetailsService extends JdbcDaoImpl {

        @Override
        protected List<UserDetails> loadUsersByUsername(String username) {
            return this.getJdbcTemplate().query(super.getUsersByUsernameQuery(), new String[]{username}, (rs, rowNum) -> {
                String username1 = rs.getString(1);
                String password = rs.getString(2);
                boolean enabled = rs.getBoolean(3);
                String id = rs.getString(4);
                boolean root = rs.getBoolean(5);
                return new LoginUser(id, root, username1, password, enabled, true, true, true, AuthorityUtils.NO_AUTHORITIES);
            });
        }

        @Override
        protected List<GrantedAuthority> loadUserAuthorities(String username) {
            List<GrantedAuthority> authorities = super.loadUserAuthorities(username);
            if (authorities.isEmpty()) {
                return Collections.singletonList(new SimpleGrantedAuthority("no_auth"));
            }
            return authorities;
        }

        @Override
        protected UserDetails createUserDetails(String username, UserDetails userFromUserQuery, List<GrantedAuthority> combinedAuthorities) {
            String returnUsername = userFromUserQuery.getUsername();
            if (!isUsernameBasedPrimaryKey()) {
                returnUsername = username;
            }
            String id = ((LoginUser) userFromUserQuery).getId();
            boolean root = ((LoginUser) userFromUserQuery).isRoot();
            return new LoginUser(id, root, returnUsername, userFromUserQuery.getPassword(), userFromUserQuery.isEnabled(), true, true, true, combinedAuthorities);
        }
    }

}
