package com.miola.backend.service.impl;

import static com.miola.backend.constant.FileConstant.DEFAULT_USER_IMAGE_PATH;
import static com.miola.backend.constant.FileConstant.DIRECTER_CREATED;
import static com.miola.backend.constant.FileConstant.DOT;
import static com.miola.backend.constant.FileConstant.FILE_SAVED_IN_FILE_SYSTEM;
import static com.miola.backend.constant.FileConstant.FORWARD_SLASH;
import static com.miola.backend.constant.FileConstant.JPG_EXTENSION;
import static com.miola.backend.constant.FileConstant.USER_FOLDER;
import static com.miola.backend.constant.FileConstant.USER_IMAGE_PATH;
import static com.miola.backend.constant.UserImplConstant.EMAIL_ALREADY_EXISTS;
import static com.miola.backend.constant.UserImplConstant.FOUND_USER_BY_USERNAME;
import static com.miola.backend.constant.UserImplConstant.NO_USER_FOUND_BY_EMAIL;
import static com.miola.backend.constant.UserImplConstant.NO_USER_FOUND_BY_USERNAME;
import static com.miola.backend.constant.UserImplConstant.USERNAME_ALREADY_EXISTS;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.http.MediaType.IMAGE_GIF_VALUE;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;

import com.miola.backend.enumeration.Role;
import com.miola.backend.exception.domain.EmailExistException;
import com.miola.backend.exception.domain.EmailNotFoundException;
import com.miola.backend.exception.domain.NotAnImageFileException;
import com.miola.backend.exception.domain.UserNotfoundException;
import com.miola.backend.exception.domain.UsernameExistException;
import com.miola.backend.repository.UserRepository;
import com.miola.backend.service.EmailService;
import com.miola.backend.service.LoginAttemptService;
import com.miola.backend.service.UserService;
import com.miola.backend.user.User;
import com.miola.backend.user.UserPrincipal;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.mail.MessagingException;
import javax.transaction.Transactional;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
@Transactional
@Qualifier("UserDetailsService")
public class UserServiceImpl implements UserService, UserDetailsService {

    private final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final EmailService emailService;

    public UserServiceImpl(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            LoginAttemptService loginAttemptService,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.emailService = emailService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findUserByUsername(username);
        if (user == null) {
            LOGGER.error(NO_USER_FOUND_BY_USERNAME + username);
            throw new UsernameNotFoundException(NO_USER_FOUND_BY_USERNAME + username);
        } else {
            validateLoginAttempt(user);
            user.setLastLoginDateDisplay(user.getLastLoginDate());
            user.setLastLoginDate(new Date());
            userRepository.save(user);
            UserPrincipal userPrincipal = new UserPrincipal(user);
            LOGGER.info(FOUND_USER_BY_USERNAME + username);
            return userPrincipal;
        }
    }

    @Override
    public User register(String firstname, String lastname, String username, String email)
            throws EmailExistException, UserNotfoundException, UsernameExistException, MessagingException {
        validateNewUsernameAndEmail(EMPTY, username, email);
        String password = generatePassword();
        User user = User.builder()
                .userId(generateId())
                .email(email)
                .firstname(firstname)
                .lastname(lastname)
                .username(username)
                .password(encodedPassword(password))
                .joinDate(new Date())
                .isActive(true)
                .isNotLocked(true)
                .authorities(Role.ROLE_USER.getAuthorities())
                .role(Role.ROLE_USER.name())
                .profilImageUrl(getTemporaryProfileImageUrl(username))
                .build();
        userRepository.save(user);
        LOGGER.info("New user password " + password);
        emailService.sendNewPasswordEmail(firstname, password, email);
        return user;
    }

    @Override
    public User addNewUser(String firstname, String lastname, String username, String email, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage)
            throws EmailExistException, UserNotfoundException, UsernameExistException, IOException, NotAnImageFileException {
        validateNewUsernameAndEmail(EMPTY, username, email);
        String password = generatePassword();
        User user = User.builder()
                .userId(generateId())
                .email(email)
                .firstname(firstname)
                .lastname(lastname)
                .username(username)
                .password(encodedPassword(password))
                .joinDate(new Date())
                .isActive(isActive)
                .isNotLocked(isNonLocked)
                .authorities(getRoleEnumName(role).getAuthorities())
                .role(getRoleEnumName(role).name())
                .profilImageUrl(getTemporaryProfileImageUrl(username))
                .build();
        userRepository.save(user);
        saveProfileImage(user, profileImage);
        LOGGER.info("New user password " + password);
//        emailService.sendNewPasswordEmail(firstname, password, email);
        return null;
    }

    @Override
    public User updateUser(String currentUsername, String newFirstname, String newLastname, String newUsername, String newEmail, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage)
            throws EmailExistException, UserNotfoundException, UsernameExistException, IOException, NotAnImageFileException {
        User currentUser = validateNewUsernameAndEmail(currentUsername, newUsername, newEmail);
        if (currentUser != null) {
            currentUser.setEmail(newEmail);
            currentUser.setFirstname(newFirstname);
            currentUser.setLastname(newLastname);
            currentUser.setUsername(newUsername);
            currentUser.setActive(isActive);
            currentUser.setNotLocked(isNonLocked);
            currentUser.setRole(getRoleEnumName(role).name());
            currentUser.setAuthorities(getRoleEnumName(role).getAuthorities());
            userRepository.save(currentUser);
            saveProfileImage(currentUser, profileImage);
        }
        return currentUser;
    }

