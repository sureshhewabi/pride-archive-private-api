package uk.ac.ebi.pride.ws.pride.models.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AapJwtToken {

    @JsonProperty("sub")
    private String aapRef;
    private String email;
    private String name;
    private String[] domains;

    public String getAapRef() {
        return aapRef;
    }

    public void setAapRef(String aapRef) {
        this.aapRef = aapRef;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getDomains() {
        return domains;
    }

    public void setDomains(String[] domains) {
        this.domains = domains;
    }
}
