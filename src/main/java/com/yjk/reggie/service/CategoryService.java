package com.yjk.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yjk.reggie.entity.Category;

public interface CategoryService extends IService<Category> {

    /**
     * 根据id删除
     * @param id
     */
    public void remove(Long id);
}
