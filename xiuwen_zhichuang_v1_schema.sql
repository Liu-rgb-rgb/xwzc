/*
 Navicat / DataGrip 可直接执行的 MySQL 建库脚本
 项目：绣纹智创——广绣 AI 纹样设计与文创定制平台
 版本：第一版 V1
 说明：
 1. 全库、全表统一使用 utf8mb4 / utf8mb4_general_ci，避免中文乱码。
 2. 所有表均使用 InnoDB，支持事务和外键。
 3. 所有表均设置自增主键 id。
 4. 所有字段均带 COMMENT 说明。
 5. 脚本包含 DROP TABLE IF EXISTS，重复执行会清空并重建表结构，请不要在有正式数据的库中直接重复执行。
*/

CREATE DATABASE IF NOT EXISTS `xwzc`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `xwzc`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `home_recommend`;
DROP TABLE IF EXISTS `home_banner`;
DROP TABLE IF EXISTS `file_resource`;
DROP TABLE IF EXISTS `learning_resource`;
DROP TABLE IF EXISTS `course`;
DROP TABLE IF EXISTS `course_category`;
DROP TABLE IF EXISTS `order_item`;
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `cart_item`;
DROP TABLE IF EXISTS `custom_design`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `product_category`;
DROP TABLE IF EXISTS `prompt_template`;
DROP TABLE IF EXISTS `pattern`;
DROP TABLE IF EXISTS `pattern_generation`;
DROP TABLE IF EXISTS `message_notice`;
DROP TABLE IF EXISTS `user_address`;
DROP TABLE IF EXISTS `shop_info`;
DROP TABLE IF EXISTS `user`;

SET FOREIGN_KEY_CHECKS = 1;

