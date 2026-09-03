package com.funwallet.backend.repository;

import com.funwallet.backend.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {
}
