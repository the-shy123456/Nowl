package com.unimarket.module.goods.controller;

import com.unimarket.common.result.PageResult;
import com.unimarket.common.result.Result;
import com.unimarket.security.UserContextHolder;
import com.unimarket.module.goods.dto.GoodsPublishDTO;
import com.unimarket.module.goods.dto.GoodsQueryDTO;
import com.unimarket.module.goods.service.GoodsService;
import com.unimarket.module.goods.vo.GoodsDetailVO;
import com.unimarket.module.goods.vo.GoodsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 商品Controller
 */
@Validated
@RestController
@RequestMapping("/goods")
@RequiredArgsConstructor
public class GoodsController {

    private final GoodsService goodsService;

    /**
     * 查询商品列表
     */
    @GetMapping
    public Result<PageResult<GoodsVO>> list(@Valid GoodsQueryDTO dto) {
        Long userId = UserContextHolder.getUserId();

        // 如果用户已认证且前端未传schoolCode，自动使用用户的学校信息
        if (dto.getSchoolCode() == null && UserContextHolder.isAuthenticated()) {
            String schoolCode = UserContextHolder.getSchoolCode();
            String campusCode = UserContextHolder.getCampusCode();
            if (schoolCode != null) {
                dto.setSchoolCode(schoolCode);
                dto.setCampusCode(campusCode);
            }
        }

        PageResult<GoodsVO> result = goodsService.list(dto, userId);
        return Result.success(result);
    }

    /**
     * 查询商品详情
     */
    @GetMapping("/{id}")
    public Result<GoodsDetailVO> detail(@PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        GoodsDetailVO detail = goodsService.getDetail(id, userId);
        return Result.success(detail);
    }

    /**
     * 发布商品
     * 权限：仅已认证用户可发布
     */
    @PostMapping
    @PreAuthorize("@bizAuth.canPublishGoods(authentication.principal.userId)")
    public Result<Void> publish(@Valid @RequestBody GoodsPublishDTO dto) {
        Long userId = UserContextHolder.getUserId();
        goodsService.publish(userId, dto);
        return Result.success();
    }

    /**
     * 更新商品
     */
    @PutMapping("/{id}")
    @PreAuthorize("@bizAuth.isGoodsOwner(#id, authentication.principal.userId)")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody GoodsPublishDTO dto) {
        Long userId = UserContextHolder.getUserId();
        goodsService.update(userId, id, dto);
        return Result.success();
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@bizAuth.isGoodsOwner(#id, authentication.principal.userId)")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        goodsService.delete(userId, id);
        return Result.success();
    }

    /**
     * 下架商品
     */
    @PutMapping("/{id}/offshelf")
    @PreAuthorize("@bizAuth.isGoodsOwner(#id, authentication.principal.userId)")
    public Result<Void> offshelf(@PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        goodsService.offshelf(userId, id);
        return Result.success();
    }

    /**
     * 收藏商品
     */
    @PostMapping("/{id}/collect")
    @PreAuthorize("@bizAuth.canMutate(authentication.principal.userId)")
    public Result<Void> collect(@PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        goodsService.collect(userId, id);
        return Result.success();
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/{id}/collect")
    @PreAuthorize("@bizAuth.canMutate(authentication.principal.userId)")
    public Result<Void> uncollect(@PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        goodsService.uncollect(userId, id);
        return Result.success();
    }

    /**
     * 查询我的商品
     */
    @GetMapping("/my")
    public Result<PageResult<GoodsVO>> getMyGoods(@Valid GoodsQueryDTO dto) {
        Long userId = UserContextHolder.getUserId();
        PageResult<GoodsVO> result = goodsService.getMyGoods(userId, dto);
        return Result.success(result);
    }

    /**
     * 查询我的收藏
     */
    @GetMapping("/collections")
    public Result<PageResult<GoodsVO>> getMyCollections(@Valid GoodsQueryDTO dto) {
        Long userId = UserContextHolder.getUserId();
        PageResult<GoodsVO> result = goodsService.getMyCollections(userId, dto);
        return Result.success(result);
    }

}
