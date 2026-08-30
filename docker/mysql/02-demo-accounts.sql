USE `artiv`;

-- 防御性清空：演示库只允许保留三个账号，不保留任何历史业务记录。
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE `user_block`;
TRUNCATE TABLE `report_case`;
TRUNCATE TABLE `report_submission`;
TRUNCATE TABLE `report_evidence`;
TRUNCATE TABLE `work`;
TRUNCATE TABLE `stall`;
TRUNCATE TABLE `user_follow`;
TRUNCATE TABLE `work_favorite`;
TRUNCATE TABLE `stall_favorite`;
TRUNCATE TABLE `project`;
TRUNCATE TABLE `application`;
TRUNCATE TABLE `invitation`;
TRUNCATE TABLE `order_info`;
TRUNCATE TABLE `order_stage`;
TRUNCATE TABLE `order_event`;
TRUNCATE TABLE `message`;
TRUNCATE TABLE `review`;
TRUNCATE TABLE `review_image`;
TRUNCATE TABLE `review_appeal`;
TRUNCATE TABLE `artist_certification`;
TRUNCATE TABLE `order_dispute`;
TRUNCATE TABLE `order_dispute_evidence`;
TRUNCATE TABLE `admin_action_log`;
SET FOREIGN_KEY_CHECKS = 1;

DELETE FROM `user` WHERE `username` NOT IN ('planner', 'artist', 'admin');

-- BCrypt 密码均为 123456。余额为 0，避免空白演示环境自带交易状态。
UPDATE `user`
SET `password` = '$2a$10$RvkldycPzn6PoC2cFW/.GOWrpFtZL.RyuLaYdPZKrFEIv1GiJAY1e',
    `balance` = 0.00,
    `status` = 0,
    `ban_reason` = NULL,
    `banned_by` = NULL,
    `banned_at` = NULL,
    `banned_until` = NULL,
    `deleted` = 0;

UPDATE `user`
SET `role` = 1, `cert_status` = 0, `nickname` = '东云绘名'
WHERE `username` = 'planner';

UPDATE `user`
SET `role` = 2,
    `cert_status` = 2,
    `nickname` = '静名御折木',
    `artist_name` = '静名御折木',
    `artist_site` = NULL
WHERE `username` = 'artist';

UPDATE `user`
SET `role` = 3, `cert_status` = 0, `nickname` = '管理员'
WHERE `username` = 'admin';

ALTER TABLE `user` AUTO_INCREMENT = 4;
