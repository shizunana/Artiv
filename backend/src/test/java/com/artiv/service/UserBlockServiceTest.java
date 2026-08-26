package com.artiv.service;

import com.artiv.common.BusinessException;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.User;
import com.artiv.entity.UserBlock;
import com.artiv.mapper.ApplicationMapper;
import com.artiv.mapper.InvitationMapper;
import com.artiv.mapper.OrderInfoMapper;
import com.artiv.mapper.UserBlockMapper;
import com.artiv.mapper.UserFollowMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.impl.UserBlockServiceImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserBlockServiceTest {
    @Mock private UserBlockMapper userBlockMapper;
    @Mock private UserMapper userMapper;
    @Mock private OrderInfoMapper orderInfoMapper;
    @Mock private UserFollowMapper userFollowMapper;
    @Mock private InvitationMapper invitationMapper;
    @Mock private ApplicationMapper applicationMapper;
    @Mock private AccountRestrictionService accountRestrictionService;
    @InjectMocks private UserBlockServiceImpl userBlockService;

    @BeforeAll
    static void initializeMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserBlock.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), OrderInfo.class);
    }

    @Test
    void blockEndsPendingRelationshipsWhenThereIsNoActiveOrder() {
        User target = new User();
        target.setId(2L);
        target.setDeleted(0);
        when(userBlockMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectById(2L)).thenReturn(target);
        when(orderInfoMapper.selectCount(any())).thenReturn(0L);

        assertTrue(userBlockService.block(1L, 2L).isBlocked());

        verify(userBlockMapper).insert(any(UserBlock.class));
        verify(userFollowMapper).deleteBetweenUsers(1L, 2L);
        verify(invitationMapper).invalidateBetweenUsers(any(), any(), any());
        verify(applicationMapper).endBetweenUsers(any(), any(), any());
    }

    @Test
    void blockRejectsUsersWithNonTerminalOrder() {
        User target = new User();
        target.setId(2L);
        target.setDeleted(0);
        when(userBlockMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectById(2L)).thenReturn(target);
        when(orderInfoMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> userBlockService.block(1L, 2L));
        verify(userBlockMapper, never()).insert(any(UserBlock.class));
        verify(invitationMapper, never()).invalidateBetweenUsers(any(), any(), any());
    }
}
