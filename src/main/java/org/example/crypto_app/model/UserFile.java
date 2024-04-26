package org.example.crypto_app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.example.crypto_app.model.enums.HashType;

import java.util.ArrayList;
import java.util.List;

@Entity @Getter @Setter
@NoArgsConstructor
public class UserFile {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  private String fileName;

  @NotNull
  private String fileType;

  @NotNull
  private Long fileSize;

  @ManyToOne
  @JoinColumn(name = "key_id")
  private CryptoKey key;

  @Lob @NotNull @Basic(fetch = FetchType.LAZY)
  private byte [] fileContent;

//  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  private List<FileHash> fileHashes;

  public UserFile(String fileName, String fileType, byte[] bytes) {
    this.fileName = fileName;
    this.fileType = fileType;
    this.fileSize = (long) bytes.length;
    this.fileContent = bytes;
  }

  public void generateHashes() {
    fileHashes = new ArrayList<>();
    fileHashes.add(new FileHash(fileContent, HashType.MD5));
    fileHashes.add(new FileHash(fileContent, HashType.SHA256));
  }
}