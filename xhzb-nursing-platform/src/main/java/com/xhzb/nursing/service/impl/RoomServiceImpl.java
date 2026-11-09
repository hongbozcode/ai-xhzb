package com.xhzb.nursing.service.impl;

import com.xhzb.nursing.mapper.AdmissionMapper;
import com.xhzb.nursing.domain.vo.CheckInRoomVo;
import com.xhzb.common.exception.ServiceException;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhzb.common.constant.CacheConstants;
import com.xhzb.nursing.domain.Room;
import com.xhzb.nursing.domain.vo.DeviceDataVo;
import com.xhzb.nursing.domain.vo.RoomDeviceVo;
import com.xhzb.nursing.mapper.RoomMapper;
import com.xhzb.nursing.service.IRoomService;
import com.xhzb.nursing.domain.vo.RoomVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 房间Service业务层处理
 *
 * @author ruoyi
 * @date 2025-03-28
 */
@Service
public class RoomServiceImpl extends ServiceImpl<RoomMapper, Room> implements IRoomService {
    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查询房间
     *
     * @param id 房间主键
     * @return 房间
     */
    @Override
    public Room selectRoomById(Long id) {
        return getById(id);
    }

    /**
     * 查询房间列表
     *
     * @param room 房间
     * @return 房间
     */
    @Override
    public List<Room> selectRoomList(Room room) {
        return roomMapper.selectRoomList(room);
    }

    /**
     * 新增房间
     *
     * @param room 房间
     * @return 结果
     */
    @Override
    public int insertRoom(Room room) {
        return save(room) ? 1 : 0;
    }

    /**
     * 修改房间
     *
     * @param room 房间
     * @return 结果
     */
    @Override
    public int updateRoom(Room room) {
        return updateById(room) ? 1 : 0;
    }

    /**
     * 批量删除房间
     *
     * @param ids 需要删除的房间主键
     * @return 结果
     */
    @Override
    public int deleteRoomByIds(Long[] ids) {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 根据楼层 id 获取房间视图对象列表
     *
     * @param floorId
     * @return
     */
    @Override
    public List<RoomVo> getRoomsByFloorId(Long floorId) {
        return roomMapper.selectByFloorId(floorId);
    }

    @Override
    public List<RoomVo> getRoomsWithDeviceByFloorId(Long floorId) {
        List<RoomVo> rooms = roomMapper.selectByFloorIdWithDevices(floorId).stream()
                .filter(room -> !room.getDeviceVos().isEmpty()
                        || room.getBedVoList().stream().anyMatch(bed -> !bed.getDeviceVos().isEmpty()))
                .toList();

        Map<String, List<RoomDeviceVo>> devicesById = rooms.stream()
                .flatMap(room -> Stream.concat(room.getDeviceVos().stream(),
                        room.getBedVoList().stream().flatMap(bed -> bed.getDeviceVos().stream())))
                .filter(device -> StringUtils.hasText(device.getIotId()))
                .collect(Collectors.groupingBy(RoomDeviceVo::getIotId, LinkedHashMap::new, Collectors.toList()));
        if (!devicesById.isEmpty()) {
            List<String> deviceIds = List.copyOf(devicesById.keySet());
            List<String> cachedReports = stringRedisTemplate.<String, String>opsForHash()
                    .multiGet(CacheConstants.IOT_DATA_LIST, deviceIds);
            for (int i = 0; i < deviceIds.size(); i++) {
                String cachedReport = cachedReports.get(i);
                if (StringUtils.hasText(cachedReport)) {
                    List<DeviceDataVo> data = JSON.parseArray(cachedReport, DeviceDataVo.class);
                    devicesById.get(deviceIds.get(i)).forEach(device -> device.setDeviceDataVos(data));
                }
            }
        }
        return rooms;
    }


    /**
     * 获取所有房间（负责老人）
     *
     * @param floorId
     * @return
     */
    @Override
    public List<RoomVo> getRoomsWithNurByFloorId(Long floorId) {
        return roomMapper.selectByFloorIdWithNur(floorId);
    }

    @Autowired
    private AdmissionMapper admissionMapper;

    /** 根据房间类型名称关联房型价格。 */
    @Override
    public CheckInRoomVo getCheckInRoom(Long id) {
        CheckInRoomVo room = admissionMapper.selectRoom(id);
        if (room == null) { throw new ServiceException("房间或房型信息不存在"); }
        return room;
    }

}
