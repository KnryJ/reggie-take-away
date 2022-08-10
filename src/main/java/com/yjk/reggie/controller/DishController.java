package com.yjk.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yjk.reggie.common.R;
import com.yjk.reggie.dto.DishDto;
import com.yjk.reggie.entity.Category;
import com.yjk.reggie.entity.Dish;
import com.yjk.reggie.entity.DishFlavor;
import com.yjk.reggie.service.CategoryService;
import com.yjk.reggie.service.DishFlavorService;
import com.yjk.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());

        dishService.saveWithFlavor(dishDto);

        //清楚所有菜品缓存数据
//        Set keys = redisTemplate.keys("dish_*");
//        redisTemplate.delete(keys);

        //精确清理
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("新增菜品成功");
    }

    /**
     * 分页
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(Integer page, Integer pageSize, String name){

        Page<Dish> pageInfo = new Page<>(page, pageSize);
        Page<DishDto> dishDtoPage = new Page<>();

        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name != null, Dish::getName, name);
        queryWrapper.orderByDesc(Dish::getUpdateTime);

        //执行分页查询
        dishService.page(pageInfo, queryWrapper);

        //对象拷贝
        BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");

        List<Dish> records = pageInfo.getRecords();
        List<DishDto> list = records.stream().map((item) -> {
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item, dishDto);
            Long categoryId = item.getCategoryId(); //分类id

            Category category = categoryService.getById(categoryId);

            if(category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }

            return dishDto;
        }).collect(Collectors.toList());

        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);
    }

    /**
     * 根据 id 查询菜品信息和口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable long id){

        DishDto dishDto = dishService.getByIdWithFlavor(id);

        return R.success(dishDto);
    }

    /**
     * 修改菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> updateDish(@RequestBody DishDto dishDto){
        dishService.updateWithFlavor(dishDto);

        //清楚所有菜品缓存数据
//        Set keys = redisTemplate.keys("dish_*");
//        redisTemplate.delete(keys);

        //精确清理
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("修改菜品成功");
    }

//    /**
//     * 根据条件查询菜品数据
//     * @param dish
//     * @return
//     */
//    @GetMapping("/list")
//    public R<List<Dish>> list(Dish dish){
//
//        //添加查询条件
//        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
//        queryWrapper.eq(Dish::getStatus, 1);
//
//        //添加排序条件
//        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
//        List<Dish> list = dishService.list(queryWrapper);
//
//        return R.success(list);
//    }

    /**
     * 根据条件查询菜品数据
     * @param dish
     * @return
     */
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){

        List<DishDto> dishDtoList =null;
        //动态构造 key
        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();

        //先从 redis 中获取缓存数据
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);

        if(dishDtoList != null){
            //如果存在，直接返回，无需查询数据库
            return R.success(dishDtoList);
        }

        //如果不存在，就查询数据库，将查询到的菜品缓存到 redis 中
        //添加查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
        queryWrapper.eq(Dish::getStatus, 1);

        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        List<Dish> list = dishService.list(queryWrapper);

        dishDtoList = list.stream().map(item -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);

            Long categoryId = item.getCategoryId();

            Category category = categoryService.getById(categoryId);
            if(category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }

            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> dishFlavorQueryWrapper = new LambdaQueryWrapper<>();
            dishFlavorQueryWrapper.eq(DishFlavor::getDishId, dishId);
            List<DishFlavor> flavors = dishFlavorService.list(dishFlavorQueryWrapper);
            dishDto.setFlavors(flavors);

            return dishDto;
        }).collect(Collectors.toList());

        //将查询到的菜品缓存到 redis 中
        redisTemplate.opsForValue().set(key, dishDtoList, 60, TimeUnit.MINUTES);

        return R.success(dishDtoList);
    }

    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){

        dishService.removeWithFlavor(ids);

        return R.success("删除菜品成功");
    }

    /**
     * 改变菜品状态
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> editStatus(@PathVariable int status, @RequestParam List<Long> ids){

        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Dish::getId, ids);
        List<Dish> list = dishService.list(queryWrapper);

        list.stream().map(item -> {
            item.setStatus(status);
            return item;
        }).collect(Collectors.toList());

        dishService.updateBatchById(list);

        return R.success("菜品状态转换成功");

    }
}
