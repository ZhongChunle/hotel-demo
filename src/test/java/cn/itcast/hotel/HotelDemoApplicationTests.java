package cn.itcast.hotel;

import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.impl.HotelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
class HotelDemoApplicationTests {
    @Autowired
    HotelService service;

    @Test
    void contextLoads() {
        RequestParams requestParams = new RequestParams();
        requestParams.setPage(1);
        requestParams.setSize(10);
        requestParams.setKey("外滩");
        requestParams.setMaxPrice(600);
        requestParams.setMinPrice(300);
        Map<String, List<String>> filters = service.filters(requestParams);
        System.out.println(filters);
    }

}
