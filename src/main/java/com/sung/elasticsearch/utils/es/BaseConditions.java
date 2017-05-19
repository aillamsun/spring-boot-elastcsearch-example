package com.sung.elasticsearch.utils.es;

import java.util.List;

/**
 * Created by sungang on 2017/4/17.
 */
public class BaseConditions {
    protected String orderBy;//order by time desc
    protected String distinct;// distint a,,b,c
    protected List<BaseCriteria> conditions;//条件

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public String getDistinct() {
        return distinct;
    }

    public void setDistinct(String distinct) {
        this.distinct = distinct;
    }

    public List<BaseCriteria> getConditions() {
        return conditions;
    }


    public void addCondition(BaseCriteria condition) {
        this.conditions.add(condition);
    }

    public void setConditions(List<BaseCriteria> conditions) {
        this.conditions = conditions;
    }


    public static class BaseCriteria {

        private String proName;
        private String condition;

        private Object value;

        private Object secondValue;

        private boolean noValue;

        private boolean singleValue;

        private boolean betweenValue;

        private boolean listValue;

        private String typeHandler;


        public void setCondition(String condition) {
            this.condition = condition;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public void setSecondValue(Object secondValue) {
            this.secondValue = secondValue;
        }

        public String getProName() {
            return proName;
        }

        public void setProName(String proName) {
            this.proName = proName;
        }

        public String getCondition() {
            return condition;
        }

        public Object getValue() {
            return value;
        }

        public Object getSecondValue() {
            return secondValue;
        }

        public boolean isNoValue() {
            return noValue;
        }

        public boolean isSingleValue() {
            return singleValue;
        }

        public boolean isBetweenValue() {
            return betweenValue;
        }

        public boolean isListValue() {
            return listValue;
        }

        public String getTypeHandler() {
            return typeHandler;
        }

        public BaseCriteria(String condition) {
            super();
            this.condition = condition;
            this.typeHandler = null;
            this.noValue = true;
        }

        public BaseCriteria(String condition, Object value, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.typeHandler = typeHandler;
            if (value instanceof List<?>) {
                this.listValue = true;
            } else {
                this.singleValue = true;
            }
        }

        public BaseCriteria(String condition, Object value) {
            this(condition, value, null);
        }

        public BaseCriteria(String condition, Object value, Object secondValue, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.secondValue = secondValue;
            this.typeHandler = typeHandler;
            this.betweenValue = true;
        }

        public BaseCriteria(String condition, Object value, Object secondValue) {
            this(condition, value, secondValue, null);
        }

    }
}
