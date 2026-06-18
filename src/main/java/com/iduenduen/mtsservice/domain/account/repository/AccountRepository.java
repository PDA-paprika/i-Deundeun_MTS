package com.iduenduen.mtsservice.domain.account.repository;

import com.iduenduen.mtsservice.domain.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}
