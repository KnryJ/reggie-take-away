package com.yjk.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yjk.reggie.common.CustomException;
import com.yjk.reggie.dto.DishDto;
import com.yjk.reggie.entity.Dish;
import com.yjk.reggie.entity.DishFlavor;
import com.yjk.reggie.entity.SetmealDish;
import com.yjk.reggie.mapper.DishMapper;
import com.yjk.reggie.service.DishFlavorService;
import com.yjk.reggie.service.DishService;
import com.yjk.reggie.service.SetmealDishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * 新增菜品，同时保存对用的口味数据
     * @param dishDto
     */
    @Override
    @Transactional      //事务控制
    public void saveWithFlavor(DishDto dishDto) {
        //保存菜品的基本信息到 dish 表
        save(dishDto);

        Long dishId = dishDto.getId();
        List<DishFlavor> flavors = dishDto.getFlavors();
        if(flavors != null){
            flavors = flavors.stream()
                    .map((item) -> {
                        item.setDishId(dishId);
                        return item;
                    })
                    .collect(Collectors.toList());
        }

        //保存菜品口味数据到菜品口味表
        dishFlavorService.saveBatch(flavors);
    }

    /**
     * 根据 id 查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @Override
    public DishDto getByIdWithFlavor(long id) {
        //查询菜品基本信息
        Dish dish = getById(id);

        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish, dishDto);

        //查询当前菜品对应的口味信息，从dish_flavor表查询
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(DishFlavor::getDishId, id);
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);
        dishDto.setFlavors(flavors);

        return dishDto;
    }

    /**
     * 更新菜品信息，同时更新口味信息
     * @param dishDto
     */
    @Override
    @Transactional
    public void updateWithFlavor(DishDto dishDto) {
        //更新 dish 表的基本信息
        updateById(dishDto);

        //清理当前菜品对应口味数据 —— dish_flavor 表中的 delete 操作
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, dishDto.getId());
        dishFlavorService.remove(queryWrapper);

        //添加当前提交过来的口味数据 —— dish_flavor 表中的 insert 操作
        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors = flavors.stream().map( item -> {
            item.setDishId(dishDto.getId());
            return item;
        } ).collect(Collectors.toList());

        dishFlavorService.saveBatch(flavors);
    }

    /**
     * 删除菜品以及删除对应的口味
     * @param ids
     */
    @Override
    @Transactional
    public void removeWithFlavor(List<Long> ids) {

        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SetmealDish::getDishId, ids);
        List<SetmealDish> list = setmealDishService.list(queryWrapper);

        if(!list.isEmpty()){
            throw new CustomException("菜品已绑定套餐,无法删除");
        }

        LambdaQueryWrapper<Dish> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.in(Dish::getId, ids);
        queryWrapper1.eq(Dish::getStatus, 1);
        int count = count(queryWrapper1);

        if(count > 0){
            throw new CustomException("菜品正在售卖，无法删除");
        }

        //删除菜品
        removeByIds(ids);

        //删除菜品关联的口味
        LambdaQueryWrapper<DishFlavor> queryWrapper2 = new LambdaQueryWrapper<>();
        queryWrapper2.in(DishFlavor::getDishId, ids);

        dishFlavorService.remove(queryWrapper2);
    }
}