/*==============================================================*/
/* 1. 用户账号表：普通用户、商家管理员、平台管理员统一存储              */
/*==============================================================*/
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID，自增主键',
    `username` VARCHAR(50) NOT NULL COMMENT '登录账号，系统内唯一',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '加密后的登录密码，不保存明文密码',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '用户昵称',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号，可用于联系或登录',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱地址',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像图片URL',
    `gender` VARCHAR(20) DEFAULT NULL COMMENT '性别：UNKNOWN未知 MALE男 FEMALE女',
    `birthday` DATE DEFAULT NULL COMMENT '生日',
    `intro` VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
    `role` VARCHAR(30) NOT NULL DEFAULT 'USER' COMMENT '用户角色：USER普通用户 MERCHANT_ADMIN商家管理员 ADMIN平台管理员',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：1正常 0禁用',
    `last_login_at` DATETIME DEFAULT NULL COMMENT '最近登录时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    UNIQUE KEY `uk_user_phone` (`phone`),
    KEY `idx_user_role` (`role`),
    KEY `idx_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户账号表';

/*==============================================================*/
/* 2. 店铺信息表：单商家平台的店铺资料                             */
/*==============================================================*/
CREATE TABLE `shop_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '店铺ID，自增主键',
    `owner_user_id` BIGINT DEFAULT NULL COMMENT '店铺负责人用户ID，对应user.id',
    `shop_name` VARCHAR(100) NOT NULL COMMENT '店铺名称',
    `logo` VARCHAR(255) DEFAULT NULL COMMENT '店铺LOGO图片URL',
    `slogan` VARCHAR(255) DEFAULT NULL COMMENT '店铺宣传语',
    `contact_name` VARCHAR(50) DEFAULT NULL COMMENT '联系人姓名',
    `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '联系邮箱',
    `address` VARCHAR(255) DEFAULT NULL COMMENT '店铺地址',
    `description` TEXT DEFAULT NULL COMMENT '店铺介绍',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '店铺状态：1正常 0关闭',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_shop_owner_user_id` (`owner_user_id`),
    CONSTRAINT `fk_shop_owner_user` FOREIGN KEY (`owner_user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='店铺信息表';

/*==============================================================*/
/* 3. 用户收货地址表                                               */
/*==============================================================*/
CREATE TABLE `user_address` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '地址ID，自增主键',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID，对应user.id',
    `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    `receiver_phone` VARCHAR(20) NOT NULL COMMENT '收货人联系电话',
    `province` VARCHAR(50) DEFAULT NULL COMMENT '省份',
    `city` VARCHAR(50) DEFAULT NULL COMMENT '城市',
    `district` VARCHAR(50) DEFAULT NULL COMMENT '区县',
    `detail_address` VARCHAR(255) NOT NULL COMMENT '详细地址',
    `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认地址：1是 0否',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '地址状态：1正常 0删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_address_user_id` (`user_id`),
    CONSTRAINT `fk_address_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户收货地址表';

/*==============================================================*/
/* 4. 消息通知表：站内消息、订单提醒、系统通知                         */
/*==============================================================*/
CREATE TABLE `message_notice` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID，自增主键',
    `user_id` BIGINT DEFAULT NULL COMMENT '接收用户ID，为NULL表示全体用户消息',
    `title` VARCHAR(100) NOT NULL COMMENT '消息标题',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `notice_type` VARCHAR(30) NOT NULL DEFAULT 'SYSTEM' COMMENT '消息类型：SYSTEM系统 ORDER订单 ACTIVITY活动 COURSE课程 CUSTOM定制',
    `related_type` VARCHAR(30) DEFAULT NULL COMMENT '关联业务类型：ORDER订单 PATTERN纹样 PRODUCT商品 COURSE课程 RESOURCE资源',
    `related_id` BIGINT DEFAULT NULL COMMENT '关联业务ID',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：1已读 0未读',
    `read_at` DATETIME DEFAULT NULL COMMENT '读取时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '消息状态：1正常 0删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_notice_user_id` (`user_id`),
    KEY `idx_notice_is_read` (`is_read`),
    KEY `idx_notice_type` (`notice_type`),
    CONSTRAINT `fk_notice_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息通知表';

/*==============================================================*/
/* 5. AI纹样生成记录表                                             */
/*==============================================================*/
CREATE TABLE `pattern_generation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '生成记录ID，自增主键',
    `user_id` BIGINT NOT NULL COMMENT '发起生成的用户ID，对应user.id',
    `keyword` VARCHAR(255) NOT NULL COMMENT '用户输入关键词',
    `style` VARCHAR(50) DEFAULT NULL COMMENT '纹样风格：classic广绣经典 new_chinese新中式 embroidery刺绣风等',
    `elements` JSON DEFAULT NULL COMMENT '纹样元素数组，例如牡丹、凤凰、祥云等',
    `color_theme` VARCHAR(50) DEFAULT NULL COMMENT '颜色主题，例如国风雅韵、富贵华彩等',
    `usage_scene` VARCHAR(50) DEFAULT NULL COMMENT '应用场景：product文创商品 clothing服饰 home家居等',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '用户补充描述',
    `reference_image_url` VARCHAR(255) DEFAULT NULL COMMENT '参考图URL',
    `prompt_text` TEXT DEFAULT NULL COMMENT '最终发送给AI模型的提示词',
    `generate_count` INT NOT NULL DEFAULT 1 COMMENT '本次生成图片数量',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '生成状态：PENDING生成中 SUCCESS成功 FAILED失败',
    `error_message` VARCHAR(500) DEFAULT NULL COMMENT '生成失败原因',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_generation_user_id` (`user_id`),
    KEY `idx_generation_status` (`status`),
    CONSTRAINT `fk_generation_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI纹样生成记录表';

/*==============================================================*/
/* 6. 纹样表：用户生成、保存、收藏、推荐的纹样                         */
/*==============================================================*/
CREATE TABLE `pattern` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '纹样ID，自增主键',
    `generation_id` BIGINT DEFAULT NULL COMMENT 'AI生成记录ID，对应pattern_generation.id',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID，对应user.id',
    `title` VARCHAR(100) DEFAULT NULL COMMENT '纹样标题',
    `image_url` VARCHAR(255) NOT NULL COMMENT '纹样原图URL',
    `thumbnail_url` VARCHAR(255) DEFAULT NULL COMMENT '纹样缩略图URL',
    `keyword` VARCHAR(255) DEFAULT NULL COMMENT '生成关键词快照',
    `style` VARCHAR(50) DEFAULT NULL COMMENT '纹样风格',
    `elements` JSON DEFAULT NULL COMMENT '纹样元素数组快照',
    `color_theme` VARCHAR(50) DEFAULT NULL COMMENT '颜色主题',
    `usage_scene` VARCHAR(50) DEFAULT NULL COMMENT '应用场景',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '纹样说明',
    `is_saved` TINYINT NOT NULL DEFAULT 1 COMMENT '是否已保存到我的纹样：1是 0否',
    `is_favorite` TINYINT NOT NULL DEFAULT 0 COMMENT '是否收藏：1是 0否',
    `is_recommend` TINYINT NOT NULL DEFAULT 0 COMMENT '是否首页推荐：1是 0否',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞或收藏热度数量',
    `use_count` INT NOT NULL DEFAULT 0 COMMENT '被应用到商品定制的次数',
    `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL正常 HIDDEN隐藏 DELETED删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_pattern_generation_id` (`generation_id`),
    KEY `idx_pattern_user_id` (`user_id`),
    KEY `idx_pattern_recommend` (`is_recommend`),
    KEY `idx_pattern_status` (`status`),
    CONSTRAINT `fk_pattern_generation` FOREIGN KEY (`generation_id`) REFERENCES `pattern_generation` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_pattern_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='纹样表';

/*==============================================================*/
/* 7. AI提示词模板表                                               */
/*==============================================================*/
CREATE TABLE `prompt_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '模板ID，自增主键',
    `name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `style` VARCHAR(50) NOT NULL COMMENT '适用风格类型',
    `usage_scene` VARCHAR(50) DEFAULT NULL COMMENT '适用场景',
    `color_theme` VARCHAR(50) DEFAULT NULL COMMENT '适用颜色主题',
    `template_text` TEXT NOT NULL COMMENT '提示词模板内容，可包含{keyword}、{colorTheme}等占位符',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '模板状态：1启用 0禁用',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_prompt_style` (`style`),
    KEY `idx_prompt_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI提示词模板表';

/*==============================================================*/
/* 8. 文创商品分类表                                               */
/*==============================================================*/
CREATE TABLE `product_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品分类ID，自增主键',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父分类ID，为NULL表示一级分类',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称，例如帆布袋、明信片、丝巾等',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT '分类图标URL',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '分类状态：1启用 0禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_category_parent_id` (`parent_id`),
    KEY `idx_product_category_status` (`status`),
    CONSTRAINT `fk_product_category_parent` FOREIGN KEY (`parent_id`) REFERENCES `product_category` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文创商品分类表';

/*==============================================================*/
/* 9. 文创商品表                                                   */
/*==============================================================*/
CREATE TABLE `product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID，自增主键',
    `category_id` BIGINT DEFAULT NULL COMMENT '商品分类ID，对应product_category.id',
    `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `subtitle` VARCHAR(150) DEFAULT NULL COMMENT '商品副标题或卖点描述',
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '商品售价',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '商品库存数量',
    `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '商品封面图URL',
    `mockup_image` VARCHAR(255) DEFAULT NULL COMMENT '商品定制预览底图URL',
    `description` TEXT DEFAULT NULL COMMENT '商品详情介绍',
    `is_customizable` TINYINT NOT NULL DEFAULT 1 COMMENT '是否支持纹样定制：1支持 0不支持',
    `is_recommend` TINYINT NOT NULL DEFAULT 0 COMMENT '是否首页推荐：1是 0否',
    `sales_count` INT NOT NULL DEFAULT 0 COMMENT '销量统计',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ON_SALE' COMMENT '商品状态：ON_SALE上架 OFF_SALE下架 SOLD_OUT售罄 DRAFT草稿',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_category_id` (`category_id`),
    KEY `idx_product_status` (`status`),
    KEY `idx_product_recommend` (`is_recommend`),
    CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `product_category` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文创商品表';

/*==============================================================*/
/* 10. 商品定制设计表：纹样套用到商品后的预览记录                    */
/*==============================================================*/
CREATE TABLE `custom_design` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '定制设计ID，自增主键',
    `user_id` BIGINT NOT NULL COMMENT '创建定制设计的用户ID，对应user.id',
    `product_id` BIGINT DEFAULT NULL COMMENT '定制商品ID，对应product.id',
    `pattern_id` BIGINT DEFAULT NULL COMMENT '使用的纹样ID，对应pattern.id',
    `preview_image_url` VARCHAR(255) NOT NULL COMMENT '商品定制预览图URL',
    `design_config` JSON DEFAULT NULL COMMENT '设计参数JSON，例如位置、尺寸、旋转角度等',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '用户定制备注',
    `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL正常 DELETED删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_design_user_id` (`user_id`),
    KEY `idx_design_product_id` (`product_id`),
    KEY `idx_design_pattern_id` (`pattern_id`),
    CONSTRAINT `fk_design_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_design_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_design_pattern` FOREIGN KEY (`pattern_id`) REFERENCES `pattern` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品定制设计表';

/*==============================================================*/
/* 11. 购物车表                                                    */
/*==============================================================*/
CREATE TABLE `cart_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '购物车项ID，自增主键',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID，对应user.id',
    `product_id` BIGINT NOT NULL COMMENT '商品ID，对应product.id',
    `pattern_id` BIGINT DEFAULT NULL COMMENT '选择的纹样ID，对应pattern.id，可为空',
    `custom_design_id` BIGINT DEFAULT NULL COMMENT '定制设计ID，对应custom_design.id，可为空',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '商品数量',
    `selected` TINYINT NOT NULL DEFAULT 1 COMMENT '是否选中结算：1选中 0未选中',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_cart_user_id` (`user_id`),
    KEY `idx_cart_product_id` (`product_id`),
    KEY `idx_cart_pattern_id` (`pattern_id`),
    KEY `idx_cart_design_id` (`custom_design_id`),
    CONSTRAINT `fk_cart_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_cart_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_cart_pattern` FOREIGN KEY (`pattern_id`) REFERENCES `pattern` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_cart_design` FOREIGN KEY (`custom_design_id`) REFERENCES `custom_design` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='购物车表';

/*==============================================================*/
/* 12. 订单主表                                                    */
/*==============================================================*/
CREATE TABLE `orders` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID，自增主键',
    `order_no` VARCHAR(50) NOT NULL COMMENT '订单编号，业务唯一',
    `user_id` BIGINT NOT NULL COMMENT '下单用户ID，对应user.id',
    `custom_design_id` BIGINT DEFAULT NULL COMMENT '订单关联的定制设计ID，对应custom_design.id',
    `total_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    `pay_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    `status` VARCHAR(30) NOT NULL DEFAULT 'WAIT_PAY' COMMENT '订单状态：WAIT_PAY待支付 WAIT_CONFIRM待接单 PRODUCING制作中 WAIT_DELIVERY待发货 DELIVERED已发货 COMPLETED已完成 CANCELLED已取消',
    `pay_status` VARCHAR(20) NOT NULL DEFAULT 'UNPAID' COMMENT '支付状态：UNPAID未支付 PAID已支付 REFUNDED已退款',
    `receiver_name` VARCHAR(50) DEFAULT NULL COMMENT '收货人姓名',
    `receiver_phone` VARCHAR(20) DEFAULT NULL COMMENT '收货人手机号',
    `receiver_address` VARCHAR(255) DEFAULT NULL COMMENT '完整收货地址',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '用户订单备注',
    `paid_at` DATETIME DEFAULT NULL COMMENT '支付时间',
    `confirmed_at` DATETIME DEFAULT NULL COMMENT '商家接单时间',
    `produced_at` DATETIME DEFAULT NULL COMMENT '制作完成时间',
    `shipped_at` DATETIME DEFAULT NULL COMMENT '发货时间',
    `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
    `cancelled_at` DATETIME DEFAULT NULL COMMENT '取消时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_orders_order_no` (`order_no`),
    KEY `idx_orders_user_id` (`user_id`),
    KEY `idx_orders_design_id` (`custom_design_id`),
    KEY `idx_orders_status` (`status`),
    CONSTRAINT `fk_orders_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_orders_design` FOREIGN KEY (`custom_design_id`) REFERENCES `custom_design` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单主表';

/*==============================================================*/
/* 13. 订单明细表                                                  */
/*==============================================================*/
CREATE TABLE `order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单明细ID，自增主键',
    `order_id` BIGINT NOT NULL COMMENT '所属订单ID，对应orders.id',
    `product_id` BIGINT DEFAULT NULL COMMENT '商品ID，对应product.id，商品删除后可为空',
    `custom_design_id` BIGINT DEFAULT NULL COMMENT '定制设计ID，对应custom_design.id',
    `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称快照',
    `product_image` VARCHAR(255) DEFAULT NULL COMMENT '商品图片快照URL',
    `pattern_id` BIGINT DEFAULT NULL COMMENT '纹样ID，对应pattern.id',
    `pattern_image_url` VARCHAR(255) DEFAULT NULL COMMENT '纹样图片快照URL',
    `preview_image_url` VARCHAR(255) DEFAULT NULL COMMENT '定制预览图快照URL',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '购买数量',
    `unit_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '商品单价',
    `total_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '明细小计金额',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_item_order_id` (`order_id`),
    KEY `idx_order_item_product_id` (`product_id`),
    KEY `idx_order_item_pattern_id` (`pattern_id`),
    KEY `idx_order_item_design_id` (`custom_design_id`),
    CONSTRAINT `fk_order_item_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_order_item_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_order_item_pattern` FOREIGN KEY (`pattern_id`) REFERENCES `pattern` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_order_item_design` FOREIGN KEY (`custom_design_id`) REFERENCES `custom_design` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单明细表';

/*==============================================================*/
/* 14. 非遗课程分类表                                              */
/*==============================================================*/
CREATE TABLE `course_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '课程分类ID，自增主键',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父分类ID，为NULL表示一级分类',
    `name` VARCHAR(50) NOT NULL COMMENT '课程分类名称',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '分类说明',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '分类状态：1启用 0禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_course_category_parent_id` (`parent_id`),
    KEY `idx_course_category_status` (`status`),
    CONSTRAINT `fk_course_category_parent` FOREIGN KEY (`parent_id`) REFERENCES `course_category` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='非遗课程分类表';

/*==============================================================*/
/* 15. 非遗课堂课程表                                              */
/*==============================================================*/
CREATE TABLE `course` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '课程ID，自增主键',
    `category_id` BIGINT DEFAULT NULL COMMENT '课程分类ID，对应course_category.id',
    `title` VARCHAR(100) NOT NULL COMMENT '课程标题',
    `subtitle` VARCHAR(150) DEFAULT NULL COMMENT '课程副标题',
    `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '课程封面图URL',
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '课程价格，0表示免费',
    `teacher_name` VARCHAR(50) DEFAULT NULL COMMENT '讲师名称',
    `duration` VARCHAR(50) DEFAULT NULL COMMENT '课程时长，例如30分钟、2课时',
    `difficulty` VARCHAR(30) DEFAULT NULL COMMENT '课程难度：BEGINNER入门 BASIC基础 ADVANCED进阶',
    `description` TEXT DEFAULT NULL COMMENT '课程简介',
    `content` LONGTEXT DEFAULT NULL COMMENT '课程正文内容',
    `video_url` VARCHAR(255) DEFAULT NULL COMMENT '课程视频URL',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `study_count` INT NOT NULL DEFAULT 0 COMMENT '学习人数',
    `is_recommend` TINYINT NOT NULL DEFAULT 0 COMMENT '是否首页推荐：1是 0否',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT '课程状态：DRAFT草稿 PUBLISHED发布 HIDDEN隐藏',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_course_category_id` (`category_id`),
    KEY `idx_course_status` (`status`),
    KEY `idx_course_recommend` (`is_recommend`),
    CONSTRAINT `fk_course_category` FOREIGN KEY (`category_id`) REFERENCES `course_category` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='非遗课堂课程表';

/*==============================================================*/
/* 16. 创作资源/学习资料表                                          */
/*==============================================================*/
CREATE TABLE `learning_resource` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '资源ID，自增主键',
    `course_id` BIGINT DEFAULT NULL COMMENT '关联课程ID，对应course.id，可为空',
    `title` VARCHAR(100) NOT NULL COMMENT '资源标题',
    `subtitle` VARCHAR(150) DEFAULT NULL COMMENT '资源副标题',
    `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '资源封面图URL',
    `resource_type` VARCHAR(30) NOT NULL COMMENT '资源类型：ARTICLE文章 VIDEO视频 PDF文档 IMAGE图片 MATERIAL素材包',
    `resource_url` VARCHAR(255) DEFAULT NULL COMMENT '资源文件URL',
    `content` LONGTEXT DEFAULT NULL COMMENT '图文内容或资源说明',
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '资源价格，0表示免费',
    `is_recommend` TINYINT NOT NULL DEFAULT 0 COMMENT '是否首页推荐：1是 0否',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `download_count` INT NOT NULL DEFAULT 0 COMMENT '下载次数',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态：DRAFT草稿 PUBLISHED发布 HIDDEN隐藏',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_resource_course_id` (`course_id`),
    KEY `idx_resource_type` (`resource_type`),
    KEY `idx_resource_status` (`status`),
    KEY `idx_resource_recommend` (`is_recommend`),
    CONSTRAINT `fk_resource_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='创作资源学习资料表';

/*==============================================================*/
/* 17. 文件资源表：统一记录头像、商品图、课程图、纹样图等文件             */
/*==============================================================*/
CREATE TABLE `file_resource` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件ID，自增主键',
    `user_id` BIGINT DEFAULT NULL COMMENT '上传用户ID，对应user.id',
    `biz_type` VARCHAR(50) NOT NULL COMMENT '业务类型：avatar头像 pattern纹样 product商品 course课程 resource资源 banner轮播 preview预览图',
    `original_name` VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
    `file_url` VARCHAR(255) NOT NULL COMMENT '文件访问URL',
    `file_path` VARCHAR(255) DEFAULT NULL COMMENT '服务器或对象存储中的文件路径',
    `file_size` BIGINT DEFAULT NULL COMMENT '文件大小，单位字节',
    `mime_type` VARCHAR(100) DEFAULT NULL COMMENT '文件MIME类型，例如image/png、application/pdf',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_file_user_id` (`user_id`),
    KEY `idx_file_biz_type` (`biz_type`),
    CONSTRAINT `fk_file_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文件资源表';

/*==============================================================*/
/* 18. 首页Banner表                                                */
/*==============================================================*/
CREATE TABLE `home_banner` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Banner ID，自增主键',
    `title` VARCHAR(100) NOT NULL COMMENT 'Banner标题',
    `subtitle` VARCHAR(255) DEFAULT NULL COMMENT 'Banner副标题',
    `image_url` VARCHAR(255) NOT NULL COMMENT 'Banner图片URL',
    `button_text` VARCHAR(50) DEFAULT NULL COMMENT '按钮文字，例如立即体验',
    `link_type` VARCHAR(30) DEFAULT NULL COMMENT '跳转类型：PATTERN纹样 PRODUCT商品 COURSE课程 RESOURCE资源 LINK外部链接',
    `link_id` BIGINT DEFAULT NULL COMMENT '站内跳转关联ID，例如商品ID、课程ID',
    `link_url` VARCHAR(255) DEFAULT NULL COMMENT '外部跳转链接',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '显示状态：1显示 0隐藏',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_banner_status` (`status`),
    KEY `idx_banner_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='首页Banner表';

/*==============================================================*/
/* 19. 首页推荐内容表：统一管理热门纹样、商品、课程、资源推荐              */
/*==============================================================*/
CREATE TABLE `home_recommend` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '推荐ID，自增主键',
    `recommend_type` VARCHAR(30) NOT NULL COMMENT '推荐类型：PATTERN纹样 PRODUCT商品 COURSE课程 RESOURCE资源',
    `related_id` BIGINT NOT NULL COMMENT '关联内容ID，根据recommend_type对应不同业务表ID',
    `title` VARCHAR(100) DEFAULT NULL COMMENT '推荐标题快照，可为空则读取业务表标题',
    `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '推荐封面图快照，可为空则读取业务表图片',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '推荐描述',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '推荐状态：1显示 0隐藏',
    `start_at` DATETIME DEFAULT NULL COMMENT '推荐开始时间',
    `end_at` DATETIME DEFAULT NULL COMMENT '推荐结束时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_recommend_type` (`recommend_type`),
    KEY `idx_recommend_related_id` (`related_id`),
    KEY `idx_recommend_status` (`status`),
    KEY `idx_recommend_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='首页推荐内容表';

/*==============================================================*/
/* 初始化可选数据：一个商家管理员账号和一条店铺信息                    */
/* 注意：password_hash 示例不是可用密码，请在后端使用加密算法生成真实值。 */
/*==============================================================*/
/*==============================================================*/
/* 逻辑删除字段补充：0 未删除，1 已删除                           */
/*==============================================================*/
ALTER TABLE `user` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除';
ALTER TABLE `shop_info` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除';
ALTER TABLE `user_address` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除';
ALTER TABLE `message_notice` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除';
ALTER TABLE `pattern_generation` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除';
ALTER TABLE `pattern` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除';
ALTER TABLE `prompt_template` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除';
ALTER TABLE `product_category` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除';
ALTER TABLE `product` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除';
ALTER TABLE `custom_design` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除';
ALTER TABLE `cart_item` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除';
ALTER TABLE `orders` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除';
ALTER TABLE `order_item` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除';
ALTER TABLE `course_category` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除';
ALTER TABLE `course` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除';
ALTER TABLE `learning_resource` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除';
ALTER TABLE `file_resource` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除';
ALTER TABLE `home_banner` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除';
ALTER TABLE `home_recommend` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除';

INSERT INTO `user` (`username`, `password_hash`, `nickname`, `phone`, `role`, `status`)
VALUES ('shopadmin', 'PLEASE_REPLACE_WITH_REAL_PASSWORD_HASH', '绣纹智创管理员', '13800000000', 'MERCHANT_ADMIN', 1);

INSERT INTO `shop_info` (`owner_user_id`, `shop_name`, `slogan`, `contact_name`, `contact_phone`, `email`, `address`, `description`, `status`)
VALUES (1, '绣纹智创', 'AI赋能广绣纹样创作与文创定制', '管理员', '13800000000', 'contact@example.com', '广东省广州市', '广绣AI纹样设计与文创定制平台', 1);

-- ============================================================
-- 测试数据（课程分类 + 课程），2026-07-22
-- 用于用户端-12. 非遗课堂接口调试：
--   [12.1] GET  /api/courses/categories  课程分类列表
--   [12.2] GET  /api/courses             课程列表（分页+筛选）
--   [12.3] GET  /api/courses/{id}        课程详情
--   [12.4] POST /api/courses/{id}/study  开始学习
-- 注意：可重复执行，执行前会先清空对应表数据
-- ============================================================

-- course_category 测试数据
INSERT INTO `course_category` (`id`, `parent_id`, `name`, `description`, `sort`, `status`) VALUES
(1,  NULL, '广绣基础',   '广绣入门基础知识与技法',       1, 1),
(2,  NULL, '刺绣进阶',   '中高级刺绣技法与创作',         2, 1),
(3,  NULL, '纹样设计',   '传统纹样设计与现代应用',       3, 1),
(4,  1,    '广绣针法',   '广绣基本针法教学',             1, 1),
(5,  1,    '色彩搭配',   '广绣配色原理与技巧',           2, 1),
(6,  2,    '双面绣技法', '双面绣进阶技法与实操',         1, 1),
(7,  3,    '文创纹样',   '文创产品纹样设计案例',         1, 1);

-- course 测试数据
INSERT INTO `course` (`id`, `category_id`, `title`, `subtitle`, `cover_image`, `price`, `teacher_name`, `duration`, `difficulty`, `description`, `content`, `video_url`, `view_count`, `study_count`, `is_recommend`, `sort`, `status`) VALUES
(1, 4, '广绣入门基础',   '从零开始学广绣',           '', 0.00,  '李慧珍', '30分钟', 'BEGINNER', '介绍广绣基本概念与入门技法，适合零基础学员。',         '<p>广绣是岭南传统文化的瑰宝，以构图饱满、色彩浓郁著称。本节从工具准备开始，带你认识广绣的基本材料与姿势。</p>', '', 1280, 356, 1, 1, 'PUBLISHED'),
(2, 4, '齐针与套针精讲', '掌握广绣两大核心针法',     '', 19.90, '李慧珍', '2课时',  'BASIC',    '系统讲解齐针、套针的运针技巧与实操练习。',               '<p>齐针是广绣最基础的针法，讲究平、齐、匀、顺。本节通过分解动作演示，确保每位学员都能掌握。</p>',              '', 890,  203, 1, 2, 'PUBLISHED'),
(3, 5, '广绣色彩美学',   '传统五色与现代表达',       '', 0.00,  '张明华', '45分钟', 'BEGINNER', '解析广绣经典配色体系，学习色彩在纹样中的运用规律。',     '<p>广绣色彩深受岭南民俗影响，红、金、绿、蓝、黑五色交织，形成独特的视觉语言。</p>',                            '', 756,  112, 0, 3, 'PUBLISHED'),
(4, 2, '刺绣创作进阶',   '从模仿到独立设计',         '', 29.90, '陈秀芳', '3课时',  'BASIC',    '培养独立创作能力，从临摹传统纹样到设计个人作品。',       '<p>临摹是学习的第一步，但真正的技艺掌握在于能够独立创作。本节带你从模仿走向创新。</p>',                        '', 621,  98,  0, 4, 'PUBLISHED'),
(5, 6, '双面绣技法精讲', '正反如一的高阶技艺',       '', 49.90, '陈秀芳', '5课时',  'ADVANCED', '深入学习双面绣的核心技法，完成一幅双面绣作品。',           '<p>双面绣要求正反两面图案同样精美，是广绣中难度最高的技法之一。本节从简单的双面平绣开始。</p>',                '', 432,  67,  1, 5, 'PUBLISHED'),
(6, 7, '纹样文创实战',   '从纹样到产品全流程',       '', 39.90, '王文博', '4课时',  'ADVANCED', '将传统纹样应用到帆布袋、丝巾等文创产品中的实战课程。',   '<p>如何将传统广绣纹样转化为现代消费者喜爱的文创产品？本节从市场调研、设计定位到生产打样全流程讲解。</p>',      '', 315,  45,  0, 6, 'PUBLISHED'),
(7, 4, '钉金绣技法入门', '广绣特色针法系列',         '', 0.00,  '李慧珍', '60分钟', 'BASIC',    '学习广绣特色技法——钉金绣的基本操作。',                  '<p>钉金绣是广绣的特色针法，使用金线在织物上盘绕固定，形成富丽堂皇的效果。</p>',                                '', 1050, 278, 1, 7, 'PUBLISHED'),
(8, 1, '广绣文化简史',   '了解广绣的前世今生',       '', 0.00,  '赵志远', '20分钟', 'BEGINNER', '从唐代到当代，回顾广绣千年的发展历程与文化价值。',       '<p>广绣的历史可追溯至唐代，明清时期达到鼎盛，与苏绣、湘绣、蜀绣并称中国四大名绣。</p>',                      '', 2100, 520, 1, 8, 'PUBLISHED');

-- ============================================================
-- 商家端测试账号，密码 merchant123，2026-07-25
-- （商家端apifox的全局token目前配的是下面这条数据的token）
-- 手动将auth的鉴权方式改为继承即可正常测试
-- ============================================================
INSERT INTO `user` (`username`, `password_hash`, `nickname`, `phone`, `role`, `status`)
VALUES ('testmerchant', 'a52f2c0dbf38ade4f715e02c7124046e', '测试商家', '13900000001', 'MERCHANT_ADMIN', 1);

-- ============================================================
-- 测试数据（创作资源），2026-07-29
-- 用于用户端-13. 创作资源 + 商家端-10. 资源管理接口调试：
--   [13.1] GET  /api/resources                      创作资源列表
--   [13.2] GET  /api/resources/{id}                 创作资源详情
--   [13.3] GET  /api/resources/{id}/download        下载资源
--   [11.1] GET  /api/admin/resources                商家资源列表
--   [11.2] POST /api/admin/resources                新增创作资源
--   [11.3] GET  /api/admin/resources/{id}           商家资源详情
--   [11.4] PUT  /api/admin/resources/{id}           编辑创作资源
--   [11.5] PUT  /api/admin/resources/{id}/status    状态切换
--   [11.6] DEL  /api/admin/resources/{id}           删除创作资源
-- 注意：可重复执行，执行前会先清空对应表数据
-- ============================================================

DELETE FROM `learning_resource` WHERE `id` >= 1;

INSERT INTO `learning_resource` (`id`, `course_id`, `title`, `subtitle`, `cover_image`, `resource_type`, `resource_url`, `content`, `price`, `is_recommend`, `view_count`, `download_count`, `sort`, `status`) VALUES
(1,  1,    '广绣入门工具清单',     '零基础学员必备工具指南',     '',  'PDF',     'https://cdn.example.com/resource/1201.pdf',  '广绣入门所需工具：绣绷、绣针、绣布、绣线的完整清单及选购建议。',                                                0.00,  1, 520,  189, 1, 'PUBLISHED'),
(2,  2,    '齐针运针分解演示视频', '高清慢动作分解教学',         '',  'VIDEO',   'https://cdn.example.com/resource/1202.mp4',  '本视频包含齐针十二种运针方向的高清慢动作演示，配合语音讲解，适合反复观看练习。',                                    19.90, 1, 340,  102, 2, 'PUBLISHED'),
(3,  3,    '广绣经典配色方案',     '传统五色搭配参考',           '',  'IMAGE',   'https://cdn.example.com/resource/1203.png',  '20组广绣经典配色方案高清图集，涵盖红金、蓝绿、黑白等主题，可直接用于纹样创作参考。',                                0.00,  1, 890,  356, 3, 'PUBLISHED'),
(4,  5,    '双面绣作品赏析图集',   '大师作品高清扫描',           '',  'IMAGE',   'https://cdn.example.com/resource/1204.jpg',  '收录30幅广绣大师双面绣代表作高清扫描图，附创作背景与技法要点批注。',                                                29.90, 0, 156,  43,  4, 'PUBLISHED'),
(5,  NULL, '岭南纹样素材包',       '50款传统岭南纹样矢量图',     '',  'MATERIAL', 'https://cdn.example.com/resource/1205.zip', '包含50款可商用岭南传统纹样矢量图（AI/EPS/SVG格式），涵盖花鸟、山水、几何、吉祥图案四大类，可直接导入设计软件使用。', 69.00, 1, 1200, 678, 5, 'PUBLISHED'),
(6,  4,    '刺绣构图设计指南',     '从基础到高阶的构图方法论',   '',  'ARTICLE',  '',                                          '一篇全面的刺绣构图指南，讲解对称构图、散点构图、满花构图等技法在广绣创作中的实际应用。',                            0.00,  0, 230,  56,  6, 'PUBLISHED'),
(7,  NULL, '广绣非遗保护政策汇编', '国家及地方非遗政策整理',     '',  'PDF',     'https://cdn.example.com/resource/1207.pdf',  '2024年度国家及广东省非遗保护相关政策文件汇编，适用于项目申报与资金申请参考。',                                      0.00,  0, 89,   21,  7, 'PUBLISHED'),
(8,  6,    '文创产品设计模板',     '帆布袋/丝巾/手机壳源文件',   '',  'MATERIAL', 'https://cdn.example.com/resource/1208.zip', '3套文创产品设计PSD源文件模板（帆布袋、丝巾、手机壳），含刀版线和出血设置，新手可直接替换图案使用。',                39.90, 0, 445,  134, 8, 'PUBLISHED'),
(9,  NULL, '广绣针法速查表',       '十二种常用针法一览',         '',  'PDF',     'https://cdn.example.com/resource/1209.pdf',  '常用十二种广绣针法的名称、图示、适用场景和难度等级的速查表，可打印随身参考。',                                      0.00,  1, 670,  245, 9, 'DRAFT'),
(10, 7,    '钉金绣实操教程',       '手把手教你钉金绣技法',       '',  'VIDEO',   'https://cdn.example.com/resource/1210.mp4',  '一小时完整钉金绣实操教程，包含金线固定、盘绕收尾、图案过度等关键技法演示。',                                        59.90, 0, 78,   12,  10, 'HIDDEN');
