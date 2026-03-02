package com.unimarket.common.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unimarket.module.chat.entity.ChatMessage;
import com.unimarket.module.chat.mapper.ChatMessageMapper;
import com.unimarket.module.dispute.entity.DisputeRecord;
import com.unimarket.module.dispute.mapper.DisputeRecordMapper;
import com.unimarket.module.errand.entity.ErrandTask;
import com.unimarket.module.errand.mapper.ErrandTaskMapper;
import com.unimarket.module.goods.entity.GoodsInfo;
import com.unimarket.module.goods.mapper.GoodsInfoMapper;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import com.unimarket.utils.CosUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 孤立文件清理定时任务
 * 清理上传后超过24小时且未被任何业务表引用的COS文件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanFileCleanupJob {

    private final CosUtils cosUtils;
    private final GoodsInfoMapper goodsInfoMapper;
    private final ErrandTaskMapper errandTaskMapper;
    private final UserInfoMapper userInfoMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final DisputeRecordMapper disputeRecordMapper;

    private static final int BATCH_SIZE = 500;

    /**
     * 每天凌晨3点执行孤立文件清理
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanOrphanFiles() {
        log.info("开始执行孤立文件清理任务");

        // 查询24小时前上传的文件
        Date cutoff = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L);
        List<String> fileUrls = cosUtils.listObjectUrls("uploads/", cutoff, 2000);

        if (fileUrls.isEmpty()) {
            log.info("无需清理的文件");
            return;
        }

        log.info("扫描到 {} 个超过24小时的文件，开始检查引用", fileUrls.size());

        // 收集所有被引用的URL
        Set<String> referencedUrls = collectReferencedUrls();

        int deletedCount = 0;
        int failedCount = 0;

        for (String url : fileUrls) {
            if (isReferenced(url, referencedUrls)) {
                continue;
            }
            try {
                cosUtils.delete(url);
                deletedCount++;
            } catch (Exception e) {
                failedCount++;
                log.warn("删除孤立文件失败: {}", url, e);
            }
        }

        log.info("孤立文件清理完成: 扫描={}, 删除={}, 失败={}", fileUrls.size(), deletedCount, failedCount);
    }

    private Set<String> collectReferencedUrls() {
        Set<String> urls = new HashSet<>();

        // 商品图片: image + imageList (JSON数组)
        List<GoodsInfo> goods = goodsInfoMapper.selectList(
                new LambdaQueryWrapper<GoodsInfo>()
                        .select(GoodsInfo::getImage, GoodsInfo::getImageList)
        );
        for (GoodsInfo g : goods) {
            addIfNotNull(urls, g.getImage());
            extractJsonArrayUrls(urls, g.getImageList());
        }

        // 跑腿图片: imageList + evidenceImage
        List<ErrandTask> errands = errandTaskMapper.selectList(
                new LambdaQueryWrapper<ErrandTask>()
                        .select(ErrandTask::getImageList, ErrandTask::getEvidenceImage)
        );
        for (ErrandTask t : errands) {
            addIfNotNull(urls, t.getEvidenceImage());
            extractJsonArrayUrls(urls, t.getImageList());
        }

        // 用户头像/证件照: imageUrl + certImage + selfImage
        List<UserInfo> users = userInfoMapper.selectList(
                new LambdaQueryWrapper<UserInfo>()
                        .select(UserInfo::getImageUrl, UserInfo::getCertImage, UserInfo::getSelfImage)
        );
        for (UserInfo u : users) {
            addIfNotNull(urls, u.getImageUrl());
            addIfNotNull(urls, u.getCertImage());
            addIfNotNull(urls, u.getSelfImage());
        }

        // 聊天图片消息: content (when messageType=1)
        List<ChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getMessageType, 1)
                        .select(ChatMessage::getContent)
        );
        for (ChatMessage m : messages) {
            addIfNotNull(urls, m.getContent());
        }

        // 纠纷证据: evidenceUrls (JSON数组)
        List<DisputeRecord> disputes = disputeRecordMapper.selectList(
                new LambdaQueryWrapper<DisputeRecord>()
                        .select(DisputeRecord::getEvidenceUrls)
        );
        for (DisputeRecord d : disputes) {
            extractJsonArrayUrls(urls, d.getEvidenceUrls());
        }

        log.info("收集到 {} 个被引用的文件URL", urls.size());
        return urls;
    }

    private boolean isReferenced(String url, Set<String> referencedUrls) {
        return referencedUrls.contains(url);
    }

    private void addIfNotNull(Set<String> set, String url) {
        if (url != null && !url.isBlank()) {
            set.add(url.trim());
        }
    }

    private void extractJsonArrayUrls(Set<String> set, String jsonArray) {
        if (jsonArray == null || jsonArray.isBlank()) {
            return;
        }
        try {
            // 简单解析JSON数组字符串 ["url1","url2"]
            String trimmed = jsonArray.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
                for (String part : trimmed.split(",")) {
                    String url = part.trim();
                    if (url.startsWith("\"") && url.endsWith("\"")) {
                        url = url.substring(1, url.length() - 1);
                    }
                    addIfNotNull(set, url);
                }
            }
        } catch (Exception e) {
            log.debug("解析JSON数组失败: {}", jsonArray);
        }
    }
}
