package com.yjk.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yjk.reggie.dto.DishDto;
import com.yjk.reggie.entity.Dish;

import java.util.List;

public interface DishService extends IService<Dish> {

    /**
     * 新增菜品，同时插入菜品对应的口味数据，需要操作两张表，dish， dish_flavor
     * @param dishDto
     */
    public void saveWithFlavor(DishDto dishDto);

    /**
     * 根据 id 查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    public DishDto getByIdWithFlavor(long id);

    /**
     * 更新菜品信息，同时更新口味信息
     * @param dishDto
     */
    public void updateWithFlavor(DishDto dishDto);

    /**
     * 删除菜品以及删除对应的口味
     * @param ids
     */
    public void removeWithFlavor(List<Long> ids);
}
