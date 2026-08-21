package com.listen.portfolio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.List;

/**
 * SkillEntity（MyBatis-Plus 实体类）。
 * 
 * 映射 skills 表。
 */
@TableName("skills")
public class SkillEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String category;
    private String categoryZh;
    private String categoryJa;
    private Integer score;

    @TableField(exist = false)
    private List<String> items;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryZh() {
        return categoryZh;
    }

    public void setCategoryZh(String categoryZh) {
        this.categoryZh = categoryZh;
    }

    public String getCategoryJa() {
        return categoryJa;
    }

    public void setCategoryJa(String categoryJa) {
        this.categoryJa = categoryJa;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }
}
