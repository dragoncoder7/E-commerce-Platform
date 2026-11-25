package com.ecommerce.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.core.exception.BusinessException;
import com.ecommerce.system.domain.MemberAccount;
import com.ecommerce.system.mapper.MemberAccountMapper;
import com.ecommerce.system.vo.MemberManagementView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** ERP 直接变更共享会员数据；数据库版本使已签发商城会话立即失效。 */
@Service
@RequiredArgsConstructor
public class MemberManagementService {
  private final MemberAccountMapper members;

  public Page<MemberManagementView> list(int pageNum, int pageSize, String keyword, Integer status) {
    if (pageNum < 1 || pageSize < 1 || pageSize > 100 || keyword != null && keyword.length() > 100
        || status != null && status != 0 && status != 1) {
      throw new BusinessException(400, "分页或筛选条件无效");
    }
    LambdaQueryWrapper<MemberAccount> query = new LambdaQueryWrapper<MemberAccount>()
        .eq(status != null, MemberAccount::getStatus, status).orderByDesc(MemberAccount::getId);
    if (keyword != null && !keyword.isBlank()) {
      String value = keyword.trim();
      query.and(search -> search.like(MemberAccount::getUsername, value).or().like(MemberAccount::getPhone, value)
          .or().like(MemberAccount::getEmail, value).or().like(MemberAccount::getNickname, value));
    }
    Page<MemberAccount> result = members.selectPage(new Page<>(pageNum, pageSize), query);
    Page<MemberManagementView> response = new Page<>(pageNum, pageSize, result.getTotal());
    response.setRecords(result.getRecords().stream().map(MemberManagementView::from).toList());
    return response;
  }

  @Transactional(rollbackFor = Exception.class)
  public void changeStatus(Long id, int status) {
    if (status != 0 && status != 1) {
      throw new BusinessException(400, "会员状态无效");
    }
    MemberAccount member = members.lockById(id);
    if (member == null) {
      throw new BusinessException(404, "会员不存在");
    }
    if (member.getStatus() == status) {
      return;
    }
    // 禁用和恢复均递增版本，恢复账号不会使历史会话重新有效。
    members.update(null, new LambdaUpdateWrapper<MemberAccount>().eq(MemberAccount::getId, id)
        .set(MemberAccount::getStatus, status).set(MemberAccount::getSessionVersion, member.getSessionVersion() + 1));
  }
}
