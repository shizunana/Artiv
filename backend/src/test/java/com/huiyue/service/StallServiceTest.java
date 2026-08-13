package com.huiyue.service;

import com.huiyue.entity.Stall;
import com.huiyue.mapper.StallMapper;
import com.huiyue.mapper.UserMapper;
import com.huiyue.service.impl.StallServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StallServiceTest {

    @Mock
    private StallMapper stallMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private StallServiceImpl stallService;

    @Test
    void deleteTakesStallOfflineBeforeLogicalDeletion() {
        Stall stall = new Stall();
        stall.setId(27L);
        stall.setPainterId(2L);
        stall.setStatus(1);
        stall.setDeleted(0);
        when(stallMapper.selectById(27L)).thenReturn(stall);

        stallService.delete(27L, 2L);

        InOrder operations = inOrder(stallMapper);
        operations.verify(stallMapper).selectById(27L);
        operations.verify(stallMapper).updateById(stall);
        operations.verify(stallMapper).deleteById(27L);
        assertEquals(0, stall.getStatus());
        assertNotNull(stall.getUpdatedAt());
    }
}
