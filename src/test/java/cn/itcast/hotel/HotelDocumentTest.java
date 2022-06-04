package cn.itcast.hotel;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.impl.HotelService;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;


/**
 * 项目名称：hotel-demo
 * 描述：文档操作
 *
 * @author zhong
 * @date 2022-06-02 12:58
 */
@SpringBootTest
public class HotelDocumentTest {
    /**
     * 注入MP插件的查询信息
     */
    @Autowired
    HotelService hotelService;

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
     * 新增文档
     */
    @Test
    void testAddDocument() throws IOException {
        // 查询酒店信息
        Hotel hotel = hotelService.getById(38609L);
        System.out.println(hotel);
        // 转换文档类型
        HotelDoc hotelDoc = new HotelDoc(hotel);

        // 1、创建对象
        IndexRequest request = new IndexRequest("hotel").id(hotelDoc.getId().toString());
        // 2、设置json文档
        request.source(JSON.toJSONString(hotelDoc),XContentType.JSON);
        // 3、发送文档
        client.index(request,RequestOptions.DEFAULT);
    }

    /**
     * 根据id查询文档信息
     */
    @Test
    void testGetDocumentById() throws IOException {
        // 1、准备request
        GetRequest request = new GetRequest("hotel", "38609");
        // 2、发送请求，得到响应
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        // 3、解析响应结过
        String sourceAsString = response.getSourceAsString();
        // 4、返回对象类型
        HotelDoc hotelDoc = JSON.parseObject(sourceAsString, HotelDoc.class);
        System.out.println("查询文档的结果："+hotelDoc);
    }

    /**
     * 根据id修改代码
     */
    @Test
    void upDaupdateById() throws IOException {
        // 1、准备req
        UpdateRequest request = new UpdateRequest("hotel", "38609");
        // 2、准备修改的参数
        request.doc(
                "price","300",
                "city","广州"
        );
        // 3、发送请求
        client.update(request, RequestOptions.DEFAULT);
    }

    /**
     * 删除文档
     */
    @Test
    void testDelectById() throws IOException {
        // 1、获取req
        DeleteRequest request = new DeleteRequest("hotel", "38609");
        // 2、发送请求
        client.delete(request, RequestOptions.DEFAULT);
    }

    /**
     * 批量导入数据
     */
    @Test
    void testBulkRequest() throws IOException {
        // 调用MP插件的查询所有数据
        List<Hotel> hotelList = hotelService.list();
        // 1、创建request
        BulkRequest request = new BulkRequest();
        // 2、封装参数,一起提交
        for (Hotel hotel : hotelList) {
            // 转换类型
            HotelDoc hotelDoc = new HotelDoc(hotel);
            // 创建新增文档的对象信息
            request.add(new IndexRequest("hotel")
                    .id(hotelDoc.getId().toString())
                    .source(JSON.toJSONString(hotelDoc),XContentType.JSON));
        }
        // 3、发送请求
        client.bulk(request, RequestOptions.DEFAULT);
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
