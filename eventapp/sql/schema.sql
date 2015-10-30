DROP DATABASE IF EXISTS isucon5fportal;

CREATE DATABASE IF NOT EXISTS isucon5fportal;

use isucon5fportal;

CREATE TABLE IF NOT EXISTS teams (
  `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `team` varchar(128) NOT NULL UNIQUE,
  `password` varchar(32) NOT NULL,
  `account` varchar(128) NOT NULL UNIQUE,
  `benchgroup` int NOT NULL,
  `priv` int NOT NULL, -- 0:organizer, 1:teams, 2:audience
  `destination` varchar(32) DEFAULT NULL,
  `ipaddrs` text DEFAULT NULL
) DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS queue (
  `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `team_id` int NOT NULL,
  `benchgroup` int NOT NULL,
  `status` varchar(16) NOT NULL, -- waiting, running, submitted, done
  `ip_address` varchar(32) NOT NULL,
  `testset_id` int NOT NULL,
  `acked_at` timestamp DEFAULT '0000-00-00 00:00:00',
  `bench_node` varchar(64) DEFAULT NULL, 
  `submitted_at` timestamp DEFAULT '0000-00-00 00:00:00',
  `json` text
) DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS scores (
  `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `team_id` int NOT NULL,
  `summary` varchar(32) NOT NULL, -- success, fail
  `score` int NOT NULL,
  `submitted_at` timestamp,
  `json` text
) DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS highscores (
  `team_id` int NOT NULL PRIMARY KEY,
  `score` int NOT NULL,
  `submitted_at` timestamp
) DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS testsets (
  `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `json` mediumtext
) DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS messages (
  `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  -- http://getbootstrap.com/components/#alerts
  `priority` varchar(16) DEFAULT 'alert-info', -- 'alert-success', 'alert-info', 'alert-warning', 'alert-danger'
  `content` TEXT NOT NULL,
  `show_at` timestamp NOT NULL,
  `hide_at` timestamp NOT NULL
) DEFAULT CHARSET=utf8mb4;
