package com.cloudstorage.service;

import com.cloudstorage.dao.FileDao;
import com.cloudstorage.dao.FolderDao;
import com.cloudstorage.exception.AppException;
import com.cloudstorage.model.Folder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис управления папками
 */
public class FolderService {
    private static final Logger logger = LoggerFactory.getLogger(FolderService.class);
    private final FolderDao folderDao;
    private final FileDao fileDao;
    private final QuotaService quotaService;

    public FolderService() {
        this.folderDao = new FolderDao();
        this.fileDao = new FileDao();
        this.quotaService = new QuotaService();
    }

    /**
     * Создать папку
     */
    public Folder createFolder(int userId, String name, Integer parentId) {
        if (name == null || name.trim().isEmpty()) {
            throw new AppException("Имя папки не может быть пустым", 400);
        }
        if (name.length() > 255) {
            throw new AppException("Имя папки слишком длинное", 400);
        }

        // Проверка родительской папки
        if (parentId != null) {
            Folder parent = folderDao.findById(parentId)
                    .orElseThrow(() -> new AppException("Родительская папка не найдена", 404));
            if (!parent.getUserId().equals(userId)) {
                throw new AppException("Нет доступа к родительской папке", 403);
            }
            if (parent.isDeleted()) {
                throw new AppException("Родительская папка удалена", 400);
            }
        }

        // Проверка на дубликат
        if (folderDao.existsByNameAndParent(userId, name.trim(), parentId)) {
            throw new AppException("Папка с таким именем уже существует", 409);
        }

        Folder folder = new Folder();
        folder.setUserId(userId);
        folder.setName(name.trim());
        folder.setParentId(parentId);
        folder.setDeleted(false);

        Folder created = folderDao.create(folder);
        logger.info("Создана папка '{}' для пользователя {}", name, userId);
        return created;
    }

    /**
     * Получить дерево папок пользователя
     */
    public List<FolderTreeNode> getFolderTree(int userId) {
        List<Folder> allFolders = folderDao.findByUserId(userId);
        return buildTree(allFolders, null);
    }

    /**
     * Построить дерево папок рекурсивно
     */
    private List<FolderTreeNode> buildTree(List<Folder> allFolders, Integer parentId) {
        List<FolderTreeNode> result = new ArrayList<>();
        for (Folder folder : allFolders) {
            if (parentId == null ? folder.getParentId() == null : folder.getParentId() != null && folder.getParentId().equals(parentId)) {
                FolderTreeNode node = new FolderTreeNode();
                node.setId(folder.getId());
                node.setName(folder.getName());
                node.setParentId(folder.getParentId());
                node.setCreatedAt(folder.getCreatedAt());
                node.setChildren(buildTree(allFolders, folder.getId()));
                result.add(node);
            }
        }
        return result;
    }

    /**
     * Удалить папку (soft delete) со всем содержимым
     */
    public void deleteFolder(int folderId, int userId) {
        Folder folder = folderDao.findById(folderId)
                .orElseThrow(() -> new AppException("Папка не найдена", 404));

        if (!folder.getUserId().equals(userId)) {
            throw new AppException("Нет доступа к папке", 403);
        }
        if (folder.isDeleted()) {
            throw new AppException("Папка уже удалена", 400);
        }

        // Получаем все вложенные папки
        List<Folder> allFolders = folderDao.findByUserId(userId);
        List<Integer> folderIdsToDelete = new ArrayList<>();
        collectFolderIds(allFolders, folderId, folderIdsToDelete);
        folderIdsToDelete.add(folderId);

        // Получаем все файлы в этих папках для пересчета квоты
        List<com.cloudstorage.model.FileItem> files = fileDao.findByFolderIds(folderIdsToDelete);
        long totalSize = files.stream().filter(f -> !f.isDeleted()).mapToLong(com.cloudstorage.model.FileItem::getSize).sum();

        // Помечаем все папки как удаленные
        for (Integer id : folderIdsToDelete) {
            folderDao.softDelete(id, userId);
        }

        // Помечаем все файлы как удаленные
        for (com.cloudstorage.model.FileItem file : files) {
            if (!file.isDeleted()) {
                fileDao.softDelete(file.getId(), userId);
            }
        }

        // Обновляем квоту
        if (totalSize > 0) {
            quotaService.removeUsedSpace(userId, totalSize);
        }

        logger.info("Удалена папка {} пользователя {} (вместе с {} файлами, {} байт)", 
                folderId, userId, files.size(), totalSize);
    }

    /**
     * Рекурсивный сбор ID всех дочерних папок
     */
    private void collectFolderIds(List<Folder> allFolders, Integer parentId, List<Integer> result) {
        for (Folder folder : allFolders) {
            if (folder.getParentId() != null && folder.getParentId().equals(parentId)) {
                result.add(folder.getId());
                collectFolderIds(allFolders, folder.getId(), result);
            }
        }
    }

    /**
     * Получить папку по ID
     */
    public Folder getFolder(int folderId, int userId) {
        Folder folder = folderDao.findById(folderId)
                .orElseThrow(() -> new AppException("Папка не найдена", 404));
        if (!folder.getUserId().equals(userId)) {
            throw new AppException("Нет доступа к папке", 403);
        }
        return folder;
    }

    /**
     * Узел дерева папок для JSON ответа
     */
    public static class FolderTreeNode {
        private Integer id;
        private String name;
        private Integer parentId;
        private java.time.Instant createdAt;
        private List<FolderTreeNode> children;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getParentId() { return parentId; }
        public void setParentId(Integer parentId) { this.parentId = parentId; }
        public java.time.Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }
        public List<FolderTreeNode> getChildren() { return children; }
        public void setChildren(List<FolderTreeNode> children) { this.children = children; }
    }
}
