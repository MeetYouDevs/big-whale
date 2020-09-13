package com.meiyouframework.bigwhale.service;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import com.meiyouframework.bigwhale.dto.DtoScript;
import com.meiyouframework.bigwhale.entity.Cluster;
import com.meiyouframework.bigwhale.entity.Script;
import com.meiyouframework.bigwhale.entity.auth.User;
import com.meiyouframework.bigwhale.service.auth.UserService;
import com.meiyouframework.bigwhale.util.WebHdfsUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;

import static com.meiyouframework.bigwhale.common.Constant.APP_APPEND_SYMBOL;

@Service
public class ScriptServiceImpl extends AbstractMysqlPagingAndSortingQueryService<Script, String> implements ScriptService {

    @Autowired
    private CmdRecordService cmdRecordService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private UserService userService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Script entity) {
        //删除jar包
        if (entity.getType() != Constant.SCRIPT_TYPE_SHELL_BATCH) {
            deleteJar(entity);
        }
        cmdRecordService.deleteByQuery("scriptId=" + entity.getId());
        super.delete(entity);
    }

    @Override
    public Page<Script> fuzzyPage(DtoScript req) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Script> criteriaQuery = builder.createQuery(clazz);
        Root<Script> root = criteriaQuery.from(clazz);
        CriteriaQuery<Long> countCriteriaQuery = builder.createQuery(Long.class);
        Root<Script> countRoot = countCriteriaQuery.from(clazz);
        countCriteriaQuery.select(builder.count(countRoot));
        this.predicate(builder, criteriaQuery, root, req);
        this.predicate(builder, countCriteriaQuery, countRoot, req);
        long totalCount = entityManager.createQuery(countCriteriaQuery).getSingleResult();
        int pageNo = req.pageNo - 1;
        int pageSize = req.pageSize;
        TypedQuery<Script> typedQuery = entityManager.createQuery(criteriaQuery);
        typedQuery.setFirstResult(pageNo * pageSize);
        typedQuery.setMaxResults(pageSize);
        List<Script> entities = typedQuery.getResultList();
        return new PageImpl<>(entities, new PageRequest(pageNo, pageSize), totalCount);
    }

    @Override
    public Map<String, Script> getAppMap(String clusterId) {
        Map<String, Script> result = new HashMap<>();
        List<Script> data = findByQuery("clusterId=" + clusterId);
        if (!CollectionUtils.isEmpty(data)) {
            for (Script script : data) {
                if (script.getType() == Constant.SCRIPT_TYPE_SHELL_BATCH) {
                    continue;
                }
                String user = script.getUser();
                String queue = script.getQueue();
                if (queue != null && !"root".equals(queue) && !queue.startsWith("root.")) {
                    queue = "root." + queue;
                }
                result.put(user + APP_APPEND_SYMBOL + queue + APP_APPEND_SYMBOL + script.getApp(), script);
            }
        }
        return result;
    }

    @Override
    public String extractJarPath(String content) {
        String [] tokens = content.split(" ");
        int jarIndex = -1;
        for (int i = 0; i < tokens.length; i ++) {
            String token = tokens[i];
            if (token.contains(".jar") || token.contains(".py")) {
                if (!"--jars".equals(tokens[i - 1]) && !"-j".equals(tokens[i - 1]) && !"--jar".equals(tokens[i - 1])) {
                    jarIndex = i;
                    break;
                }
            }
        }
        if (jarIndex != -1) {
            return tokens[jarIndex];
        }
        return null;
    }

    @Override
    public void deleteJar(Script entity) {
        String jarPath = extractJarPath(entity.getScript());
        if (jarPath != null) {
            //检查是否还被引用
            boolean used = false;
            for (Script item : findByQuery("id!=" + entity.getId() + ";uid=" + entity.getUid())) {
                if (jarPath.equals(extractJarPath(item.getScript()))) {
                    used = true;
                    break;
                }
            }
            if (!used) {
                for (Cluster cluster : clusterService.findAll()) {
                    if (jarPath.startsWith(cluster.getFsDefaultFs())) {
                        String fsDefaultFs = cluster.getFsDefaultFs();
                        if (fsDefaultFs.endsWith("/")) {
                            fsDefaultFs = fsDefaultFs.substring(0, fsDefaultFs.length() - 1);
                        }
                        jarPath = jarPath.substring(fsDefaultFs.length());
                        User user = userService.findById(entity.getUid());
                        WebHdfsUtils.delete(cluster.getFsWebhdfs(), jarPath, user.getUsername());
                        break;
                    }
                }
            }
        }
    }

    /**
     * 拼接查询表达式
     * @param builder
     * @param criteriaQuery
     * @param root
     * @param req
     */
    private void predicate(CriteriaBuilder builder, CriteriaQuery<?> criteriaQuery, Root<Script> root, DtoScript req) {
        List<Predicate> orPredicates = new ArrayList<>();
        if (StringUtils.isNotBlank(req.getText())) {
            Predicate predicate1 = builder.conjunction();
            predicate1.getExpressions().add(builder.like(root.get("name"), "%" + req.getText() + "%"));
            orPredicates.add(predicate1);
            Predicate predicate2 = builder.conjunction();
            predicate2.getExpressions().add(builder.like(root.get("description"), "%" + req.getText() + "%"));
            orPredicates.add(predicate2);
            Predicate predicate3 = builder.conjunction();
            predicate3.getExpressions().add(builder.like(root.get("script"), "%" + req.getText() + "%"));
            orPredicates.add(predicate3);
        }
        Predicate andPredicate = builder.conjunction();
        if (StringUtils.isNotBlank(req.getUid())) {
            andPredicate.getExpressions().add(builder.equal(root.get("uid"), req.getUid()));
        }
        if (req.getType() != null) {
            andPredicate.getExpressions().add(builder.equal(root.get("type"), req.getType()));
        }
        if (StringUtils.isNotBlank(req.getAgentId())) {
            andPredicate.getExpressions().add(builder.equal(root.get("agentId"), req.getAgentId()));
        }
        if (StringUtils.isNotBlank(req.getClusterId())) {
            andPredicate.getExpressions().add(builder.equal(root.get("clusterId"), req.getClusterId()));
        }
        if (StringUtils.isNotBlank(req.getId())) {
            andPredicate.getExpressions().add(builder.equal(root.get("id"), req.getId()));
        }
        if (!orPredicates.isEmpty()) {
            Predicate orPredicate = builder.or(orPredicates.toArray(new Predicate[0]));
            criteriaQuery.where(andPredicate, orPredicate);
        } else {
            criteriaQuery.where(andPredicate);
        }
    }

}
