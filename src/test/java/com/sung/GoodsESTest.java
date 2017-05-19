package com.sung;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sung.elasticsearch.SpringBootElastcsearchExampleApplication;
import com.sung.elasticsearch.model.GoodsModel;
import com.sung.elasticsearch.repository.GoodsESDocRepository;
import com.sung.elasticsearch.service.GoodsEsDocService;
import com.sung.elasticsearch.utils.es.BasePage;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.hamcrest.core.Is.is;

/**
 * Created by sungang on 2017/4/17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {SpringBootElastcsearchExampleApplication.class})
@WebAppConfiguration
public class GoodsESTest {


    @Autowired
    private GoodsEsDocService goodsEsDocService;

    @Autowired
    private GoodsESDocRepository goodsESDocRepository;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;


    @Before
    public void before() {
        elasticsearchTemplate.deleteIndex(GoodsModel.class);
        elasticsearchTemplate.createIndex(GoodsModel.class);
        elasticsearchTemplate.putMapping(GoodsModel.class);
        initData();
        elasticsearchTemplate.refresh(GoodsModel.class);
    }

    public void initData() {
        GoodsModel goodsESDoc = new GoodsModel();
        goodsESDoc.setId(1L);
        goodsESDoc.setGoodsId(1L);
        goodsESDoc.setGoodsCode("1");
        goodsESDoc.setGoodsName("百事可乐拉罐");
        goodsESDoc.setDescription("百事可乐拉罐");
        goodsESDoc.setPrice(2d);
        goodsESDoc.setGoodsImgs("http://oo8a9uu18.bkt.clouddn.com/20170414095840_440.jpeg,http://oo8a9uu18.bkt.clouddn.com/20170414095840_575.jpeg");


        GoodsModel goodsESDoc2 = new GoodsModel();
        goodsESDoc2.setId(2L);
        goodsESDoc2.setGoodsId(2L);
        goodsESDoc.setGoodsCode("2");
        goodsESDoc2.setGoodsName("百事可乐六联包");
        goodsESDoc2.setDescription("百事可乐六联包");
        goodsESDoc2.setPrice(21d);
        goodsESDoc2.setGoodsImgs("http://oo8a9uu18.bkt.clouddn.com/20170414095840_440.jpeg,http://oo8a9uu18.bkt.clouddn.com/20170414095840_575.jpeg");

        List<GoodsModel> goodsESDocs = Arrays.asList(goodsESDoc, goodsESDoc2);

        goodsESDocRepository.save(goodsESDocs);
    }

    @Test
    public void findOne() {
        GoodsModel goodsESDoc1 = goodsESDocRepository.findOne(1L);
        System.out.println(JSON.toJSONString(goodsESDoc1));
    }

    /**
     * 分词 查询 商品名称
     */
    @Test
    public void testSelect1() {
        //组装查询
        BoolQueryBuilder builder = boolQuery();
        builder.must(matchQuery("goodsName", "百事"));

        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(builder).build();

        Page<GoodsModel> page = elasticsearchTemplate.queryForPage(searchQuery, GoodsModel.class);
        System.out.println(page.getSize());

        List<GoodsModel> GoodsESDocs = page.getContent();

        System.out.println(JSON.toJSONString(GoodsESDocs));

        Assert.assertThat(page.getTotalElements(), is(2L));
    }

    /**
     * 分词 查询 商品名称 and 描述
     */
    @Test
    public void testSelect2() {
        //组装查询
        BoolQueryBuilder builder = boolQuery();
        builder.must(matchQuery("goodsName", "百事")).must(matchQuery("description", "百事"));

        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(builder).build();

        Page<GoodsModel> page = elasticsearchTemplate.queryForPage(searchQuery, GoodsModel.class);
        System.out.println(page.getSize());

        List<GoodsModel> GoodsESDocs = page.getContent();

        System.out.println(JSON.toJSONString(GoodsESDocs));

        Assert.assertThat(page.getTotalElements(), is(2L));
    }


    /**
     * 分词 分页查询
     */
    @Test
    public void testSelect3() {
        //组装查询
        Map<String, Object> filedContentMap = Maps.newHashMap();
        filedContentMap.put("goodsName", "可乐");
        String sortFields[] = {};
        //排序字段  默认综合排序
        sortFields = new String[]{"price"};
        String sort_type = "asc";

        BasePage<GoodsModel> page = new BasePage<>();
        page.setPageNo(1);
        page.setPageSize(10);

        String[] resultFields = {"id", "goodsCode", "goodsName", "goodsImgs", "price", "goodsId"};

        BasePage<GoodsModel> goodsESDocBasePage = goodsEsDocService.queryPage(filedContentMap, null, sortFields, sort_type, page, resultFields);
        List<GoodsModel> goodsESDocs = goodsESDocBasePage.getResults();

        JSONObject result = new JSONObject();
        result.put("datas", goodsESDocs);
        result.put("page_no", goodsESDocBasePage.getPageNo());
        result.put("page_size", goodsESDocBasePage.getPageSize());
        result.put("total_page", goodsESDocBasePage.getTotalPage());
        result.put("total_record", goodsESDocBasePage.getTotalRecord());

        System.out.println(JSON.toJSONString(result));
    }


    /**
     * 分词 分页查询
     */
    @Test
    public void testSelect4() {
        List<GoodsModel> goodsModels = goodsEsDocService.searchGoods(1,10,"百事可乐拉罐");
        System.out.println(JSON.toJSONString(goodsModels));
    }


    /**
     * 分词 查询 商品名称 and 描述 价格排序
     */
    @Test
    public void testSelectSort() {
        //组装查询
        BoolQueryBuilder builder = boolQuery();
        builder.must(matchQuery("goodsName", "百事")).must(matchQuery("description", "百事"));

        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(builder).build();
        searchQuery.addSort(new Sort(Sort.Direction.DESC, new String[]{"price"}));

        Page<GoodsModel> page = elasticsearchTemplate.queryForPage(searchQuery, GoodsModel.class);
        System.out.println(page.getSize());

        List<GoodsModel> GoodsESDocs = page.getContent();

        System.out.println(JSON.toJSONString(GoodsESDocs));

        Assert.assertThat(page.getTotalElements(), is(2L));
    }


    /**
     *
     */
    @Test
    public void insertOrUpdate() {
        try {
            GoodsModel goodsModel = new GoodsModel();
            goodsModel.setId(1L);
            goodsModel.setGoodsName("巴西牛肉");
            goodsEsDocService.insertOrUpdate(goodsModel);
            GoodsModel goodsESDoc1 = goodsESDocRepository.findOne(1L);
            Assert.assertThat(goodsESDoc1.getGoodsName(), is("巴西牛肉"));
        } catch (Exception e) {
        }
    }


    /**
     *
     */
    @Test
    public void deleteById() {
        try {
            goodsEsDocService.deleteById(1L);
            GoodsModel goodsESDoc1 = goodsESDocRepository.findOne(1L);
            Assert.assertNull(goodsESDoc1);
        } catch (Exception e) {
        }
    }

    @Test
    public void deleteByIds() {
        try {
            CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
            List<String> idList = Lists.newArrayList();
            criteriaQuery.setIds(idList);
            elasticsearchTemplate.delete(criteriaQuery, GoodsModel.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 根据条件查询
     *
     * @return
     */
    @Test
    public void deleteByQuery() {
        try {
            DeleteQuery dq = new DeleteQuery();
            Map<String, Object> filedContentMap = Maps.newHashMap();
            filedContentMap.put("id", "1");
            BoolQueryBuilder qb = QueryBuilders.boolQuery();
            if (filedContentMap != null)
                for (String key : filedContentMap.keySet()) {//字段查询
                    qb.must(QueryBuilders.matchQuery(key, filedContentMap.get(key)));
                }
            dq.setQuery(qb);
            elasticsearchTemplate.delete(dq, GoodsModel.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
