package com.meiyou.bigwhale.dao.auth;

import com.meiyou.bigwhale.entity.auth.Role;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author Suxy
 * @date 2019/9/25
 * @description file description
 */
public interface RoleDao extends PagingAndSortingRepository<Role, Integer> {

}
