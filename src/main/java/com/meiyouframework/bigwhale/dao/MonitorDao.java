package com.meiyouframework.bigwhale.dao;

import com.meiyouframework.bigwhale.entity.Monitor;
import org.springframework.data.repository.PagingAndSortingRepository;


public interface MonitorDao extends PagingAndSortingRepository<Monitor, String> {

}
