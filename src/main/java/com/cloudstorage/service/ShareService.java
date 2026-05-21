package com.cloudstorage.service;

import com.cloudstorage.dao.FileDao;
import com.cloudstorage.dao.FolderDao;
import com.cloudstorage.dao.ShareDao;
import com.cloudstorage.dao.UserDao;
import com.cloudstorage.exception.AppException;
import com.cloudstorage.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервис управления общим доступом к файлам и папкам
 */
public class ShareService {
    private static final Logger logger = LoggerFactory.getLogger(ShareService.class);
    private final ShareDao shareDao;
    private final FileDao fileDao;
    private final FolderDao folderDao;
    private final UserDao userDao;

    public ShareService() {
        this.shareDao = new ShareDao();
        this.fileDao = new FileDao();
        this.folderDao = new FolderDao();
        this.userDao = new UserDao();
    }

    /**
     * Создать публичную ссылку
     */
    public PublicShare createPublicShare(int ownerId, int itemId, String itemType, Instant expiresAt) {
        if (!"FILE".equals(itemType) && !"FOLDER".equals(itemType)) {
            throw new AppException("Неверный тип элемента", 400);
        }
        validateOwnership(ownerId, itemId, itemType);
        PublicShare share = new PublicShare();
        share.setToken(UUID.randomUUID().toString());
        share.setItemId(itemId);
        share.setItemType(itemType);
        share.setUserId(ownerId);
        share.setExpiresAt(expiresAt);
        PublicShare created = shareDao.createPublicShare(share);
        logger.info("Создана публичная ссылка {} для {} {}", share.getToken(), itemType, itemId);
        return created;
    }

    /**
     * Получить публичную ссылку по токену
     */
    public Optional<PublicShare> getPublicShare(String token) {
        return shareDao.findPublicByToken(token);
    }

    /**
     * Создать доступ для конкретного пользователя
     */
    public UserShare createUserShare(int ownerId, int itemId, String itemType, int targetUserId, String permission) {
        if (!"FILE".equals(itemType) && !"FOLDER".equals(itemType)) {
            throw new AppException("Неверный тип элемента", 400);
        }
        if (!"READ".equals(permission) && !"WRITE".equals(permission)) {
            throw new AppException("Неверный тип разрешения", 400);
        }
        if (userDao.findById(targetUserId).isEmpty()) {
            throw new AppException("Целевой пользователь не найден", 404);
        }
        if (ownerId == targetUserId) {
            throw new AppException("Нельзя поделиться с самим собой", 400);
        }
        validateOwnership(ownerId, itemId, itemType);
        if (shareDao.findUserShare(itemId, itemType, targetUserId).isPresent()) {
            throw new AppException("Доступ уже предоставлен", 409);
        }
        UserShare share = new UserShare();
        share.setItemId(itemId);
        share.setItemType(itemType);
        share.setOwnerId(ownerId);
        share.setTargetUserId(targetUserId);
        share.setPermission(permission);
        UserShare created = shareDao.createUserShare(share);
        logger.info("Предоставлен доступ {} пользователю {} к {} {}", permission, targetUserId, itemType, itemId);
        return created;
    }

    /**
     * Проверить доступ пользователя к файлу
     */
    public boolean canAccessFile(int fileId, Integer userId) {
        if (userId == null) return false;
        Optional<FileItem> file = fileDao.findById(fileId);
        if (file.isEmpty()) return false;
        // Владелец
        if (file.get().getUserId() == userId) return true;
        // Проверка user_shares
        Optional<UserShare> share = shareDao.findUserShare(fileId, "FILE", userId);
        return share.isPresent();
    }

    /**
     * Проверить доступ пользователя к папке
     */
    public boolean canAccessFolder(int folderId, Integer userId) {
        if (userId == null) return false;
        Optional<Folder> folder = folderDao.findById(folderId);
        if (folder.isEmpty()) return false;
        // Владелец
        if (folder.get().getUserId() == userId) return true;
        // Проверка user_shares
        Optional<UserShare> share = shareDao.findUserShare(folderId, "FOLDER", userId);
        return share.isPresent();
    }

    /**
     * Получить содержимое публичной папки
     */
    public List<FileItem> getPublicFolderContents(int folderId) {
        return fileDao.findByFolder(
                folderDao.findById(folderId).map(Folder::getUserId).orElse(0),
                folderId
        );
    }

    /**
     * Получить шаринги пользователя
     */
    public List<UserShare> getUserShares(int targetUserId) {
        return shareDao.findUserSharesByTarget(targetUserId);
    }

    /**
     * Удалить публичную ссылку
     */
    public void deletePublicShare(String token, int userId) {
        Optional<PublicShare> share = shareDao.findPublicByToken(token);
        if (share.isEmpty()) {
            throw new AppException("Ссылка не найдена", 404);
        }
        if (share.get().getUserId() != userId) {
            throw new AppException("Нет доступа к ссылке", 403);
        }
        shareDao.deletePublicShare(token, userId);
        logger.info("Удалена публичная ссылка {} пользователем {}", token, userId);
    }

    /**
     * Проверить, что пользователь владеет элементом
     */
    private void validateOwnership(int ownerId, int itemId, String itemType) {
        if ("FILE".equals(itemType)) {
            FileItem file = fileDao.findById(itemId)
                    .orElseThrow(() -> new AppException("Файл не найден", 404));
            if (file.getUserId() != ownerId) {
                throw new AppException("Нет доступа к файлу", 403);
            }
        } else {
            Folder folder = folderDao.findById(itemId)
                    .orElseThrow(() -> new AppException("Папка не найдена", 404));
            if (folder.getUserId() != ownerId) {
                throw new AppException("Нет доступа к папке", 403);
            }
        }
    }
}