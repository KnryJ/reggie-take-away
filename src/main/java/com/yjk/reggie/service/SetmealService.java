package com.yjk.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yjk.reggie.dto.SetmealDto;
import com.yjk.reggie.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {

    /**
     * 新增套餐，同时保存套餐和菜品的关联关系
     * @param setmealDto
     */
    public void saveWithDish(SetmealDto setmealDto);

    /**
     * 删除套餐，同时删除套餐和菜品的关联数据
     * @param ids
     */
    public void removeWithDish(List<Long> ids);

    /**
     * 根据 id 查询套餐信息以及绑定的菜品
     * @param id
     */
    public SetmealDto getByIdWithDish(Long id);

    /**
     * 修改套餐和套餐关联的菜品信息
     * @param setmealDto
     */
    public void updateWithDish(SetmealDto setmealDto);
}
