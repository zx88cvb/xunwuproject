package com.imocc.service.house.search;

import com.imocc.XunwuProjectApplicationTests;
import com.imocc.service.search.ISearchService;
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
}
