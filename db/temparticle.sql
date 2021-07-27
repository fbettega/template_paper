-- phpMyAdmin SQL Dump
-- version 4.8.3
-- https://www.phpmyadmin.net/
--
-- Host: 5.196.65.5:53306
-- Generation Time: Jul 26, 2021 at 02:38 PM
-- Server version: 10.6.3-MariaDB-1:10.6.3+maria~focal
-- PHP Version: 7.2.8

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `temparticle`
--
CREATE DATABASE IF NOT EXISTS `temparticle` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `temparticle`;

DELIMITER $$
--
-- Functions
--
CREATE DEFINER=`root`@`%` FUNCTION `funcArticleID` (`p_title` TEXT, `p_doi` TEXT, `p_date` DATE, `p_venue` TEXT, `p_abstract` TEXT, `p_lang` TEXT) RETURNS INT(11) MODIFIES SQL DATA
    DETERMINISTIC
    SQL SECURITY INVOKER
BEGIN
	DECLARE r INT;
    SET @r = (SELECT id FROM articles WHERE doi=p_doi);
    IF (@r IS NULL)
	THEN
		INSERT INTO `articles` (`id`, `title`, `doi`, `date`, `venue`, `abstract`, `lang`)
		VALUES (NULL, p_title, p_doi, p_date, funcVenueID(p_venue), p_abstract, p_lang);
    	SET @r = LAST_INSERT_ID();
    END IF;
    RETURN(@r);
END$$

CREATE DEFINER=`root`@`%` FUNCTION `funcAuthorID` (`p_first_name` TEXT, `p_last_name` TEXT) RETURNS INT(11) MODIFIES SQL DATA
    DETERMINISTIC
    SQL SECURITY INVOKER
BEGIN
	DECLARE r INT;
    SET @r = (SELECT id FROM authors WHERE first_name=p_first_name AND last_name=p_last_name);
    IF (@r IS NULL)
	THEN
		INSERT INTO authors(id, first_name, last_name)
    	VALUES(NULL, p_first_name, p_last_name);
    	SET @r = LAST_INSERT_ID();
    END IF;
    RETURN(@r);
END$$

CREATE DEFINER=`root`@`%` FUNCTION `funcVenueID` (`p_venue` TEXT) RETURNS INT(11) MODIFIES SQL DATA
    DETERMINISTIC
    SQL SECURITY INVOKER
BEGIN
	DECLARE r INT;
    SET @r = (SELECT id FROM venues WHERE name=p_venue);
    IF (@r IS NULL)
	THEN
		INSERT INTO venues(id,name,type)
    	VALUES(NULL, p_venue, NULL);
    	SET @r = LAST_INSERT_ID();
    END IF;
    RETURN(@r);
END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `articles`
--

CREATE TABLE `articles` (
  `id` int(11) NOT NULL,
  `title` text NOT NULL,
  `doi` text NOT NULL,
  `date` date NOT NULL,
  `venue` int(11) NOT NULL,
  `abstract` text NOT NULL,
  `lang` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `authors`
--

CREATE TABLE `authors` (
  `id` int(11) NOT NULL,
  `first_name` text NOT NULL,
  `last_name` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `authorships`
--

CREATE TABLE `authorships` (
  `article` int(11) NOT NULL,
  `author` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `comparaisons`
--

CREATE TABLE `comparaisons` (
  `article_1` int(11) NOT NULL,
  `article_2` int(11) NOT NULL,
  `distance` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `venues`
--

CREATE TABLE `venues` (
  `id` int(11) NOT NULL,
  `name` text NOT NULL,
  `type` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `articles`
--
ALTER TABLE `articles`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `doi` (`doi`) USING HASH,
  ADD KEY `FK_venues` (`venue`);

--
-- Indexes for table `authors`
--
ALTER TABLE `authors`
  ADD PRIMARY KEY (`id`),
  ADD KEY `name` (`first_name`(80),`last_name`(80)) USING BTREE;

--
-- Indexes for table `authorships`
--
ALTER TABLE `authorships`
  ADD KEY `FK_authorship_author` (`author`),
  ADD KEY `FK_authorship_article` (`article`);

--
-- Indexes for table `comparaisons`
--
ALTER TABLE `comparaisons`
  ADD KEY `FK_comp_a_1` (`article_1`),
  ADD KEY `FK_comp_a_2` (`article_2`);

--
-- Indexes for table `venues`
--
ALTER TABLE `venues`
  ADD PRIMARY KEY (`id`),
  ADD KEY `name` (`name`(768));

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `articles`
--
ALTER TABLE `articles`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `authors`
--
ALTER TABLE `authors`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `venues`
--
ALTER TABLE `venues`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `articles`
--
ALTER TABLE `articles`
  ADD CONSTRAINT `FK_venues` FOREIGN KEY (`venue`) REFERENCES `venues` (`id`);

--
-- Constraints for table `authorships`
--
ALTER TABLE `authorships`
  ADD CONSTRAINT `FK_authorship_article` FOREIGN KEY (`article`) REFERENCES `articles` (`id`),
  ADD CONSTRAINT `FK_authorship_author` FOREIGN KEY (`author`) REFERENCES `authors` (`id`);

--
-- Constraints for table `comparaisons`
--
ALTER TABLE `comparaisons`
  ADD CONSTRAINT `FK_comp_a_1` FOREIGN KEY (`article_1`) REFERENCES `articles` (`id`),
  ADD CONSTRAINT `FK_comp_a_2` FOREIGN KEY (`article_2`) REFERENCES `articles` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
