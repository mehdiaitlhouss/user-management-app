package com.miola.backend.resource;

import static com.miola.backend.constant.FileConstant.FORWARD_SLASH;
import static com.miola.backend.constant.FileConstant.TEMP_PROFILE_IMAGE_BASE_URL;
import static com.miola.backend.constant.FileConstant.USER_FOLDER;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;

import com.miola.backend.constant.SecurityConstant;
import com.miola.backend.exception.domain.EmailExistException;
import com.miola.backend.exception.domain.EmailNotFoundException;
import com.miola.backend.exception.domain.ExceptionHandling;
import com.miola.backend.exception.domain.NotAnImageFileException;
import com.miola.backend.exception.domain.UserNotfoundException;
import com.miola.backend.exception.domain.UsernameExistException;
import com.miola.backend.service.UserService;
import com.miola.backend.user.HttpResponse;
import com.miola.backend.user.User;
import com.miola.backend.user.UserPrincipal;
import com.miola.backend.utility.JWTTokenProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import javax.mail.MessagingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(path = {"/", "/user"})
public class UserResource extends ExceptionHandling {

    public static final String USER_DELETE_SUCCESSFULLY = "User delete successfully";
    private static final String EMAIL_SENT = "Email sent ";
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JWTTokenProvider jwtTokenProvider;

    public UserResource(UserService userService,
            AuthenticationManager authenticationManager,
            JWTTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user)
            throws UserNotfoundException, EmailExistException, UsernameExistException, MessagingException {
        User newUser = userService.register(
                user.getFirstname(),
                user.getLastname(),
                user.getUsername(),
                user.getEmail()
        );
        return new ResponseEntity<>(newUser, OK);
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody User user) {
        authentication(user.getUsername(), user.getPassword());
        User loginUser = userService.findUserByUsername(user.getUsername());
        UserPrincipal userPrincipal = new UserPrincipal(loginUser);
        HttpHeaders jwtHeader = getJwtHeader(userPrincipal);
        return new ResponseEntity<>(loginUser, jwtHeader, OK);
    }

    @PostMapping("/add")
    public ResponseEntity<User> addNewUser(
            @RequestParam("firstname") String firstname,
            @RequestParam("lastname") String lastname,
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("role") String role,
            @RequestParam("isActive") String isActive,
            @RequestParam("isNotLocked") String isNotLocked,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage
    )
            throws EmailExistException, UserNotfoundException, IOException, UsernameExistException, MessagingException, NotAnImageFileException {
        User newUser = userService.addNewUser(
                firstname,
                lastname,
                username,
                email,
                role,
                Boolean.parseBoolean(isNotLocked),
                Boolean.parseBoolean(isActive),
                profileImage
        );
        return new ResponseEntity<>(newUser, OK);
    }

    @PostMapping("/update")
    public ResponseEntity<User> update(
            @RequestParam("currentUsername") String currentUsername,
            @RequestParam("firstname") String firstname,
            @RequestParam("lastname") String lastname,
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("role") String role,
            @RequestParam("isActive") String isActive,
            @RequestParam("isNotLocked") String isNotLocked,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage
    )
            throws EmailExistException, UserNotfoundException, IOException, UsernameExistException, NotAnImageFileException {
        User updatedUser = userService.updateUser(
                currentUsername,
                firstname,
                lastname,
                username,
                email,
                role,
                Boolean.parseBoolean(isNotLocked),
                Boolean.parseBoolean(isActive),
                profileImage
        );
        return new ResponseEntity<>(updatedUser, OK);
    }

    @DeleteMapping("/delete/{username}")
    @PreAuthorize("hasAnyAuthority('user:delete')")
    public ResponseEntity<HttpResponse> delete(@PathVariable("username") String username)
            throws IOException {
        userService.deleteUser(username);
        return new ResponseEntity<>
                (
                        new HttpResponse
                                (
                                        OK.value(),
                                        OK,
                                        OK.getReasonPhrase().toUpperCase(),
                                        USER_DELETE_SUCCESSFULLY
                                ),
                        OK
                );
    }

    @GetMapping("/find/{username}")
    public ResponseEntity<User> getUser(@PathVariable("username") String username) {
        User user = userService.findUserByUsername(username);
        return new ResponseEntity<>(user, OK);
    }

    @GetMapping("/list")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getUsers();
        return new ResponseEntity<>(users, OK);
    }

    @GetMapping("/resetPassword/{email}")
    public ResponseEntity<HttpResponse> resetPassword(@PathVariable("email") String email)
            throws EmailNotFoundException, MessagingException {
        userService.resetPassword(email);
        return new ResponseEntity<>
                (
                        new HttpResponse
                                (
                                        OK.value(),
                                        OK,
                                        OK.getReasonPhrase().toUpperCase(),
                                        (EMAIL_SENT + email).toUpperCase()
                                ),
                        OK
                );
    }

    @GetMapping(value = "/image/{username}/{fileName}", produces = IMAGE_JPEG_VALUE)
    public byte[] getProfileImage(@PathVariable("username") String username, @PathVariable("fileName") String fileName)
            throws IOException {
        return Files.readAllBytes(Paths.get(USER_FOLDER + username + FORWARD_SLASH + fileName));
    }

    @GetMapping(value = "/image/profile/{username}", produces = IMAGE_JPEG_VALUE)
    public byte[] getTempProfileImage(@PathVariable("username") String username)
            throws IOException {
        URL url = new URL(TEMP_PROFILE_IMAGE_BASE_URL + username);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try (InputStream inputStream = url.openStream()) {
            int bytesRead;
            byte[] chunk = new byte[1024];
            while ((bytesRead = inputStream.read(chunk)) > 0) {
                stream.write(chunk, 0, bytesRead);
            }
        }
        return stream.toByteArray();
    }

    @PostMapping("/updateProfileImage")
    public ResponseEntity<User> updateProfileImage(
            @RequestParam("username") String username,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage
    )
            throws EmailExistException, UserNotfoundException, IOException, UsernameExistException, NotAnImageFileException {
        User updatedUserProfileImage = userService.updateProfileImage(
                username,
                profileImage
        );
        return new ResponseEntity<>(updatedUserProfileImage, OK);
    }

    private HttpHeaders getJwtHeader(UserPrincipal userPrincipal) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(SecurityConstant.JWT_TOKEN_HEADER, jwtTokenProvider.generateJwtToken(userPrincipal));
        return headers;
    }

    private void authentication(String username, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }

}


