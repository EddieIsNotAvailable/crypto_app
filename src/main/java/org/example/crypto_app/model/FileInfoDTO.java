package org.example.crypto_app.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileInfoDTO {
    private Long id;
    private String fileName;
    private String fileType;
    private Integer fileSize;
    private List<FileHash> fileHashes;
    private CryptoKey skey;
}
