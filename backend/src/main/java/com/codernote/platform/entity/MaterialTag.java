package com.codernote.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("material_tag")
public class MaterialTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("material_id")
    private Long materialId;

    @TableField("tag_id")
    private Long tagId;
}
