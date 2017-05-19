package com.sung.elasticsearch.base;


import com.sung.elasticsearch.utils.PropertyHelper;
import com.sung.elasticsearch.utils.ReflectUtils;
import com.sung.elasticsearch.utils.es.BaseEsModel;
import com.sung.elasticsearch.utils.es.BasePage;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.highlight.HighlightBuilder.Field;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by sungang on 2017/4/17.
 */
@Component
public abstract class EsBaseDaoImpl<T> implements EsBaseDao<T> {

    private Logger logger = LoggerFactory.getLogger(EsBaseDaoImpl.class);

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private Client esClient;
    /**
     * 实体类型
     */
    protected final Class<T> entityClass;

    /**
     * 得到类型
     *
     * @return
     */
    public EsBaseDaoImpl() {
        this.entityClass = ReflectUtils.findParameterizedType(getClass(), 0);
    }

    ;

    /**
     * 添加各自类的影射
     */
    public abstract void putClassMapping();


    /**
     * 插入或等新，需要有id，id 为null 需要自己生成
     *
     * @param ts
     * @return
     */
    @Override
    public boolean insertOrUpdate(List<T> ts) {
        List<IndexQuery> queries = new ArrayList<IndexQuery>();
        for (T t : ts) {
            Long id = ((BaseEsModel) t).getId();
//            if (id == null) {
//                id = UuidHelper.getRandomUUID();
//                ((EsBaseBean) t).setId(id);
//            }
            IndexQuery indexQuery = new IndexQueryBuilder().withId(String.valueOf(id)).withObject(t).build();
            queries.add(indexQuery);
        }
        elasticsearchTemplate.bulkIndex(queries);
        return true;
    }


    /**
     * 插入或更新
     *
     * @param t
     * @return
     */
    @Override
    public boolean insertOrUpdate(T t) {
        Long id = ((BaseEsModel) t).getId();
        try {
            IndexQuery indexQuery = new IndexQueryBuilder().withId(String.valueOf(id)).withObject(t).build();
            elasticsearchTemplate.index(indexQuery);
            return true;
        } catch (Exception e) {
            logger.error("insert or update user info error.", e);
            return false;
        }
    }

    @Override
    public boolean deleteById(String id) {
        try {
            elasticsearchTemplate.delete(entityClass, id);
            return true;
        } catch (Exception e) {
            logger.error("delete " + entityClass + " by id " + id
                    + " error.", e);
            return false;
        }
    }

