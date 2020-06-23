package com.meiyouframework.bigwhale.dao;


import com.meiyouframework.bigwhale.entity.Agent;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface AgentDao extends PagingAndSortingRepository<Agent, String> {

}
