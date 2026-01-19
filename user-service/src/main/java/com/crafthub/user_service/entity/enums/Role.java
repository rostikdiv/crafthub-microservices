package com.crafthub.user_service.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Статичний імпорт для скорочення коду
import static com.crafthub.user_service.entity.enums.Permission.*;

@RequiredArgsConstructor
@Getter
public enum Role {

    BUYER(Set.of(
            PRODUCT_READ,
            ORDER_CREATE,
            ORDER_READ_MY,
            PROFILE_UPDATE
    )),

    SELLER(Set.of(
            PRODUCT_READ,
            PRODUCT_CREATE,
            PRODUCT_UPDATE,
            PRODUCT_DELETE,
            ORDER_READ_MY,       // Продавець бачить замовлення на свої товари
            ORDER_UPDATE_STATUS, // Може змінювати статус (наприклад, "Відправлено")
            PROFILE_UPDATE
    )),

    MILITARY_UNIT(Set.of(
            PRODUCT_READ,        // Доступ до RESTRICTED товарів перевіряється окремо в коді
            ORDER_CREATE,
            ORDER_READ_MY,
            PROFILE_UPDATE,
            PRODUCT_BUY_RESTRICTED
    )),

    ADMIN(Set.of(
            // Адмін має всі права
            PRODUCT_READ, PRODUCT_CREATE, PRODUCT_UPDATE, PRODUCT_DELETE, PRODUCT_BUY_RESTRICTED,
            ORDER_READ_ALL, ORDER_UPDATE_STATUS,
            PROFILE_UPDATE,
            USER_BAN, USER_VERIFY,
            ORDER_CREATE, ORDER_READ_MY
    ));

    private final Set<Permission> permissions;

    public List<SimpleGrantedAuthority> getAuthorities() {
        var authorities = getPermissions()
                .stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getPermission()))
                .collect(Collectors.toList());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
        return authorities;
    }
}