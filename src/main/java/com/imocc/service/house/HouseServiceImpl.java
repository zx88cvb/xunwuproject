package com.imocc.service.house;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.imocc.base.HouseSort;
import com.imocc.base.HouseStatus;
import com.imocc.base.LoginUserUtil;
import com.imocc.entity.*;
import com.imocc.repository.*;
import com.imocc.service.IHouseService;
import com.imocc.service.ServiceMultiResult;
import com.imocc.service.ServiceResult;
import com.imocc.service.search.ISearchService;
import com.imocc.web.controller.form.DatatableSearch;
import com.imocc.web.controller.form.HouseForm;
import com.imocc.web.controller.form.PhotoForm;
import com.imocc.web.controller.form.RentSearch;
import com.imocc.web.controller.house.HouseDTO;
import com.imocc.web.controller.house.HouseDetailDTO;
import com.imocc.web.controller.house.HousePictureDTO;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2018/1/11.
 */
@Service
public class HouseServiceImpl implements IHouseService {

    @Resource
    private HouseRepository houseRepository;

    @Autowired
    private HouseDetailRepository houseDetailRepository;

    @Autowired
    private HousePictureRepository housePictureRepository;

    @Autowired
    private HouseTagRepository houseTagRepository;

    @Autowired
    private SubwayRepository subwayRepository;

    @Autowired
    private SubwayStationRepository subwayStationRepository;

    @Autowired
    private HouseSubscribeRespository subscribeRespository;

    @Autowired
    private IQiNiuService qiNiuService;

    @Resource
    private ModelMapper modelMapper;

    @Autowired
    private ISearchService searchService;

    /*@Value("${qiniu.cdn.prefix}")
    private String cdnPrefix;*/

    @Override
    @Transactional
    public ServiceResult<HouseDTO> save(HouseForm houseForm) {
        HouseDetail detail = new HouseDetail();
        ServiceResult<HouseDTO> subwayValidtionResult = wrapperDetailInfo(detail, houseForm);
        if (subwayValidtionResult != null) {
            return subwayValidtionResult;
        }

        House house = new House();
        modelMapper.map(houseForm, house);

        Date now = new Date();
        house.setCreateTime(now);
        house.setLastUpdateTime(now);
        house.setAdminId(LoginUserUtil.getLoginUserId());
        house = houseRepository.save(house);

        detail.setHouseId(house.getId());
        detail = houseDetailRepository.save(detail);

        List<HousePicture> pictures = generatePictures(houseForm, house.getId());
        Iterable<HousePicture> housePictures = housePictureRepository.save(pictures);

        HouseDTO houseDTO = modelMapper.map(house, HouseDTO.class);
        HouseDetailDTO houseDetailDTO = modelMapper.map(detail, HouseDetailDTO.class);

        houseDTO.setHouseDetail(houseDetailDTO);

        List<HousePictureDTO> pictureDTOS = new ArrayList<>();
        housePictures.forEach(housePicture -> pictureDTOS.add(modelMapper.map(housePicture, HousePictureDTO.class)));
        houseDTO.setPictures(pictureDTOS);
        houseDTO.setCover("this.cdnPrefix" + houseDTO.getCover());

        List<String> tags = houseForm.getTags();
        if (tags != null && !tags.isEmpty()) {
            List<HouseTag> houseTags = new ArrayList<>();
            for (String tag : tags) {
                houseTags.add(new HouseTag(house.getId(), tag));
            }
            houseTagRepository.save(houseTags);
            houseDTO.setTags(tags);
        }

        return new ServiceResult<HouseDTO>(true, null, houseDTO);
    }

