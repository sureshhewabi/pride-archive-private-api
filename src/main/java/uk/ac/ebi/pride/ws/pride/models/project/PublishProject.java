package uk.ac.ebi.pride.ws.pride.models.project;

import lombok.Builder;

public class PublishProject extends PublishProjectRequest{
    private String userName;
    private boolean authorized;

    @Builder
    private PublishProject(String pubmedId, String doi, String referenceLine, String publishJustification,
                           String userName, boolean authorized){

        super(pubmedId, doi, referenceLine, publishJustification);
        this.userName = userName;
        this.authorized = authorized;
    }

    public String getUserName() {
        return userName;
    }

    public boolean isAuthorized() {
        return authorized;
    }
}
