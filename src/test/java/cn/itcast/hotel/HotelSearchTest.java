package cn.itcast.hotel;

import cn.itcast.hotel.pojo.HotelDoc;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 项目名称：hotel-demo
 * 描述：酒店信息搜索
 *
 * @author zhong
 * @date 2022-06-04 10:29
 */
public class HotelSearchTest {
    private RestHighLevelClient client;

    /**
     * 执行之前进行连接
     */
    @BeforeEach
    void setUp(){
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.26.131:9200")
        ));
    }

    /**
     * 查询所有的数据
     */
    @Test
    void testSearch() throws IOException {
        // 1、获取
        SearchRequest request = new SearchRequest("hotel");
        // 2、设置请求
        request.source().query(QueryBuilders.matchAllQuery());
        // 3、发送请求
        SearchResponse search = client.search(request, RequestOptions.DEFAULT);
        // 4、解析响应数据
        SearchHits hits = search.getHits();
        extracted(search);
    }

    /**
     * 全局检索
     * @throws IOException
     */
    @Test
    void testMatch() throws IOException {
        // 1、获取request
        SearchRequest request = new SearchRequest("hotel");
        // 2、设置DSL
        // request.source().query(QueryBuilders.matchQuery("all","如家"));
        // 多字段查询
        request.source().query(QueryBuilders.multiMatchQuery("如家","name","business"));
        // 3、发送请求
        SearchResponse search = client.search(request, RequestOptions.DEFAULT);
        extracted(search);
    }

    /**
     * 布尔查询
     * @throws IOException
     */
    @Test
    void testMBoll() throws IOException {
        // 1、获取request
        SearchRequest request = new SearchRequest("hotel");
        // 2、设置DSL
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 添加team
        boolQueryBuilder.must(QueryBuilders.termQuery("city","上海"));
        // 添加range
        boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").lte(250));
        request.source().query(boolQueryBuilder);
        // 3、发送请求
        SearchResponse search = client.search(request, RequestOptions.DEFAULT);
        extracted(search);
    }

    /**
     * 分页排序查询所有数据
     * @throws IOException
     */
    @Test
    void testPage() throws IOException {
        int page = 2;
        int size = 5;
        // 1、获取request
        SearchRequest request = new SearchRequest("hotel");
        // 2、设置DSL
        // 2.1、查询所有的数据
       request.source().query(QueryBuilders.matchAllQuery());
       // 2.2、设置排序
        request.source().sort("price", SortOrder.ASC);
        // 2.3、设置分页信息
        request.source().from((page-1)*size).size(5);
        // 3、发送请求
        SearchResponse search = client.search(request, RequestOptions.DEFAULT);
        extracted(search);
    }

    /**
     * 高亮显示
     * @throws IOException
     */
    @Test
    void testHi() throws IOException {
        // 1、获取request
        SearchRequest request = new SearchRequest("hotel");
        // 2、设置DSL
        // 2.1、查询所有的数据
        request.source().query(QueryBuilders.matchQuery("all","如家"));
        // 2.2、设置高亮的字段以及全局匹配
        request.source().highlighter(new HighlightBuilder().field("name").requireFieldMatch(false));
        // 3、发送请求
        SearchResponse search = client.search(request, RequestOptions.DEFAULT);
        extracted(search);
    }

    /**
     * 封装的提统一使用的重构步骤
     * @param search
     */
    private void extracted(SearchResponse search) {
        // 4、解析响应数据
        SearchHits hits = search.getHits();
        // 4.1、获取总条数
        long value = hits.getTotalHits().value;
        System.out.println("共搜索到："+value+"条数据");
        // 4.2、获取文档数组
        SearchHit[] hitsArray = hits.getHits();
        // 4.3、遍历数组
        for (SearchHit documentFields : hitsArray) {
            // 获取文档
            String sourceAsString = documentFields.getSourceAsString();
            // 将文档放序列化为json对象
            HotelDoc hotelDoc = JSON.parseObject(sourceAsString, HotelDoc.class);
            // 获取高亮结果
            Map<String, HighlightField> highlightFields = documentFields.getHighlightFields();
            // 判断是否为空
            if(!CollectionUtils.isEmpty(highlightFields)){
                // 根据字段名称获取高亮信息
                HighlightField name = highlightFields.get("name");
                if(name!=null){
                    // 获取高亮值
                    String naemStr = name.getFragments()[0].string();
                    // 将高亮的值覆盖原来的name值
                    hotelDoc.setName(naemStr);
                }
            }
            System.out.println("获取到的数组数据："+hotelDoc);
        }
    }

    /**
     * 实现聚合分类
     */
    @Test
    void testAggregation() throws IOException {
        // 1、准备
        SearchRequest request = new SearchRequest("hotel");
        // 2、准备DSL
        request.source().size(0);
        request.source().aggregation(
                AggregationBuilders
                        // 名称
                        .terms("brandAgg")
                        // 字段
                        .field("brand")
                        // 数量
                        .size(10)
        );
        // 3、发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4、解析响应结果
        Aggregations aggregations = response.getAggregations();
        // 4.1、根据聚合名称获取聚合结果
        Terms brandTerms = aggregations.get("brandAgg");
        // 4.2、获取buckets
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
        // 4.4、遍历结果集
        for (Terms.Bucket bucket : buckets) {
            // 获取key
            String key = bucket.getKeyAsString();
            System.out.println(key);
        }
    }

    /**
     * 实现关键字查询
     */
    @Test
    void testSuggestar() throws IOException {
        // 1、准备额request
        SearchRequest request = new SearchRequest("hotel");
        // 2、准备DSL
        request.source().suggest(
                new SuggestBuilder().addSuggestion(
                        "suggestions",
                        SuggestBuilders.completionSuggestion("suggestion")
                                .prefix("hz")
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
            System.out.println(text);
        }
    }

    /**
     * 执行完之后销毁
     * @throws IOException
     */
    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }
}