    @Override
    public ServiceMultiResult<HouseDTO> adminQuery(DatatableSearch searchBody) {
        List<HouseDTO> houseDTOS= Lists.newArrayList();

        //条件查询
        Specification<House> specification=(root,query,cb) -> {
            Predicate predicate=cb.equal(root.get("adminId"),LoginUserUtil.getLoginUserId());
            predicate=cb.and(predicate,cb.notEqual(root.get("status"), HouseStatus.DELETED.getValue()));

            if(searchBody.getCity()!=null){
                predicate=cb.and(predicate,cb.equal(root.get("cityEnName"),searchBody.getCity()));
            }

            if(searchBody.getStatus()!=null){
                predicate=cb.and(predicate,cb.equal(root.get("status"),searchBody.getStatus()));
            }

            if(searchBody.getCreateTimeMin()!=null){
                predicate=cb.and(predicate,cb.greaterThanOrEqualTo(root.get("createTime"),searchBody.getCreateTimeMin()));
            }

            if(searchBody.getCreateTimeMax()!=null){
                predicate=cb.and(predicate,cb.lessThanOrEqualTo(root.get("createTime"),searchBody.getCreateTimeMax()));
            }

            if(searchBody.getTitle()!=null){
                predicate=cb.and(predicate,cb.like(root.get("title"),"%"+searchBody.getTitle()+"%"));
            }

            return predicate;
        };
        //分页
        Sort sort=new Sort(Sort.Direction.fromString(searchBody.getDirection()),searchBody.getOrderBy());
        int page=searchBody.getStart()/searchBody.getLength();
        Pageable pageable=new PageRequest(page,searchBody.getLength(),sort);
        Page<House> houses = houseRepository.findAll(specification,pageable);
        houses.forEach(house -> {
            HouseDTO houseDTO = modelMapper.map(house, HouseDTO.class);
            houseDTOS.add(houseDTO);
        });
        return new ServiceMultiResult(houses.getTotalElements(),houseDTOS);
    }

    @Override
    public ServiceResult<HouseDTO> findCompleteOne(Long id) {
        House house = houseRepository.findOne(id);
        if (house == null) {
            return ServiceResult.notFound();
        }

        HouseDetail detail = houseDetailRepository.findByHouseId(id);
        List<HousePicture> pictures = housePictureRepository.findAllByHouseId(id);

        HouseDetailDTO detailDTO = modelMapper.map(detail, HouseDetailDTO.class);
        List<HousePictureDTO> pictureDTOS = new ArrayList<>();
        for (HousePicture picture : pictures) {
            HousePictureDTO pictureDTO = modelMapper.map(picture, HousePictureDTO.class);
            pictureDTOS.add(pictureDTO);
        }


        List<HouseTag> tags = houseTagRepository.findAllByHouseId(id);
        List<String> tagList = new ArrayList<>();
        for (HouseTag tag : tags) {
            tagList.add(tag.getName());
        }

        HouseDTO result = modelMapper.map(house, HouseDTO.class);
        result.setHouseDetail(detailDTO);
        result.setPictures(pictureDTOS);
        result.setTags(tagList);

        if (LoginUserUtil.getLoginUserId() > 0) { // 已登录用户
            HouseSubscribe subscribe = subscribeRespository.findByHouseIdAndUserId(house.getId(), LoginUserUtil.getLoginUserId());
            if (subscribe != null) {
                result.setSubscribeStatus(subscribe.getStatus());
            }
        }

        return ServiceResult.of(result);
    }

    @Override
    @Transactional
    public ServiceResult update(HouseForm houseForm) {
        House house = this.houseRepository.findOne(houseForm.getId());
        if (house == null) {
            return ServiceResult.notFound();
        }

        HouseDetail detail = this.houseDetailRepository.findByHouseId(house.getId());
        if (detail == null) {
            return ServiceResult.notFound();
        }

        ServiceResult wrapperResult = wrapperDetailInfo(detail, houseForm);
        if (wrapperResult != null) {
            return wrapperResult;
        }

        houseDetailRepository.save(detail);

        List<HousePicture> pictures = generatePictures(houseForm, houseForm.getId());
        housePictureRepository.save(pictures);

        if (houseForm.getCover() == null) {
            houseForm.setCover(house.getCover());
        }

        modelMapper.map(houseForm, house);
        house.setLastUpdateTime(new Date());
        houseRepository.save(house);

        if (house.getStatus() == HouseStatus.PASSES.getValue()) {
            searchService.index(house.getId());
        }

        return ServiceResult.success();
    }

