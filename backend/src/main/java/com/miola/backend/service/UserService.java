package com.miola.backend.service;

import com.miola.backend.exception.domain.EmailExistException;
import com.miola.backend.exception.domain.EmailNotFoundException;
import com.miola.backend.exception.domain.NotAnImageFileException;
import com.miola.backend.exception.domain.UserNotfoundException;
import com.miola.backend.exception.domain.UsernameExistException;
import com.miola.backend.user.User;
import java.io.IOException;
import java.util.List;
import javax.mail.MessagingException;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    User register(String firstname, String lastname, String username, String email) throws EmailExistException, UserNotfoundException, UsernameExistException, MessagingException;

    List<User> getUsers();

    User findUserByUsername(String username);

    User findUserByEmail(String email);

    User addNewUser(String firstname, String lastname, String username, String email, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage)
            throws EmailExistException, UserNotfoundException, UsernameExistException, IOException, MessagingException, NotAnImageFileException;

    User updateUser(String currentUsername, String newFirstname, String newLastname, String newUsername, String newEmail, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage)
            throws EmailExistException, UserNotfoundException, UsernameExistException, IOException, NotAnImageFileException;

    void deleteUser(long id);
    void deleteUser(String id) throws IOException;

    void resetPassword(String email) throws EmailNotFoundException, MessagingException;

    User updateProfileImage(String username, MultipartFile profileImage)
            throws EmailExistException, UserNotfoundException, UsernameExistException, IOException, NotAnImageFileException;
}
