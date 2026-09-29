package com.company.jobmanagement.service;

import com.company.jobmanagement.model.entity.Notification;
import com.company.jobmanagement.model.enums.NotificationType;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.exception.ForbiddenOperationException;
import com.company.jobmanagement.repository.NotificationRepository;
import com.company.jobmanagement.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@Transactional
@Slf4j
public class NotificationService extends AbstractBaseService<Notification, Long, NotificationRepository> {

    private final CurrentUser currentUser;

    public NotificationService(NotificationRepository repository, CurrentUser currentUser) {
        super(repository);
        this.currentUser = currentUser;
    }

    @Override
    protected String getEntityName() {
        return "Notification";
    }

    public Notification notify(User recipient, NotificationType type, String title, String message, Long relatedTaskId) {
        Notification notification = Notification.builder()
                .user(recipient)
                .type(type)
                .title(title)
                .message(message)
                .relatedTaskId(relatedTaskId)
                .isRead(false)
                .build();
        return create(notification);
    }

    public Notification createReminder(Long userId, String title, String message, ZonedDateTime remindAt) {
        Notification notification = Notification.builder()
                .user(User.builder().id(userId).build())
                .type(NotificationType.REMINDER)
                .title(title)
                .message(message)
                .remindAt(remindAt)
                .isRead(false)
                .build();
        return create(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForCurrentUser() {
        User user = currentUser.getCurrentUser();
        if (user == null) {
            throw new ForbiddenOperationException("User not authenticated");
        }
        return repository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadForCurrentUser() {
        User user = currentUser.getCurrentUser();
        if (user == null) {
            throw new ForbiddenOperationException("User not authenticated");
        }
        return repository.findByUserIdAndIsReadFalse(user.getId());
    }

    public Notification markAsRead(Long notificationId) {
        Notification notification = getById(notificationId);
        User currentUser = this.currentUser.getCurrentUser();

        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenOperationException("Can only mark your own notifications as read");
        }

        notification.setIsRead(true);
        return repository.save(notification);
    }

    public int markAllAsReadForCurrentUser() {
        List<Notification> unread = getUnreadForCurrentUser();
        unread.forEach(n -> n.setIsRead(true));
        repository.saveAll(unread);
        return unread.size();
    }

    public void deleteOwn(Long notificationId) {
        Notification notification = getById(notificationId);
        User user = this.currentUser.getCurrentUser();

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ForbiddenOperationException("Can only delete your own notifications");
        }

        repository.deleteById(notificationId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getRemindersToProcess() {
        return repository.findByRemindAtLessThanEqualAndIsReadFalse(ZonedDateTime.now());
    }
}
