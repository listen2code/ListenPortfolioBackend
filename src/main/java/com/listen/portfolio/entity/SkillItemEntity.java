package com.listen.portfolio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * SkillItemEntity（MyBatis-Plus 实体类）。
 * 
 * 映射 skill_items 表，支持多语言扩展字段。
 */
@TableName("skill_items")
public class SkillItemEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long skillId;

    private String itemName;

    private String itemNameZh;

    private String itemNameJa;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSkillId() {
        return skillId;
    }

    public void setSkillId(Long skillId) {
        this.skillId = skillId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemNameZh() {
        return itemNameZh;
    }

    public void setItemNameZh(String itemNameZh) {
        this.itemNameZh = itemNameZh;
    }

    public String getItemNameJa() {
        return itemNameJa;
    }

    public void setItemNameJa(String itemNameJa) {
        this.itemNameJa = itemNameJa;
    }
}