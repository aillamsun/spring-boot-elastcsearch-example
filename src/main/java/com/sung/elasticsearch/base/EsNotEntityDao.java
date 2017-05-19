package com.sung.elasticsearch.base;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sungang on 2017/4/17.
 */
@Component
public class EsNotEntityDao {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private Client esClient;


    /**
     * 根据id查询
     *
     * @param id
     * @return
     */
    public Object queryById(String id) {
        StringQuery stringQuery = new StringQuery("id=" + id);
        Object object = elasticsearchTemplate.queryForObject(stringQuery, Object.class);
        return object;

    }

    /**
     * 库和类型
     *
     * @param indexs
     * @param types
     * @return
     */
    public long count(String indexs[], String types[]) {
        SearchQuery searchQuery = null;
        searchQuery = new NativeSearchQueryBuilder().withTypes(types)
                .withQuery(QueryBuilders.matchAllQuery()).build();
        long num = elasticsearchTemplate.count(searchQuery);
        return num;
    }


    /**
     * 清空types
     *
     * @param index
     * @param types
     * @return
     */
    public boolean clearIndexTypes(String index, String types[]) {
        CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
        criteriaQuery.addIndices(new String[]{index});
        criteriaQuery.addTypes(types);
        elasticsearchTemplate.delete(criteriaQuery, Object.class);
        ;
        return true;
    }

    /**
     * 清空 index
     *
     * @param index
     * @return
     */
    public boolean clearIndex(String index) {
        DeleteQuery dq = new DeleteQuery();
        dq.setIndex(index);
        elasticsearchTemplate.delete(dq);
        ;
        return true;
    }


    /**
     * 查询返回一个Map对象
     *
     * @param esIndexName
     * @param type
     * @param fields
     * @param content
     * @param sortField
     * @param order
     * @param from
     * @param size
     * @return
     */

    public List<Map<String, Object>> queryForObject(String esIndexName, String type, String[] fields, String content, String sortField, SortOrder order, List<String> heightFields, int from, int size) {
        SearchRequestBuilder reqBuilder = esClient.prepareSearch(esIndexName)
                .setTypes(type).setSearchType(SearchType.DEFAULT)
                .setExplain(true);
        QueryStringQueryBuilder queryString = QueryBuilders.queryStringQuery("\"" + content + "\"");
        for (String k : fields) {
            queryString.field(k);
        }
        queryString.minimumShouldMatch("10");
        reqBuilder.setExplain(true);

        reqBuilder.setQuery(QueryBuilders.boolQuery().should(queryString))
                .setExplain(true);
        if (StringUtils.isNotEmpty(sortField) && order != null) {
            reqBuilder.addSort(sortField, order);
        }
        if (from >= 0 && size > 0) {
            reqBuilder.setFrom(from).setSize(size);
        }
        //设置高亮显示
        if (heightFields != null) {
            for (String hs : heightFields)
                reqBuilder.addHighlightedField(hs);
        }
        reqBuilder.setHighlighterPreTags("<span style=\"color:red\">");
        reqBuilder.setHighlighterPostTags("</span>");
        SearchResponse resp = reqBuilder.execute().actionGet();
        SearchHit[] hits = resp.getHits().getHits();

        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        for (SearchHit hit : hits) {
            Map<String, Object> map = new HashMap<String, Object>();
            for (String key : hit.getSource().keySet()) {
                if (heightFields != null && heightFields.contains(key)) {
                    map.put(key, hit.getHighlightFields().get(key).fragments()[0]);
                } else
                    map.put(key, hit.getSource().get(key));

            }
            results.add(map);
        }
        return results;
    }

    public ElasticsearchTemplate getElasticsearchTemplate() {
        return elasticsearchTemplate;
    }


    public Client getEsClient() {
        return esClient;
    }
}
