package com.meiyou.bigwhale.service;

import com.meiyou.bigwhale.entity.YarnApp;
import com.meiyou.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import org.springframework.stereotype.Service;

@Service
public class YarnAppServiceImpl extends AbstractMysqlPagingAndSortingQueryService<YarnApp, Integer> implements YarnAppService {

}
