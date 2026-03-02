package com.unimarket.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unimarket.admin.dto.AdminScopeBindingUpsertDTO;
import com.unimarket.admin.dto.UserRoleGrantDTO;
import com.unimarket.admin.service.AdminIamService;
import com.unimarket.admin.vo.AdminScopeBindingVO;
import com.unimarket.admin.vo.IamRoleVO;
import com.unimarket.admin.vo.UserRoleBindingVO;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.module.audit.entity.AuditPermissionChange;
import com.unimarket.module.audit.mapper.AuditPermissionChangeMapper;
import com.unimarket.module.iam.entity.IamAdminScopeBinding;
import com.unimarket.module.iam.entity.IamRole;
import com.unimarket.module.iam.entity.IamUserRole;
import com.unimarket.module.iam.mapper.IamAdminScopeBindingMapper;
import com.unimarket.module.iam.mapper.IamRoleMapper;
import com.unimarket.module.iam.mapper.IamUserRoleMapper;
import com.unimarket.module.iam.service.IamAccessService;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 管理后台-IAM运维服务实现
 */
@Service
@RequiredArgsConstructor
public class AdminIamServiceImpl implements AdminIamService {

    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_DISABLED = 0;
    private static final Set<String> SCOPE_TYPES = Set.of("ALL", "SCHOOL", "CAMPUS");

    private final IamRoleMapper iamRoleMapper;
    private final IamUserRoleMapper iamUserRoleMapper;
    private final IamAdminScopeBindingMapper iamAdminScopeBindingMapper;
    private final UserInfoMapper userInfoMapper;
    private final IamAccessService iamAccessService;
    private final AuditPermissionChangeMapper auditPermissionChangeMapper;

    @Override
    public List<IamRoleVO> listRoles() {
        return iamRoleMapper.selectList(new LambdaQueryWrapper<IamRole>()
                        .eq(IamRole::getStatus, STATUS_ENABLED)
                        .orderByAsc(IamRole::getRoleLevel)
                        .orderByAsc(IamRole::getRoleId))
                .stream()
                .map(role -> BeanUtil.copyProperties(role, IamRoleVO.class))
                .toList();
    }

