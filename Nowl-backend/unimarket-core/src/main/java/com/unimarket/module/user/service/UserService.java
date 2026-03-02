package com.unimarket.module.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.module.user.dto.ResetPasswordDTO;
import com.unimarket.module.user.dto.UserLoginDTO;
import com.unimarket.module.user.dto.UserRegisterDTO;
import com.unimarket.module.user.dto.UserUpdateDTO;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.vo.FollowUserVO;
import com.unimarket.module.user.vo.LoginVO;
import com.unimarket.module.user.vo.UserInfoVO;

/**
 * 用户Service接口
 */
public interface UserService {

    /**
     * 用户注册
     */
    void register(UserRegisterDTO dto);

    /**
     * 用户登录
     */
    LoginVO login(UserLoginDTO dto);

    /**
     * 获取当前用户信息
     */
    UserInfoVO getCurrentUserInfo(Long userId);

    /**
     * 根据用户ID获取用户信息
     */
    UserInfoVO getUserInfo(Long userId);

    /**
     * 根据用户ID获取公开用户信息（不含IAM权限字段）
     */
    UserInfoVO getPublicUserInfo(Long userId);

    /**
     * 更新用户信息
     */
    void updateUserInfo(Long userId, UserUpdateDTO dto);

    /**
     * 根据学号获取用户信息
     */
    UserInfo getByStudentNo(String studentNo);

    /**
     * 根据手机号获取用户信息
     */
    UserInfo getByPhone(String phone);

    /**
     * 发送短信验证码
     */
    void sendSmsCode(String phone);

    /**
     * 重置密码
     * @param dto 重置密码表单
     */
    void resetPassword(ResetPasswordDTO dto);

    /**
     * 刷新令牌
     * @param refreshToken 刷新令牌
     * @return 登录信息（包含新Access Token）
     */
    LoginVO refreshToken(String refreshToken);
    /**
     * 获取图形验证码
     * @param uuid 唯一标识
     * @return Base64编码的图片
     */
    String getCaptcha(String uuid);

    /**
     * 退出登录
     * @param userId 当前用户ID
     */
    void logout(Long userId, String accessToken, String refreshToken);

    /**
     * 申请成为跑腿员
     */
    void applyRunner(Long userId);


    /**
     * 关注用户
     */
    void followUser(Long userId, Long targetUserId);

    /**
     * 取消关注
     */
    void unfollowUser(Long userId, Long targetUserId);

    /**
     * 检查是否关注了某用户
     */
    boolean isFollowing(Long userId, Long targetUserId);

    /**
     * 获取关注列表
     */
    Page<FollowUserVO> getFollowingList(Long userId, Long currentUserId, int pageNum, int pageSize);

    /**
     * 获取粉丝列表
     */
    Page<FollowUserVO> getFollowerList(Long userId, Long currentUserId, int pageNum, int pageSize);
}
