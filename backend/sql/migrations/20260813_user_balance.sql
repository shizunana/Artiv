-- 订单流程测试准备：账户余额基础字段。
-- 当前仅用于读取与展示；充值、提现、冻结/解冻流水将在正式资金模块中实现。
-- MySQL 8.0.41 不支持 ADD COLUMN IF NOT EXISTS；部署时先执行下列查询：
-- SELECT COUNT(*) FROM information_schema.columns
-- WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'balance';
-- 若结果为 0，再执行本语句。
ALTER TABLE `user`
    ADD COLUMN `balance` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '账户可用余额'
    AFTER `avatar`;
