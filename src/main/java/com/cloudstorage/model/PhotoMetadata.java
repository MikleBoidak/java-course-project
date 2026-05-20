package com.cloudstorage.model;

import java.time.Instant;

/**
 * Модель метаданных фотографии (EXIF)
 */
public class PhotoMetadata {
    private Integer id;
    private Integer fileId;
    private Instant dateTaken;
    private String cameraModel;
    private Double latitude;
    private Double longitude;

    public PhotoMetadata() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getFileId() { return fileId; }
    public void setFileId(Integer fileId) { this.fileId = fileId; }

    public Instant getDateTaken() { return dateTaken; }
    public void setDateTaken(Instant dateTaken) { this.dateTaken = dateTaken; }

    public String getCameraModel() { return cameraModel; }
    public void setCameraModel(String cameraModel) { this.cameraModel = cameraModel; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
