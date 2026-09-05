package com.paysentry.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paysentry.backend.entity.MobileDeviceToken;
import com.paysentry.backend.entity.User;

public interface MobileDeviceTokenRepository extends JpaRepository<MobileDeviceToken, Long> {

    Optional<MobileDeviceToken> findByFcmToken(String fcmToken);

    List<MobileDeviceToken> findByUserAndActiveTrue(User user);
}
