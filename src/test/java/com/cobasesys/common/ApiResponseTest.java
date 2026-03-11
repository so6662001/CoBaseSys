package com.cobasesys.common;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void ok_withData() {
        ApiResponse<String> resp = ApiResponse.ok("hello");
        assertEquals(0, resp.getCode());
        assertEquals("success", resp.getMessage());
        assertEquals("hello", resp.getData());
    }

    @Test
    void ok_withoutData() {
        ApiResponse<Void> resp = ApiResponse.ok();
        assertEquals(0, resp.getCode());
        assertNull(resp.getData());
    }

    @Test
    void error_withCode() {
        ApiResponse<Void> resp = ApiResponse.error(10001, "参数错误");
        assertEquals(10001, resp.getCode());
        assertEquals("参数错误", resp.getMessage());
    }

    @Test
    void error_withMessage() {
        ApiResponse<Void> resp = ApiResponse.error("操作失败");
        assertEquals(-1, resp.getCode());
        assertEquals("操作失败", resp.getMessage());
    }

    @Test
    void pageResult_fromPage() {
        var page = new PageImpl<>(List.of("a", "b", "c"), PageRequest.of(0, 10), 3);
        PageResult<String> result = PageResult.from(page);
        assertEquals(3, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(10, result.getPageSize());
        assertEquals(3, result.getItems().size());
    }
}
