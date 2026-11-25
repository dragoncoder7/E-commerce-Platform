package com.ecommerce.system.service;

import com.ecommerce.common.core.exception.BusinessException;
import com.ecommerce.system.domain.Role;
import com.ecommerce.system.domain.SysUser;
import com.ecommerce.system.dto.RbacRequests;
import com.ecommerce.system.mapper.MenuMapper;
import com.ecommerce.system.mapper.RoleMapper;
import com.ecommerce.system.mapper.UserMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 后台管理员不可锁死和授权输入边界。事务及权限生效另用真实 MySQL 验证。 */
class RbacManagementTest {
  private UserMapper users;
  private RoleMapper roles;
  private MenuMapper menus;
  private JdbcTemplate jdbc;
  private RbacManagementService service;

  @BeforeEach
  void setUp() {
    users = mock(UserMapper.class);
    roles = mock(RoleMapper.class);
    menus = mock(MenuMapper.class);
    jdbc = mock(JdbcTemplate.class);
    service = new RbacManagementService(users, roles, menus, jdbc);
    when(jdbc.queryForList(anyString(), eq(Long.class))).thenReturn(List.of(1L));
  }

  @Test
  void cannotDeleteCurrentStaff() {
    SysUser current = new SysUser();
    current.setId(5L);
    when(users.selectById(5L)).thenReturn(current);
    assertThrows(BusinessException.class, () -> service.deleteStaff(5L, 5L));
    verify(users, never()).deleteById(anyLong());
  }

  @Test
  void cannotDisableOrRenameReservedAdminRole() {
    Role admin = new Role();
    admin.setId(1L);
    admin.setCode("admin");
    when(roles.selectById(1L)).thenReturn(admin);
    assertThrows(BusinessException.class, () -> service.saveRole(1L,
        new RbacRequests.Role("管理员", "admin", 0, 0, "", List.of())));
    assertThrows(BusinessException.class, () -> service.saveRole(1L,
        new RbacRequests.Role("管理员", "renamed", 1, 0, "", List.of())));
    verify(roles, never()).updateById(any(Role.class));
  }

  @Test
  void cannotDeleteReferencedRole() {
    Role role = new Role();
    role.setCode("staff");
    when(roles.selectById(2L)).thenReturn(role);
    when(jdbc.queryForObject(anyString(), eq(Long.class), eq(2L))).thenReturn(1L);
    assertThrows(BusinessException.class, () -> service.deleteRole(2L));
    verify(roles, never()).deleteById(anyLong());
  }

  @Test
  void cannotAssignDuplicateMenuPermissions() {
    assertThrows(BusinessException.class, () -> service.saveRole(null,
        new RbacRequests.Role("测试", "test_role", 1, 0, "", List.of(2L, 2L))));
    verify(roles, never()).insert(any(Role.class));
  }

  @Test
  void cannotAssignMissingMenuPermissions() {
    when(menus.selectBatchIds(List.of(99L))).thenReturn(List.of());
    assertThrows(BusinessException.class, () -> service.saveRole(null,
        new RbacRequests.Role("测试", "test_role", 1, 0, "", List.of(99L))));
    verify(roles, never()).insert(any(Role.class));
  }
}
