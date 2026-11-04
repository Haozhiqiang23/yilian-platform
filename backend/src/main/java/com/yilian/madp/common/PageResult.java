package com.yilian.madp.common;

import java.util.List;

//用于分页列表
public class PageResult<T> {
    private List<T> records;

    private Integer total;

    public PageResult() {
    }

    public PageResult(List<T> records, Integer total) {
        this.records = records;
        this.total = total;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }
}
