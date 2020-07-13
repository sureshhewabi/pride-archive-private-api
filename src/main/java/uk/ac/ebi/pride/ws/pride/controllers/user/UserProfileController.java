package uk.ac.ebi.pride.ws.pride.controllers.user;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.pride.archive.repo.models.user.UserProfile;
import uk.ac.ebi.pride.archive.repo.models.user.UserSummary;
import uk.ac.ebi.pride.ws.pride.service.user.UserProfileService;
import uk.ac.ebi.pride.ws.pride.utils.WsContastants;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping(path = "/user")
@Slf4j
public class UserProfileController {

    @Autowired
    private UserProfileService userProfileService;

    @ApiOperation(notes = "Register a new user", value = "registration", nickname = "registerNewUser", tags = {"User"})
    @RequestMapping(method = RequestMethod.POST, path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> registerNewUser(@RequestBody @Valid UserSummary userSummary, BindingResult errors) {

        if (errors.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors.getAllErrors());
        }

        try {
            String userRef = userProfileService.registerNewUser(userSummary);
            return ResponseEntity.ok(String.valueOf(userRef));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

    }

    /*@ApiOperation(notes = "Change password for a user. User needs to be authenticated to access", value = "change-pwd", nickname = "changePwd", tags = {"User"})
    @PreAuthorize("isAuthenticated()")
    @PostMapping(path = "/change-password", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> changePassword(@RequestBody @Valid ChangePassword changePassword,
                                                 BindingResult errors,
                                                 Authentication authentication) {
        if (errors.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors.getAllErrors());
        } else {
            try {
                User currentUser = (User) (authentication).getDetails();
                if (!currentUser.getEmail().equalsIgnoreCase(changePassword.getEmail())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email mismatch occurred");
                }
                userProfileService.changePassword(currentUser.getUserReference(), changePassword);
                return ResponseEntity.ok().build();
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
            }
        }
    }*/

    @ApiOperation(notes = "View user's profile. User needs to be authenticated to access", value = "viewProfile", nickname = "viewProfile", tags = {"User"})
    @PreAuthorize("isAuthenticated()")
    @GetMapping(path = "/view-profile", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getProfile(HttpServletRequest request) {
        String token = getToken(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(WsContastants.AAP_TOKEN_MISMATCH_ERROR);
        }
        try {
            UserSummary userSummary = userProfileService.getProfile(token);
            return ResponseEntity.ok(userSummary);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error fetching user profile:" + e.getMessage());
        }
    }

    @ApiOperation(notes = "Update user's profile. User needs to be authenticated to access", value = "updateProfile", nickname = "updateProfile", tags = {"User"})
    @PreAuthorize("isAuthenticated()")
    @PostMapping(path = "/update-profile", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> updateProfile(@RequestBody @Valid UserProfile userProfile,
                                                HttpServletRequest request) {
        String token = getToken(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(WsContastants.AAP_TOKEN_MISMATCH_ERROR);
        }
        try {
            return ResponseEntity.ok(userProfileService.updateProfile(token, userProfile));
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }

    }

   /* @PostMapping(path = "/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPassword resetPassword) {
        ResponseEntity<String> responseEntity = aapService.resetPassword(resetPassword);
        userProfileService.updateLocalPassword(resetPassword.getUsername(), resetPassword.getPassword());
        return responseEntity;
    }*/


    private String getToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.replaceFirst("Bearer ", "");
        } else {
            return null;
        }
    }
}
