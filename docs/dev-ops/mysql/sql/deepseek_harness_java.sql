# ************************************************************
# Sequel Ace SQL dump
# 版本号： 20094
#
# https://sequel-ace.com/
# https://github.com/Sequel-Ace/Sequel-Ace
#
# 主机: 127.0.0.1 (MySQL 8.0.42)
# 数据库: deepseek_harness_java
# 生成时间: 2026-09-19 00:35:58 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
SET NAMES utf8mb4;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE='NO_AUTO_VALUE_ON_ZERO', SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE database if NOT EXISTS `deepseek_harness_java` default character set utf8mb4 collate utf8mb4_0900_ai_ci;
use `deepseek_harness_java`;

# 转储表 collaboration_artifact
# ------------------------------------------------------------

DROP TABLE IF EXISTS `collaboration_artifact`;

CREATE TABLE `collaboration_artifact` (
  `id` varchar(64) NOT NULL,
  `room_id` varchar(64) NOT NULL,
  `task_id` varchar(64) DEFAULT NULL,
  `producer_id` varchar(64) DEFAULT NULL,
  `kind` varchar(24) NOT NULL,
  `title` varchar(255) NOT NULL,
  `version` int NOT NULL DEFAULT '1',
  `content` text,
  `trace_id` varchar(64) DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_artifact_room` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# 转储表 collaboration_room
# ------------------------------------------------------------

DROP TABLE IF EXISTS `collaboration_room`;

CREATE TABLE `collaboration_room` (
  `id` varchar(64) NOT NULL,
  `workspace_id` varchar(128) NOT NULL,
  `title` varchar(255) NOT NULL,
  `objective` text,
  `orchestration_mode` varchar(24) NOT NULL DEFAULT 'MANUAL',
  `run_state` varchar(16) NOT NULL DEFAULT 'idle',
  `created_at` timestamp(3) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# 转储表 collaboration_task
# ------------------------------------------------------------

DROP TABLE IF EXISTS `collaboration_task`;

CREATE TABLE `collaboration_task` (
  `id` varchar(64) NOT NULL,
  `room_id` varchar(64) NOT NULL,
  `parent_task_id` varchar(64) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `instruction` text NOT NULL,
  `assigned_to` varchar(64) DEFAULT NULL,
  `requested_by` varchar(64) NOT NULL,
  `state` varchar(24) NOT NULL DEFAULT 'READY',
  `depends_on` varchar(512) DEFAULT NULL,
  `input_artifacts` varchar(512) DEFAULT NULL,
  `output_artifacts` varchar(512) DEFAULT NULL,
  `remote_session_id` varchar(128) DEFAULT NULL,
  `trace_id` varchar(64) NOT NULL,
  `error_summary` varchar(1024) DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL,
  `updated_at` timestamp(3) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_task_room_state` (`room_id`,`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# 转储表 digital_human
# ------------------------------------------------------------

DROP TABLE IF EXISTS `digital_human`;

CREATE TABLE `digital_human` (
  `id` varchar(64) NOT NULL,
  `display_name` varchar(128) NOT NULL,
  `avatar_ref` varchar(255) DEFAULT NULL,
  `purpose` varchar(1024) NOT NULL,
  `role_tags` varchar(512) DEFAULT NULL,
  `theme_color` varchar(16) DEFAULT NULL,
  `approval_policy` varchar(32) NOT NULL DEFAULT 'WRITE_REQUIRES_APPROVAL',
  `concurrency_limit` int NOT NULL DEFAULT '1',
  `created_at` timestamp(3) NOT NULL,
  `updated_at` timestamp(3) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# 转储表 digital_human_endpoint
# ------------------------------------------------------------

DROP TABLE IF EXISTS `digital_human_endpoint`;

CREATE TABLE `digital_human_endpoint` (
  `id` varchar(64) NOT NULL,
  `digital_human_id` varchar(64) NOT NULL,
  `endpoint_type` varchar(24) NOT NULL,
  `base_url` varchar(512) DEFAULT NULL,
  `credential_ref` varchar(128) DEFAULT NULL,
  `protocol_version` varchar(16) DEFAULT NULL,
  `agent_card` text,
  `health_state` varchar(16) NOT NULL DEFAULT 'unknown',
  `last_checked_at` timestamp(3) NULL DEFAULT NULL,
  `latency_ms` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_dh_endpoint_human` (`digital_human_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# 转储表 harness_goal_state
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_goal_state`;

CREATE TABLE `harness_goal_state` (
  `session_id` varchar(64) NOT NULL,
  `goal_id` varchar(128) NOT NULL,
  `revision` bigint NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text,
  `status` varchar(32) NOT NULL,
  `blocked_reason` text,
  `max_rounds` int NOT NULL,
  `rounds_started` int NOT NULL,
  `continuation_enabled` tinyint(1) NOT NULL,
  `created_at` timestamp(3) NOT NULL,
  `updated_at` timestamp(3) NOT NULL,
  PRIMARY KEY (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# 转储表 harness_model_setting
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_model_setting`;

CREATE TABLE `harness_model_setting` (
  `channel_code` varchar(64) NOT NULL,
  `display_name` varchar(128) NOT NULL,
  `provider_code` varchar(64) NOT NULL,
  `model_code` varchar(128) NOT NULL,
  `base_url` varchar(255) DEFAULT NULL,
  `api_key_ref` varchar(128) DEFAULT NULL,
  `protocol` varchar(32) NOT NULL DEFAULT 'openai',
  `enabled` tinyint(1) NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '0',
  `updated_at` timestamp(3) NOT NULL,
  PRIMARY KEY (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# 转储表 harness_plugin_config
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_plugin_config`;

CREATE TABLE `harness_plugin_config` (
  `plugin_id` varchar(128) NOT NULL,
  `config_key` varchar(128) NOT NULL,
  `config_value` text,
  `config_layer` varchar(32) NOT NULL,
  `updated_at` timestamp(3) NOT NULL,
  PRIMARY KEY (`plugin_id`,`config_key`,`config_layer`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# 转储表 harness_plugin_installation
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_plugin_installation`;

CREATE TABLE `harness_plugin_installation` (
  `plugin_id` varchar(128) NOT NULL,
  `display_name` varchar(255) NOT NULL,
  `plugin_version` varchar(64) NOT NULL,
  `runtime_type` varchar(64) NOT NULL,
  `install_mode` varchar(64) NOT NULL,
  `source_path` varchar(512) NOT NULL,
  `entrypoint` varchar(512) NOT NULL,
  `status` varchar(32) NOT NULL,
  `installed_at` timestamp(3) NOT NULL,
  PRIMARY KEY (`plugin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# 转储表 harness_plugin_runtime_binding
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_plugin_runtime_binding`;

CREATE TABLE `harness_plugin_runtime_binding` (
  `plugin_id` varchar(128) NOT NULL,
  `package_type` varchar(64) NOT NULL,
  `manifest_path` varchar(512) DEFAULT NULL,
  `working_directory` varchar(512) DEFAULT NULL,
  `runtime_command` varchar(1024) DEFAULT NULL,
  `bridge_ready` tinyint(1) NOT NULL,
  `message` text NOT NULL,
  `checked_at` timestamp(3) NOT NULL,
  PRIMARY KEY (`plugin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# 转储表 harness_session
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_session`;

CREATE TABLE `harness_session` (
  `session_id` varchar(64) NOT NULL,
  `workspace_id` varchar(128) NOT NULL,
  `profile_code` varchar(64) NOT NULL,
  `execution_mode` varchar(32) NOT NULL,
  `resolved_model` varchar(128) NOT NULL,
  `channel_code` varchar(64) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `created_at` timestamp(3) NOT NULL,
  PRIMARY KEY (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# 转储表 harness_session_event
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_session_event`;

CREATE TABLE `harness_session_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL,
  `event_type` varchar(128) NOT NULL,
  `payload` text NOT NULL,
  `occurred_at` timestamp(3) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_harness_session_event_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# 转储表 harness_session_event_log
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_session_event_log`;

CREATE TABLE `harness_session_event_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL,
  `seq` bigint NOT NULL,
  `event_type` varchar(64) NOT NULL,
  `event_time` timestamp(3) NOT NULL,
  `payload_json` longtext,
  `surface_op` varchar(16) DEFAULT NULL,
  `source_event_seqs` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `session_id` (`session_id`,`seq`),
  KEY `idx_harness_session_event_log_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# 转储表 harness_session_header
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_session_header`;

CREATE TABLE `harness_session_header` (
  `session_id` varchar(64) NOT NULL,
  `version` int NOT NULL,
  `created_at` timestamp(3) NOT NULL,
  `cwd` varchar(512) DEFAULT NULL,
  `parent_session` varchar(64) DEFAULT NULL,
  `seed_length` int NOT NULL DEFAULT '0',
  `origin` varchar(32) DEFAULT NULL,
  `delegation_depth` int NOT NULL DEFAULT '0',
  `agent_preset` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# 转储表 harness_task
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_task`;

CREATE TABLE `harness_task` (
  `task_id` varchar(64) NOT NULL,
  `session_id` varchar(64) NOT NULL,
  `prompt` text NOT NULL,
  `enabled_tools_json` text NOT NULL,
  `status` varchar(32) NOT NULL,
  `submitted_at` timestamp(3) NOT NULL,
  PRIMARY KEY (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# 转储表 harness_tool_catalog
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_tool_catalog`;

CREATE TABLE `harness_tool_catalog` (
  `tool_code` varchar(128) NOT NULL,
  `display_name` varchar(255) NOT NULL,
  `description` text,
  `category` varchar(64) NOT NULL,
  `requires_approval` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`tool_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

LOCK TABLES `harness_tool_catalog` WRITE;
/*!40000 ALTER TABLE `harness_tool_catalog` DISABLE KEYS */;

INSERT INTO `harness_tool_catalog` (`tool_code`, `display_name`, `description`, `category`, `requires_approval`)
VALUES
	('fs.read','Read File','Read file contents from the workspace.','fs',0),
	('fs.write','Write File','Write or overwrite file contents in the workspace.','fs',1),
	('mcp.call','MCP Tool','Invoke an MCP server tool.','mcp',1),
	('shell.exec','Execute Shell','Run a shell command in the sandbox.','shell',1),
	('web.search','Web Search','Search the web for information.','web',0);

/*!40000 ALTER TABLE `harness_tool_catalog` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 harness_tool_profile_binding
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_tool_profile_binding`;

CREATE TABLE `harness_tool_profile_binding` (
  `profile_code` varchar(64) NOT NULL,
  `tool_code` varchar(128) NOT NULL,
  PRIMARY KEY (`profile_code`,`tool_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

LOCK TABLES `harness_tool_profile_binding` WRITE;
/*!40000 ALTER TABLE `harness_tool_profile_binding` DISABLE KEYS */;

INSERT INTO `harness_tool_profile_binding` (`profile_code`, `tool_code`)
VALUES
	('headless','fs.read'),
	('headless','web.search'),
	('web','fs.read'),
	('web','web.search');

/*!40000 ALTER TABLE `harness_tool_profile_binding` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 room_event
# ------------------------------------------------------------

DROP TABLE IF EXISTS `room_event`;

CREATE TABLE `room_event` (
  `id` varchar(64) NOT NULL,
  `room_id` varchar(64) NOT NULL,
  `seq` bigint NOT NULL,
  `task_id` varchar(64) DEFAULT NULL,
  `participant_id` varchar(64) DEFAULT NULL,
  `digital_human_id` varchar(64) DEFAULT NULL,
  `event_type` varchar(48) NOT NULL,
  `occurred_at` timestamp(3) NOT NULL,
  `visibility` varchar(16) NOT NULL DEFAULT 'room',
  `payload` text,
  `trace_id` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_event_seq` (`room_id`,`seq`),
  KEY `idx_event_room_seq` (`room_id`,`seq`),
  KEY `idx_event_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# 转储表 room_participant
# ------------------------------------------------------------

DROP TABLE IF EXISTS `room_participant`;

CREATE TABLE `room_participant` (
  `id` varchar(64) NOT NULL,
  `room_id` varchar(64) NOT NULL,
  `digital_human_id` varchar(64) NOT NULL,
  `room_role` varchar(16) NOT NULL DEFAULT 'member',
  `presence` varchar(24) NOT NULL DEFAULT 'idle',
  `active_task_label` varchar(255) DEFAULT NULL,
  `joined_at` timestamp(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_participant` (`room_id`,`digital_human_id`),
  KEY `idx_participant_room` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;




/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
