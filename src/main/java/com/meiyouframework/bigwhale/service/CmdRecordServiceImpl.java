package com.meiyouframework.bigwhale.service;

import com.meiyouframework.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import com.meiyouframework.bigwhale.entity.CmdRecord;
import org.springframework.stereotype.Service;

@Service
public class CmdRecordServiceImpl extends AbstractMysqlPagingAndSortingQueryService<CmdRecord, String> implements CmdRecordService {

}
