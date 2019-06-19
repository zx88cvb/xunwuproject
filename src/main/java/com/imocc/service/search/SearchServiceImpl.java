package com.imocc.service.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.imocc.entity.House;
import com.imocc.entity.HouseDetail;
import com.imocc.entity.HouseTag;
import com.imocc.repository.HouseDetailRepository;
import com.imocc.repository.HouseRepository;
import com.imocc.repository.HouseTagRepository;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.elasticsearch.rest.RestStatus;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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

    @Override
    public boolean index(Long houseId) {
        House house = houseRepository.findOne(houseId);
        if(house==null){
            LOGGER.error("Index house {} dose not exist!",houseId);
            return false;
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
        return success;
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
        DeleteByQueryRequestBuilder builder = DeleteByQueryAction.INSTANCE
                .newRequestBuilder(transportClient)
                .filter(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseId))
                .source(INDEX_NAME);

        LOGGER.debug("Delete by query for house: " + builder);
        BulkByScrollResponse scrollResponse = builder.get();
        long deleted = scrollResponse.getDeleted();

        LOGGER.debug("DELETE total "+ deleted);
    }
}
