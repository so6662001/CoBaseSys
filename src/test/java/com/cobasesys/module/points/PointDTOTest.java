package com.cobasesys.module.points;

import com.cobasesys.module.points.dto.PointDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PointDTOTest {

    @Test
    void sourceTypeText_allTypes() {
        assertEquals("规则赚取", PointDTO.sourceTypeText("EARNED"));
        assertEquals("人工赠送", PointDTO.sourceTypeText("GIFT_MANUAL"));
        assertEquals("订单赠送", PointDTO.sourceTypeText("GIFT_ORDER"));
        assertEquals("活动赠送", PointDTO.sourceTypeText("GIFT_ACTIVITY"));
        assertEquals("系统调整", PointDTO.sourceTypeText("SYSTEM"));
    }

    @Test
    void sourceTypeText_null_shouldReturnDefault() {
        assertEquals("赚取", PointDTO.sourceTypeText(null));
    }

    @Test
    void sourceTypeText_unknown_shouldReturnRaw() {
        assertEquals("CUSTOM", PointDTO.sourceTypeText("CUSTOM"));
    }
}
