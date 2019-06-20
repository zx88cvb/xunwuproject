package com.imocc.service.house.search;

import com.imocc.XunwuProjectApplicationTests;
import com.imocc.service.ServiceMultiResult;
import com.imocc.service.search.ISearchService;
import com.imocc.web.controller.form.RentSearch;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Author: Angel
 * @Date: 2019/6/19.
 * @Description:
 */
public class SearchServiceTest extends XunwuProjectApplicationTests {
    @Autowired
    private ISearchService searchService;

    @Test
    public void testIndex () {
        searchService.index(15L);
    }

    @Test
    public void testRemove () {
        searchService.remove(15L);
    }

    @Test
    public void testQuery() {
        RentSearch rentSearch = new RentSearch();
        rentSearch.setCityEnName("bj");
        rentSearch.setStart(0);
        rentSearch.setSize(10);
        ServiceMultiResult<Long> query = searchService.query(rentSearch);
        System.out.println(query.getTotal());
    }
}
