package com.imocc.service.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.imocc.base.HouseSort;
import com.imocc.base.RentValueBlock;
import com.imocc.entity.House;
import com.imocc.entity.HouseDetail;
import com.imocc.entity.HouseTag;
import com.imocc.repository.HouseDetailRepository;
import com.imocc.repository.HouseRepository;
import com.imocc.repository.HouseTagRepository;
import com.imocc.service.ServiceMultiResult;
import com.imocc.web.controller.form.RentSearch;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.sort.SortOrder;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Administrator on 2018/1/14.
 */
@Service
public class SearchServiceImpl implements ISearchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ISearchService.class);

    private static final String INDEX_NAME="xunwu";

    private static final String INDEX_TYPE="house";

    private static final String INDEX_TOPIC="house_build";

    @Resource
    private HouseRepository houseRepository;

    @Resource
    private HouseDetailRepository houseDetailRepository;

    @Resource
    private HouseTagRepository tagRepository;

    @Resource
    private ModelMapper modelMapper;

    @Resource
    private TransportClient transportClient;

    @Resource
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 通过kafka监听消费创建或删除索引
     * @param content 消息体
     */
    @KafkaListener(topics = INDEX_TOPIC)
    private void handleMessage(String content) {
        try {
            HouseIndexMessage houseIndexMessage = objectMapper.readValue(content, HouseIndexMessage.class);

            switch (houseIndexMessage.getOperation()) {
                case HouseIndexMessage.INDEX:
                    this.createOrUpdateIndex(houseIndexMessage);
                    break;
                case HouseIndexMessage.REMOVE:
                    this.removeIndex(houseIndexMessage);
                    break;
                default:
                    LOGGER.warn("Not support message content " + content);
                    break;
            }
        } catch (IOException e) {
            LOGGER.error("Cannot parse json for " + content);
            e.printStackTrace();
        }
    }

    private void removeIndex(HouseIndexMessage houseIndexMessage) {
        Long houseId = houseIndexMessage.getHouseId();

        DeleteByQueryRequestBuilder builder = DeleteByQueryAction.INSTANCE
                .newRequestBuilder(transportClient)
                .filter(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseId))
                .source(INDEX_NAME);

        LOGGER.debug("Delete by query for house: " + builder);
        BulkByScrollResponse scrollResponse = builder.get();
        long deleted = scrollResponse.getDeleted();

        LOGGER.debug("DELETE total "+ deleted);

        if (deleted <= 0) {
            this.remove(houseId, houseIndexMessage.getRetry() + 1);
        }
    }

    /**
     * 创建或修改索引
     * @param houseIndexMessage 消息体
     */
    private void createOrUpdateIndex(HouseIndexMessage houseIndexMessage) {
        Long houseId = houseIndexMessage.getHouseId();

        House house = houseRepository.findOne(houseId);
        if(house==null){
            LOGGER.error("Index house {} dose not exist!",houseId);
            this.index(houseId, houseIndexMessage.getRetry() + 1);
            return;
        }
        HouseIndexTemplate houseIndexTemplate=new HouseIndexTemplate();
        modelMapper.map(house,houseIndexTemplate);

        // 查询详情
        HouseDetail houseDetail = houseDetailRepository.findByHouseId(houseId);
        if (houseDetail == null) {
            // TODO 异常
            throw new NullPointerException();
        }

        modelMapper.map(houseDetail,houseIndexTemplate);

        // 查询tag
        List<HouseTag> houseTagList = tagRepository.findAllByHouseId(houseId);
        if (!houseTagList.isEmpty()) {
            ArrayList<String> tagStrings = Lists.newArrayList();
            houseTagList.forEach(houseTag -> tagStrings.add(houseTag.getName()));
            houseIndexTemplate.setTags(tagStrings);
        }

        // 查询索引是否存在
        SearchRequestBuilder searchRequestBuilder = this.transportClient.prepareSearch(INDEX_NAME).setTypes(INDEX_TYPE)
                .setQuery(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseId));

        LOGGER.debug(searchRequestBuilder.toString());

        SearchResponse searchResponse = searchRequestBuilder.get();
        // 获取总数
        long totalHits = searchResponse.getHits().getTotalHits();

        boolean success;
        if (totalHits == 0) {
            // create
            success = create(houseIndexTemplate);
        } else if (totalHits == 1) {
            String esId = searchResponse.getHits().getAt(0).getId();
            // update
            success = update(esId, houseIndexTemplate);
        } else {
            success = deleteAndCreate(totalHits, houseIndexTemplate);
        }

        if (success) {
            LOGGER.debug("Index success with house " + houseId);
        }
    }

    /**
     * 用户调用方法 创建或修改索引
     * @param houseId houseId
     */
    @Override
    public void index(Long houseId) {
        index(houseId, 0);
    }

    /**
     * 构造消息体 并发送kafka创建索引
     * @param houseId houseId
     * @param retry 重试次数
     */
    private void index (Long houseId, int retry) {
        if (retry > HouseIndexMessage.MAX_RETRY) {
            LOGGER.error("Retry index times over 3 for house " + houseId);
            return;
        }
        HouseIndexMessage houseIndexMessage = new HouseIndexMessage(houseId, HouseIndexMessage.INDEX, retry);
        try {
            kafkaTemplate.send(INDEX_TOPIC, objectMapper.writeValueAsString(houseIndexMessage));
        } catch (JsonProcessingException e) {
            LOGGER.error("JSON encode error for "+ houseIndexMessage);
            e.printStackTrace();
        }
    }

    /**
     * 创建索引
     * @param houseIndexTemplate
     * @return
     */
    private boolean create(HouseIndexTemplate houseIndexTemplate){
        try {
            IndexResponse indexResponse = this.transportClient.prepareIndex(INDEX_NAME, INDEX_TYPE)
                    .setSource(objectMapper.writeValueAsBytes(houseIndexTemplate), XContentType.JSON)
                    .get();
            LOGGER.debug("Create index with house: " + houseIndexTemplate.getHouseId());
            if(indexResponse.status()== RestStatus.CREATED){
                return true;
            }else{
                return false;
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Error to index house " + houseIndexTemplate.getHouseId(), e);
            return false;
        }
    }

    /**
     * 修改
     * @param esId id
     * @param houseIndexTemplate
     * @return
     */
    private boolean update(String esId,HouseIndexTemplate houseIndexTemplate){
        try {
            UpdateResponse updateResponse = this.transportClient.prepareUpdate(INDEX_NAME, INDEX_TYPE,esId)
                    .setDoc(objectMapper.writeValueAsBytes(houseIndexTemplate), XContentType.JSON)
                    .get();
            LOGGER.debug("Update index with house: " + houseIndexTemplate.getHouseId());
            if(updateResponse.status()== RestStatus.OK){
                return true;
            }else{
                return false;
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Error to index house " + houseIndexTemplate.getHouseId(), e);
            return false;
        }
    }

    /**
     * 删除和创建
     * @param totalHit
     * @param houseIndexTemplate
     * @return
     */
    public boolean deleteAndCreate(Long totalHit,HouseIndexTemplate houseIndexTemplate){
        DeleteByQueryRequestBuilder builder = DeleteByQueryAction.INSTANCE
                .newRequestBuilder(transportClient)
                .filter(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseIndexTemplate.getHouseId()))
                .source(INDEX_NAME);

        LOGGER.debug("Delete by query for house: " + builder);
        BulkByScrollResponse scrollResponse = builder.get();
        long deleted = scrollResponse.getDeleted();
        if(deleted!=totalHit){
            LOGGER.warn("Need delete {}, but {} was deleted!", totalHit, deleted);
            return false;
        }else{
            return create(houseIndexTemplate);
        }

    }

    @Override
    public void remove(Long houseId) {
        remove(houseId, 0);
    }

    @Override
    public ServiceMultiResult<Long> query(RentSearch rentSearch) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 根据城市英文名
        boolQueryBuilder.filter(QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME, rentSearch.getCityEnName()));

        // 根据地区英文名
        if (rentSearch.getRegionEnName() != null && !"*".equals(rentSearch.getRegionEnName())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery(HouseIndexKey.REGION_EN_NAME, rentSearch.getRegionEnName()));
        }

        // 面积
        RentValueBlock area = RentValueBlock.matchArea(rentSearch.getAreaBlock());

        if (!RentValueBlock.ALL.equals(area)) {
            // 范围查询
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(HouseIndexKey.AREA);

            if (area.getMax() > 0) {
                rangeQueryBuilder.lte(area.getMax());
            }

            if (area.getMin() > 0) {
                rangeQueryBuilder.gte(area.getMin());
            }
            boolQueryBuilder.filter(rangeQueryBuilder);

        }

        // 价格
        RentValueBlock price = RentValueBlock.matchPrice(rentSearch.getPriceBlock());

        if (!RentValueBlock.ALL.equals(price)) {
            // 范围查询
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(HouseIndexKey.PRICE);

            if (price.getMax() > 0) {
                rangeQueryBuilder.lte(price.getMax());
            }

            if (price.getMin() > 0) {
                rangeQueryBuilder.gte(price.getMin());
            }
            boolQueryBuilder.filter(rangeQueryBuilder);

        }

        // 朝向
        if (rentSearch.getDirection() > 0) {
            boolQueryBuilder.filter(
                    QueryBuilders.termQuery(HouseIndexKey.DISTRICT, rentSearch.getDirection())
            );
        }

        // 租赁方式
        if (rentSearch.getRentWay() > -1) {
            boolQueryBuilder.filter(
                    QueryBuilders.termQuery(HouseIndexKey.RENT_WAY, rentSearch.getRentWay())
            );
        }

        boolQueryBuilder.must(QueryBuilders.multiMatchQuery(rentSearch.getKeywords(),
                HouseIndexKey.TITLE,
                HouseIndexKey.TRAFFIC,
                HouseIndexKey.DISTRICT,
                HouseIndexKey.ROUND_SERVICE,
                HouseIndexKey.SUBWAY_LINE_NAME,
                HouseIndexKey.SUBWAY_STATION_NAME
                ));

        // 通过es查询
        SearchRequestBuilder searchRequestBuilder = this.transportClient.prepareSearch(INDEX_NAME)
                .setTypes(INDEX_TYPE)
                .setQuery(boolQueryBuilder)
                .addSort(
                        HouseSort.getSortKey(rentSearch.getOrderBy()),
                        SortOrder.fromString(rentSearch.getOrderDirection())
                )
                .setFrom(rentSearch.getStart())
                .setSize(rentSearch.getSize());

        LOGGER.debug(searchRequestBuilder.toString());

        List<Long> houseIds = Lists.newArrayList();

        // 获取查询结果
        SearchResponse searchResponse = searchRequestBuilder.get();
        if (searchResponse.status() != RestStatus.OK) {
            LOGGER.warn("search status is not ok for:" + searchResponse);
            return new ServiceMultiResult<>(0, houseIds);
        }

        // 将符合结果id存入list集合
        searchResponse.getHits().forEach(hit -> {
            houseIds.add(Longs.tryParse(String.valueOf(hit.getSource().get(HouseIndexKey.HOUSE_ID))));
        });
        return new ServiceMultiResult<>(searchResponse.getHits().totalHits, houseIds);
    }

    public void remove(Long houseId, int retry) {
        if (retry > HouseIndexMessage.MAX_RETRY) {
            LOGGER.error("Retry index times over 3 for house " + houseId);
            return;
        }
        HouseIndexMessage houseIndexMessage = new HouseIndexMessage(houseId, HouseIndexMessage.REMOVE, retry);
        try {
            kafkaTemplate.send(INDEX_TOPIC, objectMapper.writeValueAsString(houseIndexMessage));
        } catch (JsonProcessingException e) {
            LOGGER.error("JSON encode error for "+ houseIndexMessage);
            e.printStackTrace();
        }
    }
}
