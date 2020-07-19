package uk.ac.ebi.pride.ws.pride.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import uk.ac.ebi.pride.archive.repo.models.user.User;
import uk.ac.ebi.pride.archive.repo.models.user.UserSummary;
import uk.ac.ebi.pride.ws.pride.models.feedback.Feedback;
import uk.ac.ebi.pride.ws.pride.models.project.PublishProject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
public class PrideSupportEmailSender {

    public static final Logger logger = LoggerFactory.getLogger(PrideSupportEmailSender.class);

    public static final String USER_FEEDBACK_EMAIL_TITLE = "PRIDE user feedback";
    public static final String REGISTRATION_EMAIL_TITLE = "PRIDE user registration";
    public static final String REGISTRATION_ACTION_NEEDED_EMAIL_TITLE = "PRIDE user registration: Action needed";
    public static final String PASSWORD_RESET_EMAIL_TITLE = "Password reset";
    public static final String PUBLISH_PROJECT_EMAIL_TITLE = "Publish project";
    public static final String USER_NAME_PLACE_HOLDER = "[USERNAME]";
    public static final String TITLE_PLACE_HOLDER = "[TITLE]";
    public static final String FIRST_NAME_PLACE_HOLDER = "[FIRST_NAME]";
    public static final String LAST_NAME_PLACE_HOLDER = "[LAST_NAME]";
    public static final String AFFILIATION_PLACE_HOLDER = "[AFFILIATION]";
    public static final String PASSWORD_PLACE_HOLDER = "[PASSWORD]";
    public static final String COMMENT_PLACE_HOLDER = "[COMMENTS]";
    public static final String TWITTER_PLACE_HOLDER = "[TWITTER]";
    public static final String EMAIL_PLACE_HOLDER = "[EMAIL]";
    public static final String COUNTRY_PLACE_HOLDER = "[COUNTRY]";
    public static final String ORCID_PLACE_HOLDER = "[ORCID]";
    public static final String OLD_EMAIL_PLACE_HOLDER = "[OLD_EMAIL]";
    public static final String NEW_EMAIL_PLACE_HOLDER = "[NEW_EMAIL]";
    public static final String PRIDE_URL_PLACE_HOLDER = "[PRIDE_URL]";
    public static final String PRIDE_ARCHIVE_HELP_URL_PLACE_HOLDER = "[PRIDE_ARCHIVE_HELP_URL]";
    public static final String PRIDE_ARCHIVE_SUBMISSION_URL_PLACE_HOLDER = "[PRIDE_ARCHIVE_SUBMISSION_URL]";
    public static final String LOGIN_URL_PLACE_HOLDER = "[PRIDE_LOGIN_URL]";
    public static final String PROFILE_UPDATE_URL_PLACE_HOLDER = "[PROFILE_UPDATE_LINK]";
    public static final String PASSWORD_RESET_URL_PLACE_HOLDER = "[PASSWORD_RESET_LINK]";
    public static final String PRIDE_ARCHIVE_LOGIN_URL_PLACE_HOLDER = "[PRIDE_ARCHIVE_LOGIN_URL]";
    public static final String PRIDE_SUPPORT_EMAIL_PLACE_HOLDER = "[PRIDE_SUPPORT]";
    public static final String PROJECT_ACCESSION_PLACE_HOLDER = "[PROJECT_ACCESSION]";
    public static final String PUBMED_PLACE_HOLDER = "[PUBMED]";
    public static final String DOI_PLACE_HOLDER = "[DOI]";
    public static final String REFERNCE_LINE_PLACE_HOLDER = "[REFLINE]";
    public static final String USER_STRING = "[USER_STRING]";
    public static final String REASON_LINE_PLACE_HOLDER = "[REASON]";
    public static final String NOT_AVAILABLE = "Not available";

    @Autowired
    private JavaMailSender mailSender;

    @Value("${pride.support.email.address}")
    private String prideSupportEmailAddress;

    @Value("${pride.url}")
    private String prideUrl;

    @Value("${pride.login_url}")
    private String prideLoginUrl;

    @Value("${pride.password_reset_url}")
    private String passwordResetUrl;

    @Value("${pride.update_profile_url}")
    private String updateProfileUrl;

    @Value("${pride.archive.help.url}")
    private String prideArchiveHelpUrl;

    @Value("${pride.archive.submission.url}")
    private String prideArchiveSubmissionUrl;

    @Value("${pride.archive.login.url}")
    private String prideArchiveLoginUrl;

    @Value("${twitter.account}")
    private String twitterAccount;

    public PrideSupportEmailSender() {
    }

