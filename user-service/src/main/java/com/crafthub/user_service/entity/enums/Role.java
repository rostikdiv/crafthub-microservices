package com.crafthub.user_service.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.crafthub.user_service.entity.enums.Permission.*;

/**
 * Enum representing user roles within the system.
 * Each role defines a set of permissions and provides Spring Security
 * authorities.
 */
@RequiredArgsConstructor
@Getter
public enum Role {

        BUYER(Set.of(
                        PRODUCT_READ,
                        ORDER_CREATE,
                        ORDER_READ_MY,
                        PROFILE_UPDATE)),

        SELLER(Set.of(
                        PRODUCT_READ,
                        PRODUCT_CREATE,
                        PRODUCT_UPDATE,
                        PRODUCT_DELETE,
                        ORDER_CREATE,
                        ORDER_READ_MY, // Seller sees orders for their products
                        ORDER_UPDATE_STATUS, // Change status (e.g., "Shipped")
                        PROFILE_UPDATE)),

        MILITARY_UNIT(Set.of(
                        PRODUCT_READ, // Access to restricted goods verified in logic
                        ORDER_CREATE,
                        ORDER_READ_MY,
                        PROFILE_UPDATE,
                        PRODUCT_BUY_RESTRICTED)),

        ADMIN(Set.of(
                        PRODUCT_READ, PRODUCT_CREATE, PRODUCT_UPDATE, PRODUCT_DELETE, PRODUCT_BUY_RESTRICTED,
                        ORDER_READ_ALL, ORDER_UPDATE_STATUS,
                        PROFILE_UPDATE,
                        USER_BAN, USER_VERIFY,
                        ORDER_CREATE, ORDER_READ_MY));

        private final Set<Permission> permissions;

        /**
         * Converts the role and its permissions into a list of Spring Security
         * GrantedAuthority objects.
         */
        public List<SimpleGrantedAuthority> getAuthorities() {
                var authorities = getPermissions()
                                .stream()
                                .map(permission -> new SimpleGrantedAuthority(permission.getPermission()))
                                .collect(Collectors.toList());
                authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
                return authorities;
        }
}