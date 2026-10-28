-- 电镀配方库建库建表脚本（首次启动自动执行，全部幂等）
CREATE DATABASE IF NOT EXISTS `electroplate` DEFAULT CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS `recipe` (
  `id`                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `recipe_code`        VARCHAR(32)   NOT NULL COMMENT '配方编号 RECyyyyMMdd-XXXX',
  `category`           VARCHAR(32)   NOT NULL COMMENT '类别，如 RC/AS/HP',
  `spec`               VARCHAR(32)   NOT NULL COMMENT '规格，如 0603/1210',
  `res_min`            DECIMAL(12,4) NOT NULL COMMENT '阻值下限(Ω)',
  `res_max`            DECIMAL(12,4) NOT NULL COMMENT '阻值上限(Ω)',
  `qty_min`            INT           NOT NULL COMMENT '数量下限(pcs)',
  `qty_max`            INT           NOT NULL COMMENT '数量上限(pcs)',
  `ni_seg1_current`    DECIMAL(8,2)  NOT NULL DEFAULT 0 COMMENT '镍段一段电流(A)',
  `ni_seg1_time`       INT           NOT NULL DEFAULT 0 COMMENT '镍段一段时间(Sec)',
  `ni_seg2_current`    DECIMAL(8,2)  NOT NULL DEFAULT 0,
  `ni_seg2_time`       INT           NOT NULL DEFAULT 0,
  `ni_seg3_current`    DECIMAL(8,2)  NOT NULL DEFAULT 0,
  `ni_seg3_time`       INT           NOT NULL DEFAULT 0,
  `ni_total_time`      INT           NOT NULL COMMENT '镍段总时间(Sec)',
  `ni_protect_current` DECIMAL(8,2)  NOT NULL COMMENT '镍段保护电流(A)',
  `ni_alarm_voltage`   DECIMAL(8,2)  NOT NULL COMMENT '镍段报警电压(V)',
  `sn_seg1_current`    DECIMAL(8,2)  NOT NULL DEFAULT 0 COMMENT '锡段一段电流(A)',
  `sn_seg1_time`       INT           NOT NULL DEFAULT 0,
  `sn_seg2_current`    DECIMAL(8,2)  NOT NULL DEFAULT 0,
  `sn_seg2_time`       INT           NOT NULL DEFAULT 0,
  `sn_seg3_current`    DECIMAL(8,2)  NOT NULL DEFAULT 0,
  `sn_seg3_time`       INT           NOT NULL DEFAULT 0,
  `sn_total_time`      INT           NOT NULL COMMENT '锡段总时间(Sec)',
  `sn_protect_current` DECIMAL(8,2)  NOT NULL COMMENT '锡段保护电流(A)',
  `sn_alarm_voltage`   DECIMAL(8,2)  NOT NULL COMMENT '锡段报警电压(V)',
  `status`             TINYINT       NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `version`            INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by`         VARCHAR(32)   DEFAULT NULL COMMENT '创建人',
  `created_at`         DATETIME      NOT NULL COMMENT '创建时间',
  `updated_by`         VARCHAR(32)   DEFAULT NULL COMMENT '修改人',
  `updated_at`         DATETIME      NOT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recipe_code` (`recipe_code`),
  KEY `idx_cat_spec_status` (`category`, `spec`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '电镀配方';

CREATE TABLE IF NOT EXISTS `recipe_log` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `recipe_id`       BIGINT       NOT NULL COMMENT '配方ID',
  `recipe_code`     VARCHAR(32)  NOT NULL COMMENT '配方编号',
  `action`          VARCHAR(16)  NOT NULL COMMENT 'CREATE/UPDATE/ENABLE/DISABLE',
  `operator`        VARCHAR(32)  DEFAULT NULL COMMENT '操作人',
  `before_snapshot` TEXT         DEFAULT NULL COMMENT '变更前完整JSON快照',
  `after_snapshot`  TEXT         DEFAULT NULL COMMENT '变更后完整JSON快照',
  `created_at`      DATETIME     NOT NULL COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_recipe_id` (`recipe_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '配方操作日志';

CREATE TABLE IF NOT EXISTS `dict_item` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dict_type`  VARCHAR(16) NOT NULL COMMENT '字典类型 CATEGORY/SPEC',
  `dict_value` VARCHAR(32) NOT NULL COMMENT '字典值',
  `created_at` DATETIME    NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_value` (`dict_type`, `dict_value`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '字典（类别/规格）';

-- 初始字典预置（幂等插入，可按现场实际调整）
INSERT IGNORE INTO `dict_item` (`dict_type`, `dict_value`, `created_at`) VALUES
('CATEGORY', 'RC', NOW()), ('CATEGORY', 'AS', NOW()), ('CATEGORY', 'HP', NOW()),
('SPEC', '0402', NOW()), ('SPEC', '0603', NOW()), ('SPEC', '1210', NOW()),
('SPEC', '2010', NOW()), ('SPEC', '2512', NOW());

CREATE TABLE IF NOT EXISTS `production_record` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `serial_no`    VARCHAR(24)  NOT NULL COMMENT '流水号 PRyyyyMMdd-XXXX',
  `operator`     VARCHAR(32)  DEFAULT NULL COMMENT '作业员',
  `rack_no`      VARCHAR(32)  NOT NULL COMMENT '吊架号',
  `feed_slot`    VARCHAR(32)  DEFAULT NULL COMMENT '投料槽位名称',
  `feed_time`    DATETIME     NOT NULL COMMENT '投料时间',
  `in_time`      DATETIME     DEFAULT NULL COMMENT '进线时间',
  `out_time`     DATETIME     DEFAULT NULL COMMENT '出线时间，空=正在加工',
  `batch_no_l`   VARCHAR(64)  DEFAULT NULL COMMENT '批号(左)',
  `item_no_l`    VARCHAR(64)  DEFAULT NULL COMMENT '料号(左)',
  `spec_l`       VARCHAR(32)  DEFAULT NULL COMMENT '规格(左)',
  `resistance_l` VARCHAR(32)  DEFAULT NULL COMMENT '阻值(左)',
  `tolerance_l`  VARCHAR(32)  DEFAULT NULL COMMENT '精度(左)',
  `qty_l`        INT          DEFAULT NULL COMMENT '数量(左)',
  `batch_no_r`   VARCHAR(64)  DEFAULT NULL COMMENT '批号(右)',
  `item_no_r`    VARCHAR(64)  DEFAULT NULL COMMENT '料号(右)',
  `spec_r`       VARCHAR(32)  DEFAULT NULL COMMENT '规格(右)',
  `resistance_r` VARCHAR(32)  DEFAULT NULL COMMENT '阻值(右)',
  `tolerance_r`  VARCHAR(32)  DEFAULT NULL COMMENT '精度(右)',
  `qty_r`        INT          DEFAULT NULL COMMENT '数量(右)',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_serial_no` (`serial_no`),
  KEY `idx_feed_time` (`feed_time`),
  KEY `idx_rack_no` (`rack_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '生产信息记录';

-- 产线槽位配置（生产参数设置模块）
CREATE TABLE IF NOT EXISTS `slot` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `slot_name`    VARCHAR(32) NOT NULL COMMENT '槽位名称，如 酸洗/镀镍1/交换',
  `slot_type`    TINYINT     NOT NULL DEFAULT 0 COMMENT '槽位类型：0 普通槽 / 1 镀槽',
  `time_seconds` INT         NOT NULL DEFAULT 0 COMMENT '时间设定值(秒)，所有槽位均需设定',
  `show_time`    TINYINT     NOT NULL DEFAULT 1 COMMENT '是否展示时间：0 隐藏 / 1 展示',
  `switch_on`    TINYINT     NOT NULL DEFAULT 1 COMMENT '开关状态：0 关 / 1 开（仅镀槽有效）',
  `sort_no`      INT         NOT NULL DEFAULT 1 COMMENT '工序序号，列表按此升序',
  `status`       TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：0 停用 / 1 启用',
  `version`      INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at`   DATETIME    NOT NULL COMMENT '创建时间',
  `updated_at`   DATETIME    NOT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_slot_name` (`slot_name`),
  UNIQUE KEY `uk_sort_no` (`sort_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '产线槽位配置';