    @Override
    public void resetPassword(String email) throws EmailNotFoundException, MessagingException {
        User user = userRepository.findUserByEmail(email);

        if (user == null) {
            throw new EmailNotFoundException(NO_USER_FOUND_BY_EMAIL + email);
        }

        String password = generatePassword();
        user.setPassword(encodedPassword(password));
        userRepository.save(user);
        emailService.sendNewPasswordEmail(user.getFirstname(), password, email);
    }

    @Override
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Override
    public User findUserByUsername(String username) {
        return userRepository.findUserByUsername(username);
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findUserByEmail(email);
    }

    @Override
    public void deleteUser(long id) {
        userRepository.deleteById(id);
    }

    @Override
    public void deleteUser(String username) throws IOException {
        Path userFolder = Paths.get(USER_FOLDER + username).toAbsolutePath().normalize();
        FileUtils.deleteDirectory(new File(userFolder.toString()));
        userRepository.deleteUserByUsername(username);
    }

    @Override
    public User updateProfileImage(String username, MultipartFile profileImage)
            throws EmailExistException, UserNotfoundException, UsernameExistException, IOException, NotAnImageFileException {
        User user = validateNewUsernameAndEmail(username, null, null);
        saveProfileImage(user, profileImage);
        return user;
    }

    private void saveProfileImage(User user, MultipartFile profileImage)
            throws IOException, NotAnImageFileException {
        if (profileImage != null) {

            if (! Arrays.asList(IMAGE_JPEG_VALUE, IMAGE_GIF_VALUE, IMAGE_PNG_VALUE).contains(profileImage.getContentType())) {
                throw new NotAnImageFileException(profileImage.getOriginalFilename() + "is not an image file. Please upload an image");
            }
            Path userFolder = Paths.get(USER_FOLDER + user.getUsername()).toAbsolutePath().normalize();
            if (! Files.exists(userFolder)) {
                Files.createDirectories(userFolder);
                LOGGER.info(DIRECTER_CREATED + userFolder);
            }
            Files.deleteIfExists(Paths.get(userFolder + user.getUsername() + DOT + JPG_EXTENSION));
            Files.copy(profileImage.getInputStream(), userFolder.resolve(user.getUsername() + DOT + JPG_EXTENSION), REPLACE_EXISTING);
            user.setProfilImageUrl(setProfilImageUrl(user.getUsername()));
            userRepository.save(user);
            LOGGER.info(FILE_SAVED_IN_FILE_SYSTEM + profileImage.getOriginalFilename());
        }
    }

    private String setProfilImageUrl(String username) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(USER_IMAGE_PATH + username + FORWARD_SLASH + username + DOT + JPG_EXTENSION)
                .toUriString();
    }

    private String getTemporaryProfileImageUrl(String username) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(DEFAULT_USER_IMAGE_PATH + username)
                .toUriString();
    }

    private Role getRoleEnumName(String role) {
        return Role.valueOf(role.toUpperCase());
    }

    private void validateLoginAttempt(User user) {
        if (user.isNotLocked()) {
            user.setNotLocked(!loginAttemptService.hasExceededMaxAttempts(user.getUsername()));
        } else {
            loginAttemptService.evictUserFromLoginAttemptCache(user.getUsername());
        }
    }

    private String encodedPassword(String password) {
        return passwordEncoder.encode(password);
    }

    private String generatePassword() {
        return RandomStringUtils.randomAlphanumeric(10);
    }

    private String generateId() {
        return RandomStringUtils.randomNumeric(10);
    }

    private User validateNewUsernameAndEmail(String currentUsername, String newUsername,
            String newEmail)
            throws UserNotfoundException, UsernameExistException, EmailExistException {
        User userByNewUsername = findUserByUsername(newUsername);
        User userByNewEmail = findUserByEmail(newEmail);

        if (StringUtils.isNotBlank(currentUsername)) {
            User currentUser = findUserByUsername(currentUsername);
            if (currentUser == null) {
                throw new UserNotfoundException(NO_USER_FOUND_BY_USERNAME + currentUsername);
            }
            if (userByNewUsername != null && !currentUser.getId()
                    .equals(userByNewUsername.getId())) {
                throw new UsernameExistException(USERNAME_ALREADY_EXISTS);
            }
            if (userByNewEmail != null && !currentUser.getId().equals(userByNewEmail.getId())) {
                throw new EmailExistException(EMAIL_ALREADY_EXISTS);
            }
            return currentUser;
        } else {
            if (userByNewUsername != null) {
                throw new UsernameExistException(USERNAME_ALREADY_EXISTS);
            }
            if (userByNewEmail != null) {
                throw new EmailExistException(EMAIL_ALREADY_EXISTS);
            }
            return null;
        }
    }
}
