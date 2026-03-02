package com.unimarket.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CosUtilsTest {

    private CosUtils buildWithBaseUrl(String baseUrl) {
        CosUtils utils = new CosUtils();
        ReflectionTestUtils.setField(utils, "baseUrl", baseUrl);
        return utils;
    }

    @Test
    @DisplayName("isOwnedByUser: 仅用户目录前缀视为本人文件")
    void isOwnedByUser_matchPrefix() {
        CosUtils utils = buildWithBaseUrl("https://cos.example.com");

        assertTrue(utils.isOwnedByUser("https://cos.example.com/uploads/18/a.jpg", 18L));
        assertFalse(utils.isOwnedByUser("https://cos.example.com/uploads/19/a.jpg", 18L));
        assertFalse(utils.isOwnedByUser("https://evil.example.com/uploads/18/a.jpg", 18L));
    }

    @Test
    @DisplayName("extractKey: 正确提取对象key")
    void extractKey_success() {
        CosUtils utils = buildWithBaseUrl("https://cos.example.com");

        String key = utils.extractKey("https://cos.example.com/uploads/8/pic.png");

        assertEquals("uploads/8/pic.png", key);
    }

    @Test
    @DisplayName("extractKey: 非法URL抛异常")
    void extractKey_invalid_throw() {
        CosUtils utils = buildWithBaseUrl("https://cos.example.com");

        assertThrows(IllegalArgumentException.class,
                () -> utils.extractKey("https://other.example.com/uploads/8/pic.png"));
    }

    @Test
    @DisplayName("extractKey/isOwnedByUser: 兼容baseUrl尾斜杠与URL参数")
    void extractKey_andOwnership_supportTrailingSlashAndQuery() {
        CosUtils utils = buildWithBaseUrl("https://cos.example.com/");

        String key = utils.extractKey("https://cos.example.com/uploads/18/a.jpg?x=1#part");

        assertEquals("uploads/18/a.jpg", key);
        assertTrue(utils.isOwnedByUser("https://cos.example.com/uploads/18/a.jpg?x=1", 18L));
        assertFalse(utils.isOwnedByUser("https://cos.example.com/uploads/19/a.jpg?x=1", 18L));
    }
}
