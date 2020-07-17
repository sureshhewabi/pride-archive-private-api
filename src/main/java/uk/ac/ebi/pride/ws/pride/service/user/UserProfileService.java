package uk.ac.ebi.pride.ws.pride.service.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import uk.ac.ebi.pride.archive.repo.client.UserProfileRepoClient;
import uk.ac.ebi.pride.archive.repo.models.user.*;
import uk.ac.ebi.pride.archive.repo.util.ObjectMapper;
import uk.ac.ebi.pride.ws.pride.utils.PrideSupportEmailSender;

@Service
@Slf4j
public class UserProfileService {

    private UserProfileRepoClient userProfileRepoClient;

    private PrideSupportEmailSender prideSupportEmailSender;

    private String registrationEmailTemplate;

    private String registrationEmailActionNeededTemplate;

    @Autowired
    public UserProfileService(UserProfileRepoClient userProfileRepoClient,
                              PrideSupportEmailSender prideSupportEmailSender,
                              String registrationEmailTemplate, String registrationEmailActionNeededTemplate) {
        this.userProfileRepoClient = userProfileRepoClient;
        this.prideSupportEmailSender = prideSupportEmailSender;
        this.registrationEmailTemplate = registrationEmailTemplate;
        this.registrationEmailActionNeededTemplate = registrationEmailActionNeededTemplate;
    }

    public String registerNewUser(UserSummary userSummary) throws Exception {
        log.info("Entered registerNewUser : " + userSummary.getEmail());
        try {
            User user = userProfileRepoClient.register(userSummary);
            log.info("Begin user email trigger");
            prideSupportEmailSender.sendRegistrationEmail(user,user.getPassword(), registrationEmailTemplate);
            log.info("Exiting registerNewUser");
            return userSummary.getUserRef();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatus.CONFLICT)) {
                log.info("User already exists in AAP but not in PRIDE. Sending email to take action for: " + userSummary.getEmail());
                prideSupportEmailSender.sendRegistrationEmailActionNeeded(ObjectMapper.mapUserSummaryToUser(userSummary), registrationEmailActionNeededTemplate);
            } else {
                throw e;
            }
        }
        return "";
    }

    public boolean updateProfile(String token, UserProfile userProfile) throws Exception {
        return userProfileRepoClient.updateProfile(userProfile, token);
    }

    public UserSummary getProfile(String jwtToken) throws Exception {
        return userProfileRepoClient.viewProfile(jwtToken);
    }

    public String getAAPToken(Credentials credentials) throws Exception {
        return userProfileRepoClient.getAAPToken(credentials);
    }

    public String resetPassword(ResetPassword resetPassword) throws Exception {
        return userProfileRepoClient.resetPassword(resetPassword);
    }
}
