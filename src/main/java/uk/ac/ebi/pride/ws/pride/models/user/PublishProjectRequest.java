package uk.ac.ebi.pride.ws.pride.models.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PublishProjectRequest {
    protected String pubmedId;
    protected String doi;
    protected String referenceLine;
    protected String publishJustification;

    public PublishProjectRequest() {}
}
