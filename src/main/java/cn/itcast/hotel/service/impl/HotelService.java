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
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * 聚合结果返回
     * @param params
     * @return
     */
    @Override
    public Map<String, List<String>> filters(RequestParams params) {
        try {
            // 1、准备
            SearchRequest request = new SearchRequest("hotel");
            // 2、准备DSL
            // 2.1、调用聚合结果
            buildBaicQuery(params, request);
            // 2.2、设置查询的文档数
            request.source().size(0);
            // 2.3、调用封装的聚合查询条件
            buildAggregation(request);
            // 3、发送请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            // 4、解析响应结果
            Map<String, List<String>> result = new HashMap<>();
            Aggregations aggregations = response.getAggregations();
            // 4.1 根据匹配的名称获取匹配结果
            List<String> brandList = getAggByName(aggregations,"brandAgg");
            result.put("brand",brandList);
            // 4.1 根据匹配的名称获取匹配结果
            List<String> cityList = getAggByName(aggregations,"cityAgg");
            result.put("city",cityList);
            // 4.1 根据匹配的名称获取匹配结果
            List<String> starList = getAggByName(aggregations,"starAgg");
            result.put("starName",starList);
            return result;
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    /**
     * 实现前端关键字搜索
     * @param key
     * @return
     */
    @Override
    public List<String> getSuggestion(String key) {
        try {
            // 1、准备额request
            SearchRequest request = new SearchRequest("hotel");
            // 2、准备DSL
            request.source().suggest(
                    new SuggestBuilder().addSuggestion(
                            "suggestions",
                            SuggestBuilders.completionSuggestion("suggestion")
                                    .prefix(key)
                                    .skipDuplicates(true)
                                    .size(10)
                    )
            );
            // 3、发送请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            // 4.解析结果
            Suggest suggest = response.getSuggest();
            // 4.1.根据补全查询名称，获取补全结果
            CompletionSuggestion suggestions = suggest.getSuggestion("suggestions");
            // 4.2.获取options
            List<CompletionSuggestion.Entry.Option> options = suggestions.getOptions();
            // 4.3.遍历
            List<String> list = new ArrayList<>(options.size());
            for (CompletionSuggestion.Entry.Option option : options) {
                String text = option.getText().toString();
                list.add(text);
            }
            return list;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 封装的聚合结果解析
     * @param aggregations 获取到聚合数据
     * @param aggName 指定的key值
     * @return
     */
    private List<String> getAggByName(Aggregations aggregations,String aggName) {
        // 4.1、根据聚合名称获取聚合结果
        Terms brandTerms = aggregations.get(aggName);
        // 4.2、获取buckets
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
        // 4.4、遍历结果集
        List<String> brandList = new ArrayList<>();
        for (Terms.Bucket bucket : buckets) {
            // 获取key
            String key = bucket.getKeyAsString();
            brandList.add(key);
        }
        return brandList;
    }

    /**
     * 封装聚合查询条件
     * @param request
     */
    private void buildAggregation(SearchRequest request) {
        request.source().aggregation(
                AggregationBuilders
                        // 名称
                        .terms("brandAgg")
                        // 字段
                        .field("brand")
                        // 数量
                        .size(100)
        );
        request.source().aggregation(
                AggregationBuilders
                        // 名称
                        .terms("cityAgg")
                        // 字段
                        .field("city")
                        // 数量
                        .size(100)
        );
        request.source().aggregation(
                AggregationBuilders
                        // 名称
                        .terms("starAgg")
                        // 字段
                        .field("starName")
                        // 数量
                        .size(100)
        );
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
