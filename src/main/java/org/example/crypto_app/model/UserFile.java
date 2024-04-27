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

  private String fileName;

  private String fileType;

  @ManyToOne
  @JoinColumn(name = "key_id")
  private CryptoKey key;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  private byte[] fileContent;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  private List<FileHash> fileHashes;

  public UserFile(String fileName, String fileType, byte[] bytes) {
    this.fileName = fileName;
    this.fileType = fileType;
    this.fileContent = bytes;
  }

  public void generateHashes() {
    fileHashes = new ArrayList<>();
    fileHashes.add(new FileHash(fileContent, HashType.MD5));
    fileHashes.add(new FileHash(fileContent, HashType.SHA256));
  }
}