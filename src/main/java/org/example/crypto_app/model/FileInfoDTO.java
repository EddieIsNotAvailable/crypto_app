package org.example.crypto_app.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter @AllArgsConstructor
public class FileInfoDTO {
    private Long id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private List<FileHash> fileHashes;
}
