package uk.ac.ebi.pride.ws.pride.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.log4j.Log4j;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.archive.dataprovider.utils.TitleConstants;
import uk.ac.ebi.pride.archive.repo.repos.user.User;
import uk.ac.ebi.pride.archive.repo.services.user.UserService;
import uk.ac.ebi.pride.archive.repo.services.user.UserSummary;
import uk.ac.ebi.pride.archive.repo.util.AAPConstants;
import uk.ac.ebi.pride.ws.pride.models.user.AapJwtToken;
import uk.ac.ebi.pride.ws.pride.models.user.Credentials;
import uk.ac.ebi.pride.ws.pride.service.user.AAPService;
import uk.ac.ebi.pride.ws.pride.service.user.UserProfileService;
import uk.ac.ebi.pride.ws.pride.utils.APIError;

import javax.validation.Valid;
import java.nio.charset.Charset;
import java.util.Date;

@RestController
@Log4j
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Value("${aap.auth.url}")
    private String auth_url;

    @Autowired
    private UserService userServiceImpl;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private AAPService aapService;

    @ApiOperation(notes = "Get a valid access token", value = "access token", nickname = "getAAPToken", tags = {"authorization"})
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = APIError.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = APIError.class),
            @ApiResponse(code = 400, message = "Bad Request", response = APIError.class)
    })
    @RequestMapping(path = "getAAPToken", method = RequestMethod.POST)
    public String getAAPToken(@RequestBody @Valid Credentials credentials) throws Exception {
        ResponseEntity<String> response = null;
        String jwtToken = null;
        String email = credentials.getUsername();
        try {
            HttpEntity<String> entity = new HttpEntity<>(createHttpHeaders(email, credentials.getPassword()));
            RestTemplate restTemplate = new RestTemplate();
            response = restTemplate.exchange
                    (auth_url, HttpMethod.GET, entity, String.class);
            jwtToken = response.getBody();
            boolean emailInUse = userServiceImpl.isEmailedInUse(email);
            if (emailInUse) {//update password for existing local user..just to avoid mismatch between AAP & our local DB
                userProfileService.updateLocalPassword(email, credentials.getPassword());
            } else { // User is in AAP DB but not in our DB
                java.util.Base64.Decoder decoder = java.util.Base64.getUrlDecoder();
                String[] parts = jwtToken.split("\\."); // split out the "parts" (header, payload and signature)
                String payloadJson = new String(decoder.decode(parts[1]));
                ObjectMapper mapper = new ObjectMapper();
                try {
                    AapJwtToken aapJwtToken = mapper.readValue(payloadJson, AapJwtToken.class);
                    UserSummary userSummary = new UserSummary();
                    userSummary.setUserRef(aapJwtToken.getAapRef());
                    userSummary.setEmail(aapJwtToken.getEmail());
                    userSummary.setPassword(credentials.getPassword());
                    userSummary.setTitle(TitleConstants.UNKNOWN);
                    userSummary.setFirstName(aapJwtToken.getName());
                    userSummary.setLastName(" ");
                    userSummary.setAffiliation(" ");
                    userSummary.setAcceptedTermsOfUse(true);
                    userSummary.setAcceptedTermsOfUseAt(new Date());
                    User user = userServiceImpl.signUp(userSummary);

                    //Add user to submitter domain in AAP
                    logger.info("Begin user domain registeration: " + userSummary.getEmail());
                    if (user.getUserRef() != null) {
                        boolean isDomainRegSuccessful = aapService.addUserToAAPDomain(user.getUserRef(), AAPConstants.PRIDE_SUBMITTER_DOMAIN);
                        if (!isDomainRegSuccessful) {
                            logger.error("Error adding user to submitter domain in AAP:" + user.getEmail());
                        }
                    }
                } catch (Exception ex) {
                    logger.error("Email doesn't exists in our DB but exists in AAP. Failed to register during login : " + email);
                    logger.error(ex.getMessage(), ex);
                }
            }

        } catch (HttpClientErrorException e) {
            String s = "Username/password wrong : " + email;
            log.info(s);
            throw new Exception(s, e);
        } catch (Exception e) {
            String message = "Error while getting AAP token : " + email;
            log.error(message);
            throw new RuntimeException(message, e);
        }
        return jwtToken;
    }

    @ApiOperation(notes = "Get a valid access token", value = "access token", nickname = "test-token-validity", tags = {"authorization"})
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = APIError.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = APIError.class),
            @ApiResponse(code = 400, message = "Bad Request", response = APIError.class)
    })
    @RequestMapping(method = RequestMethod.POST, path = "/token-validation")
    public String getTokenValidity() {
        return "Token Valid";
    }

    private static HttpHeaders createHttpHeaders(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(Charset.forName("US-ASCII")));
        String authHeader = "Basic " + new String(encodedAuth);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", authHeader);
        return headers;
    }

}
