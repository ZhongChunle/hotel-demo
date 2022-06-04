package cn.itcast.hotel.pojo;

import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * 项目名称：hotel-demo
 * 描述：查询分页放回的数据
 *
 * @author zhong
 * @date 2022-06-04 13:17
 */
@Data
@ToString
public class PageResult {
    /**
     * 返回的条数
     */
    private Long total;
    /**
     * 返回的分页数据
     */
    private List<HotelDoc> hotels;

    public PageResult() {

    }

    public PageResult(Long total, List<HotelDoc> hotels) {
        this.total = total;
        this.hotels = hotels;
    }
}
