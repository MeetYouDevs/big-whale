package com.meiyou.bigwhale.dao;

import com.meiyou.bigwhale.entity.ScriptHistory;
import org.springframework.data.repository.PagingAndSortingRepository;


public interface ScriptHistoryDao extends PagingAndSortingRepository<ScriptHistory, Integer> {

}
