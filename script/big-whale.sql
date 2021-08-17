-- --------------------------------------------------------
-- big-whale:                      v1.3
-- Copyright (c):                  © Meetyou. Big data group technical support.
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

CREATE TABLE IF NOT EXISTS `agent` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `instances` varchar(255) NOT NULL,
  `cluster_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `name_unique` (`name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE IF NOT EXISTS `auth_resource` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `code_unique` (`code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

/*!40000 ALTER TABLE `auth_resource` DISABLE KEYS */;
INSERT IGNORE INTO `auth_resource` (`id`, `code`, `name`, `url`) VALUES
	(1, 'cluster_manage', '集群管理', '/admin/cluster/*'),
	(2, 'cluster_cluster_user_manage', '集群用户', '/admin/cluster/cluster_user/*,/auth/user/*'),
	(3, 'cluster_agent_manage', '代理管理', '/admin/cluster/agent/*'),
	(4, 'cluster_compute_framework_manage', '版本管理', '/admin/cluster/compute_framework/*'),
	(5, 'auth_resource_manage', '资源管理', '/auth/resource/*'),
	(6, 'auth_role_manage', '角色管理', '/auth/role/*'),
	(7, 'auth_user_manage', '用户管理', '/auth/user/*');
/*!40000 ALTER TABLE `auth_resource` ENABLE KEYS */;


CREATE TABLE IF NOT EXISTS `auth_role` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `code_unique` (`code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

/*!40000 ALTER TABLE `auth_role` DISABLE KEYS */;
INSERT IGNORE INTO `auth_role` (`id`, `code`, `name`) VALUES
	(1, 'cluster_manager', '集群管理员'),
	(2, 'auth_manager', '权限管理员');
/*!40000 ALTER TABLE `auth_role` ENABLE KEYS */;


CREATE TABLE IF NOT EXISTS `auth_role_resource` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `role` varchar(255) NOT NULL,
  `resource` varchar(255) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

/*!40000 ALTER TABLE `auth_role_resource` DISABLE KEYS */;
INSERT IGNORE INTO `auth_role_resource` (`id`, `role`, `resource`) VALUES
	(1, 'cluster_manager', 'cluster_manage'),
	(2, 'cluster_manager', 'cluster_cluster_user_manage'),
	(3, 'cluster_manager', 'cluster_agent_manage'),
	(4, 'cluster_manager', 'cluster_compute_framework_manage'),
	(5, 'auth_manager', 'auth_resource_manage'),
	(6, 'auth_manager', 'auth_role_manage'),
	(7, 'auth_manager', 'auth_user_manage');
/*!40000 ALTER TABLE `auth_role_resource` ENABLE KEYS */;


CREATE TABLE IF NOT EXISTS `auth_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `nickname` varchar(255) DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `root` bit(1) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `username_unique` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

/*!40000 ALTER TABLE `auth_user` DISABLE KEYS */;
INSERT IGNORE INTO `auth_user` (`id`, `username`, `password`, `nickname`, `enabled`, `root`, `email`, `phone`, `create_time`, `update_time`) VALUES
	(1, 'admin', '368020dc2a7d717b694fe9bc00a832c6cf6bc452265f2091f4a4d473eea9bbae7a429ec24f9072fc', '超级管理员', b'1', b'1', NULL, NULL, now(), now());
/*!40000 ALTER TABLE `auth_user` ENABLE KEYS */;


CREATE TABLE IF NOT EXISTS `auth_user_role` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `role` varchar(255) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE IF NOT EXISTS `cluster` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `yarn_url` varchar(255) NOT NULL,
  `default_file_cluster` bit(1) NOT NULL,
  `flink_proxy_user_enabled` bit(1) NOT NULL,
  `fs_default_fs` varchar(255) NOT NULL,
  `fs_webhdfs` varchar(255) DEFAULT NULL,
  `fs_user` varchar(255) NOT NULL,
  `fs_dir` varchar(255) NOT NULL,
  `stream_black_node_list` varchar(255) DEFAULT NULL,
  `batch_black_node_list` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `name_unique` (`name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE IF NOT EXISTS `cluster_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `cluster_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `queue` varchar(255) NOT NULL,
  `user` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `cluster_id,user_id_unique` (`cluster_id`,`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE IF NOT EXISTS `compute_framework` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` varchar(255) NOT NULL,
  `version` varchar(255) NOT NULL,
  `command` varchar(255) NOT NULL,
  `orders` int(11) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `type,version_unique` (`type`,`version`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE IF NOT EXISTS `monitor` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `cycle` int(11) DEFAULT NULL,
  `intervals` int(11) DEFAULT NULL,
  `minute` int(11) DEFAULT NULL,
  `hour` int(11) DEFAULT NULL,
  `week` varchar(255) DEFAULT NULL,
  `cron` varchar(255) DEFAULT NULL,
  `ex_restart` bit(1) NOT NULL,
  `waiting_batches` int(11) NOT NULL,
  `blocking_restart` bit(1) NOT NULL,
  `send_email` bit(1) NOT NULL,
  `dingding_hooks` varchar(255) DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `real_fire_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE IF NOT EXISTS `qrtz_blob_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `BLOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `qrtz_blob_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE IF NOT EXISTS `qrtz_calendars` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `CALENDAR_NAME` varchar(200) NOT NULL,
  `CALENDAR` blob NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`CALENDAR_NAME`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE IF NOT EXISTS `qrtz_cron_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `CRON_EXPRESSION` varchar(200) NOT NULL,
  `TIME_ZONE_ID` varchar(80) DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE IF NOT EXISTS `qrtz_fired_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `ENTRY_ID` varchar(95) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `INSTANCE_NAME` varchar(200) NOT NULL,
  `FIRED_TIME` bigint(13) NOT NULL,
  `SCHED_TIME` bigint(13) NOT NULL,
  `PRIORITY` int(11) NOT NULL,
  `STATE` varchar(16) NOT NULL,
  `JOB_NAME` varchar(200) DEFAULT NULL,
  `JOB_GROUP` varchar(200) DEFAULT NULL,
  `IS_NONCONCURRENT` varchar(1) DEFAULT NULL,
  `REQUESTS_RECOVERY` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`,`ENTRY_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE IF NOT EXISTS `qrtz_job_details` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `JOB_NAME` varchar(200) NOT NULL,
  `JOB_GROUP` varchar(200) NOT NULL,
  `DESCRIPTION` varchar(250) DEFAULT NULL,
  `JOB_CLASS_NAME` varchar(250) NOT NULL,
  `IS_DURABLE` varchar(1) NOT NULL,
  `IS_NONCONCURRENT` varchar(1) NOT NULL,
  `IS_UPDATE_DATA` varchar(1) NOT NULL,
  `REQUESTS_RECOVERY` varchar(1) NOT NULL,
  `JOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE IF NOT EXISTS `qrtz_locks` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `LOCK_NAME` varchar(40) NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`LOCK_NAME`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE IF NOT EXISTS `qrtz_paused_trigger_grps` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_GROUP`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE IF NOT EXISTS `qrtz_scheduler_state` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `INSTANCE_NAME` varchar(200) NOT NULL,
  `LAST_CHECKIN_TIME` bigint(13) NOT NULL,
  `CHECKIN_INTERVAL` bigint(13) NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`INSTANCE_NAME`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE IF NOT EXISTS `qrtz_simple_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `REPEAT_COUNT` bigint(7) NOT NULL,
  `REPEAT_INTERVAL` bigint(12) NOT NULL,
  `TIMES_TRIGGERED` bigint(10) NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE IF NOT EXISTS `qrtz_simprop_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `STR_PROP_1` varchar(512) DEFAULT NULL,
  `STR_PROP_2` varchar(512) DEFAULT NULL,
  `STR_PROP_3` varchar(512) DEFAULT NULL,
  `INT_PROP_1` int(11) DEFAULT NULL,
  `INT_PROP_2` int(11) DEFAULT NULL,
  `LONG_PROP_1` bigint(20) DEFAULT NULL,
  `LONG_PROP_2` bigint(20) DEFAULT NULL,
  `DEC_PROP_1` decimal(13,4) DEFAULT NULL,
  `DEC_PROP_2` decimal(13,4) DEFAULT NULL,
  `BOOL_PROP_1` varchar(1) DEFAULT NULL,
  `BOOL_PROP_2` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `qrtz_simprop_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE IF NOT EXISTS `qrtz_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `JOB_NAME` varchar(200) NOT NULL,
  `JOB_GROUP` varchar(200) NOT NULL,
  `DESCRIPTION` varchar(250) DEFAULT NULL,
  `NEXT_FIRE_TIME` bigint(13) DEFAULT NULL,
  `PREV_FIRE_TIME` bigint(13) DEFAULT NULL,
  `PRIORITY` int(11) DEFAULT NULL,
  `TRIGGER_STATE` varchar(16) NOT NULL,
  `TRIGGER_TYPE` varchar(8) NOT NULL,
  `START_TIME` bigint(13) NOT NULL,
  `END_TIME` bigint(13) DEFAULT NULL,
  `CALENDAR_NAME` varchar(200) DEFAULT NULL,
  `MISFIRE_INSTR` smallint(2) DEFAULT NULL,
  `JOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`) USING BTREE,
  KEY `SCHED_NAME` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`) USING BTREE,
  CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) REFERENCES `qrtz_job_details` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE IF NOT EXISTS `schedule` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `cycle` int(11) DEFAULT NULL,
  `intervals` int(11) DEFAULT NULL,
  `minute` int(11) DEFAULT NULL,
  `hour` int(11) DEFAULT NULL,
  `week` varchar(255) DEFAULT NULL,
  `cron` varchar(255) DEFAULT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `topology` mediumtext NOT NULL,
  `send_email` bit(1) NOT NULL,
  `dingding_hooks` varchar(255) DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `real_fire_time` datetime DEFAULT NULL,
  `need_fire_time` datetime DEFAULT NULL,
  `next_fire_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `create_by` int(11) NOT NULL,
  `update_time` datetime NOT NULL,
  `update_by` int(11) NOT NULL,
  `keyword` text NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `name_unique` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE IF NOT EXISTS `schedule_snapshot` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `schedule_id` int(11) NOT NULL,
  `snapshot_time` datetime NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `cycle` int(11) DEFAULT NULL,
  `intervals` int(11) DEFAULT NULL,
  `minute` int(11) DEFAULT NULL,
  `hour` int(11) DEFAULT NULL,
  `week` varchar(255) DEFAULT NULL,
  `cron` varchar(255) DEFAULT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `topology` mediumtext NOT NULL,
  `send_email` bit(1) NOT NULL,
  `dingding_hooks` varchar(255) DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `schedule_id_index` (`schedule_id`),
  KEY `snapshot_time_index` (`snapshot_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE IF NOT EXISTS `script` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `type` varchar(255) NOT NULL,
  `schedule_id` int(11) DEFAULT NULL,
  `schedule_top_node_id` varchar(255) DEFAULT NULL,
  `monitor_id` int(11) DEFAULT NULL,
  `monitor_enabled` bit(1) DEFAULT NULL,
  `agent_id` int(11) DEFAULT NULL,
  `cluster_id` int(11) DEFAULT NULL,
  `timeout` int(11) NOT NULL,
  `content` text NOT NULL,
  `input` varchar(255) DEFAULT NULL,
  `output` varchar(255) DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `create_by` int(11) NOT NULL,
  `update_time` datetime NOT NULL,
  `update_by` int(11) NOT NULL,
  `user` varchar(255) DEFAULT NULL,
  `queue` varchar(255) DEFAULT NULL,
  `app` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `schedule_id_index` (`schedule_id`),
  KEY `schedule_top_node_id_index` (`schedule_top_node_id`),
  KEY `monitor_id_index` (`monitor_id`),
  KEY `cluster_id_index` (`cluster_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE IF NOT EXISTS `script_history` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `schedule_id` int(11) DEFAULT NULL,
  `schedule_top_node_id` varchar(255) DEFAULT NULL,
  `schedule_snapshot_id` int(11) DEFAULT NULL,
  `schedule_instance_id` varchar(255) DEFAULT NULL,
  `schedule_retry_num` int(11) DEFAULT NULL,
  `schedule_history_mode` varchar(255) DEFAULT NULL,
  `schedule_history_time` datetime DEFAULT NULL,
  `monitor_id` int(11) DEFAULT NULL,
  `script_id` int(11) DEFAULT NULL,
  `script_type` varchar(255) NOT NULL,
  `agent_id` int(11) DEFAULT NULL,
  `cluster_id` int(11) DEFAULT NULL,
  `timeout` int(11) NOT NULL,
  `content` text NOT NULL,
  `outputs` text,
  `errors` text,
  `create_time` datetime NOT NULL,
  `create_by` int(11) NOT NULL,
  `start_time` datetime DEFAULT NULL,
  `finish_time` datetime DEFAULT NULL,
  `state` varchar(255) NOT NULL,
  `steps` varchar(255) NOT NULL,
  `job_id` varchar(255) DEFAULT NULL,
  `job_url` varchar(255) DEFAULT NULL,
  `job_final_status` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `schedule_id_index` (`schedule_id`),
  KEY `schedule_top_node_id_index` (`schedule_top_node_id`),
  KEY `schedule_instance_id_index` (`schedule_instance_id`),
  KEY `script_id_index` (`script_id`),
  KEY `cluster_id_index` (`cluster_id`),
  KEY `create_time_index` (`create_time`),
  KEY `state_index` (`state`),
  KEY `job_final_status_index` (`job_final_status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE IF NOT EXISTS `yarn_app` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `cluster_id` int(11) NOT NULL,
  `user_id` int(11) DEFAULT NULL,
  `app_id` varchar(255) NOT NULL,
  `user` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `queue` varchar(255) NOT NULL,
  `state` varchar(255) NOT NULL,
  `final_status` varchar(255) NOT NULL,
  `tracking_url` varchar(255) DEFAULT NULL,
  `application_type` varchar(255) NOT NULL,
  `started_time` datetime NOT NULL,
  `refresh_time` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


ALTER TABLE `script`
	CHANGE COLUMN `monitor_id` `monitor_id` INT(11) NULL DEFAULT NULL AFTER `type`,
	CHANGE COLUMN `monitor_enabled` `monitor_enabled` BIT(1) NULL DEFAULT NULL AFTER `monitor_id`,
	CHANGE COLUMN `cluster_id` `cluster_id` INT(11) NULL DEFAULT NULL AFTER `schedule_top_node_id`;


ALTER TABLE `script_history`
	CHANGE COLUMN `monitor_id` `monitor_id` INT(11) NULL DEFAULT NULL AFTER `id`,
	CHANGE COLUMN `schedule_retry_num` `schedule_failure_handle` VARCHAR(255) NULL DEFAULT NULL AFTER `schedule_instance_id`,
	CHANGE COLUMN `schedule_history_mode` `schedule_supplement` BIT NULL DEFAULT NULL COLLATE 'utf8_general_ci' AFTER `schedule_failure_handle`,
	CHANGE COLUMN `schedule_history_time` `schedule_operate_time` DATETIME NULL DEFAULT NULL AFTER `schedule_supplement`,
	ADD COLUMN `previous_schedule_top_node_id` VARCHAR(255) NULL DEFAULT NULL AFTER `schedule_operate_time`,
	ADD COLUMN `script_name` VARCHAR(255) NOT NULL AFTER `script_id`,
	CHANGE COLUMN `cluster_id` `cluster_id` INT(11) NULL DEFAULT NULL AFTER `script_type`,
	CHANGE COLUMN `state` `state` VARCHAR(255) NOT NULL COLLATE 'utf8_general_ci' AFTER `content`,
	CHANGE COLUMN `steps` `steps` VARCHAR(255) NOT NULL COLLATE 'utf8_general_ci' AFTER `state`,
	ADD COLUMN `job_params` VARCHAR(255) NULL DEFAULT NULL AFTER `finish_time`,
	DROP COLUMN `schedule_snapshot_id`;


DROP TABLE `schedule_snapshot`;


ALTER TABLE `script_history`
 DROP COLUMN `schedule_supplement`,
 DROP COLUMN `schedule_operate_time`,
 ADD COLUMN `schedule_runnable` bit NULL AFTER `schedule_failure_handle`,
 ADD COLUMN `schedule_retry` bit NULL AFTER `schedule_runnable`,
 ADD COLUMN `schedule_empty` bit NULL AFTER `schedule_retry`,
 ADD COLUMN `schedule_rerun` bit NULL AFTER `schedule_empty`,
 ADD COLUMN `business_time` datetime NOT NULL AFTER `create_by`,
 ADD COLUMN `delay_time` datetime NULL AFTER `business_time`,
 ADD COLUMN `submit_time` datetime NULL AFTER `delay_time`;


ALTER TABLE `script_history`
 ADD INDEX `schedule_runnable_index` (`schedule_runnable`) USING BTREE,
 ADD INDEX `delay_time_index` (`delay_time`) USING BTREE;


/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
