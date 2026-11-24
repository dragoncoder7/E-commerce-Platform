package com.ecommerce.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.system.entity.ProductCategory;
import com.ecommerce.system.mapper.ProductCategoryMapper;
import com.ecommerce.system.service.ProductCategoryService;
import com.ecommerce.system.dto.CategorySaveRequest;
import com.ecommerce.system.entity.Product;
import com.ecommerce.system.mapper.ProductMapper;
import com.ecommerce.common.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl extends ServiceImpl<ProductCategoryMapper, ProductCategory> implements ProductCategoryService {
    private final ProductMapper productMapper;

    @Override
    public void requireEnabled(Long id) {
        if (id == null || id <= 0) throw new BusinessException(400, "请选择有效的商品类别");
        Set<Long> visited = new HashSet<>();
        while (id != null && id != 0) {
            ProductCategory category = getById(id);
            if (category == null || !Integer.valueOf(1).equals(category.getStatus()) || !visited.add(id)) {
                throw new BusinessException(400, "商品类别或其上级不存在或已停用");
            }
            id = category.getParentId();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductCategory saveCategory(CategorySaveRequest request, boolean updating) {
        ProductCategory existing = updating && request.getId() != null ? getById(request.getId()) : null;
        if (updating && existing == null) throw new BusinessException(404, "类别不存在或已删除");
        if (!updating && request.getId() != null) throw new BusinessException(400, "新增类别不能指定 ID");
        if (request.getParentId() == null || request.getParentId() < 0
                || request.getStatus() == null || (request.getStatus() != 0 && request.getStatus() != 1)
                || request.getSortOrder() == null || request.getSortOrder() < 0
                || request.getCategoryName() == null || request.getCategoryName().isBlank() || request.getCategoryName().length() > 100
                || request.getCategoryCode() == null || request.getCategoryCode().isBlank() || request.getCategoryCode().length() > 50
                || (request.getDescription() != null && request.getDescription().length() > 500)) {
            throw new BusinessException(400, "请填写有效的类别信息");
        }
        int level = 1;
        Long parentId = request.getParentId();
        Set<Long> visited = new HashSet<>();
        if (request.getId() != null) visited.add(request.getId());
        while (parentId != null && parentId != 0) {
            if (!visited.add(parentId)) throw new BusinessException(400, "上级类别不能是自身或下级类别");
            ProductCategory parent = getById(parentId);
            if (parent == null) throw new BusinessException(400, "上级类别不存在");
            if (request.getStatus() == 1 && !Integer.valueOf(1).equals(parent.getStatus())) {
                throw new BusinessException(400, "请先启用上级类别");
            }
            level++;
            parentId = parent.getParentId();
        }
        if (level > 127) throw new BusinessException(400, "类别层级过深");
        if (existing != null && request.getStatus() == 0) {
            if (count(new LambdaQueryWrapper<ProductCategory>().eq(ProductCategory::getParentId, existing.getId())
                    .eq(ProductCategory::getStatus, 1)) > 0) throw new BusinessException(400, "请先停用子类别");
            requireNoProducts(existing.getId());
        }
        if (baseMapper.countCodeUsage(request.getCategoryCode().trim(), request.getId()) > 0) {
            throw new BusinessException(409, "类别编号已使用，请更换编号（包含已删除类别）");
        }
        ProductCategory category = new ProductCategory();
        BeanUtils.copyProperties(request, category);
        category.setCategoryName(request.getCategoryName().trim());
        category.setCategoryCode(request.getCategoryCode().trim());
        category.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());
        category.setLevel(level);
        category.setUpdateTime(LocalDateTime.now());
        try {
            if (!(updating ? updateById(category) : save(category))) throw new BusinessException(404, "类别不存在或已删除");
        } catch (DuplicateKeyException e) {
            throw new BusinessException(409, "类别编号已使用，请更换编号");
        }
        if (existing != null && !request.getParentId().equals(existing.getParentId())) {
            updateChildLevels(category.getId(), level, new HashSet<>());
        }
        return category;
    }

    private void updateChildLevels(Long id, int level, Set<Long> visited) {
        if (!visited.add(id)) throw new BusinessException(400, "类别层级存在循环");
        for (ProductCategory child : list(new LambdaQueryWrapper<ProductCategory>().eq(ProductCategory::getParentId, id))) {
            if (level >= 127) throw new BusinessException(400, "类别层级过深");
            ProductCategory patch = new ProductCategory();
            patch.setId(child.getId());
            patch.setLevel(level + 1);
            if (!updateById(patch)) throw new BusinessException(409, "子类别已变更，请刷新后重试");
            updateChildLevels(child.getId(), level + 1, visited);
        }
    }

    private void requireNoProducts(Long id) {
        if (productMapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, id)) > 0) {
            throw new BusinessException(400, "类别仍有关联商品，请先调整商品类别");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        if (getById(id) == null) throw new BusinessException(404, "类别不存在或已删除");
        if (count(new LambdaQueryWrapper<ProductCategory>().eq(ProductCategory::getParentId, id)) > 0) {
            throw new BusinessException(400, "类别下存在子类别，请先删除子类别");
        }
        requireNoProducts(id);
        if (!removeById(id)) throw new BusinessException(404, "类别不存在或已删除");
    }
    
    @Override
    public List<ProductCategory> getTreeList() {
        return list(new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getStatus, 1)
                .orderByAsc(ProductCategory::getSortOrder));
    }
    
    @Override
    public List<ProductCategory> getChildrenByParentId(Long parentId) {
        return list(new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getParentId, parentId)
                .eq(ProductCategory::getStatus, 1)
                .orderByAsc(ProductCategory::getSortOrder));
    }
}
