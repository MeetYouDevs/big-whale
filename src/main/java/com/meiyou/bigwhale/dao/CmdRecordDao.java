package com.meiyou.bigwhale.dao;

import com.meiyou.bigwhale.entity.CmdRecord;
import org.springframework.data.repository.PagingAndSortingRepository;


public interface CmdRecordDao extends PagingAndSortingRepository<CmdRecord, String> {

}
