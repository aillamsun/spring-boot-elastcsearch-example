package com.sung.elasticsearch.base;

import com.sung.elasticsearch.utils.es.BasePage;
import org.elasticsearch.search.sort.SortOrder;

import java.util.List;
import java.util.Map;

/**
 * Created by sungang on 2017/4/17.
 */
public interface EsBaseDao<T>{

    /**
     * 插入或等新，需要有id，id需要自己生成
     * @param tList
     * @return
     */
    public boolean insertOrUpdate(List<T> tList) ;

    /**
     * 插入或更新
     * @param t
     * @return
     */
    public boolean insertOrUpdate(T t) ;

    /**
     * 删除
     * @param id
     * @return
     */
    public boolean deleteById(String id);

    /**
     * 检查健康状态
     * @return
     */
    public boolean ping();

    /**
     * 条件分页查询 basePage.getRecordNo(), basePage.getPageSize()
     * @param filedContentMap
     * @param heightFields
     * @param sortField
     * @param order
     * @param basePage
     * @return
     */
    public BasePage<T> queryPage(Map<String, Object> filedContentMap, List<String> heightFields, String sortField, SortOrder order, BasePage<T> basePage);

    /**
     * 列表查询，不分页
     * @param filedContentMap
     * @param heightFields
     * @param sortField
     * @param order
     * @return
     */
    public List<T> queryList(Map<String, Object> filedContentMap, List<String> heightFields, String sortField, SortOrder order);

    /**
     * 本类查询
     * @param id
     * @return
     */
    public T queryById(String id);


    /**
     * 删除 by ids
     * @param idList
     * @return
     */
    public boolean deleteByIds(List<String> idList);

    /**
     * 根据条件查询
     * @param filedContentMap
     * @return
     */
    public boolean deleteByQuery(Map<String, Object> filedContentMap);
}
