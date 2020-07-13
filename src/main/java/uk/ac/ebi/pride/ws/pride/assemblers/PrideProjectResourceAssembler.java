package uk.ac.ebi.pride.ws.pride.assemblers;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.security.core.Authentication;
import uk.ac.ebi.pride.archive.repo.models.project.Project;
import uk.ac.ebi.pride.ws.pride.controllers.project.ProjectController;
import uk.ac.ebi.pride.ws.pride.models.dataset.PrideProject;
import uk.ac.ebi.pride.ws.pride.models.dataset.ProjectResource;
import uk.ac.ebi.pride.ws.pride.transformers.Transformer;
import uk.ac.ebi.pride.ws.pride.utils.WsContastants;

import java.util.ArrayList;
import java.util.List;

/**
 * This code is licensed under the Apache License, Version 2.0 (the
 *
 * @author ypriverol
 */
@Slf4j
public class PrideProjectResourceAssembler extends ResourceAssemblerSupport<Project, ProjectResource> {

    Authentication authentication;

    public PrideProjectResourceAssembler(Authentication authentication, Class<?> controllerClass, Class<ProjectResource> resourceType) {
        super(controllerClass, resourceType);
        this.authentication = authentication;
    }

    @Override
    public ProjectResource toResource(Project oracleProject) {
        List<Link> links = new ArrayList<>();
        try {
            links.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(ProjectController.class).getFilesByProject(authentication, oracleProject.getAccession(), 100, 0)).withRel(WsContastants.HateoasEnum.files.name()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return new ProjectResource(Transformer.transformOracleProject(oracleProject), links);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @Override
    public List<ProjectResource> toResources(Iterable<? extends Project> entities) {

        List<ProjectResource> projects = new ArrayList<>();

        for (Project oracleProject : entities) {
            PrideProject project = Transformer.transformOracleProject(oracleProject);
            List<Link> links = new ArrayList<>();
            links.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(ProjectController.class).getPrivateProject(authentication, oracleProject.getAccession())).withSelfRel());
            projects.add(new ProjectResource(project, links));
        }

        return projects;
    }


}
