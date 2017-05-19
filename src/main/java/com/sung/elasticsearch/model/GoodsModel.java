package com.sung.elasticsearch.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sung.elasticsearch.utils.es.BaseEsModel;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.annotations.FieldType;


/**
 * Created by sungang on 2017/5/19.
 */
@Document(indexName = "goods_test_index", type = "goods_test_type", refreshInterval = "-1")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GoodsModel extends BaseEsModel {

    /**
     * 商品Id
     */
    @Field(type = FieldType.Long, index = FieldIndex.not_analyzed, store = true)
    private Long goodsId;
    /**
     * 商品编码
     */
    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String goodsCode;

    /**
     * 商品名称
     */
    @Field(type = FieldType.String, analyzer = "ik", searchAnalyzer = "ik", store = true)
    private String goodsName;
    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String goodsImgs;
    @Field(type = FieldType.Double, index = FieldIndex.not_analyzed, store = true)
    private Double price;
    /**
     * 商品详情
     */
    @Field(type = FieldType.String, analyzer = "ik", searchAnalyzer = "ik", store = true)
    private String description;
    /**
     * 设置状态：
     * 1-编辑
     * 2-已上架
     * 3-已下架
     * 4-删除
     */
    @Field(type = FieldType.Integer, store = true)
    private Integer status;



    @Field(type = FieldType.Integer, store = true)
    private Integer stock;


    public Long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Long goodsId) {
        this.goodsId = goodsId;
    }

    public String getGoodsCode() {
        return goodsCode;
    }

    public void setGoodsCode(String goodsCode) {
        this.goodsCode = goodsCode;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

    public String getGoodsImgs() {
        return goodsImgs;
    }

    public void setGoodsImgs(String goodsImgs) {
        this.goodsImgs = goodsImgs;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }
}
