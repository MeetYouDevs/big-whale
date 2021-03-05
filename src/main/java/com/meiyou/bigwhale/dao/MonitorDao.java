package com.meiyou.bigwhale.dao;

import com.meiyou.bigwhale.entity.Monitor;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author Suxy
 * @date 2021/2/6
 * @description file description
 */
public interface MonitorDao extends PagingAndSortingRepository<Monitor, Integer> {
}
