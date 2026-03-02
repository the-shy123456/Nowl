package com.unimarket.admin.service.impl.domain;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.admin.service.impl.support.AdminScopeSupport;
import com.unimarket.admin.service.impl.support.AdminSchoolInfoSupport;
import com.unimarket.common.result.PageQuery;
import com.unimarket.module.goods.entity.GoodsInfo;
import com.unimarket.module.goods.mapper.GoodsInfoMapper;
import com.unimarket.module.iam.entity.IamAdminScopeBinding;
import com.unimarket.module.order.entity.OrderInfo;
import com.unimarket.module.order.mapper.OrderInfoMapper;
import com.unimarket.module.order.vo.OrderVO;
import com.unimarket.module.school.entity.SchoolInfo;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrderDomainService {

    private final OrderInfoMapper orderInfoMapper;
    private final UserInfoMapper userInfoMapper;
    private final GoodsInfoMapper goodsInfoMapper;
    private final AdminScopeSupport scopeSupport;
    private final AdminSchoolInfoSupport schoolInfoSupport;

    public Page<OrderVO> getOrderList(Long operatorId,
                                      PageQuery query,
                                      String keyword,
                                      String schoolCode,
                                      String campusCode,
                                      Integer orderStatus) {
        List<IamAdminScopeBinding> scopes = scopeSupport.getOperatorScopes(operatorId);

        Page<OrderInfo> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(OrderInfo::getOrderNo, keyword);
        }
        wrapper.eq(StrUtil.isNotBlank(schoolCode), OrderInfo::getSchoolCode, schoolCode)
                .eq(StrUtil.isNotBlank(campusCode), OrderInfo::getCampusCode, campusCode)
                .eq(orderStatus != null, OrderInfo::getOrderStatus, orderStatus);
        wrapper.orderByDesc(OrderInfo::getCreateTime);
        scopeSupport.applyScopeFilter(wrapper, scopes, OrderInfo::getSchoolCode, OrderInfo::getCampusCode);

        Page<OrderInfo> orderPage = orderInfoMapper.selectPage(page, wrapper);
        List<OrderInfo> records = orderPage.getRecords();

        if (records.isEmpty()) {
            return new Page<OrderVO>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        }

        Set<Long> userIds = records.stream()
                .flatMap(o -> List.of(o.getBuyerId(), o.getSellerId()).stream())
                .collect(Collectors.toSet());
        Map<Long, UserInfo> userMap = userInfoMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserInfo::getUserId, u -> u));

        Set<Long> productIds = records.stream()
                .map(OrderInfo::getProductId)
                .collect(Collectors.toSet());
        Map<Long, GoodsInfo> goodsMap = goodsInfoMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(GoodsInfo::getProductId, g -> g));

        Map<String, SchoolInfo> schoolMap = schoolInfoSupport.buildSchoolInfoMap(records, OrderInfo::getSchoolCode);

        List<OrderVO> vos = records.stream()
                .map(order -> {
                    OrderVO vo = BeanUtil.copyProperties(order, OrderVO.class);
                    UserInfo buyer = userMap.get(order.getBuyerId());
                    if (buyer != null) {
                        vo.setBuyerName(buyer.getNickName());
                        vo.setBuyerAvatar(buyer.getImageUrl());
                    }
                    UserInfo seller = userMap.get(order.getSellerId());
                    if (seller != null) {
                        vo.setSellerName(seller.getNickName());
                        vo.setSellerAvatar(seller.getImageUrl());
                    }
                    GoodsInfo goods = goodsMap.get(order.getProductId());
                    if (goods != null) {
                        vo.setProductTitle(goods.getTitle());
                        vo.setProductImage(goods.getImage());
                    }
                    schoolInfoSupport.fillSchoolCampusNames(
                            order.getSchoolCode(),
                            order.getCampusCode(),
                            schoolMap,
                            vo::setSchoolName,
                            vo::setCampusName
                    );
                    return vo;
                })
                .toList();

        return new Page<OrderVO>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal()).setRecords(vos);
    }
}

