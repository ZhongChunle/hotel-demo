package cn.itcast.hotel.webs;

import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.impl.HotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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

    /**
     * 聚合查询所有的酒店信息，包括限定查询
     * @param params
     * @return
     */
    @PostMapping("/filters")
    public Map<String, List<String>> getFiltered(@RequestBody RequestParams params){
        return hotelService.filters(params);
    }

    /**
     * 前端页面请求完成关键字查询
     * @param key
     * @return
     */
    @GetMapping("/suggestion")
    public List<String> getSuggestion(@RequestParam("key") String key){
        return hotelService.getSuggestion(key);
    }
}
