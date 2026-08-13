create database nody;
use nody;

CREATE TABLE users (
    userid VARCHAR(50) NOT NULL unique,
    passwd VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL unique,
    email VARCHAR(100) NOT NULL unique,
    phone VARCHAR(20) NOT NULL unique,
    banned_until datetime default null,
    point int default 0 not null,
    PRIMARY KEY (userid)
);
select * from users;
UPDATE `nody`.`users` SET `banned_until` = null WHERE (`userid` = 'user20');

create table boards (
	idx int auto_increment,
    title varchar(100) not null,
    contents text not null,
    writer varchar(50) not null,
    viewcount int default 0,
    regdate datetime default now(),
    primary key(idx),
    foreign key (writer) references users(userid)
);
select * from boards;
drop table boards;

create table comments(
	idx int auto_increment,
    boardby int not null,
    writeby varchar(50) not null,
    texts text not null,
    times datetime default now(),
    primary key(idx),
    foreign key (writeby) references users(userid),
    foreign key (boardby) references boards(idx) ON DELETE CASCADE
);
select * from comments;
drop table comments;

create table images(
	idx INT AUTO_INCREMENT PRIMARY KEY,
    boardimg int not null,
    content_type VARCHAR(100) NOT NULL,
    image_data LONGBLOB NOT NULL,
    foreign key (boardimg) references boards(idx) ON DELETE CASCADE 
);
SHOW CREATE TABLE images;
select * from images;
drop table images;

create table channels(
	idx int auto_increment,
    channel_title varchar(100) not null,
    sport varchar(50) not null,
    channel_comment text not null,
    start_date date not null,
    end_date date not null,
    modes varchar(50) not null,
    complete boolean default false,
    channel_build boolean default false,
    countuser int default 0,
    goal int default 0,
    primary key(idx)
);
select * from channels;
drop table channels;

create table channeluser(
	idx int  auto_increment,
    channel_id int not null,
    channel_user varchar(50) not null,
    primary key(idx),
    foreign key (channel_id) references channels(idx) ON DELETE CASCADE,
    foreign key (channel_user) references users(userid)
);
select * from channeluser;
drop table channeluser;

SHOW CREATE TABLE channeluser;

CREATE TABLE fitness_auth (
    idx INT AUTO_INCREMENT PRIMARY KEY,
    userid VARCHAR(50) NOT NULL,
    channel_id INT NOT NULL,
    auth_date DATE NOT NULL,         -- 인증 날짜
    goal_met BOOLEAN DEFAULT false,  -- 목표 달성 여부
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    achieved_value double default 0,
    FOREIGN KEY (userid) REFERENCES users(userid) ON DELETE CASCADE,
    FOREIGN KEY (channel_id) REFERENCES channels(idx) ON DELETE CASCADE,

    UNIQUE (userid, channel_id, auth_date)  -- 하루에 1회만 인증 가능
);
INSERT INTO fitness_auth (userid, channel_id, auth_date, goal_met, achieved_value)
VALUES ('user2', 1, '2025-06-27', true, 8000);
INSERT INTO fitness_auth (userid, channel_id, auth_date, goal_met, achieved_value)
VALUES ('user3', 1, '2025-06-27', true, 7500);
INSERT INTO fitness_auth (userid, channel_id, auth_date, goal_met, achieved_value)
VALUES ('user4', 1, '2025-06-27', true, 7000);
INSERT INTO fitness_auth (userid, channel_id, auth_date, goal_met, achieved_value)
VALUES ('user5', 1, '2025-06-27', true, 6500);
INSERT INTO fitness_auth (userid, channel_id, auth_date, goal_met, achieved_value)
VALUES ('user6', 1, '2025-06-27', true, 6000);
select * from fitness_auth;
drop table fitness_auth;

CREATE TABLE reports (
    idx INT AUTO_INCREMENT,
    target_type ENUM('post', 'comment', 'channel') NOT NULL,
    target_id INT NOT NULL,
    reporter VARCHAR(50) NOT NULL,
    reason TEXT NOT NULL,
    report_date datetime DEFAULT now(),
    primary key(idx),
    FOREIGN KEY (reporter) REFERENCES users(userid)
);
select * from reports;
drop table reports;



INSERT INTO reports (target_type, target_id, reporter, reason) VALUES
('post', 1, 'user34', '욕설이 포함되어 있습니다.'),
('post', 3, 'user51', '허위 정보가 포함되어 있습니다.'),
('post', 6, 'user53', '허위 정보가 포함되어 있습니다.'),
('comment', 1, 'user24', '광고성 댓글입니다.'),
('comment', 15, 'user23', '광고성 댓글입니다.'),
('comment', 12, 'user21', '광고성 댓글입니다.'),
('comment', 14, 'user22', '광고성 댓글입니다.'),
('channel', 5, 'user45', '운영자가 비정상적인 활동을 합니다.'),
('channel', 3, 'user41', '부적절한 내용의 동아리입니다.');