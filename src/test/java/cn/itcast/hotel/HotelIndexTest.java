package cn.itcast.hotel;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static constants.HotelConstants.MAPPING_TEMPLATE;

/**
 * 项目名称：hotel-demo
 * 描述：酒店数据单元测试
 *
 * @author zhong
 * @date 2022-06-02 12:58
 */
public class HotelIndexTest {
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
     * 测试是否连接
     */
    @Test
    void testIndex(){
        System.out.println(client);
    }

    /**
     * 创建索引库
     */
    @Test
    void createHotelIndex() throws IOException {
        // 1、创建request对象
        CreateIndexRequest request = new CreateIndexRequest("hotel");
        // 2、准备请求的参数，DSL语句，在官网写好拷贝
        request.source(MAPPING_TEMPLATE, XContentType.JSON);
        // 3、发送请求
        client.indices().create(request, RequestOptions.DEFAULT);
    }

    /**
     * 删除索引库
     */
    @Test
    void DeleteHotelIndex() throws IOException {
        // 1、创建request对象
        DeleteIndexRequest request = new DeleteIndexRequest("hotel");
        // 2、发送请求
        client.indices().delete(request, RequestOptions.DEFAULT);
    }

    /**
     * 判断是否存在索引库
     */
    @Test
    void testExistsHotelIndex() throws IOException {
        // 1、创建request对象
       GetIndexRequest request = new GetIndexRequest("hotel");
        // 2、发送请求
        boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);
        System.out.println(exists?"索引库已存在":"索引库不存在");
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
