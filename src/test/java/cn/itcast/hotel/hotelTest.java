package cn.itcast.hotel;

import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.impl.HotelService;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 项目名称：hotel-demo
 * 描述：测试
 *
 * @author zhong
 * @date 2022-06-04 13:52
 */
public class hotelTest {
    @Autowired
    RestHighLevelClient client;

    @Autowired
    private HotelService hotelService;

    @Test
    void test(){
        RequestParams requestParams = new RequestParams();
        requestParams.setKey("如家");
        requestParams.setPage(1);
        requestParams.setSize(5);

        PageResult search = hotelService.search(requestParams);
        System.out.println(search);
    }
}
