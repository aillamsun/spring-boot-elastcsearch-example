package com.sung.elasticsearch.utils.es;

import com.sung.elasticsearch.utils.BaseModel;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;


/**
 * Created by sungang on 2017/4/17.
 */
public class BaseEsModel extends BaseModel {

    @Id
    @Field(index = FieldIndex.not_analyzed, store = true)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