    @Override
    @Transactional
    public ServiceResult updateCover(Long coverId, Long targetId) {
        HousePicture cover = housePictureRepository.findOne(coverId);
        if (cover == null) {
            return ServiceResult.notFound();
        }

        houseRepository.updateCover(targetId, cover.getPath());
        return ServiceResult.success();
    }

    @Override
    @Transactional
    public ServiceResult removePhoto(Long id) {
        HousePicture picture = housePictureRepository.findOne(id);
        if (picture == null) {
            return ServiceResult.notFound();
        }

        try {
            Response response = this.qiNiuService.delete(picture.getPath());
            if (response.isOK()) {
                housePictureRepository.delete(id);
                return ServiceResult.success();
            } else {
                return new ServiceResult(false, response.error);
            }
        } catch (QiniuException e) {
            e.printStackTrace();
            return new ServiceResult(false, e.getMessage());
        }
    }

    @Override
    @Transactional
    public ServiceResult addTag(Long houseId, String tag) {
        House house = houseRepository.findOne(houseId);
        if (house == null) {
            return ServiceResult.notFound();
        }

        HouseTag houseTag = houseTagRepository.findByNameAndHouseId(tag, houseId);
        if (houseTag != null) {
            return new ServiceResult(false, "标签已存在");
        }

        houseTagRepository.save(new HouseTag(houseId, tag));
        return ServiceResult.success();
    }

    @Override
    @Transactional
    public ServiceResult removeTag(Long houseId, String tag) {
        House house = houseRepository.findOne(houseId);
        if (house == null) {
            return ServiceResult.notFound();
        }

        HouseTag houseTag = houseTagRepository.findByNameAndHouseId(tag, houseId);
        if (houseTag == null) {
            return new ServiceResult(false, "标签不存在");
        }

        houseTagRepository.delete(houseTag.getId());
        return ServiceResult.success();
    }

    @Override
    @Transactional
    public ServiceResult updateStatus(Long id, int status) {
        House house = houseRepository.findOne(id);
        if(house==null){
            return ServiceResult.notFound();
        }

        if(house.getStatus()==status){
            return new ServiceResult(false,"状态未发生变化");
        }

        if(house.getStatus()==HouseStatus.RENTED.getValue()){
            return new ServiceResult(false,"此房屋已出租,不允许修改");
        }

        if (house.getStatus() == HouseStatus.DELETED.getValue()) {
            return new ServiceResult(false, "已删除的资源不允许操作");
        }

        houseRepository.updateStatus(id, status);

        // 上架更新索引 其他情况删除索引
        if (status == HouseStatus.PASSES.getValue()) {
            searchService.index(id);
        } else {
            searchService.remove(id);
        }
        return ServiceResult.success();
    }

