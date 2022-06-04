package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {
    /**
     * 注入
     */
    @Autowired
    RestHighLevelClient client;

    /**
     * 拼接筛选条件
     *
     * @param params
     * @return
     */
    @Override
    public PageResult search(RequestParams params) {
        try {
            // 1、准备requeue
            SearchRequest request = new SearchRequest("hotel");
            // 2、准备DSL
            buildBaicQuery(params, request);

            // 2.2、分页搜索
            int page = params.getPage();
            int size = params.getSize();
            request.source().from((page - 1) * size).size(size);

            // 坐标范围排序
            String location = params.getLocation();
            if (location != null && !location.equals("")) {
                request.source().sort(SortBuilders.geoDistanceSort("location", new GeoPoint(location))
                        .order(SortOrder.ASC).unit(DistanceUnit.KILOMETERS));

            }

            // 3、发送请求，得到响应
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            // 4、解析响应
            return extracted(response);
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    /**
     * 重构筛选条件
     *
     * @param params
     * @param request
     */
    private void buildBaicQuery(RequestParams params, SearchRequest request) {
        // 将查询的条件较多，所以封装在一起
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // 2.1、关键字搜索
        String key = params.getKey();
        if (key == null || "".equals(key)) {
            boolQuery.must(QueryBuilders.matchAllQuery());
        } else {
            boolQuery.must(QueryBuilders.matchQuery("all", key));
        }
        // 城市条件查询，不要参与算分
        if (params.getCity() != null && !params.getCity().equals("")) {
            boolQuery.filter(QueryBuilders.matchQuery("city", params.getCity()));
        }
        // 匹配条件
        if (params.getBrand() != null && !params.getBrand().equals("")) {
            boolQuery.filter(QueryBuilders.matchQuery("brand", params.getBrand()));
        }
        // 星级条件
        if (params.getStarName() != null && !params.getStarName().equals("")) {
            boolQuery.filter(QueryBuilders.matchQuery("starName", params.getStarName()));
        }
        // 价格判断
        if (params.getMinPrice() != null && !params.getMaxPrice().equals("")) {
            // 大于等于和小于等于
            boolQuery.filter(QueryBuilders.rangeQuery("price").gte(params.getMinPrice()).lte(params.getMaxPrice()));
        }

        // 2、算分控制
        FunctionScoreQueryBuilder functionScoreQueryBuilder =
                QueryBuilders.functionScoreQuery(
                        // 原始算分方法
                        boolQuery,
                        // function score的数组
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                                // 其中一个function score原始
                           new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                   // 过滤条件
                                   QueryBuilders.termQuery("isAD",true),
                                   // 算分函数
                                   ScoreFunctionBuilders.weightFactorFunction(10)
                           )
                        }
                );

        request.source().query(boolQuery);
    }

    /**
     * 封装的提统一使用的重构步骤
     *
     * @param search
     */
    private PageResult extracted(SearchResponse search) {
        // 4、解析响应数据
        SearchHits hits = search.getHits();
        // 4.1、获取总条数
        long value = hits.getTotalHits().value;
        // 4.2、获取文档数组
        SearchHit[] hitsArray = hits.getHits();
        List<HotelDoc> hotelDocs = new ArrayList<>();
        // 4.3、遍历数组
        for (SearchHit documentFields : hitsArray) {
            // 获取文档
            String sourceAsString = documentFields.getSourceAsString();
            // 将文档放序列化为json对象
            HotelDoc hotelDoc = JSON.parseObject(sourceAsString, HotelDoc.class);
            // 获取到距离排序的值
            Object[] rawSortValues = documentFields.getSortValues();
            if (rawSortValues.length > 0) {
                Object sortValue = rawSortValues[0];
                // 添加距离
                hotelDoc.setDistance(sortValue);
                System.out.println("查询返回的公里数：" + sortValue);
            }
            hotelDocs.add(hotelDoc);
        }
        return new PageResult(value, hotelDocs);
    }
}
