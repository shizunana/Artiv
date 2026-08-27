-- 统一旧版橱窗分类到当前 10 项类型枚举。
-- 新建橱窗接口已限制只能写入统一类型；本迁移负责清理历史和早期测试数据。
UPDATE `stall`
SET `category` = CASE `category`
  WHEN '国潮插画' THEN '平面设计'
  WHEN '角色立绘' THEN '立绘'
  WHEN 'Q版' THEN 'Q版全身'
  ELSE `category`
END,
`updated_at` = NOW()
WHERE `category` IN ('国潮插画', '角色立绘', 'Q版');
