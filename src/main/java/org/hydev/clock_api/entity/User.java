package org.hydev.clock_api.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hydev.clock_api.error.ErrorCode;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;

@Data
@Entity(name = "users")
public class User {
    // https://digitalfortress.tech/tricks/top-15-commonly-used-regex/

    // https://www.regexpal.com/?fam=104030
    public static final String RE_USERNAME = "^[a-z0-9_-]{3,16}$";
    // https://www.regexpal.com/?fam=104029
    public static final String RE_PASSWORD = "(?=(.*[0-9]))((?=.*[A-Za-z0-9])(?=.*[A-Z])(?=.*[a-z]))^.{8,}$";

    // https://stackoverflow.com/questions/21517102/regex-to-match-md5-hashes
    // https://stackoverflow.com/questions/45153520/are-md5-hashes-always-either-capital-or-lowercase
    public static final String RE_LOWER_MD5 = "^[a-f0-9]{32}$";

    // https://stackoverflow.com/questions/45635827/how-do-i-stop-spring-data-jpa-from-doing-a-select-before-a-save
    @Id

    // Define a generator, and use it.
    // https://thorben-janssen.com/generate-uuids-primary-keys-hibernate/
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @GeneratedValue(generator = "UUID")
    private String uuid;

    @NotNull(message = ErrorCode.INNER_USERNAME_IS_NULL)
    @Pattern(regexp = RE_USERNAME, message = ErrorCode.INNER_USERNAME_NOT_MATCH_REGEX)
    private String username;

    @NotNull(message = ErrorCode.INNER_PASSWORD_MD5_IS_NULL)
    @Pattern(regexp = RE_LOWER_MD5, message = ErrorCode.INNER_PASSWORD_MD5_NOT_MATCH_REGEX)
    private String passwordMd5;

    // https://stackoverflow.com/questions/8202154/how-to-create-an-auto-generated-date-timestamp-field-in-a-play-jpa/8207652
    @CreationTimestamp
    private LocalDateTime joinDate;
}