    @Override
    public ServiceMultiResult<HouseDTO> query(RentSearch rentSearch) {
        Sort sort= HouseSort.generateSort(rentSearch.getOrderBy(),rentSearch.getOrderDirection());
        int page=rentSearch.getStart()/rentSearch.getSize();
        Pageable pageable=new PageRequest(page,rentSearch.getSize(),sort);
        Specification<House> specification=(root, query, cb) -> {
            Predicate predicate = cb.equal(root.get("status"),HouseStatus.PASSES.getValue());
            predicate=cb.and(predicate,cb.equal(root.get("cityEnName"),rentSearch.getCityEnName()));

            /*防止附近没有地铁*/
            if(HouseSort.DISTANCE_TO_SUBWAY_KEY.equals(rentSearch.getOrderBy())){
                predicate=cb.and(predicate,cb.gt(root.get(HouseSort.DISTANCE_TO_SUBWAY_KEY),-1));
            }
            return predicate;
        };
        Page<House> houses = houseRepository.findAll(specification, pageable);
        List<HouseDTO> houseDTOList=Lists.newArrayList();

        List<Long> houseIds=Lists.newArrayList();
        Map<Long,HouseDTO> idToHouseDTO= Maps.newHashMap();
        houses.forEach(house -> {
            HouseDTO houseDTO =modelMapper.map(house,HouseDTO.class);
            houseDTO.setCover("D:/Software_Development/Java_Development/WorkSpace/IntelliJ-IDEA-workspace/github/xunwuproject/temp/无标题.png");
            houseDTOList.add(houseDTO);

            //将id存入List集合
            houseIds.add(house.getId());
            idToHouseDTO.put(house.getId(),houseDTO);
        });

        wrapperHouseList(houseIds,idToHouseDTO);
        return new ServiceMultiResult<HouseDTO>(houses.getTotalElements(),houseDTOList);
    }

    private void wrapperHouseList(List<Long> houseIds,Map<Long,HouseDTO> idToHouseDTO){
        //详情
        List<HouseDetail> details = houseDetailRepository.findAllByHouseIdIn(houseIds);
        details.forEach(houseDetail -> {
            HouseDTO houseDto=idToHouseDTO.get(houseDetail.getHouseId());
            HouseDetailDTO houseDetailDTO=modelMapper.map(houseDetail,HouseDetailDTO.class);
            houseDto.setHouseDetail(houseDetailDTO);
        });

        //标签
        List<HouseTag> tags = houseTagRepository.findAllByHouseIdIn(houseIds);
        tags.forEach(houseTag -> {
            HouseDTO houseDto=idToHouseDTO.get(houseTag.getHouseId());
            houseDto.getTags().add(houseTag.getName());
        });

    }


    /**
     * 图片对象列表信息填充
     * @param form
     * @param houseId
     * @return
     */
    private List<HousePicture> generatePictures(HouseForm form, Long houseId) {
        List<HousePicture> pictures = new ArrayList<>();
        if (form.getPhotos() == null || form.getPhotos().isEmpty()) {
            return pictures;
        }

        for (PhotoForm photoForm : form.getPhotos()) {
            HousePicture picture = new HousePicture();
            picture.setHouseId(houseId);
            picture.setCdnPrefix("cdnPrefix");
            picture.setPath(photoForm.getPath());
            picture.setWidth(photoForm.getWidth());
            picture.setHeight(photoForm.getHeight());
            pictures.add(picture);
        }
        return pictures;
    }

    /**
     * 房源详细信息对象填充
     * @param houseDetail
     * @param houseForm
     * @return
     */
    private ServiceResult<HouseDTO> wrapperDetailInfo(HouseDetail houseDetail, HouseForm houseForm) {
        Subway subway = subwayRepository.findOne(houseForm.getSubwayLineId());
        if (subway == null) {
            return new ServiceResult<>(false, "Not valid subway line!");
        }

        SubwayStation subwayStation = subwayStationRepository.findOne(houseForm.getSubwayStationId());
        if (subwayStation == null || subway.getId() != subwayStation.getSubwayId()) {
            return new ServiceResult<>(false, "Not valid subway station!");
        }

        houseDetail.setSubwayLineId(subway.getId());
        houseDetail.setSubwayLineName(subway.getName());

        houseDetail.setSubwayStationId(subwayStation.getId());
        houseDetail.setSubwayStationName(subwayStation.getName());

        houseDetail.setDescription(houseForm.getDescription());
        houseDetail.setDetailAddress(houseForm.getDetailAddress());
        houseDetail.setLayoutDesc(houseForm.getLayoutDesc());
        houseDetail.setRentWay(houseForm.getRentWay());
        houseDetail.setRoundService(houseForm.getRoundService());
        houseDetail.setTraffic(houseForm.getTraffic());
        return null;

    }
}
