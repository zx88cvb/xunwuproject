package com.imocc.address;

import com.imocc.XunwuProjectApplicationTests;
import com.imocc.service.IAddressService;
import com.imocc.service.ServiceResult;
import com.imocc.service.search.BaiduMapLocation;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Author: Angel
 * @Date: 2019/7/24.
 * @Description:
 */
public class AddressServiceTest extends XunwuProjectApplicationTests {

    @Autowired
    private IAddressService iAddressService;

    @Test
    public void testGetMapLocation() {
        String city = "北京";
        String address = "北京市昌平区";
        ServiceResult<BaiduMapLocation> serviceResult = iAddressService.getBaiduMapLocation(city, address);

        Assert.assertTrue(serviceResult.isSuccess());
        Assert.assertTrue(serviceResult.getResult().getLongitude() > 0);
        Assert.assertTrue(serviceResult.getResult().getLatitude() > 0);
    }
}
