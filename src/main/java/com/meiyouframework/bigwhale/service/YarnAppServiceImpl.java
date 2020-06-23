package com.meiyouframework.bigwhale.service;

import com.meiyouframework.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import com.meiyouframework.bigwhale.entity.YarnApp;
import org.springframework.stereotype.Service;


@Service
public class YarnAppServiceImpl extends AbstractMysqlPagingAndSortingQueryService<YarnApp, String> implements YarnAppService {

}