    @Override
    public List<UserRoleBindingVO> listUserRoles(Long operatorId, Long userId) {
        UserInfo target = assertTargetUserInScope(operatorId, userId);
        List<IamUserRole> bindings = iamUserRoleMapper.selectList(new LambdaQueryWrapper<IamUserRole>()
                .eq(IamUserRole::getUserId, target.getUserId())
                .orderByDesc(IamUserRole::getUpdateTime));
        if (bindings.isEmpty()) {
            return List.of();
        }

        Map<Long, IamRole> roleMap = iamRoleMapper.selectBatchIds(bindings.stream().map(IamUserRole::getRoleId).toList())
                .stream().collect(Collectors.toMap(IamRole::getRoleId, r -> r));

        return bindings.stream().map(binding -> {
            UserRoleBindingVO vo = BeanUtil.copyProperties(binding, UserRoleBindingVO.class);
            IamRole role = roleMap.get(binding.getRoleId());
            if (role != null) {
                vo.setRoleCode(role.getRoleCode());
                vo.setRoleName(role.getRoleName());
            }
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantUserRole(Long operatorId, UserRoleGrantDTO dto) {
        UserInfo target = assertTargetUserInScope(operatorId, dto.getUserId());
        IamRole role = iamRoleMapper.selectOne(new LambdaQueryWrapper<IamRole>()
                .eq(IamRole::getRoleCode, dto.getRoleCode().trim().toUpperCase())
                .eq(IamRole::getStatus, STATUS_ENABLED)
                .last("LIMIT 1"));
        if (role == null) {
            throw new BusinessException("角色不存在或已停用");
        }

        IamUserRole binding = iamUserRoleMapper.selectOne(new LambdaQueryWrapper<IamUserRole>()
                .eq(IamUserRole::getUserId, target.getUserId())
                .eq(IamUserRole::getRoleId, role.getRoleId())
                .last("LIMIT 1"));

        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        if (binding == null) {
            binding = new IamUserRole();
            binding.setUserId(target.getUserId());
            binding.setRoleId(role.getRoleId());
            binding.setStatus(STATUS_ENABLED);
            binding.setExpiredTime(dto.getExpiredTime());
            iamUserRoleMapper.insert(binding);

            after.put("status", STATUS_ENABLED);
            after.put("expiredTime", dto.getExpiredTime());
        } else {
            before.put("status", binding.getStatus());
            before.put("expiredTime", binding.getExpiredTime());

            binding.setStatus(STATUS_ENABLED);
            binding.setExpiredTime(dto.getExpiredTime());
            binding.setUpdateTime(LocalDateTime.now());
            iamUserRoleMapper.updateById(binding);

            after.put("status", STATUS_ENABLED);
            after.put("expiredTime", dto.getExpiredTime());
        }

        writePermissionAudit(
                operatorId,
                "ROLE_GRANT",
                target.getUserId(),
                role.getRoleId(),
                null,
                before,
                after,
                dto.getReason()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeUserRole(Long operatorId, Long bindingId, String reason) {
        if (bindingId == null) {
            throw new BusinessException("角色绑定ID不能为空");
        }
        IamUserRole binding = iamUserRoleMapper.selectById(bindingId);
        if (binding == null) {
            throw new BusinessException("角色绑定不存在");
        }
        UserInfo target = assertTargetUserInScope(operatorId, binding.getUserId());

        Map<String, Object> before = new HashMap<>();
        before.put("status", binding.getStatus());
        before.put("expiredTime", binding.getExpiredTime());

        binding.setStatus(STATUS_DISABLED);
        binding.setUpdateTime(LocalDateTime.now());
        iamUserRoleMapper.updateById(binding);

        Map<String, Object> after = new HashMap<>();
        after.put("status", STATUS_DISABLED);
        after.put("expiredTime", binding.getExpiredTime());

        writePermissionAudit(
                operatorId,
                "ROLE_REVOKE",
                target.getUserId(),
                binding.getRoleId(),
                null,
                before,
                after,
                reason
        );
    }

    @Override
    public List<AdminScopeBindingVO> listAdminScopes(Long operatorId, Long userId) {
        UserInfo target = assertTargetUserInScope(operatorId, userId);
        List<IamAdminScopeBinding> bindings = iamAdminScopeBindingMapper.selectList(
                new LambdaQueryWrapper<IamAdminScopeBinding>()
                        .eq(IamAdminScopeBinding::getUserId, target.getUserId())
                        .orderByDesc(IamAdminScopeBinding::getUpdateTime)
        );
        return bindings.stream()
                .map(binding -> BeanUtil.copyProperties(binding, AdminScopeBindingVO.class))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upsertAdminScope(Long operatorId, AdminScopeBindingUpsertDTO dto) {
        UserInfo target = assertTargetUserInScope(operatorId, dto.getUserId());
        String scopeType = normalizeScopeType(dto.getScopeType());
        validateScopeValue(scopeType, dto.getSchoolCode(), dto.getCampusCode());
        assertOperatorCanAssignScope(operatorId, scopeType, dto.getSchoolCode(), dto.getCampusCode());

        IamAdminScopeBinding existed = iamAdminScopeBindingMapper.selectOne(
                new LambdaQueryWrapper<IamAdminScopeBinding>()
                        .eq(IamAdminScopeBinding::getUserId, target.getUserId())
                        .eq(IamAdminScopeBinding::getScopeType, scopeType)
                        .eq(scopeType.equals("SCHOOL"), IamAdminScopeBinding::getSchoolCode, dto.getSchoolCode())
                        .eq(scopeType.equals("CAMPUS"), IamAdminScopeBinding::getSchoolCode, dto.getSchoolCode())
                        .eq(scopeType.equals("CAMPUS"), IamAdminScopeBinding::getCampusCode, dto.getCampusCode())
                        .last("LIMIT 1"));

        Map<String, Object> after = new HashMap<>();
        after.put("scopeType", scopeType);
        after.put("schoolCode", dto.getSchoolCode());
        after.put("campusCode", dto.getCampusCode());
        after.put("status", STATUS_ENABLED);

        if (existed == null) {
            IamAdminScopeBinding binding = new IamAdminScopeBinding();
            binding.setUserId(target.getUserId());
            binding.setScopeType(scopeType);
            binding.setSchoolCode(scopeType.equals("ALL") ? null : dto.getSchoolCode());
            binding.setCampusCode(scopeType.equals("CAMPUS") ? dto.getCampusCode() : null);
            binding.setStatus(STATUS_ENABLED);
            iamAdminScopeBindingMapper.insert(binding);
        } else {
            existed.setStatus(STATUS_ENABLED);
            existed.setSchoolCode(scopeType.equals("ALL") ? null : dto.getSchoolCode());
            existed.setCampusCode(scopeType.equals("CAMPUS") ? dto.getCampusCode() : null);
            existed.setUpdateTime(LocalDateTime.now());
            iamAdminScopeBindingMapper.updateById(existed);
        }

        writePermissionAudit(
                operatorId,
                "SCOPE_BIND",
                target.getUserId(),
                null,
                null,
                null,
                after,
                dto.getReason()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableAdminScope(Long operatorId, Long bindingId, String reason) {
        if (bindingId == null) {
            throw new BusinessException("范围绑定ID不能为空");
        }
        IamAdminScopeBinding binding = iamAdminScopeBindingMapper.selectById(bindingId);
        if (binding == null) {
            throw new BusinessException("范围绑定不存在");
        }
        assertTargetUserInScope(operatorId, binding.getUserId());

        binding.setStatus(STATUS_DISABLED);
        binding.setUpdateTime(LocalDateTime.now());
        iamAdminScopeBindingMapper.updateById(binding);

        Map<String, Object> before = new HashMap<>();
        before.put("scopeType", binding.getScopeType());
        before.put("schoolCode", binding.getSchoolCode());
        before.put("campusCode", binding.getCampusCode());

        Map<String, Object> after = new HashMap<>();
        after.put("status", STATUS_DISABLED);

        writePermissionAudit(
                operatorId,
                "SCOPE_UNBIND",
                binding.getUserId(),
                null,
                null,
                before,
                after,
                reason
        );
    }

    private UserInfo assertTargetUserInScope(Long operatorId, Long userId) {
        if (userId == null) {
            throw new BusinessException("目标用户不能为空");
        }
        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("目标用户不存在");
        }
        iamAccessService.assertCanManageScope(operatorId, user.getSchoolCode(), user.getCampusCode());
        return user;
    }

    private String normalizeScopeType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("范围类型不能为空");
        }
        String type = raw.trim().toUpperCase();
        if (!SCOPE_TYPES.contains(type)) {
            throw new BusinessException("不支持的范围类型: " + type);
        }
        return type;
    }

    private void validateScopeValue(String scopeType, String schoolCode, String campusCode) {
        if ("ALL".equals(scopeType)) {
            return;
        }
        if (StrUtil.isBlank(schoolCode)) {
            throw new BusinessException("学校编码不能为空");
        }
        if ("CAMPUS".equals(scopeType) && StrUtil.isBlank(campusCode)) {
            throw new BusinessException("校区编码不能为空");
        }
    }

    private void assertOperatorCanAssignScope(Long operatorId, String scopeType, String schoolCode, String campusCode) {
        if ("ALL".equals(scopeType)) {
            boolean hasAll = iamAccessService.getAdminScopes(operatorId).stream()
                    .anyMatch(scope -> "ALL".equalsIgnoreCase(scope.getScopeType()) && Integer.valueOf(1).equals(scope.getStatus()));
            if (!hasAll) {
                throw new BusinessException("当前管理员无权授予ALL范围");
            }
            return;
        }
        iamAccessService.assertCanManageScope(operatorId, schoolCode, campusCode);
    }

    private void writePermissionAudit(Long operatorId,
                                      String changeType,
                                      Long targetUserId,
                                      Long targetRoleId,
                                      Long targetPermissionId,
                                      Object beforeData,
                                      Object afterData,
                                      String reason) {
        AuditPermissionChange audit = new AuditPermissionChange();
        audit.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        audit.setOperatorId(operatorId);
        audit.setChangeType(changeType);
        audit.setTargetUserId(targetUserId);
        audit.setTargetRoleId(targetRoleId);
        audit.setTargetPermissionId(targetPermissionId);
        audit.setBeforeData(beforeData == null ? null : JSON.toJSONString(beforeData));
        audit.setAfterData(afterData == null ? null : JSON.toJSONString(afterData));
        audit.setReason(reason);
        audit.setCreateTime(LocalDateTime.now());
        auditPermissionChangeMapper.insert(audit);
    }
}

