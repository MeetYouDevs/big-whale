package com.meiyouframework.bigwhale.service;

import com.meiyouframework.bigwhale.data.service.PagingAndSortingQueryService;
import com.meiyouframework.bigwhale.dto.DtoScript;
import com.meiyouframework.bigwhale.entity.Script;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface ScriptService extends PagingAndSortingQueryService<Script, String> {

    Page<Script> fuzzyPage(DtoScript req);

    Map<String, Script> getAppMap(String clusterId);

    String extractJarPath(String content);

    void deleteJar(Script entity);
}
