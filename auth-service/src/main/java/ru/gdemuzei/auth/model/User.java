package ru.gdemuzei.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String password;  // Argon2id hash

    private List<String> authorities;  // ["ROLE_ADMIN", "SCOPE_read"]

    private LocalDateTime createdAt;

    private LocalDateTime lastLogin;

    private boolean enabled;
}
