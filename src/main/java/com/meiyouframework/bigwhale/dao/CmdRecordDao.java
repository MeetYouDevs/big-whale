package com.meiyouframework.bigwhale.dao;

import com.meiyouframework.bigwhale.entity.CmdRecord;
import org.springframework.data.repository.PagingAndSortingRepository;


public interface CmdRecordDao extends PagingAndSortingRepository<CmdRecord, String> {

}
