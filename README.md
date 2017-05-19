## spring-boot-elastcsearch-example. 

------
## 技术
Spring Boot(1.5.1.RELEASE) elastcsearch-2.4.1



------


## 更新
```java
 public boolean insertOrUpdate(GoodsESDoc goodsESDoc) {
        List<IndexQuery> queries = new ArrayList<IndexQuery>();
        Long id = goodsESDoc.getId();
        IndexQuery indexQuery = new IndexQueryBuilder().withId(String.valueOf(id)).withObject(goodsESDoc).build();
        queries.add(indexQuery);
        elasticsearchTemplate.bulkIndex(queries);
        elasticsearchTemplate.refresh(GoodsESDoc.class);
        return true;
    }
```


## 删除

```java
 public void deleteById(Long id) {
        goodsESDocRepository.delete(id);
    }
```





## 分页条件DSL查询 (支持高亮)
```java

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
    public BasePage<GoodsESDoc> queryPage(Map<String, Object> filedContentMap, final List<String> heightFields, String[] sortFields, String order, BasePage<GoodsESDoc> basePage, String[] resultFields) {

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


        Page<GoodsESDoc> page = null;
        //如果设置高亮
        if (heightFields != null && heightFields.size() > 0) {
            page = elasticsearchTemplate.queryForPage(searchQuery, GoodsESDoc.class, new SearchResultMapper() {
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
                        chunk.add((T) PropertyHelper.getFansheObj(GoodsESDoc.class, entityMap));
                    }
                    if (chunk.size() > 0) {
                        return new AggregatedPageImpl<T>((List<T>) chunk);
                    }
                    return new AggregatedPageImpl<T>(new ArrayList<T>());
                }

            });
        } else {//如果不设置高亮
            logger.info("#################" + qb.toString());
            page = elasticsearchTemplate.queryForPage(searchQuery, GoodsESDoc.class);
        }
        basePage.setTotalRecord(page.getTotalElements());
        basePage.setResults(page.getContent());
        return basePage;
    }

```


------

## Test Code
```java

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

```
