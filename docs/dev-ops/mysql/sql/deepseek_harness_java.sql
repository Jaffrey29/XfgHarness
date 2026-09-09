# ************************************************************
# Sequel Ace SQL dump
# 版本号： 20099
#
# https://sequel-ace.com/
# https://github.com/Sequel-Ace/Sequel-Ace
#
# 主机: 192.168.1.108 (MySQL 8.0.32)
# 数据库: deepseek_harness_java
# 生成时间: 2026-09-09 12:22:01 +0000
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

# 转储表 harness_goal_state
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_goal_state`;

CREATE TABLE `harness_goal_state` (
  `session_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `goal_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `revision` bigint NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `blocked_reason` text COLLATE utf8mb4_unicode_ci,
  `max_rounds` int NOT NULL,
  `rounds_started` int NOT NULL,
  `continuation_enabled` tinyint(1) NOT NULL,
  `created_at` timestamp(3) NOT NULL,
  `updated_at` timestamp(3) NOT NULL,
  PRIMARY KEY (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# 转储表 harness_model_setting
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_model_setting`;

CREATE TABLE `harness_model_setting` (
  `profile_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `provider_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `model_code` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `base_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `api_key_ref` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL,
  `updated_at` timestamp(3) NOT NULL,
  PRIMARY KEY (`profile_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

LOCK TABLES `harness_model_setting` WRITE;
/*!40000 ALTER TABLE `harness_model_setting` DISABLE KEYS */;

INSERT INTO `harness_model_setting` (`profile_code`, `provider_code`, `model_code`, `base_url`, `api_key_ref`, `enabled`, `updated_at`)
VALUES
	('deepseek','deepseek','agnes-2.0-flash','http://127.0.0.1:8777/v1','sk-waliapi-7ae2ec845ed04815b6374a058d025439',1,'2026-09-09 09:21:10.758'),
	('headless','deepseek','glm-5.3-flash','http://127.0.0.1:8777/v1','sk-waliapi-7ae2ec845ed04815b6374a058d025439',1,'2026-09-09 09:20:27.191'),
	('web','deepseek','glm-5.3-flash','http://127.0.0.1:8777/v1','sk-waliapi-7ae2ec845ed04815b6374a058d025439',1,'2026-09-09 09:20:32.408');

/*!40000 ALTER TABLE `harness_model_setting` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 harness_plugin_config
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_plugin_config`;

CREATE TABLE `harness_plugin_config` (
  `plugin_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `config_key` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `config_value` text COLLATE utf8mb4_unicode_ci,
  `config_layer` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp(3) NOT NULL,
  PRIMARY KEY (`plugin_id`,`config_key`,`config_layer`),
  CONSTRAINT `fk_harness_plugin_config_plugin` FOREIGN KEY (`plugin_id`) REFERENCES `harness_plugin_installation` (`plugin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# 转储表 harness_plugin_installation
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_plugin_installation`;

CREATE TABLE `harness_plugin_installation` (
  `plugin_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `plugin_version` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `runtime_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `install_mode` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_path` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL,
  `entrypoint` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `installed_at` timestamp(3) NOT NULL,
  PRIMARY KEY (`plugin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

LOCK TABLES `harness_plugin_installation` WRITE;
/*!40000 ALTER TABLE `harness_plugin_installation` DISABLE KEYS */;

INSERT INTO `harness_plugin_installation` (`plugin_id`, `display_name`, `plugin_version`, `runtime_type`, `install_mode`, `source_path`, `entrypoint`, `status`, `installed_at`)
VALUES
	('dsh-demo-plugin','DSH Demo Plugin','1.0.0','DSH_NODE_BRIDGE','NODE_BRIDGE','/Users/fuzhengwei/DevOps/deepseek-harness-java/plugins/dsh-demo-plugin','index.js','ACTIVE','2026-09-08 07:06:51.298'),
	('mall-weekend-assistant','2D Weekend Mall Assistant','1.0.0-SNAPSHOT','JAVA_NATIVE','NATIVE','/Users/fuzhengwei/DevOps/deepseek-harness-java/plugins/mall-weekend-assistant.jar','mall-weekend-assistant.jar','ACTIVE','2026-09-09 15:03:05.469'),
	('sample-tools','Sample Tools Plugin','1.0.0','JAVA_NATIVE','NATIVE','/Users/fuzhengwei/DevOps/deepseek-harness-java/plugins/sample-tools.jar','sample-tools.jar','ACTIVE','2026-08-29 06:57:42.456');

/*!40000 ALTER TABLE `harness_plugin_installation` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 harness_plugin_runtime_binding
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_plugin_runtime_binding`;

CREATE TABLE `harness_plugin_runtime_binding` (
  `plugin_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `package_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `manifest_path` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `working_directory` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `runtime_command` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bridge_ready` tinyint(1) NOT NULL,
  `message` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `checked_at` timestamp(3) NOT NULL,
  PRIMARY KEY (`plugin_id`),
  CONSTRAINT `fk_harness_plugin_runtime_binding_plugin` FOREIGN KEY (`plugin_id`) REFERENCES `harness_plugin_installation` (`plugin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

LOCK TABLES `harness_plugin_runtime_binding` WRITE;
/*!40000 ALTER TABLE `harness_plugin_runtime_binding` DISABLE KEYS */;

INSERT INTO `harness_plugin_runtime_binding` (`plugin_id`, `package_type`, `manifest_path`, `working_directory`, `runtime_command`, `bridge_ready`, `message`, `checked_at`)
VALUES
	('dsh-demo-plugin','DSH_PACKAGE','/Users/fuzhengwei/DevOps/deepseek-harness-java/plugins/dsh-demo-plugin/package.json','/Users/fuzhengwei/DevOps/deepseek-harness-java/plugins/dsh-demo-plugin','node /Users/fuzhengwei/DevOps/deepseek-harness-java/plugins/dsh-demo-plugin/index.js',1,'dsh package detected with executable Node entrypoint','2026-09-09 19:18:53.995'),
	('mall-weekend-assistant','DSH_PACKAGE','/Users/fuzhengwei/DevOps/deepseek-harness-java/plugins/mall-weekend-assistant.jar','/Users/fuzhengwei/DevOps/deepseek-harness-java/plugins','java-in-process:mall-weekend-assistant',1,'java-native plugin JAR ready for in-process loading','2026-09-09 19:18:53.787'),
	('sample-tools','DSH_PACKAGE','/Users/fuzhengwei/DevOps/deepseek-harness-java/plugins/sample-tools.jar','/Users/fuzhengwei/DevOps/deepseek-harness-java/plugins','java-in-process:sample-tools',1,'java-native plugin JAR ready for in-process loading','2026-09-09 19:19:54.941');

/*!40000 ALTER TABLE `harness_plugin_runtime_binding` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 harness_session
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_session`;

CREATE TABLE `harness_session` (
  `session_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `workspace_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `profile_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `execution_mode` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `resolved_model` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp(3) NOT NULL,
  PRIMARY KEY (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

LOCK TABLES `harness_session` WRITE;
/*!40000 ALTER TABLE `harness_session` DISABLE KEYS */;

INSERT INTO `harness_session` (`session_id`, `workspace_id`, `profile_code`, `execution_mode`, `resolved_model`, `status`, `created_at`)
VALUES
	('3ed4595a-35ea-47fd-b9a0-ba67a5342506','default','web','WEB','glm_for_coding','COMPLETED','2026-08-27 20:46:20.226');

/*!40000 ALTER TABLE `harness_session` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 harness_session_event
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_session_event`;

CREATE TABLE `harness_session_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_type` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `payload` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `occurred_at` timestamp(3) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_harness_session_event_session` (`session_id`),
  CONSTRAINT `fk_harness_session_event_session` FOREIGN KEY (`session_id`) REFERENCES `harness_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

LOCK TABLES `harness_session_event` WRITE;
/*!40000 ALTER TABLE `harness_session_event` DISABLE KEYS */;

INSERT INTO `harness_session_event` (`id`, `session_id`, `event_type`, `payload`, `occurred_at`)
VALUES
	(1,'3ed4595a-35ea-47fd-b9a0-ba67a5342506','task.submitted','taskId=c37da38f-a976-4dbe-8af6-480d4677e6d4,profile=web','2026-08-27 20:46:21.459'),
	(2,'3ed4595a-35ea-47fd-b9a0-ba67a5342506','user.message','hello','2026-08-27 20:46:21.459'),
	(3,'3ed4595a-35ea-47fd-b9a0-ba67a5342506','assistant.message','任务已提交，等待审批。profile \'web\' requires human approval','2026-08-27 20:46:21.459'),
	(4,'3ed4595a-35ea-47fd-b9a0-ba67a5342506','task.approval.required','profile \'web\' requires human approval','2026-08-27 20:46:21.459'),
	(5,'3ed4595a-35ea-47fd-b9a0-ba67a5342506','task.approved','approved from web console','2026-08-27 20:59:03.082'),
	(6,'3ed4595a-35ea-47fd-b9a0-ba67a5342506','task.execution.started','model=glm_for_coding','2026-08-27 20:59:03.553'),
	(7,'3ed4595a-35ea-47fd-b9a0-ba67a5342506','assistant.message','你好！我是Z.ai训练的GLM大语言模型，很高兴能与你交流。有什么我能帮助你的问题或者你想了解的话题吗？无论是工作学习上的疑问，还是日常生活中的好奇，我都很乐意为你提供帮助。','2026-08-27 20:59:20.674'),
	(8,'3ed4595a-35ea-47fd-b9a0-ba67a5342506','task.execution.completed','model=glm_for_coding','2026-08-27 20:59:20.674');

/*!40000 ALTER TABLE `harness_session_event` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 harness_session_event_log
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_session_event_log`;

CREATE TABLE `harness_session_event_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `seq` bigint NOT NULL,
  `event_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_time` timestamp(3) NOT NULL,
  `payload_json` text COLLATE utf8mb4_unicode_ci,
  `surface_op` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source_event_seqs` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `session_id` (`session_id`,`seq`),
  KEY `idx_harness_session_event_log_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# 转储表 harness_session_header
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_session_header`;

CREATE TABLE `harness_session_header` (
  `session_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` int NOT NULL,
  `created_at` timestamp(3) NOT NULL,
  `cwd` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `parent_session` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `seed_length` int NOT NULL DEFAULT '0',
  `origin` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `delegation_depth` int NOT NULL DEFAULT '0',
  `agent_preset` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# 转储表 harness_task
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_task`;

CREATE TABLE `harness_task` (
  `task_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `session_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `prompt` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `enabled_tools_json` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `submitted_at` timestamp(3) NOT NULL,
  PRIMARY KEY (`task_id`),
  KEY `fk_harness_task_session` (`session_id`),
  CONSTRAINT `fk_harness_task_session` FOREIGN KEY (`session_id`) REFERENCES `harness_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

LOCK TABLES `harness_task` WRITE;
/*!40000 ALTER TABLE `harness_task` DISABLE KEYS */;

INSERT INTO `harness_task` (`task_id`, `session_id`, `prompt`, `enabled_tools_json`, `status`, `submitted_at`)
VALUES
	('c37da38f-a976-4dbe-8af6-480d4677e6d4','3ed4595a-35ea-47fd-b9a0-ba67a5342506','hello','[]','PENDING_APPROVAL','2026-08-27 20:46:20.226');

/*!40000 ALTER TABLE `harness_task` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 harness_tool_catalog
# ------------------------------------------------------------

DROP TABLE IF EXISTS `harness_tool_catalog`;

CREATE TABLE `harness_tool_catalog` (
  `tool_code` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `category` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `requires_approval` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`tool_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
  `profile_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tool_code` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`profile_code`,`tool_code`),
  KEY `fk_harness_tool_binding_tool` (`tool_code`),
  CONSTRAINT `fk_harness_tool_binding_tool` FOREIGN KEY (`tool_code`) REFERENCES `harness_tool_catalog` (`tool_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

LOCK TABLES `harness_tool_profile_binding` WRITE;
/*!40000 ALTER TABLE `harness_tool_profile_binding` DISABLE KEYS */;

INSERT INTO `harness_tool_profile_binding` (`profile_code`, `tool_code`)
VALUES
	('headless','fs.read'),
	('web','fs.read'),
	('headless','web.search'),
	('web','web.search');

/*!40000 ALTER TABLE `harness_tool_profile_binding` ENABLE KEYS */;
UNLOCK TABLES;



/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
