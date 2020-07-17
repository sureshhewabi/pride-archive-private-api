package uk.ac.ebi.pride.ws.pride.controllers;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import uk.ac.ebi.pride.archive.repo.models.user.ResetPassword;
import uk.ac.ebi.pride.archive.repo.models.user.UserProfile;
import uk.ac.ebi.pride.archive.repo.models.user.UserSummary;
import uk.ac.ebi.pride.ws.pride.service.user.UserProfileService;
import uk.ac.ebi.pride.ws.pride.utils.WsContastants;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = "/user")
@Slf4j
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Autowired
    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @ApiOperation(notes = "Register a new user", value = "registration", nickname = "registerNewUser", tags = {"User"})
    @RequestMapping(method = RequestMethod.POST, path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> registerNewUser(@RequestBody UserSummary userSummary) {
        try {
            String userRef = userProfileService.registerNewUser(userSummary);
            return ResponseEntity.ok(String.valueOf(userRef));
        } catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                log.error(((HttpClientErrorException) e).getStatusText(), e);
                throw (HttpClientErrorException) e;
            }
            log.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @ApiOperation(notes = "View user's profile. User needs to be authenticated to access", value = "viewProfile", nickname = "viewProfile", tags = {"User"})
    @PreAuthorize("isAuthenticated()")
    @GetMapping(path = "/view-profile", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getProfile(HttpServletRequest request) throws HttpClientErrorException {
        String token = getToken(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(WsContastants.AAP_TOKEN_MISMATCH_ERROR);
        }
        try {
            UserSummary userSummary = userProfileService.getProfile(token);
            return ResponseEntity.ok(userSummary);
        } catch (Exception ex) {
            if (ex instanceof HttpClientErrorException) {
                throw (HttpClientErrorException) ex;
            }
            log.error(ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error fetching user profile:" + ex.getMessage());
        }
    }

    @ApiOperation(notes = "Update user's profile. User needs to be authenticated to access", value = "updateProfile", nickname = "updateProfile", tags = {"User"})
    @PreAuthorize("isAuthenticated()")
    @PostMapping(path = "/update-profile", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> updateProfile(@RequestBody UserProfile userProfile,
                                                HttpServletRequest request) throws HttpClientErrorException {
        String token = getToken(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(WsContastants.AAP_TOKEN_MISMATCH_ERROR);
        }
        try {
            return ResponseEntity.ok(userProfileService.updateProfile(token, userProfile));
        } catch (Exception ex) {
            if (ex instanceof HttpClientErrorException) {
                throw (HttpClientErrorException) ex;
            }
            log.error(ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    @ApiOperation(notes = "Reset password", value = "resetPassword", nickname = "resetPassword", tags = {"User"})
    @PostMapping(path = "/reset-password")
    public String resetPassword(@RequestBody ResetPassword resetPassword) throws Exception {
        return userProfileService.resetPassword(resetPassword);
    }

    private String getToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.replaceFirst("Bearer ", "");
        } else {
            return null;
        }
    }
}
