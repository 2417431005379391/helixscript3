package com.usthe.tom.service.impl;

import com.usthe.sureness.matcher.TreePathRoleMatcher;
import com.usthe.tom.dao.AuthResourceDao;
import com.usthe.tom.dao.AuthRoleDao;
import com.usthe.tom.dao.AuthRoleResourceBindDao;
import com.usthe.tom.pojo.entity.AuthResource;
import com.usthe.tom.pojo.entity.AuthRole;
import com.usthe.tom.pojo.entity.AuthRoleResourceBind;
import com.usthe.tom.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * @author tomsun28
 * @date 13:10 2019-08-04
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class RoleServiceImpl implements RoleService {

    @Autowired
    private AuthRoleDao authRoleDao;

    @Autowired
    private AuthResourceDao authResourceDao;

    @Autowired
    private AuthRoleResourceBindDao roleResourceBindDao;

    @Autowired
    private TreePathRoleMatcher treePathRoleMatcher;

    @Override
    public boolean isRoleExist(AuthRole authRole) {
        AuthRole role = AuthRole.builder()
                .name(authRole.getName()).code(authRole.getCode()).build();
        return authRoleDao.exists(Example.of(role));
    }

    @Override
    public boolean addRole(AuthRole authRole) {
        if (isRoleExist(authRole)) {
            return false;
        } else {
            authRoleDao.saveAndFlush(authRole);
            return true;
        }
    }

    @Override
    public boolean updateRole(AuthRole authRole) {
        if (authRoleDao.existsById(authRole.getId())) {
            authRoleDao.saveAndFlush(authRole);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean deleteRole(Long roleId) {
        if (authRoleDao.existsById(roleId)) {
            authRoleDao.deleteById(roleId);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Optional<List<AuthRole>> getAllRole() {
        List<AuthRole> roleList = authRoleDao.findAll();
        return Optional.of(roleList);
    }

    @Override
    public Page<AuthRole> getPageRole(Integer currentPage, Integer pageSize) {
        PageRequest pageRequest = PageRequest.of(currentPage, pageSize);
        return authRoleDao.findAll(pageRequest);
    }

    @Override
    public Page<AuthResource> getPageResourceOwnRole(Long roleId, Integer currentPage, Integer pageSize) {
        PageRequest pageRequest = PageRequest.of(currentPage, pageSize, Sort.Direction.ASC, "id");
        return authResourceDao.findRoleOwnResource(roleId, pageRequest);
    }

    @Override
    public Page<AuthResource> getPageResourceNotOwnRole(Long roleId, Integer currentPage, Integer pageSize) {
        PageRequest pageRequest = PageRequest.of(currentPage, pageSize, Sort.Direction.ASC, "id");
        return authResourceDao.findRoleNotOwnResource(roleId, pageRequest);
    }

    @Override
    public void authorityRoleResource(Long roleId, Long resourceId) {
        // Determine whether this resource and role exist
        if (!authRoleDao.existsById(roleId) || !authResourceDao.existsById(resourceId)) {
            throw new DataConflictException("roleId or resourceId not exist");
        }
        // insert it in database, if existed the unique index will work
        AuthRoleResourceBind bind = AuthRoleResourceBind
                .builder().roleId(roleId).resourceId(resourceId).build();
        roleResourceBindDao.saveAndFlush(bind);
        // refresh resource path data tree
        treePathRoleMatcher.rebuildTree();
    }

    @Override
    public void deleteAuthorityRoleResource(Long roleId, Long resourceId) {
        roleResourceBindDao.deleteRoleResourceBind(roleId, resourceId);
        // refresh resource path data tree
        treePathRoleMatcher.rebuildTree();
    }
}
