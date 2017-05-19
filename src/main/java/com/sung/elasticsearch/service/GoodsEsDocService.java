package com.sung.elasticsearch.service;

import com.sung.elasticsearch.model.GoodsModel;
import com.sung.elasticsearch.repository.GoodsESDocRepository;
import com.sung.elasticsearch.utils.PropertyHelper;
import com.sung.elasticsearch.utils.es.BasePage;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by sungang on 2017/5/19.
 */
@Service
public class GoodsEsDocService {

    private Logger logger = LoggerFactory.getLogger(GoodsEsDocService.class);

    @Autowired
    private GoodsESDocRepository goodsESDocRepository;
    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;


    public boolean insert(GoodsModel goodsESDoc) {
        goodsESDocRepository.save(goodsESDoc);

        return true;
    }

    /**
     * @param goodsESDoc
     * @return
     */
    public boolean insertOrUpdate(GoodsModel goodsESDoc) {
        List<IndexQuery> queries = new ArrayList<IndexQuery>();
        Long id = goodsESDoc.getId();
        IndexQuery indexQuery = new IndexQueryBuilder().withId(String.valueOf(id)).withObject(goodsESDoc).build();
        queries.add(indexQuery);
        elasticsearchTemplate.bulkIndex(queries);
        elasticsearchTemplate.refresh(GoodsModel.class);
        return true;
    }

    /**
     * 根据条件查询
     *
     * @param filedContentMap 不能为null
     * @return
     */
    public boolean deleteByQuery(Map<String, Object> filedContentMap) {
        try {
            DeleteQuery dq = new DeleteQuery();

            BoolQueryBuilder qb = QueryBuilders.boolQuery();
            if (filedContentMap != null)
                for (String key : filedContentMap.keySet()) {//字段查询
                    qb.must(QueryBuilders.matchQuery(key, filedContentMap.get(key)));
                }
            dq.setQuery(qb);
            ;
            elasticsearchTemplate.delete(dq, GoodsModel.class);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public void deleteById(Long id) {
        goodsESDocRepository.delete(id);
    }

    /**
     * 条件查询
     *
     * @param filedContentMap 条件
     * @param heightFields    高亮字段 不需要设置为null
     * @param sortFields      排序字段
     * @param order           排序类型
     * @param basePage        分页
     * @return
     */
    public BasePage<GoodsModel> queryPage(Map<String, Object> filedContentMap, final List<String> heightFields, String[] sortFields, String order, BasePage<GoodsModel> basePage, String[] resultFields) {

        HighlightBuilder.Field[] hfields = new HighlightBuilder.Field[0];
        if (heightFields != null) {
            hfields = new HighlightBuilder.Field[heightFields.size()];
            for (int i = 0; i < heightFields.size(); i++) {
                hfields[i] = new HighlightBuilder.Field(heightFields.get(i)).preTags("<em style='color:red'>").postTags("</em>").fragmentSize(250);
            }
        }
        NativeSearchQueryBuilder nsb = new NativeSearchQueryBuilder().withHighlightFields(hfields);//高亮字段

        if (basePage != null) {//分页
            nsb.withPageable(new PageRequest(basePage.getPageNo() - 1, basePage.getPageSize()));
        }

        BoolQueryBuilder qb = QueryBuilders.boolQuery();
        for (String key : filedContentMap.keySet()) {//字段查询
            // and
            qb.must(QueryBuilders.matchQuery(key, filedContentMap.get(key)));
            // or
            if ("description".equals(key)) {
                qb.should(QueryBuilders.matchQuery(key, filedContentMap.get(key)));
            }

        }
        //模糊查询
//        qb.must(  QueryBuilders.fuzzyQuery("",1).boost(0.5F));


        nsb.withQuery(qb);
        SearchQuery searchQuery = nsb.build();//查询建立
        searchQuery.addFields(resultFields);


        if (sortFields != null && sortFields.length > 0 && order != null) {//排序
//            nsb.withSort(new FieldSortBuilder(sortField).ignoreUnmapped(true).order(order));
//            searchQuery.addSort()
            if ("desc".equals(order)) {//降序
                searchQuery.addSort(new Sort(Sort.Direction.DESC, sortFields));
            }
            if ("asc".equals(order)) {//升序
                searchQuery.addSort(new Sort(Sort.Direction.ASC, sortFields));
            }
        }


        Page<GoodsModel> page = null;
        //如果设置高亮
        if (heightFields != null && heightFields.size() > 0) {
            page = elasticsearchTemplate.queryForPage(searchQuery, GoodsModel.class, new SearchResultMapper() {
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
                        chunk.add((T) PropertyHelper.getFansheObj(GoodsModel.class, entityMap));
                    }
                    if (chunk.size() > 0) {
                        return new AggregatedPageImpl<T>((List<T>) chunk);
                    }
                    return new AggregatedPageImpl<T>(new ArrayList<T>());
                }

            });
        } else {//如果不设置高亮
            logger.info("#################" + qb.toString());
            page = elasticsearchTemplate.queryForPage(searchQuery, GoodsModel.class);
        }
        basePage.setTotalRecord(page.getTotalElements());
        basePage.setResults(page.getContent());
        return basePage;
    }


    /**
     * @param filedContentMap
     * @param heightFields
     * @param sortFields
     * @param order
     * @return
     */
    public List<GoodsModel> queryList(Map<String, Object> filedContentMap, final List<String> heightFields, String[] sortFields, String order, String[] resultFields) {
        HighlightBuilder.Field[] hfields = new HighlightBuilder.Field[0];
        if (heightFields != null) {
            hfields = new HighlightBuilder.Field[heightFields.size()];
            for (int i = 0; i < heightFields.size(); i++) {
                //String o="{\"abc\" : \"[abc]\"}";
                hfields[i] = new HighlightBuilder.Field(heightFields.get(i)).preTags("<em>").postTags("</em>").fragmentSize(250);
            }
        }
        NativeSearchQueryBuilder nsb = new NativeSearchQueryBuilder().withHighlightFields(hfields);//高亮字段

//        if (sortField != null && order != null){//排序
//            nsb.withSort(new FieldSortBuilder(sortField).ignoreUnmapped(true).order(order));
//        }

        BoolQueryBuilder qb = QueryBuilders.boolQuery();
        for (String key : filedContentMap.keySet()) {//字段查询
            qb.must(QueryBuilders.matchQuery(key, filedContentMap.get(key)));

        }
        nsb.withQuery(qb);
        SearchQuery searchQuery = nsb.build();//查询建立
        searchQuery.addFields(resultFields);

        if (sortFields != null && sortFields.length > 0 && order != null) {//排序
            if ("desc".equals(order)) {//降序
                searchQuery.addSort(new Sort(Sort.Direction.DESC, sortFields));
            }
            if ("asc".equals(order)) {//升序
                searchQuery.addSort(new Sort(Sort.Direction.ASC, sortFields));
            }
        }

        Page<GoodsModel> page = null;
        if (heightFields != null && heightFields.size() > 0) {//如果设置高亮
            page = elasticsearchTemplate.queryForPage(searchQuery, GoodsModel.class, new SearchResultMapper() {
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
                        chunk.add((T) PropertyHelper.getFansheObj(GoodsModel.class, entityMap));
                    }
                    if (chunk.size() > 0) {
                        return new AggregatedPageImpl<T>((List<T>) chunk);
                    }
                    return null;
                }

            });
        } else//如果不设置高亮
            page = elasticsearchTemplate.queryForPage(searchQuery, GoodsModel.class);

        return page.getContent();
    }


}
