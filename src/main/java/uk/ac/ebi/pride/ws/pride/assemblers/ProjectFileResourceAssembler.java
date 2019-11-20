package uk.ac.ebi.pride.ws.pride.assemblers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import uk.ac.ebi.pride.archive.dataprovider.param.CvParam;
import uk.ac.ebi.pride.archive.dataprovider.utils.MSFileTypeConstants;
import uk.ac.ebi.pride.archive.repo.repos.file.ProjectFile;
import uk.ac.ebi.pride.ws.pride.controllers.project.ProjectController;
import uk.ac.ebi.pride.ws.pride.models.file.PrideFile;
import uk.ac.ebi.pride.ws.pride.models.file.PrideFileResource;

import java.util.ArrayList;
import java.util.List;
/**
 * @author ypriverol
 */
@Slf4j
public class ProjectFileResourceAssembler extends ResourceAssemblerSupport<ProjectFile, PrideFileResource> {

    String accession;
    public ProjectFileResourceAssembler(String projectAccession, Class<?> controller, Class<PrideFileResource> resourceType) {
        super(controller, resourceType);
        this.accession = projectAccession;
    }

    @Override
    public PrideFileResource toResource(ProjectFile oracleFile) {

        MSFileTypeConstants fileType = MSFileTypeConstants.OTHER;
        for(MSFileTypeConstants currentFileType: MSFileTypeConstants.values())
            if(currentFileType.getFileType().getName().equalsIgnoreCase(oracleFile.getFileType().getName()))
                fileType = currentFileType;

        PrideFile file = PrideFile.builder()
                .accession(String.valueOf(oracleFile.getId()))
                .fileName(oracleFile.getFileName())
                .fileSizeBytes(oracleFile.getFileSize())
                .fileCategory(new CvParam(fileType.getFileType().getCv().getCvLabel(), fileType.getFileType().getCv().getAccession(),
                        fileType.getFileType().getCv().getName(), fileType.getFileType().getCv().getValue()))
                .build();
        List<Link> links = new ArrayList<>();
        links.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(ProjectController.class).getFileByProject(this.accession, oracleFile.getId())).withSelfRel());
        return new PrideFileResource(file, links);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<PrideFileResource> toResources(Iterable<? extends ProjectFile> entities) {

        List<PrideFileResource> datasets = new ArrayList<>();

        for(ProjectFile mongoFile: entities){
            datasets.add(toResource(mongoFile));
        }

        return datasets;
    }
}
