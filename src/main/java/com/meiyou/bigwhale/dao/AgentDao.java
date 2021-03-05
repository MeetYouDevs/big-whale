package com.meiyou.bigwhale.dao;


import com.meiyou.bigwhale.entity.Agent;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface AgentDao extends PagingAndSortingRepository<Agent, Integer> {

}
