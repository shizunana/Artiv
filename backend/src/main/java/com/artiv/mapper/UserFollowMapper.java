package com.artiv.mapper;

import com.artiv.entity.User;
import com.artiv.entity.UserFollow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserFollowMapper extends BaseMapper<UserFollow> {

    @Select("""
            <script>
            SELECT u.*
            FROM user_follow f
            JOIN user u ON u.id = f.painter_id
            WHERE f.user_id = #{userId}
              AND u.deleted = 0
              AND u.status = 0
              AND u.cert_status = 2
            <if test="keyword != null and keyword != ''">
              AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
                OR u.bio LIKE CONCAT('%', #{keyword}, '%')
                OR u.tags LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <foreach collection="subjects" item="subject">
              AND FIND_IN_SET(#{subject}, REPLACE(COALESCE(u.tags, ''), ' ', '')) &gt; 0
            </foreach>
            ORDER BY f.created_at DESC, f.id DESC
            </script>
            """)
    Page<User> selectMine(Page<User> page,
                          @Param("userId") Long userId,
                          @Param("subjects") List<String> subjects,
                          @Param("keyword") String keyword);
}
