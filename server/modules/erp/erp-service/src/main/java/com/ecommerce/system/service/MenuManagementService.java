package com.ecommerce.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.core.exception.BusinessException;
import com.ecommerce.system.api.vo.MenuVO;
import com.ecommerce.system.domain.Menu;
import com.ecommerce.system.dto.RbacRequests;
import com.ecommerce.system.mapper.MenuMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 菜单树维护，禁止循环、无效链接及删除仍被角色引用的节点。 */
@Service
@RequiredArgsConstructor
public class MenuManagementService {
  private final MenuMapper menus;
  private final JdbcTemplate jdbc;

  public List<MenuVO> list(String title, String type, Integer status) {
    List<Menu> all = menus.selectList(new LambdaQueryWrapper<Menu>().orderByAsc(Menu::getSort).orderByAsc(Menu::getId));
    return tree(all, 0L, title, type, status, new HashSet<>());
  }

  private List<MenuVO> tree(List<Menu> all, Long parent, String title, String type, Integer status, Set<Long> visited) {
    List<MenuVO> result = new ArrayList<>();
    for (Menu item : all) {
      if (!parent.equals(item.getParentId()) || !visited.add(item.getId())) {
        continue;
      }
      List<MenuVO> children = tree(all, item.getId(), title, type, status, visited);
      boolean matches = (title == null || title.isBlank() || item.getTitle().contains(title.trim()))
          && (type == null || type.isBlank() || type.equals(item.getType()))
          && (status == null || status.equals(item.getStatus()));
      if (matches || !children.isEmpty()) {
        MenuVO view = view(item);
        view.setChildren(children);
        result.add(view);
      }
    }
    return result;
  }

  @Transactional(rollbackFor = Exception.class)
  public MenuVO save(Long id, RbacRequests.Menu request) {
    lockTree();
    Menu item = id == null ? new Menu() : menus.selectById(id);
    if (item == null) {
      throw new BusinessException(404, "菜单不存在");
    }
    Set<Long> visited = new HashSet<>();
    if (id != null) {
      visited.add(id);
    }
    Long parentId = request.parentId();
    while (parentId != 0) {
      if (!visited.add(parentId) || visited.size() > 32) {
        throw new BusinessException(400, "菜单存在循环或超过32层");
      }
      Menu parent = menus.selectById(parentId);
      if (parent == null || "button".equals(parent.getType())) {
        throw new BusinessException(400, "请选择有效的父目录或菜单");
      }
      parentId = parent.getParentId();
    }
    if ("button".equals(request.type()) && id != null
        && menus.selectCount(new LambdaQueryWrapper<Menu>().eq(Menu::getParentId, id)) > 0) {
      throw new BusinessException(409, "有子菜单的节点不能改为按钮");
    }
    if (!"button".equals(request.type()) && (request.path() == null || !request.path().matches("/[A-Za-z0-9_/-]*")
        || request.path().startsWith("//") || List.of("/login", "/forbidden", "/").contains(request.path()))) {
      throw new BusinessException(400, "请输入有效的站内菜单路径");
    }
    if ("menu".equals(request.type()) && (request.component() == null || !request.component().matches("[A-Za-z0-9_/-]+"))) {
      throw new BusinessException(400, "菜单组件路径无效");
    }
    if (("button".equals(request.type()) || request.permission() != null && !request.permission().isBlank())
        && (request.permission() == null || !request.permission().matches("[a-z][a-z0-9_-]*(?::[a-z][a-z0-9_-]*)+"))) {
      throw new BusinessException(400, "权限标识应为 resource:action 格式");
    }
    if (menus.selectCount(new LambdaQueryWrapper<Menu>().eq(Menu::getName, request.name()).ne(id != null, Menu::getId, id)) > 0
        || !"button".equals(request.type()) && menus.selectCount(new LambdaQueryWrapper<Menu>()
            .eq(Menu::getPath, request.path()).ne(id != null, Menu::getId, id)) > 0) {
      throw new BusinessException(409, "菜单名称或路由路径已存在");
    }
    item.setUpdateTime(LocalDateTime.now());
    if (id == null) {
      item.setCreateTime(item.getUpdateTime());
    }
    item.setParentId(request.parentId());
    item.setName(request.name());
    item.setTitle(request.title().trim());
    item.setType(request.type());
    item.setPath("button".equals(request.type()) ? "" : request.path());
    item.setComponent(request.component());
    item.setIcon(request.icon());
    item.setPermission(request.permission());
    item.setSort(request.sort());
    item.setVisible(request.visible() ? 1 : 0);
    item.setKeepAlive(request.keepAlive() ? 1 : 0);
    item.setStatus(request.status());
    if (id == null) {
      menus.insert(item);
    } else {
      menus.updateById(item);
    }
    return view(item);
  }

  @Transactional(rollbackFor = Exception.class)
  public void delete(Long id) {
    lockTree();
    if (menus.selectById(id) == null) {
      throw new BusinessException(404, "菜单不存在");
    }
    Long references = jdbc.queryForObject("SELECT COUNT(*) FROM sys_role_menu WHERE menu_id=?", Long.class, id);
    if (menus.selectCount(new LambdaQueryWrapper<Menu>().eq(Menu::getParentId, id)) > 0 || references != null && references > 0) {
      throw new BusinessException(409, "请先移除子菜单和角色授权引用");
    }
    menus.deleteById(id);
  }

  private void lockTree() {
    if (jdbc.queryForList("SELECT id FROM sys_role WHERE code='admin' AND deleted=0 FOR UPDATE", Long.class).isEmpty()) {
      throw new BusinessException(409, "请先执行后台权限迁移");
    }
  }

  private MenuVO view(Menu item) {
    MenuVO result = new MenuVO();
    BeanUtils.copyProperties(item, result);
    result.setVisible(Integer.valueOf(1).equals(item.getVisible()));
    result.setKeepAlive(Integer.valueOf(1).equals(item.getKeepAlive()));
    return result;
  }
}
