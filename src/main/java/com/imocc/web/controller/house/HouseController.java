package com.imocc.web.controller.house;

import com.google.common.collect.Lists;
import com.imocc.base.ApiResponse;
import com.imocc.base.RentValueBlock;
import com.imocc.entity.SupportAddress;
import com.imocc.service.*;
import com.imocc.service.search.ISearchService;
import com.imocc.web.controller.form.RentSearch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2018/1/11.
 */
@Controller
public class HouseController {

    @Resource  
    private IAddressService iAddressService;
    @Resource
    private IHouseService iHouseService;
    @Resource
    private IUserService iUserService;
    @Autowired
    private ISearchService iSearchService;

    /**
     * 自动补全接口
     * @param prefix 输入前缀
     * @return List结果
     */
    @GetMapping("rent/house/autocomplete")
    @ResponseBody
    public ApiResponse autocomplete(String prefix) {
        if (prefix.isEmpty()) {
            return ApiResponse.ofStatus(ApiResponse.Status.BAD_REQUEST);
        }
        ServiceResult<List<String>> result = iSearchService.suggest(prefix);
        return ApiResponse.ofSuccess(result.getResult());
    }

    @GetMapping("address/support/cities")
    @ResponseBody
    public ApiResponse getSupportCities(){
        ServiceMultiResult<SupportAddressDTO> result = iAddressService.findAllCities();
        if (result.getResultSize()==0){
            return ApiResponse.ofMessage(ApiResponse.Status.NOT_FOUND.getCode(),ApiResponse.Status.NOT_FOUND.getStandardMessage());
        }
        return ApiResponse.ofSuccess(result.getResult());
    }

    /**
     * 获取对应城市支持区域列表
     * @param cityEnName
     * @return
     */
    @GetMapping("address/support/regions")
    @ResponseBody
    public ApiResponse getSupportRegions(@RequestParam(name = "city_name") String cityEnName) {
        ServiceMultiResult<SupportAddressDTO> addressResult = iAddressService.findAllRegionsByCityName(cityEnName);
        if (addressResult.getResult() == null || addressResult.getTotal() < 1) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        }
        return ApiResponse.ofSuccess(addressResult.getResult());
    }

    /**
     * 获取具体城市所支持的地铁线路
     * @param cityEnName
     * @return
     */
    @GetMapping("address/support/subway/line")
    @ResponseBody
    public ApiResponse getSupportSubwayLine(@RequestParam(name = "city_name") String cityEnName) {
        List<SubwayDTO> subways = iAddressService.findAllSubwayByCity(cityEnName);
        if (subways.isEmpty()) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        }

        return ApiResponse.ofSuccess(subways);
    }

    /**
     * 获取对应地铁线路所支持的地铁站点
     * @param subwayId
     * @return
     */
    @GetMapping("address/support/subway/station")
    @ResponseBody
    public ApiResponse getSupportSubwayStation(@RequestParam(name = "subway_id") Long subwayId) {
        List<SubwayStationDTO> stationDTOS = iAddressService.findAllStationBySubway(subwayId);
        if (stationDTOS.isEmpty()) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        }

        return ApiResponse.ofSuccess(stationDTOS);
    }

    @GetMapping("rent/house")
        public String rentHousePage(@ModelAttribute RentSearch rentSearch, Model model,
                                HttpSession session, RedirectAttributes redirectAttributes){
        if(rentSearch.getCityEnName()==null){
            String cityEnName = (String) session.getAttribute("cityEnName");
            if(cityEnName==null){
                redirectAttributes.addAttribute("msg","must_chose_city");
                return "redirect:/index";
            }else {
                rentSearch.setCityEnName(cityEnName);
            }
        }else {
            session.setAttribute("cityEnName", rentSearch.getCityEnName());
        }

        ServiceResult<SupportAddressDTO> city = iAddressService.findCity(rentSearch.getCityEnName());
        if(!city.isSuccess()){
            redirectAttributes.addAttribute("msg","must_chose_city");
            return "redirect:/index";
        }
        model.addAttribute("currentCity",city.getResult());

        ServiceMultiResult regionsByCityName = iAddressService.findAllRegionsByCityName(rentSearch.getCityEnName());
        if(regionsByCityName.getResult()==null || regionsByCityName.getTotal()<1){
            redirectAttributes.addAttribute("msg","must_chose_city");
            return "redirect:/index";
        }
        ServiceMultiResult<HouseDTO> query = iHouseService.query(rentSearch);

        model.addAttribute("total",query.getTotal());
        model.addAttribute("houses", query.getResult());
        if(rentSearch.getRegionEnName()==null){
            rentSearch.setRegionEnName("*");
        }
        model.addAttribute("searchBody",rentSearch);
        model.addAttribute("regions",regionsByCityName.getResult());

        model.addAttribute("priceBlocks", RentValueBlock.PRICE_BLOCK);
        model.addAttribute("areaBlocks", RentValueBlock.AREA_BLOCK);

        model.addAttribute("currentPriceBlock", RentValueBlock.matchPrice(rentSearch.getPriceBlock()));
        model.addAttribute("currentAreaBlock", RentValueBlock.matchArea(rentSearch.getAreaBlock()));
        return "rent-list";
    }

    @GetMapping("rent/house/show/{id}")
    public String show(@PathVariable(value = "id") Long houseId,
                       Model model) {
        if (houseId <= 0) {
            return "404";
        }

        ServiceResult<HouseDTO> serviceResult = iHouseService.findCompleteOne(houseId);
        if (!serviceResult.isSuccess()) {
            return "404";
        }

        HouseDTO houseDTO = serviceResult.getResult();
        Map<SupportAddress.Level, SupportAddressDTO>
                addressMap = iAddressService.findCityAndRegion(houseDTO.getCityEnName(), houseDTO.getRegionEnName());

        SupportAddressDTO city = addressMap.get(SupportAddress.Level.CITY);
        SupportAddressDTO region = addressMap.get(SupportAddress.Level.REGION);

        model.addAttribute("city", city);
        model.addAttribute("region", region);

        ServiceResult<UserDTO> userDTOServiceResult = iUserService.findById(houseDTO.getAdminId());
        model.addAttribute("agent", userDTOServiceResult.getResult());
        model.addAttribute("house", houseDTO);


        model.addAttribute("houseCountInDistrict", 0);

        return "house-detail";
    }
}
