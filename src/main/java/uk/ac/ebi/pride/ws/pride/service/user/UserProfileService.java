package uk.ac.ebi.pride.ws.pride.service.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.ac.ebi.pride.archive.repo.client.UserProfileRepoClient;
import uk.ac.ebi.pride.archive.repo.models.user.User;
import uk.ac.ebi.pride.archive.repo.models.user.UserProfile;
import uk.ac.ebi.pride.archive.repo.models.user.UserSummary;
import uk.ac.ebi.pride.archive.repo.util.ObjectMapper;
import uk.ac.ebi.pride.ws.pride.utils.PrideSupportEmailSender;

@Service
@Slf4j
public class UserProfileService {

    private UserProfileRepoClient userProfileRepoClient;

    private PrideSupportEmailSender prideSupportEmailSender;

    private String registrationEmailTemplate;

    private String registrationEmailActionNeededTemplate;

    public UserProfileService(UserProfileRepoClient userProfileRepoClient,
                              PrideSupportEmailSender prideSupportEmailSender,
                              String registrationEmailTemplate, String registrationEmailActionNeededTemplate,
                              String passwordChangeEmailTemplate) {
        this.userProfileRepoClient = userProfileRepoClient;
        this.prideSupportEmailSender = prideSupportEmailSender;
        this.registrationEmailTemplate = registrationEmailTemplate;
        this.registrationEmailActionNeededTemplate = registrationEmailActionNeededTemplate;
    }

    public String registerNewUser(UserSummary userSummary) {
        log.info("Entered registerNewUser : " + userSummary.getEmail());
        try {
            User user = userProfileRepoClient.register(userSummary);

            log.info("Begin user email trigger");
            prideSupportEmailSender.sendRegistrationEmail(ObjectMapper.mapUserSummaryToUser(userSummary), user.getPassword(), registrationEmailTemplate);
            log.info("Exiting registerNewUser");
            return user.getUserRef();
        } catch (Exception e) {
            log.info("User already exists in AAP but not in PRIDE. Sending email to take action for: " + userSummary.getEmail());
            prideSupportEmailSender.sendRegistrationEmailActionNeeded(ObjectMapper.mapUserSummaryToUser(userSummary), registrationEmailActionNeededTemplate);
        }
        return "";
    }

    /*public void changePassword(String userReference, ChangePassword changePassword) throws Exception {
        User user = userRepository.findByUserRef(userReference);
        user.setPassword(changePassword.getNewPassword());
        //update in aap
        boolean isChangeSuccessful = aapService.changeAAPPassword(userReference, changePassword);
        if (isChangeSuccessful) {
            //update in pride
            user = userRepository.save(user);
            UserSummary userSummary = new UserSummary();
            userSummary.setEmail(user.getEmail());
            userSummary.setFirstName(user.getFirstName());
            userSummary.setLastName(user.getLastName());
            userSummary.setPassword(user.getPassword());
            prideSupportEmailSender.sendPasswordChangeEmail(userSummary, passwordChangeEmailTemplate);
        } else {
            throw new Exception("Failed to update pwd in AAP");
        }
    }*/

    public boolean updateProfile(String token, UserProfile userProfile) throws Exception {
        return userProfileRepoClient.updateProfile(userProfile, token);
    }


    public UserSummary getProfile(String jwtToken) throws Exception {
        return userProfileRepoClient.viewProfile(jwtToken);
    }
}
