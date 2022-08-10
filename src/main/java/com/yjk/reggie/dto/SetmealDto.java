package com.yjk.reggie.dto;

import com.yjk.reggie.entity.Setmeal;
import com.yjk.reggie.entity.SetmealDish;
import lombok.Data;
import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}
