package com.yjk.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yjk.reggie.common.CustomException;
import com.yjk.reggie.entity.Category;
import com.yjk.reggie.entity.Dish;
import com.yjk.reggie.entity.Setmeal;
import com.yjk.reggie.mapper.CategoryMapper;
import com.yjk.reggie.service.CategoryService;
import com.yjk.reggie.service.DishService;
import com.yjk.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private DishService dishService;
    @Autowired
    private SetmealService setmealService;

    /**
     * 根据 id 删除分类，删除之前进行判断
     * @param id
     */
    @Override
    public void remove(Long id) {
        //查询当前分类是否关联了菜品，如果已经关联，抛出一个业务异常
        LambdaQueryWrapper<Dish> dishQueryWrapper = new LambdaQueryWrapper<>();
        dishQueryWrapper.eq(Dish::getCategoryId, id);
        int dishCount = dishService.count(dishQueryWrapper);

        if(dishCount > 0){
            //已经关联菜品，抛出一个业务异常
            throw new CustomException("当前分类已关联菜品，无法删除");
        }

        //查询当前分类是否已经关联了套餐，如果已经关联，抛出一个业务异常
        LambdaQueryWrapper<Setmeal> setMealQueryMapper = new LambdaQueryWrapper<>();
        setMealQueryMapper.eq(Setmeal::getCategoryId, id);
        int mealCount = setmealService.count(setMealQueryMapper);

        if(mealCount > 0){
            //已经关联套餐，抛出一个业务异常
            throw new CustomException("当前分类已关联套餐，无法删除");
        }

        //正常进行删除
        super.removeById(id);
    }
}
