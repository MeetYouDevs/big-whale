package com.meiyou.bigwhale.dao.auth;

import com.meiyou.bigwhale.entity.auth.UserRole;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author Suxy
 * @date 2019/10/24
 * @description file description
 */
public interface UserRoleDao extends PagingAndSortingRepository<UserRole, Integer> {

}
