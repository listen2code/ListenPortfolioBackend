package com.listen.portfolio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * LanguageEntity（MyBatis-Plus 实体类）。
 * 
 * 映射 languages 表。
 */
@TableName("languages")
public class LanguageEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;
    private String nameZh;
    private String nameJa;

    private String level;
    private String levelZh;
    private String levelJa;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getNameZh() {
        return nameZh;
    }

    public void setNameZh(String nameZh) {
        this.nameZh = nameZh;
    }

    public String getNameJa() {
        return nameJa;
    }

    public void setNameJa(String nameJa) {
        this.nameJa = nameJa;
    }

    public String getLevelZh() {
        return levelZh;
    }

    public void setLevelZh(String levelZh) {
        this.levelZh = levelZh;
    }

    public String getLevelJa() {
        return levelJa;
    }

    public void setLevelJa(String levelJa) {
        this.levelJa = levelJa;
    }
}
