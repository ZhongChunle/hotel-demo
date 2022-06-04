package cn.itcast.hotel.pojo;

import lombok.Data;
import lombok.ToString;

/**
 * 项目名称：hotel-demo
 * 描述：请求参数实体类
 *
 * @author zhong
 * @date 2022-06-04 13:15
 */
@Data
@ToString
public class RequestParams {
    /**
     * 查询字段
     */
    private String key;
    /**
     * 当前的页数
     */
    private Integer page;
    /**
     * 查询数据的条数
     */
    private Integer size;
    /**
     * 排序字段
     */
    private String sortBy;

    // 下面是新增的过滤条件参数
    private String city;
    private String brand;
    private String starName;
    private Integer minPrice;
    private Integer maxPrice;
    private String location;
}
