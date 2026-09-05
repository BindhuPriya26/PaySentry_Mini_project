package com.paysentry.backend.monitoring;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paysentry.backend.entity.MobileDeviceToken;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.repository.MobileDeviceTokenRepository;

@Service
public class MobileDeviceService {

    private final MobileDeviceTokenRepository mobileDeviceTokenRepository;

    public MobileDeviceService(MobileDeviceTokenRepository mobileDeviceTokenRepository) {
        this.mobileDeviceTokenRepository = mobileDeviceTokenRepository;
    }

    @Transactional
    public void register(User user, String fcmToken, String deviceName) {
        MobileDeviceToken token = mobileDeviceTokenRepository.findByFcmToken(fcmToken).orElseGet(MobileDeviceToken::new);
        token.setUser(user);
        token.setFcmToken(fcmToken);
        token.setDeviceName(deviceName == null ? "Unknown device" : deviceName.trim());
        token.setActive(true);
        mobileDeviceTokenRepository.save(token);
    }
}
