package com.usthe.tom.dao;


import com.usthe.tom.pojo.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * @author tomsun28
 * @date 16:43 2019-07-27
 */
public interface AuthUserDao extends JpaRepository<AuthUser, Long> {

    /**
     * Get user by username
     * @param username username
     * @return user
     */
    @Query("select au from AuthUser au where au.username = :username")
    Optional<AuthUser> findAuthUserByUsername(@Param("username") String username);

    /**
     * Query the role owned by the current user
     * @param username username
     * @return role list
     */
    @Query("select ar.code from AuthRole ar, AuthUser au, AuthUserRoleBind bind " +
            "where ar.id = bind.roleId and au.id = bind.userId and au.username = :username")
    List<String> findAccountOwnRoles(@Param("username") String username);
}
