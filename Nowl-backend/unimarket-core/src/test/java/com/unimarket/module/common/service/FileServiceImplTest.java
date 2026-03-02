package com.unimarket.module.common.service;

import com.unimarket.common.exception.BusinessException;
import com.unimarket.common.result.ResultCode;
import com.unimarket.module.common.service.impl.FileServiceImpl;
import com.unimarket.utils.CosUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private CosUtils cosUtils;

    @InjectMocks
    private FileServiceImpl fileService;

    @Mock
    private MultipartFile file;

    @Test
    @DisplayName("uploadFile: 非图片或小文件走原图上传并使用用户前缀")
    void uploadFile_shouldUseUserScopedPrefix() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/pdf");
        when(cosUtils.upload(file, "uploads/12/")).thenReturn("https://cdn/12/a.pdf");

        String url = fileService.uploadFile(12L, file);

        assertEquals("https://cdn/12/a.pdf", url);
        verify(cosUtils).upload(file, "uploads/12/");
    }

    @Test
    @DisplayName("uploadFile: 未登录用户拒绝上传")
    void uploadFile_userNotLogin_throw() {
        when(file.isEmpty()).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> fileService.uploadFile(null, file));

        assertEquals(ResultCode.USER_NOT_LOGIN.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("uploadFiles: 空文件会被跳过")
    void uploadFiles_skipEmptyFile() {
        MultipartFile empty = org.mockito.Mockito.mock(MultipartFile.class);
        MultipartFile normal = org.mockito.Mockito.mock(MultipartFile.class);

        when(empty.isEmpty()).thenReturn(true);
        when(normal.isEmpty()).thenReturn(false);
        when(normal.getContentType()).thenReturn("text/plain");
        when(cosUtils.upload(normal, "uploads/9/")).thenReturn("https://cdn/9/a.txt");

        List<String> urls = fileService.uploadFiles(9L, new MultipartFile[]{empty, normal});

        assertEquals(1, urls.size());
        assertEquals("https://cdn/9/a.txt", urls.get(0));
    }

    @Test
    @DisplayName("deleteFile: 非本人文件禁止删除")
    void deleteFile_notOwned_throw() {
        when(cosUtils.isOwnedByUser("https://cdn/uploads/2/a.jpg", 1L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> fileService.deleteFile(1L, "https://cdn/uploads/2/a.jpg"));

        assertEquals(ResultCode.NOT_POWER.getCode(), ex.getCode());
        verify(cosUtils, never()).delete(anyString());
    }

    @Test
    @DisplayName("deleteFile: 本人文件允许删除")
    void deleteFile_owned_ok() {
        when(cosUtils.isOwnedByUser("https://cdn/uploads/3/a.jpg", 3L)).thenReturn(true);

        fileService.deleteFile(3L, "https://cdn/uploads/3/a.jpg");

        verify(cosUtils).delete("https://cdn/uploads/3/a.jpg");
    }

    @Test
    @DisplayName("deleteFile: 空URL直接忽略")
    void deleteFile_blankUrl_ignore() {
        fileService.deleteFile(3L, " ");
        verifyNoInteractions(cosUtils);
    }
}
