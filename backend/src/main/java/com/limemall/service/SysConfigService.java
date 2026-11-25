package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.limemall.entity.SysConfig;
import com.limemall.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统参数配置服务：键值读取与更新（后台可视化配置的存储层）。
 */
@Service
@RequiredArgsConstructor
public class SysConfigService {

    private final SysConfigMapper configMapper;

    /** 全部配置（key → value） */
    public Map<String, String> getAll() {
        Map<String, String> result = new HashMap<>();
        configMapper.selectList(null)
                .forEach(item -> result.put(item.getCfgKey(), item.getCfgValue()));
        return result;
    }

    public String get(String key) {
        SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getCfgKey, key));
        return config == null ? null : config.getCfgValue();
    }

    /** 写入/更新配置项 */
    public void put(String key, String value, String remark) {
        SysConfig exists = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getCfgKey, key));
        if (exists == null) {
            SysConfig config = new SysConfig();
            config.setCfgKey(key);
            config.setCfgValue(value);
            config.setRemark(remark);
            configMapper.insert(config);
        } else {
            configMapper.update(null, new LambdaUpdateWrapper<SysConfig>()
                    .set(SysConfig::getCfgValue, value)
                    .set(remark != null, SysConfig::getRemark, remark)
                    .eq(SysConfig::getId, exists.getId()));
        }
    }
}