    public String getEmailTemplate(Resource emailTemplate) throws IOException {
        StringBuilder message = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(emailTemplate.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                message.append(line);
                message.append(System.getProperty("line.separator"));
            }
        }

        return message.toString();
    }

    public void sendPasswordResetEmail(UserSummary user, String emailTemplate) {
        String emailBody = emailTemplate;
        emailBody = emailBody.replace(USER_NAME_PLACE_HOLDER, user.getFirstName() + " " + user.getLastName());
        emailBody = emailBody.replace(PASSWORD_PLACE_HOLDER, user.getPassword());
        emailBody = emailBody.replace(PRIDE_ARCHIVE_LOGIN_URL_PLACE_HOLDER, prideArchiveLoginUrl);

        emailBody = formatCommonFields(emailBody);

        try {
            sendEmail(new String[]{user.getEmail()}, PASSWORD_RESET_EMAIL_TITLE, emailBody);
        } catch (MailException ex) {
            String message = "Failed to send password reset email to: " + user.getEmail();
            logger.error(message, ex);
        }
    }

    public void sendEmailChangeEmail(UserSummary user, String oldEmail, String emailTemplate) {
        String emailBody = emailTemplate;
        emailBody = emailBody.replace(USER_NAME_PLACE_HOLDER, user.getFirstName() + " " + user.getLastName());
        emailBody = emailBody.replace(OLD_EMAIL_PLACE_HOLDER, oldEmail);
        emailBody = emailBody.replace(NEW_EMAIL_PLACE_HOLDER, user.getEmail());

        emailBody = formatCommonFields(emailBody);

        try {
            sendEmail(new String[]{oldEmail}, PASSWORD_RESET_EMAIL_TITLE, emailBody);
        } catch (MailException ex) {
            String message = "Failed to send email change email to: " + oldEmail;
            logger.error(message, ex);
        }
    }

    public void sendFeedbackEmail(Feedback feedback, String emailTemplate) {
        String emailBody = emailTemplate;
        emailBody = emailBody.replace(COMMENT_PLACE_HOLDER, feedback.getComment());
        emailBody = emailBody.replace(EMAIL_PLACE_HOLDER, feedback.getEmail());

        try {
            sendEmail(new String[]{prideSupportEmailAddress}, USER_FEEDBACK_EMAIL_TITLE, emailBody);
        } catch (MailException ex) {
            String message = "Failed to send feedback email to: " + prideSupportEmailAddress;
            logger.error(message, ex);
        }
    }

    public void sendPasswordChangeEmail(UserSummary user, String emailTemplate) {
        String emailBody = emailTemplate;
        emailBody = emailBody.replace(USER_NAME_PLACE_HOLDER, user.getFirstName() + " " + user.getLastName());

        emailBody = formatCommonFields(emailBody);

        try {
            sendEmail(new String[]{user.getEmail()}, PASSWORD_RESET_EMAIL_TITLE, emailBody);
        } catch (MailException ex) {
            String message = "Failed to send email change email to: " + user.getEmail();
            logger.error(message, ex);
        }
    }

    public void sendPublishProjectEmail(PublishProject publishProject, String projectAccession, String publishProjectEmailTemplate) {
        String emailBody = publishProjectEmailTemplate;
        emailBody = emailBody.replace(PROJECT_ACCESSION_PLACE_HOLDER, projectAccession);

        String pubmedId = publishProject.getPubmedId();
        emailBody = emailBody.replace(PUBMED_PLACE_HOLDER, pubmedId == null ? NOT_AVAILABLE : pubmedId);

        String doi = publishProject.getDoi();
        emailBody = emailBody.replace(DOI_PLACE_HOLDER, doi == null ? NOT_AVAILABLE : doi);

        String referenceLine = publishProject.getReferenceLine();
        emailBody = emailBody.replace(REFERNCE_LINE_PLACE_HOLDER, referenceLine == null ? NOT_AVAILABLE : referenceLine);

        String reasonLine = publishProject.getPublishJustification();
        emailBody = emailBody.replace(REASON_LINE_PLACE_HOLDER, reasonLine == null ? NOT_AVAILABLE : reasonLine);

        String userString = publishProject.getUserName();
        if (publishProject.isAuthorized()) {
            userString += "\n(users status: authorized)";
        } else {
            userString += "\n(users status: not authorized)";
        }
        emailBody = emailBody.replace(USER_STRING, userString);

        sendEmail(new String[]{prideSupportEmailAddress}, PUBLISH_PROJECT_EMAIL_TITLE, emailBody);
    }

    public void sendRegistrationEmail(User user, String password, String emailTemplate) {
        String emailBody = emailTemplate;
        emailBody = emailBody.replace(USER_NAME_PLACE_HOLDER, user.getFirstName() + " " + user.getLastName());
        emailBody = emailBody.replace(EMAIL_PLACE_HOLDER, user.getEmail());
        emailBody = emailBody.replace(PASSWORD_PLACE_HOLDER, password);
        emailBody = emailBody.replace(TITLE_PLACE_HOLDER, user.getTitle().getTitle());
        emailBody = emailBody.replace(FIRST_NAME_PLACE_HOLDER, user.getFirstName());
        emailBody = emailBody.replace(LAST_NAME_PLACE_HOLDER, user.getLastName());
        emailBody = emailBody.replace(AFFILIATION_PLACE_HOLDER, user.getAffiliation());
        emailBody = emailBody.replace(COUNTRY_PLACE_HOLDER, user.getCountry());
        emailBody = emailBody.replace(ORCID_PLACE_HOLDER, user.getOrcid());
        emailBody = emailBody.replace(PRIDE_ARCHIVE_SUBMISSION_URL_PLACE_HOLDER, prideArchiveSubmissionUrl);
        emailBody = emailBody.replace(PRIDE_ARCHIVE_HELP_URL_PLACE_HOLDER, prideArchiveHelpUrl);
        emailBody = formatCommonFields(emailBody);
        try {
            sendEmail(new String[]{user.getEmail()}, REGISTRATION_EMAIL_TITLE, emailBody);
        } catch (MailException ex) {
            String message = "Failed to register user: " + user.getEmail();
            logger.error(message, ex);
        }
    }

    public void sendRegistrationEmailActionNeeded(User user, String emailTemplate) {

        String emailBody = emailTemplate;
        emailBody = emailBody.replace(USER_NAME_PLACE_HOLDER, user.getFirstName() + " " + user.getLastName());
        emailBody = emailBody.replace(EMAIL_PLACE_HOLDER, user.getEmail());
        emailBody = emailBody.replace(PASSWORD_RESET_URL_PLACE_HOLDER, passwordResetUrl);
        emailBody = emailBody.replace(LOGIN_URL_PLACE_HOLDER, prideLoginUrl);
        emailBody = emailBody.replace(PROFILE_UPDATE_URL_PLACE_HOLDER, updateProfileUrl);
        emailBody = emailBody.replace(PRIDE_ARCHIVE_SUBMISSION_URL_PLACE_HOLDER, prideArchiveSubmissionUrl);
        emailBody = emailBody.replace(PRIDE_ARCHIVE_HELP_URL_PLACE_HOLDER, prideArchiveHelpUrl);
        emailBody = formatCommonFields(emailBody);
        try {
            sendEmail(new String[]{user.getEmail()}, REGISTRATION_ACTION_NEEDED_EMAIL_TITLE, emailBody);
        } catch (MailException ex) {
            String message = "Failed to register user: " + user.getEmail();
            logger.error(message, ex);
        }
    }

    private String formatCommonFields(String emailBody) {
        emailBody = emailBody.replace(PRIDE_SUPPORT_EMAIL_PLACE_HOLDER, prideSupportEmailAddress);
        emailBody = emailBody.replace(PRIDE_URL_PLACE_HOLDER, prideUrl);
        emailBody = emailBody.replace(TWITTER_PLACE_HOLDER, twitterAccount);
        return emailBody;
    }

    private void sendEmail(String[] mailTo, String title, String emailBody) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(prideSupportEmailAddress);
        msg.setReplyTo(prideSupportEmailAddress);
        msg.setTo(mailTo);
        msg.setSubject(title);
        msg.setText(emailBody);
        mailSender.send(msg);
    }

    @Autowired
    private ResourceLoader resourceLoader;

    @Bean("registrationEmailTemplate")
    public String getRegistrationEmailTemplate() throws IOException {
        Resource emailTemplateResource = resourceLoader.getResource("classpath:email-template/registration.template");
        return getEmailTemplate(emailTemplateResource);
    }

    @Bean("registrationEmailActionNeededTemplate")
    public String getRegistrationEmailActionNeededTemplate() throws IOException {
        Resource emailTemplateResource = resourceLoader.getResource("classpath:email-template/registration-action-needed.template");
        return getEmailTemplate(emailTemplateResource);
    }

    @Bean("passwordChangeEmailTemplate")
    public String getChangePwdEmailTemplate() throws IOException {
        Resource emailTemplateResource = resourceLoader.getResource("classpath:email-template/password-change.template");
        return getEmailTemplate(emailTemplateResource);
    }

    public String getpublishProjectEmailTemplate() throws IOException {
        Resource emailTemplateResource = resourceLoader.getResource("classpath:email-template/publish-project.template");
        return getEmailTemplate(emailTemplateResource);
    }
}
