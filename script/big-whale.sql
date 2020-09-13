/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 50726
 Source Host           : 127.0.0.1:3306
 Source Schema         : big-whale

 Target Server Type    : MySQL
 Target Server Version : 50726
 File Encoding         : 65001

 Date: 22/06/2020 10:56:43
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for agent
-- ----------------------------
CREATE TABLE IF NOT EXISTS `agent`  (
  `id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `ip` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `host` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `mac` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `cluster_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `socket_port` int(11) NULL DEFAULT NULL,
  `status` int(11) NOT NULL,
  `create_time` datetime(0) NOT NULL,
  `last_conn_time` datetime(0) NULL DEFAULT NULL,
  `user` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `mac`(`mac`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for auth_resource
-- ----------------------------
CREATE TABLE IF NOT EXISTS `auth_resource`  (
  `id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `created` datetime(0) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `code`(`code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of auth_resource
-- ----------------------------
INSERT ignore INTO `auth_resource` VALUES ('2c9083b26e062cef016e062fac2e0000', 'cluster_manage', '集群管理', '/admin/cluster/*', now());
INSERT ignore INTO `auth_resource` VALUES ('2c9083b26e062cef016e0632eb0e0001', 'cluster_cluster_user_manage', '集群用户', '/admin/cluster/cluster_user/*,/auth/user/*', now());
INSERT ignore INTO `auth_resource` VALUES ('2c9083b26e0635fa016e0637f9660000', 'cluster_agent_manage', '代理管理', '/admin/cluster/agent/*', now());
INSERT ignore INTO `auth_resource` VALUES ('8a80813a70bd13b60170c8d4af45331a', 'cluster_compute_framework_manage', '版本管理', '/admin/cluster/compute_framework/*', now());
INSERT ignore INTO `auth_resource` VALUES ('2c9083b26e063b62016e063c32260000', 'auth_resource_manage', '资源管理', '/auth/resource/*', now());
INSERT ignore INTO `auth_resource` VALUES ('2c9083b26e063b62016e063cacee0001', 'auth_role_manage', '角色管理', '/auth/role/*', now());
INSERT ignore INTO `auth_resource` VALUES ('2c9083b26e063b62016e063d18700002', 'auth_user_manage', '用户管理', '/auth/user/*', now());

-- ----------------------------
-- Table structure for auth_role
-- ----------------------------
CREATE TABLE IF NOT EXISTS `auth_role`  (
  `id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created` datetime(0) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `code`(`code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of auth_role
-- ----------------------------
INSERT ignore INTO `auth_role` VALUES ('2c9083b26e063b62016e063de6c20007', 'cluster_manager', '集群管理员', now());
INSERT ignore INTO `auth_role` VALUES ('2c9083b26e063b62016e064194a8000d', 'auth_manager', '权限管理员', now());

-- ----------------------------
-- Table structure for auth_role_resource
-- ----------------------------
CREATE TABLE IF NOT EXISTS `auth_role_resource`  (
  `id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `role` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `resource` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of auth_role_resource
-- ----------------------------
INSERT ignore INTO `auth_role_resource` VALUES ('8a80813a70c9453e0170c9473c5b0c46', 'auth_manager', 'auth_resource_manage');
INSERT ignore INTO `auth_role_resource` VALUES ('8a80813a70c9453e0170c9473c5b0c47', 'auth_manager', 'auth_role_manage');
INSERT ignore INTO `auth_role_resource` VALUES ('8a80813a70c9453e0170c9473c5c0c48', 'auth_manager', 'auth_user_manage');
INSERT ignore INTO `auth_role_resource` VALUES ('8a80813a715529a20171552a39ef0567', 'cluster_manager', 'cluster_manage');
INSERT ignore INTO `auth_role_resource` VALUES ('8a80813a715529a20171552a39ef0568', 'cluster_manager', 'cluster_cluster_user_manage');
INSERT ignore INTO `auth_role_resource` VALUES ('8a80813a715529a20171552a39ef0569', 'cluster_manager', 'cluster_agent_manage');
INSERT ignore INTO `auth_role_resource` VALUES ('8a80813a715529a20171552a39ef056a', 'cluster_manager', 'cluster_compute_framework_manage');

-- ----------------------------
-- Table structure for auth_user
-- ----------------------------
CREATE TABLE IF NOT EXISTS `auth_user`  (
  `id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `username` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `nickname` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `root` bit(1) NOT NULL,
  `level` int(11) NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `phone` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `created` datetime(0) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of auth_user
-- ----------------------------
INSERT ignore INTO `auth_user` VALUES ('8a80813a70e647650170eb4cab253357', 'admin', '368020dc2a7d717b694fe9bc00a832c6cf6bc452265f2091f4a4d473eea9bbae7a429ec24f9072fc', '超级管理员', b'1', b'1', 0, NULL, NULL, now());

-- ----------------------------
-- Table structure for auth_user_role
-- ----------------------------
CREATE TABLE IF NOT EXISTS `auth_user_role`  (
  `id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `username` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `role` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for cluster
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cluster`  (
  `id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `yarn_url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `default_file_cluster` bit(1) NOT NULL,
  `flink_proxy_user_enabled` bit(1) NOT NULL,
  `fs_default_fs` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `fs_webhdfs` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `fs_user` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `fs_dir` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `stream_black_node_list` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `batch_black_node_list` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `name`(`name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for cluster_user
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cluster_user`  (
  `id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `cluster_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `queue` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `uid` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `user` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `cluster_id,uid`(`cluster_id`, `uid`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for cmd_record
-- ----------------------------
CREATE TABLE IF NOT EXISTS `cmd_record`  (
  `id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `parent_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `agent_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `script_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `sub_script_ids` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `status` int(11) NOT NULL,
  `time_out` int(11) NOT NULL,
  `uid` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `cluster_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `monitor_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `scheduling_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NOT NULL,
  `start_time` datetime(0) NULL DEFAULT NULL,
  `finish_time` datetime(0) NULL DEFAULT NULL,
  `args` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  `outputs` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  `errors` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  `job_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `job_final_status` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `status_index`(`status`) USING BTREE,
  INDEX `create_time_index`(`create_time`) USING BTREE,
  INDEX `uid_index`(`uid`) USING BTREE,
  INDEX `job_final_status_index`(`job_final_status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for compute_framework
-- ----------------------------
CREATE TABLE IF NOT EXISTS `compute_framework`  (
  `id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `type` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `version` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `command` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `orders` int(11) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `type,version`(`type`, `version`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for monitor
-- ----------------------------
CREATE TABLE IF NOT EXISTS `monitor`  (
  `id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `uid` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `type` int(11) NOT NULL,
  `cron` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `status` int(11) NOT NULL,
  `send_mail` bit(1) NOT NULL,
  `script_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `ex_auto_restart` bit(1) NULL DEFAULT NULL,
  `waiting_batches` int(11) NULL DEFAULT NULL,
  `auto_restart` bit(1) NULL DEFAULT NULL,
  `url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `method` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `body` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `dingding_hooks` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NOT NULL,
  `update_time` datetime(0) NOT NULL,
  `execute_time` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_blob_triggers
-- ----------------------------
CREATE TABLE IF NOT EXISTS `qrtz_blob_triggers`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `BLOB_DATA` blob NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `qrtz_blob_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_calendars
-- ----------------------------
CREATE TABLE IF NOT EXISTS `qrtz_calendars`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `CALENDAR_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `CALENDAR` blob NOT NULL,
  PRIMARY KEY (`SCHED_NAME`, `CALENDAR_NAME`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_cron_triggers
-- ----------------------------
CREATE TABLE IF NOT EXISTS `qrtz_cron_triggers`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `CRON_EXPRESSION` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TIME_ZONE_ID` varchar(80) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_fired_triggers
-- ----------------------------
CREATE TABLE IF NOT EXISTS `qrtz_fired_triggers`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `ENTRY_ID` varchar(95) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `INSTANCE_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `FIRED_TIME` bigint(13) NOT NULL,
  `SCHED_TIME` bigint(13) NOT NULL,
  `PRIORITY` int(11) NOT NULL,
  `STATE` varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `JOB_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `JOB_GROUP` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `IS_NONCONCURRENT` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `REQUESTS_RECOVERY` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`, `ENTRY_ID`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_job_details
-- ----------------------------
CREATE TABLE IF NOT EXISTS `qrtz_job_details`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `JOB_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `JOB_GROUP` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `DESCRIPTION` varchar(250) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `JOB_CLASS_NAME` varchar(250) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `IS_DURABLE` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `IS_NONCONCURRENT` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `IS_UPDATE_DATA` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `REQUESTS_RECOVERY` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `JOB_DATA` blob NULL,
  PRIMARY KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_locks
-- ----------------------------
CREATE TABLE IF NOT EXISTS `qrtz_locks`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `LOCK_NAME` varchar(40) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  PRIMARY KEY (`SCHED_NAME`, `LOCK_NAME`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_paused_trigger_grps
-- ----------------------------
CREATE TABLE IF NOT EXISTS `qrtz_paused_trigger_grps`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_GROUP`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_scheduler_state
-- ----------------------------
CREATE TABLE IF NOT EXISTS `qrtz_scheduler_state`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `INSTANCE_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `LAST_CHECKIN_TIME` bigint(13) NOT NULL,
  `CHECKIN_INTERVAL` bigint(13) NOT NULL,
  PRIMARY KEY (`SCHED_NAME`, `INSTANCE_NAME`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_simple_triggers
-- ----------------------------
CREATE TABLE IF NOT EXISTS `qrtz_simple_triggers`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `REPEAT_COUNT` bigint(7) NOT NULL,
  `REPEAT_INTERVAL` bigint(12) NOT NULL,
  `TIMES_TRIGGERED` bigint(10) NOT NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_simprop_triggers
-- ----------------------------
CREATE TABLE IF NOT EXISTS `qrtz_simprop_triggers`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `STR_PROP_1` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `STR_PROP_2` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `STR_PROP_3` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `INT_PROP_1` int(11) NULL DEFAULT NULL,
  `INT_PROP_2` int(11) NULL DEFAULT NULL,
  `LONG_PROP_1` bigint(20) NULL DEFAULT NULL,
  `LONG_PROP_2` bigint(20) NULL DEFAULT NULL,
  `DEC_PROP_1` decimal(13, 4) NULL DEFAULT NULL,
  `DEC_PROP_2` decimal(13, 4) NULL DEFAULT NULL,
  `BOOL_PROP_1` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `BOOL_PROP_2` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `qrtz_simprop_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_triggers
-- ----------------------------
CREATE TABLE IF NOT EXISTS `qrtz_triggers`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `JOB_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `JOB_GROUP` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `DESCRIPTION` varchar(250) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `NEXT_FIRE_TIME` bigint(13) NULL DEFAULT NULL,
  `PREV_FIRE_TIME` bigint(13) NULL DEFAULT NULL,
  `PRIORITY` int(11) NULL DEFAULT NULL,
  `TRIGGER_STATE` varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `TRIGGER_TYPE` varchar(8) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `START_TIME` bigint(13) NOT NULL,
  `END_TIME` bigint(13) NULL DEFAULT NULL,
  `CALENDAR_NAME` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `MISFIRE_INSTR` smallint(2) NULL DEFAULT NULL,
  `JOB_DATA` blob NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) USING BTREE,
  INDEX `SCHED_NAME`(`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) USING BTREE,
  CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) REFERENCES `qrtz_job_details` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for scheduling
-- ----------------------------
CREATE TABLE IF NOT EXISTS `scheduling`  (
  `id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `uid` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `script_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `sub_script_ids` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `cron` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `cycle` int(11) NULL DEFAULT NULL,
  `intervals` int(11) NULL DEFAULT NULL,
  `minute` int(11) NULL DEFAULT NULL,
  `hour` int(11) NULL DEFAULT NULL,
  `week` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `start_time` datetime(0) NOT NULL,
  `end_time` datetime(0) NOT NULL,
  `repeat_submit` bit(1) NOT NULL,
  `send_mail` bit(1) NOT NULL,
  `dingding_hooks` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `status` int(11) NOT NULL,
  `last_execute_time` datetime(0) NULL DEFAULT NULL,
  `create_time` datetime(0) NOT NULL,
  `update_time` datetime(0) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for script
-- ----------------------------
CREATE TABLE IF NOT EXISTS `script`  (
  `id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `type` int(11) NOT NULL,
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `description` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `script` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `time_out` int(11) NOT NULL,
  `uid` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `cluster_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `agent_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NOT NULL,
  `update_time` datetime(0) NOT NULL,
  `input` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `output` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `queue` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `user` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `app` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `name_index`(`name`) USING BTREE,
  INDEX `description_index`(`description`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for yarn_app
-- ----------------------------
CREATE TABLE IF NOT EXISTS `yarn_app`  (
  `id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `app_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `queue` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `user` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `state` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `final_status` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `tracking_url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `application_type` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `uid` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `script_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `cluster_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `started_time` datetime(0) NOT NULL,
  `update_time` datetime(0) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE `script` CHANGE COLUMN `time_out` `timeout` int(11) NOT NULL AFTER `script`;

ALTER TABLE `cmd_record` CHANGE COLUMN `time_out` `timeout` int(11) NOT NULL AFTER `status`;
ALTER TABLE `cmd_record` ADD COLUMN `scheduling_instance_id` varchar(255) NULL AFTER `scheduling_id`;

-- v1.1
ALTER TABLE `scheduling` ADD COLUMN `topology` text NOT NULL AFTER `uid`;
ALTER TABLE `scheduling` ADD COLUMN `script_ids` varchar(255) NOT NULL AFTER `topology`;
-- 需要从v1.0升级上来的，请先注释以下两句SQL
ALTER TABLE `scheduling` DROP COLUMN `script_id`; -- 1
ALTER TABLE `scheduling` DROP COLUMN `sub_script_ids`; -- 2

ALTER TABLE `cmd_record` DROP COLUMN `sub_script_ids`;
ALTER TABLE `cmd_record` ADD COLUMN `scheduling_node_id` varchar(255) NULL AFTER `scheduling_instance_id`;

ALTER TABLE `agent` CHANGE COLUMN `host` `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL AFTER `id`;
ALTER TABLE `agent` ADD UNIQUE INDEX `name`(`name`);
ALTER TABLE `agent` ADD COLUMN `description` varchar(255) NULL AFTER `name`;
ALTER TABLE `agent` CHANGE COLUMN `ip` `instances` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL AFTER `description`;
ALTER TABLE `agent` DROP COLUMN `mac`;
ALTER TABLE `agent` DROP COLUMN `socket_port`;
ALTER TABLE `agent` DROP COLUMN `status`;
ALTER TABLE `agent` DROP COLUMN `create_time`;
ALTER TABLE `agent` DROP COLUMN `last_conn_time`;
ALTER TABLE `agent` DROP COLUMN `user`;
ALTER TABLE `agent` DROP COLUMN `password`;

ALTER TABLE `cmd_record` MODIFY COLUMN `agent_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL AFTER `parent_id`;
ALTER TABLE `cmd_record` ADD COLUMN `agent_instance` varchar(255) NULL AFTER `agent_id`;

-- 需要从v1.0升级上来的，请先注释以下所有SQL
ALTER TABLE `auth_resource` DROP COLUMN `created`;

ALTER TABLE `auth_role` DROP COLUMN `created`;

ALTER TABLE `auth_user` DROP COLUMN `level`;
ALTER TABLE `auth_user` CHANGE COLUMN `created` `create_time` datetime NOT NULL AFTER `phone`;
ALTER TABLE `auth_user` ADD COLUMN `update_time` datetime NULL AFTER `create_time`;
UPDATE `auth_user` SET `update_time` = now();
ALTER TABLE `auth_user` MODIFY COLUMN `update_time` datetime NOT NULL AFTER `create_time`;

ALTER TABLE `scheduling` MODIFY COLUMN `script_ids`  varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL AFTER `uid`;
ALTER TABLE `scheduling` MODIFY COLUMN `cycle`  int(11) NOT NULL AFTER `script_ids`;
ALTER TABLE `scheduling` MODIFY COLUMN `intervals`  int(11) NULL DEFAULT NULL AFTER `cycle`;
ALTER TABLE `scheduling` MODIFY COLUMN `minute`  int(11) NULL DEFAULT NULL AFTER `intervals`;
ALTER TABLE `scheduling` MODIFY COLUMN `hour`  int(11) NULL DEFAULT NULL AFTER `minute`;
ALTER TABLE `scheduling` MODIFY COLUMN `week`  varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL AFTER `hour`;
ALTER TABLE `scheduling` MODIFY COLUMN `cron`  varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL AFTER `week`;
ALTER TABLE `scheduling` MODIFY COLUMN `start_time`  datetime NOT NULL AFTER `cron`;
ALTER TABLE `scheduling` MODIFY COLUMN `end_time`  datetime NOT NULL AFTER `start_time`;
ALTER TABLE `scheduling` MODIFY COLUMN `topology`  text CHARACTER SET utf8 COLLATE utf8_general_ci NULL AFTER `end_time`;
ALTER TABLE `scheduling` MODIFY COLUMN `last_execute_time`  datetime NULL DEFAULT NULL AFTER `repeat_submit`;
ALTER TABLE `scheduling` CHANGE COLUMN `send_mail` `send_email`  bit(1) NOT NULL AFTER `last_execute_time`;
ALTER TABLE `scheduling` MODIFY COLUMN `create_time`  datetime NOT NULL AFTER `dingding_hooks`;
ALTER TABLE `scheduling` MODIFY COLUMN `update_time`  datetime NOT NULL AFTER `create_time`;
ALTER TABLE `scheduling` CHANGE COLUMN `status` `enabled`  bit(1) NOT NULL AFTER `update_time`;
ALTER TABLE `scheduling` ADD COLUMN `type`  int(11) NOT NULL AFTER `uid`;
ALTER TABLE `scheduling` ADD COLUMN `ex_restart`  bit(1) NOT NULL AFTER `repeat_submit`;
ALTER TABLE `scheduling` ADD COLUMN `waiting_batches`  int(11) NOT NULL AFTER `ex_restart`;
ALTER TABLE `scheduling` ADD COLUMN `blocking_restart`  bit(1) NOT NULL AFTER `waiting_batches`;
INSERT INTO scheduling (
	id,
	uid,
	type,
	script_ids,
	cycle,
	cron,
	start_time,
	end_time,
	ex_restart,
	waiting_batches,
	blocking_restart,
	last_execute_time,
	send_email,
	dingding_hooks,
	create_time,
	update_time,
	enabled
) SELECT
	id,
	uid,
	1,
	script_id,
	1,
	cron,
	NOW(),
	DATE_ADD(NOW(), INTERVAL 10 YEAR),
	ex_auto_restart,
	waiting_batches,
	auto_restart,
	execute_time,
	send_mail,
	dingding_hooks,
	create_time,
	update_time,
	status
FROM
	monitor;

DROP TABLE monitor;

ALTER TABLE `cmd_record` DROP COLUMN `monitor_id`;

ALTER TABLE `cluster` CHANGE COLUMN `stream_black_node_list` `streaming_black_node_list` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL AFTER `fs_dir`;
