package cn.itcast.hotel.webs;

import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.impl.HotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 项目名称：hotel-demo
 * 描述：请求控制器
 *
 * @author zhong
 * @date 2022-06-04 13:20
 */
@RestController
@RequestMapping("/hotel")
public class HotelController {
    /**
     * 注入业务层
     */
    @Autowired
    private HotelService hotelService;

    /**
     * 请求查询分页信息并返回
     * @return
     */
    @PostMapping("/list")
    public PageResult search(@RequestBody RequestParams params) throws IOException {
        System.out.println("接收到请求数据："+params);
        return hotelService.search(params);
    }
}