    @Override
    public boolean deleteByIds(List<String> idList) {
        try {
            CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
            criteriaQuery.setIds(idList);
            elasticsearchTemplate.delete(criteriaQuery, entityClass);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * 根据条件查询
     *
     * @param filedContentMap 不能为null
     * @return
     */
    @Override
    public boolean deleteByQuery(Map<String, Object> filedContentMap) {
        try {
            DeleteQuery dq = new DeleteQuery();

            BoolQueryBuilder qb = QueryBuilders.boolQuery();
            if (filedContentMap != null)
                for (String key : filedContentMap.keySet()) {//字段查询
                    qb.must(QueryBuilders.matchQuery(key, filedContentMap.get(key)));
                }
            dq.setQuery(qb);
            elasticsearchTemplate.delete(dq, entityClass);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检查健康状态
     *
     * @return
     */
    @Override
    public boolean ping() {
        try {
            ActionFuture<ClusterHealthResponse> health = esClient.admin().cluster().health(new ClusterHealthRequest());
            ClusterHealthStatus status = health.actionGet().getStatus();
            if (status.value() == ClusterHealthStatus.RED.value()) {
                throw new RuntimeException(
                        "elasticsearch cluster health status is red.");
            }
            return true;
        } catch (Exception e) {
            logger.error("ping elasticsearch error.", e);
            return false;
        }

    }

    /**
     * 条件查询
     *
     * @param filedContentMap 字段和查询内容
     * @param heightFields
     * @param sortField
     * @param order
     * @param basePage
     * @return
     */
    @Override
    public BasePage<T> queryPage(Map<String, Object> filedContentMap, final List<String> heightFields, String sortField, SortOrder order, BasePage<T> basePage) {
        Field[] hfields = new Field[0];
        if (heightFields != null) {
            hfields = new Field[heightFields.size()];
            for (int i = 0; i < heightFields.size(); i++) {
                hfields[i] = new Field(heightFields.get(i)).preTags("<em style='color:red'>").postTags("</em>").fragmentSize(250);
            }
        }
        NativeSearchQueryBuilder nsb = new NativeSearchQueryBuilder().withHighlightFields(hfields);//高亮字段
        if (sortField != null && order != null)//排序
            nsb.withSort(new FieldSortBuilder(sortField).ignoreUnmapped(true).order(order));
        if (basePage != null)//分页
            nsb.withPageable(new PageRequest(basePage.getPageNo() - 1, basePage.getPageSize()));
        BoolQueryBuilder qb = QueryBuilders.boolQuery();
        for (String key : filedContentMap.keySet()) {//字段查询
            qb.must(QueryBuilders.matchQuery(key, filedContentMap.get(key)));

        }
        nsb.withQuery(qb);
        SearchQuery searchQuery = nsb.build();//查询建立


        Page<T> page = null;
        if (heightFields != null && heightFields.size() > 0) {//如果设置高亮
            page = elasticsearchTemplate.queryForPage(searchQuery, entityClass, new SearchResultMapper() {
                @SuppressWarnings("unchecked")
                @Override
                public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
                    List<T> chunk = new ArrayList<T>();
                    for (SearchHit searchHit : response.getHits()) {
                        if (response.getHits().getHits().length <= 0) {
                            return null;
                        }

                        Map<String, Object> entityMap = searchHit.getSource();
                        for (String highName : heightFields) {
                            Text text[] = searchHit.getHighlightFields().get(highName).fragments();
                            if (text.length > 0) {
                                String highValue = searchHit.getHighlightFields().get(highName).fragments()[0].toString();
                                entityMap.put(highName, highValue);
                            }
                        }
                        chunk.add((T) PropertyHelper.getFansheObj(entityClass, entityMap));
                    }
                    if (chunk.size() > 0) {
                        return new AggregatedPageImpl<T>((List<T>) chunk);
                    }
                    return new AggregatedPageImpl<T>(new ArrayList<T>());
                }

            });
        } else {//如果不设置高亮
            logger.info("#################" + qb.toString());
            page = elasticsearchTemplate.queryForPage(searchQuery, entityClass);
        }


        //	List<T> ts = page.getContent();

        basePage.setTotalRecord(page.getTotalElements());
        basePage.setResults(page.getContent());
        return basePage;
    }

    /**
     * @param filedContentMap
     * @param heightFields
     * @param sortField
     * @param order
     * @return
     */
    @Override
    public List<T> queryList(Map<String, Object> filedContentMap, final List<String> heightFields, String sortField, SortOrder order) {
        Field[] hfields = new Field[0];
        if (heightFields != null) {
            hfields = new Field[heightFields.size()];
            for (int i = 0; i < heightFields.size(); i++) {
                //String o="{\"abc\" : \"[abc]\"}";
                hfields[i] = new Field(heightFields.get(i)).preTags("<em>").postTags("</em>").fragmentSize(250);
            }
        }
        NativeSearchQueryBuilder nsb = new NativeSearchQueryBuilder().withHighlightFields(hfields);//高亮字段
        if (sortField != null && order != null)//排序
            nsb.withSort(new FieldSortBuilder(sortField).ignoreUnmapped(true).order(order));
        BoolQueryBuilder qb = QueryBuilders.boolQuery();
        for (String key : filedContentMap.keySet()) {//字段查询
            qb.must(QueryBuilders.matchQuery(key, filedContentMap.get(key)));

        }
        nsb.withQuery(qb);
        SearchQuery searchQuery = nsb.build();//查询建立
        Page<T> page = null;
        if (heightFields != null && heightFields.size() > 0) {//如果设置高亮
            page = elasticsearchTemplate.queryForPage(searchQuery, entityClass, new SearchResultMapper() {
                @Override
                public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
                    List<T> chunk = new ArrayList<T>();
                    for (SearchHit searchHit : response.getHits()) {
                        if (response.getHits().getHits().length <= 0) {
                            return null;
                        }

                        Map<String, Object> entityMap = searchHit.getSource();
                        for (String highName : heightFields) {
                            String highValue = searchHit.getHighlightFields().get(highName).fragments()[0].toString();
                            entityMap.put(highName, highValue);
                        }
                        chunk.add((T) PropertyHelper.getFansheObj(entityClass, entityMap));
                    }
                    if (chunk.size() > 0) {
                        return new AggregatedPageImpl<T>((List<T>) chunk);
                    }
                    return null;
                }

            });
        } else//如果不设置高亮
            page = elasticsearchTemplate.queryForPage(searchQuery, entityClass);

        return page.getContent();
    }

    @Override
    public T queryById(String id) {
        StringQuery stringQuery = new StringQuery("id=" + id);
        T t = elasticsearchTemplate.queryForObject(stringQuery, entityClass);
        return t;
    }

    public ElasticsearchTemplate getElasticsearchTemplate() {
        return elasticsearchTemplate;
    }

    public void setElasticsearchTemplate(ElasticsearchTemplate elasticsearchTemplate) {
        this.elasticsearchTemplate = elasticsearchTemplate;
    }

    public Client getEsClient() {
        return esClient;
    }

    public void setEsClient(Client esClient) {
        this.esClient = esClient;
    }
}
