package com.ecommerce.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.core.exception.BusinessException;
import com.ecommerce.system.domain.Role;
import com.ecommerce.system.domain.SysUser;
import com.ecommerce.system.dto.RbacRequests;
import com.ecommerce.system.mapper.MenuMapper;
import com.ecommerce.system.mapper.RoleMapper;
import com.ecommerce.system.mapper.UserMapper;
import com.ecommerce.system.vo.RbacViews;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 后台授权事务，保留 admin 角色及至少一名可用管理员。 */
@Service
@RequiredArgsConstructor
public class RbacManagementService {
  private static final BCryptPasswordEncoder PASSWORDS = new BCryptPasswordEncoder(12);
  private final UserMapper users;
  private final RoleMapper roles;
  private final MenuMapper menus;
  private final JdbcTemplate jdbc;

  public Page<RbacViews.Staff> staff(int pageNum, int pageSize, String keyword) {
    if (pageNum < 1 || pageSize < 1 || pageSize > 100 || keyword != null && keyword.length() > 100) {
      throw new BusinessException(400, "分页或搜索参数无效");
    }
    LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getId);
    if (keyword != null && !keyword.isBlank()) {
      query.and(part -> part.like(SysUser::getUsername, keyword.trim()).or().like(SysUser::getNickname, keyword.trim()));
    }
    Page<SysUser> result = users.selectPage(new Page<>(pageNum, pageSize), query);
    Page<RbacViews.Staff> view = new Page<>(pageNum, pageSize, result.getTotal());
    view.setRecords(result.getRecords().stream().map(this::staffView).toList());
    return view;
  }

  public List<RbacViews.Role> roles() {
    return roles.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getSort).orderByAsc(Role::getId))
        .stream().map(this::roleView).toList();
  }

  @Transactional(rollbackFor = Exception.class)
  public RbacViews.Staff saveStaff(Long id, RbacRequests.Staff request, Long actor) {
    Long adminRole = lockAuthorization();
    List<Long> roleIds = request.roleIds().stream().distinct().toList();
    if (roleIds.size() != request.roleIds().size() || roles.selectCount(new LambdaQueryWrapper<Role>()
        .in(Role::getId, roleIds).eq(Role::getStatus, 1)) != roleIds.size()) {
      throw new BusinessException(400, "请选择有效且启用的角色");
    }
    SysUser existing = id == null ? null : users.selectById(id);
    if (id != null && existing == null) {
      throw new BusinessException(404, "后台人员不存在");
    }
    boolean wasAdmin = existing != null && staffView(existing).roleIds().contains(adminRole);
    if ((wasAdmin || roleIds.contains(adminRole)) && !users.selectRoleCodesByUserId(actor).contains("admin")) {
      throw new BusinessException(403, "只有管理员可以管理管理员账号");
    }
    if (id != null && id.equals(actor) && (request.status() == 0 || wasAdmin && !roleIds.contains(adminRole))) {
      throw new BusinessException(409, "不能停用自身或移除自身管理员角色");
    }
    if (wasAdmin && Integer.valueOf(1).equals(existing.getStatus()) && (request.status() == 0 || !roleIds.contains(adminRole))) {
      requireAnotherAdmin(id, adminRole);
    }
    SysUser user = existing == null ? new SysUser() : existing;
    user.setUpdateTime(LocalDateTime.now());
    if (existing == null) {
      user.setCreateTime(user.getUpdateTime());
    }
    user.setUsername(request.username().trim());
    user.setNickname(request.nickname().trim());
    user.setStatus(request.status());
    if (existing == null || request.password() != null && !request.password().isBlank()) {
      validatePassword(request.password());
      user.setPassword(PASSWORDS.encode(request.password()));
    }
    user.setSessionVersion(existing == null ? 0L : existing.getSessionVersion() + 1);
    if (existing == null) {
      users.insert(user);
    } else {
      users.updateById(user);
    }
    jdbc.update("DELETE FROM sys_user_role WHERE user_id=?", user.getId());
    for (Long roleId : roleIds) {
      jdbc.update("INSERT INTO sys_user_role(user_id,role_id) VALUES(?,?)", user.getId(), roleId);
    }
    return staffView(user);
  }

  @Transactional(rollbackFor = Exception.class)
  public void deleteStaff(Long id, Long actor) {
    Long adminRole = lockAuthorization();
    SysUser user = users.selectById(id);
    if (user == null) {
      throw new BusinessException(404, "后台人员不存在");
    }
    if (id.equals(actor)) {
      throw new BusinessException(409, "不能删除当前登录账号");
    }
    if (staffView(user).roleIds().contains(adminRole)) {
      if (!users.selectRoleCodesByUserId(actor).contains("admin")) {
        throw new BusinessException(403, "只有管理员可以删除管理员账号");
      }
      requireAnotherAdmin(id, adminRole);
    }
    users.deleteById(id);
    jdbc.update("DELETE FROM sys_user_role WHERE user_id=?", id);
  }

  @Transactional(rollbackFor = Exception.class)
  public RbacViews.Role saveRole(Long id, RbacRequests.Role request) {
    lockAuthorization();
    Role role = id == null ? new Role() : roles.selectById(id);
    if (role == null) {
      throw new BusinessException(404, "角色不存在");
    }
    if ("admin".equals(role.getCode()) && (!"admin".equals(request.code()) || request.status() != 1)
        || !"admin".equals(role.getCode()) && "admin".equals(request.code())) {
      throw new BusinessException(409, "系统管理员角色编码和启用状态不可修改");
    }
    List<Long> ids = request.menuIds().stream().distinct().toList();
    if (ids.size() != request.menuIds().size() || !ids.isEmpty() && menus.selectBatchIds(ids).size() != ids.size()) {
      throw new BusinessException(400, "包含不存在或重复的菜单权限");
    }
    role.setUpdateTime(LocalDateTime.now());
    if (id == null) {
      role.setCreateTime(role.getUpdateTime());
    }
    role.setName(request.name().trim());
    role.setCode(request.code());
    role.setStatus(request.status());
    role.setSort(request.sort());
    role.setRemark(request.remark());
    if (id == null) {
      roles.insert(role);
    } else {
      roles.updateById(role);
    }
    jdbc.update("DELETE FROM sys_role_menu WHERE role_id=?", role.getId());
    for (Long menuId : ids) {
      jdbc.update("INSERT INTO sys_role_menu(role_id,menu_id) VALUES(?,?)", role.getId(), menuId);
    }
    return roleView(role);
  }

  @Transactional(rollbackFor = Exception.class)
  public void deleteRole(Long id) {
    lockAuthorization();
    Role role = roles.selectById(id);
    if (role == null) {
      throw new BusinessException(404, "角色不存在");
    }
    Long count = jdbc.queryForObject("SELECT COUNT(*) FROM sys_user_role WHERE role_id=?", Long.class, id);
    if ("admin".equals(role.getCode()) || count != null && count > 0) {
      throw new BusinessException(409, "系统角色或仍关联人员的角色不能删除");
    }
    roles.deleteById(id);
    jdbc.update("DELETE FROM sys_role_menu WHERE role_id=?", id);
  }

  private Long lockAuthorization() {
    List<Long> ids = jdbc.queryForList("SELECT id FROM sys_role WHERE code='admin' AND deleted=0 FOR UPDATE", Long.class);
    if (ids.size() != 1) {
      throw new BusinessException(409, "系统管理员角色缺失，请先执行权限迁移");
    }
    return ids.getFirst();
  }

  private void requireAnotherAdmin(Long excluded, Long roleId) {
    Long count = jdbc.queryForObject("SELECT COUNT(*) FROM sys_user u JOIN sys_user_role ur ON ur.user_id=u.id WHERE ur.role_id=? AND u.status=1 AND u.deleted=0 AND u.id<>?", Long.class, roleId, excluded);
    if (count == null || count == 0) {
      throw new BusinessException(409, "必须保留至少一个启用的管理员账号");
    }
  }

  private RbacViews.Staff staffView(SysUser user) {
    return new RbacViews.Staff(user.getId(), user.getUsername(), user.getNickname(), user.getStatus(), user.getCreateTime(),
        jdbc.queryForList("SELECT role_id FROM sys_user_role WHERE user_id=? ORDER BY role_id", Long.class, user.getId()));
  }

  private RbacViews.Role roleView(Role role) {
    return new RbacViews.Role(role.getId(), role.getName(), role.getCode(), role.getStatus(), role.getSort(), role.getRemark(),
        jdbc.queryForList("SELECT menu_id FROM sys_role_menu WHERE role_id=? ORDER BY menu_id", Long.class, role.getId()));
  }

  private void validatePassword(String password) {
    if (password == null || password.length() < 8 || password.length() > 64
        || password.getBytes(StandardCharsets.UTF_8).length > 72 || !password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")) {
      throw new BusinessException(400, "密码须为8–64位并包含字母和数字，最多72字节");
    }
  }
}
