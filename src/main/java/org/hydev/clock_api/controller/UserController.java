package org.hydev.clock_api.controller;

import org.hydev.clock_api.entity.User;
import org.hydev.clock_api.error.ErrorCode;
import org.hydev.clock_api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.function.Function;

@Validated
@RestController
@RequestMapping("/user")
public class UserController {
    private final UserRepository userRepository;

    @Autowired
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Create salted hash for user's password
     * Format: "$username + $password".toLowerMd5();
     *
     * @param username Unique username used as a salt
     * @param password Password initial hash
     * @return Salted hash
     */
    private static String userToSaltedMd5(String username, String password) {
        String beforeMd5 = String.format("%s + %s", username, password);
        return DigestUtils.md5DigestAsHex(beforeMd5.getBytes()).toLowerCase();
    }

    /**
     * Register a user to the database.
     * <p>
     * https://www.baeldung.com/spring-rest-http-headers
     * This method should be synchronized to avoid race condition.
     * Also, this method should not be private, or else cannot use userRepository.
     * <p>
     * TODO: 2021/1/22 Need a better design!
     * Controller Return error code list as List<String>, or return uuid as String.
     *
     * @param username Unique username (Not empty, and should match the regex {@code User.RE_USERNAME})
     * @param password Password initial hash (Not empty)
     * @return Success or error
     */
    @PostMapping("/register")
    @SuppressWarnings("rawtypes")
    public synchronized ResponseEntity register(
            // [!] @RequestHeader(required = false) makes no need make another error handler.
            // [!] And also, ExceptionHandler of MissingRequestHeaderException cannot deal with all missing fields.
            @Pattern(regexp = User.RE_USERNAME, message = ErrorCode.USER_NAME_NOT_MATCH_REGEX)
            @RequestHeader String username,
            @Pattern(regexp = User.RE_PASSWORD, message = ErrorCode.USER_PASSWORD_NOT_MATCH_REGEX)
            @RequestHeader String password
    ) {
        // First, spring will check args. If not pass there regex, raise ConstraintViolationException.
        // Then, we check if username not exists. If username already exists, return ErrorCode with 409 conflict.
        if (userRepository.existsByUsername(username))
            // TODO: 2021/1/22 Using library instead of hand make.
            return ResponseEntity.status(HttpStatus.CONFLICT).body(List.of(ErrorCode.USER_NAME_ALREADY_EXISTS));

        User user = new User();
        user.setUsername(username);
        user.setPasswordMd5(userToSaltedMd5(username, password));

        // After save and flush, uuid field will be generated automatically.
        userRepository.saveAndFlush(user);
        return ResponseEntity.ok(user.getUuid());
    }

    /**
     * Check username & password.
     * - User doesn't exist -> http 404
     * - Password doesn't match -> http 401
     * - All match -> Execute operation and return the resulting String.
     *
     * @param username  Unique username
     * @param password  Password initial hash
     * @param operation Callback on success
     * @return Callback result or the error response
     */
    private ResponseEntity<String> checkPasswordAndDo(String username, String password,
                                                      Function<User, String> operation) {
        User user = userRepository.queryByUsername(username);
        if (user == null) return ResponseEntity.notFound().build();

        if (!user.getPasswordMd5().equals(userToSaltedMd5(username, password)))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String result = operation.apply(user);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/delete")
    public synchronized ResponseEntity<String> delete(@RequestHeader String username, @RequestHeader String password) {
        return checkPasswordAndDo(username, password, user -> {
            userRepository.delete(user);
            return "";
        });
    }

    @PostMapping("/login")
    public synchronized ResponseEntity<String> login(@RequestHeader String username, @RequestHeader String password) {
        return checkPasswordAndDo(username, password, user -> userRepository.queryByUsername(username).getUuid());
    }
}
