package com.xhzb.nursing.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.xhzb.nursing.domain.*;
import com.xhzb.nursing.domain.vo.*;

/** 入住办理关联查询及并发控制。 */
@Mapper
public interface AdmissionMapper {
    /** 锁定同一身份证的评估，串行处理同一老人入住。 */
    List<HealthAssessment> lockAssessments(@Param("idCard") String idCard);
    /** 当前读已入住记录，避免事务快照遗漏刚提交的入住。 */
    List<CheckIn> lockActiveCheckIns(@Param("idCard") String idCard);
    /** 查询楼层房间床位级联数据。 */
    List<CascaderVo> selectBedTree(@Param("status") Integer status);
    /** 查询房间及房型价格。 */
    CheckInRoomVo selectRoom(@Param("id") Long id);
}
