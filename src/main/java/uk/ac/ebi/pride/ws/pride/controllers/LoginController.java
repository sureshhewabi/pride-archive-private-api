package uk.ac.ebi.pride.ws.pride.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import uk.ac.ebi.pride.archive.repo.models.user.Credentials;
import uk.ac.ebi.pride.ws.pride.service.user.UserProfileService;
import uk.ac.ebi.pride.ws.pride.utils.APIError;

import javax.validation.Valid;
import java.nio.charset.Charset;

@RestController
@Slf4j
public class LoginController {

    private UserProfileService userProfileService;

    public LoginController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

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
            jwtToken = userProfileService.getAAPToken(credentials);
        } catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                log.error(((HttpClientErrorException) e).getResponseBodyAsString(), e);
                throw e;
            }
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
