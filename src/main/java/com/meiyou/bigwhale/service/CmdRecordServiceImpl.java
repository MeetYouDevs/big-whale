package com.meiyou.bigwhale.service;

import com.meiyou.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import com.meiyou.bigwhale.entity.CmdRecord;
import org.springframework.stereotype.Service;

@Service
public class CmdRecordServiceImpl extends AbstractMysqlPagingAndSortingQueryService<CmdRecord, String> implements CmdRecordService {

}
