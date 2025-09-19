package org.example.pingpongsystem.service;

import lombok.RequiredArgsConstructor;
import org.example.pingpongsystem.entity.NotificationEntity;
import org.example.pingpongsystem.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * 创建通知
     */
    @Transactional
    public void createNotification(Long userId, NotificationEntity.UserType userType,
                                   Long appointmentId, String content) {
        NotificationEntity notification = new NotificationEntity();
        notification.setUserId(userId);
        notification.setUserType(userType);
        notification.setAppointmentId(appointmentId);
        notification.setContent(content);
        notification.setRead(false);
        notification.setCreateTime(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    /**
     * 获取用户未读通知
     */
    public List<NotificationEntity> getUnreadNotifications(Long userId, NotificationEntity.UserType userType) {
        return notificationRepository.findByUserIdAndUserTypeAndIsReadFalse(userId, userType);
    }

    /**
     * 标记通知为已读
     */
    @Transactional
    public void markNotificationAsRead(Long notificationId) {
        notificationRepository.findById(notificationId)
                .ifPresent(notification -> {
                    notification.setRead(true);
                    notificationRepository.save(notification);
                });
    }
}