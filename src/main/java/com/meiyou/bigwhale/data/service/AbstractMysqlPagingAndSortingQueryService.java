package com.meiyou.bigwhale.data.service;

import com.meiyou.bigwhale.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Suxy
 * @date 2019/10/16
 * @description file description
 */
public abstract class AbstractMysqlPagingAndSortingQueryService<T, ID extends Serializable> extends AbstractPagingAndSortingService<T, ID> implements PagingAndSortingQueryService<T, ID> {

    @PersistenceContext
    protected EntityManager entityManager;

    @Override
    public T findOneByQuery(String filters) {
        T t;
        try {
            t = entityManager.createQuery(buildQuery(filters)).setMaxResults(1).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
        return t;
    }

    @Override
    public T findOneByQuery(String filters, Sort sort) {
        T t;
        try {
            t = entityManager.createQuery(buildQuery(filters, sort)).setMaxResults(1).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
        return t;
    }

    @Override
    public List<T> findByQuery(String filters) {
        return entityManager.createQuery(buildQuery(filters)).getResultList();
    }

    @Override
    public List<T> findByQuery(String filters, Sort sort) {
        return entityManager.createQuery(buildQuery(filters, sort)).getResultList();
    }

    @Override
    public Page<T> pageByQuery(Pageable pageable) {
        long totalCount = entityManager.createQuery(buildCount(pageable.getFilters())).getSingleResult();
        int pageNo = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        TypedQuery<T> typedQuery = entityManager.createQuery(buildQuery(pageable.getFilters(), pageable.getSort()));
        typedQuery.setFirstResult(pageNo * pageSize);
        typedQuery.setMaxResults(pageSize);
        List<T> entities = typedQuery.getResultList();
        return new PageImpl<>(entities, pageable, totalCount);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteByQuery(String filters) {
        entityManager.createQuery(buildDelete(filters)).executeUpdate();
    }

    private CriteriaQuery<T> buildQuery(String filters) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = builder.createQuery(clazz);
        Root<T> root = criteriaQuery.from(clazz);
        criteriaQuery.where(predicate(builder, root, filters));
        return criteriaQuery;
    }

    private CriteriaQuery<T> buildQuery(String filters, Sort sort) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = builder.createQuery(clazz);
        Root<T> root = criteriaQuery.from(clazz);
        criteriaQuery.where(predicate(builder, root, filters));
        criteriaQuery.orderBy(orders(builder, root, sort));
        return criteriaQuery;
    }

    private CriteriaQuery<Long> buildCount(String filters) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
        Root<T> root = criteriaQuery.from(clazz);
        criteriaQuery.select(builder.count(root));
        criteriaQuery.where(predicate(builder, root, filters));
        return criteriaQuery;
    }

    private CriteriaDelete<T> buildDelete(String filters) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaDelete<T> criteriaDelete = builder.createCriteriaDelete(clazz);
        Root<T> root = criteriaDelete.from(clazz);
        criteriaDelete.where(predicate(builder, root, filters));
        return criteriaDelete;
    }

    private Predicate predicate(CriteriaBuilder builder, Root<T> root, String filters) {
        Predicate predicate = builder.conjunction();
        if (!StringUtils.isEmpty(filters)) {
            String[] expressions = filters.split(";");
            for (String expression : expressions) {
                if (expression.contains(">=")) {
                    String[] fieldValue = expression.split(">=");
                    String field = fieldValue[0];
                    String value = fieldValue[1];
                    Object suitValue = suitValue(field, value);
                    if (suitValue instanceof Number) {
                        predicate.getExpressions().add(builder.ge(root.get(field), (Number) suitValue));
                    } else {
                        predicate.getExpressions().add(builder.greaterThanOrEqualTo(root.get(field), (Date) suitValue));
                    }
                } else if (expression.contains("<=")) {
                    String[] fieldValue = expression.split("<=");
                    String field = fieldValue[0];
                    String value = fieldValue[1];
                    Object suitValue = suitValue(field, value);
                    if (suitValue instanceof Number) {
                        predicate.getExpressions().add(builder.le(root.get(field), (Number) suitValue));
                    } else {
                        predicate.getExpressions().add(builder.lessThanOrEqualTo(root.get(field), (Date) suitValue));
                    }
                } else if (expression.contains(">")) {
                    String[] fieldValue = expression.split(">");
                    String field = fieldValue[0];
                    String value = fieldValue[1];
                    Object suitValue = suitValue(field, value);
                    if (suitValue instanceof Number) {
                        predicate.getExpressions().add(builder.gt(root.get(field), (Number) suitValue));
                    } else {
                        predicate.getExpressions().add(builder.greaterThan(root.get(field), (Date) suitValue));
                    }
                } else if (expression.contains("<")) {
                    String[] fieldValue = expression.split("<");
                    String field = fieldValue[0];
                    String value = fieldValue[1];
                    Object suitValue = suitValue(field, value);
                    if (suitValue instanceof Number) {
                        predicate.getExpressions().add(builder.lt(root.get(field), (Number) suitValue));
                    } else {
                        predicate.getExpressions().add(builder.lessThan(root.get(field), (Date) suitValue));
                    }
                } else if (expression.contains("!=")) {
                    String[] fieldValue = expression.split("!=");
                    String field = fieldValue[0];
                    String value = fieldValue[1];
                    if (!value.contains(",")) {
                        predicate.getExpressions().add(builder.notEqual(root.get(field), suitValue(field, value)));
                    } else {
                        String [] values = value.split(",");
                        for (String s : values) {
                            predicate.getExpressions().add(builder.notEqual(root.get(field), suitValue(field, s)));
                        }
                    }
                } else if (expression.contains("=")) {
                    String[] fieldValue = expression.split("=");
                    String field = fieldValue[0];
                    String value = fieldValue[1];
                    if (!value.contains(",")) {
                        predicate.getExpressions().add(builder.equal(root.get(field), suitValue(field, value)));
                    } else {
                        String [] values = value.split(",");
                        Object [] suitValues = new Object[values.length];
                        for (int i = 0; i < values.length; i ++) {
                            suitValues[i] = suitValue(field, values[i]);
                        }
                        predicate.getExpressions().add(root.get(field).in(suitValues));
                    }
                } else if (expression.contains("?")) {
                    String[] fieldValue = expression.split("\\?");
                    predicate.getExpressions().add(builder.like(root.get(fieldValue[0]), "%" + fieldValue[1] + "%"));
                } else if (expression.contains("+")) {
                    String[] fieldValue = expression.split("\\+");
                    predicate.getExpressions().add(builder.isNotNull(root.get(fieldValue[0])));
                } else if (expression.contains("-")) {
                    String[] fieldValue = expression.split("-");
                    predicate.getExpressions().add(builder.isNull(root.get(fieldValue[0])));
                }
            }
        }
        return predicate;
    }

    private List<Order> orders(CriteriaBuilder builder, Root<T> root, Sort sort) {
        List<Order> orderList = new ArrayList<>();
        if (sort != null) {
            sort.forEach(item -> {
                if (item.getDirection().isAscending()) {
                    orderList.add(builder.asc(root.get(item.getProperty())));
                } else {
                    orderList.add(builder.desc(root.get(item.getProperty())));
                }
            });
        }
        return orderList;
    }

}
