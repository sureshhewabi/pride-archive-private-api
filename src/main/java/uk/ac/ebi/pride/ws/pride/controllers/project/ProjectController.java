package uk.ac.ebi.pride.ws.pride.controllers.project;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.pride.archive.repo.repos.file.ProjectFile;
import uk.ac.ebi.pride.archive.repo.repos.project.Project;
import uk.ac.ebi.pride.utilities.util.Tuple;
import uk.ac.ebi.pride.ws.pride.assemblers.PrideProjectResourceAssembler;
import uk.ac.ebi.pride.ws.pride.assemblers.ProjectFileResourceAssembler;
import uk.ac.ebi.pride.ws.pride.models.dataset.ProjectResource;
import uk.ac.ebi.pride.ws.pride.models.file.PrideFileResource;
import uk.ac.ebi.pride.ws.pride.service.project.FileStorageService;
import uk.ac.ebi.pride.ws.pride.service.project.ProjectService;
import uk.ac.ebi.pride.ws.pride.service.user.AAPService;
import uk.ac.ebi.pride.ws.pride.utils.APIError;
import uk.ac.ebi.pride.ws.pride.utils.WsContastants;
import uk.ac.ebi.pride.ws.pride.utils.WsUtils;
import uk.ac.ebi.tsc.aap.client.model.Profile;
import uk.ac.ebi.tsc.aap.client.model.User;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;


/**
 * The Dataset/Project Controller enables to retrieve the information for each PRIDE Project/CompactProjectResource through a RestFull API.
 *
 * @author ypriverol
 */

@RestController
@Slf4j
public class ProjectController {

    final private ProjectService projectService;
    final private FileStorageService fileStorageService;
    final private AAPService aapService;

    @Autowired
    public ProjectController(ProjectService projectService, FileStorageService fileStorageService, AAPService aapService) {
        this.projectService = projectService;
        this.fileStorageService = fileStorageService;
        this.aapService = aapService;
    }

    @ApiOperation(notes = "List of Private PRIDE Archive Projects submitted by the user. User needs to be authenticated to view his private submissions",
            value = "my submissions", nickname = "getMySubmissions", tags = {"projects"} )
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = APIError.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = APIError.class)})
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/projects/private-submissions", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<PagedResources> getPrivateProjects(Authentication authentication,
                                                            @RequestParam(value="pageSize", defaultValue = "100", required = false) int pageSize,
                                                            @RequestParam(value="page", defaultValue = "0" ,  required = false) int page) {

        User currentUser = (User) (authentication).getDetails();

        List<Project> projectsList = projectService.findUserProjects(currentUser.getUserReference(),false);
        PrideProjectResourceAssembler assembler = new PrideProjectResourceAssembler(authentication, ProjectController.class, ProjectResource.class);
        projectsList = projectsList.stream().filter(project -> !project.isPublicProject()).collect(Collectors.toList());
        List<ProjectResource> resources = assembler.toResources(projectsList);

        long totalElements = projectsList.size();
        long totalPages = totalElements / pageSize + 1;
        PagedResources.PageMetadata pageMetadata = new PagedResources.PageMetadata(pageSize, page, totalElements, totalPages);

        PagedResources<ProjectResource> pagedResources = new PagedResources<>(resources, pageMetadata,
                linkTo(methodOn(ProjectController.class).getReviewerProjects(authentication, pageSize, page)).withSelfRel(),
                linkTo(methodOn(ProjectController.class).getReviewerProjects(authentication, pageSize, (int) WsUtils.validatePage(page + 1, totalPages)))
                        .withRel(WsContastants.HateoasEnum.next.name()),
                linkTo(methodOn(ProjectController.class).getReviewerProjects( authentication, pageSize, (int) WsUtils.validatePage(page - 1, totalPages)))
                        .withRel(WsContastants.HateoasEnum.previous.name()),
                linkTo(methodOn(ProjectController.class).getReviewerProjects(authentication, pageSize, 0))
                        .withRel(WsContastants.HateoasEnum.first.name()),
                linkTo(methodOn(ProjectController.class).getReviewerProjects(authentication, pageSize, (int) totalPages))
                        .withRel(WsContastants.HateoasEnum.last.name())
        ) ;

        return new HttpEntity<>(pagedResources);
    }

    @ApiOperation(notes = "Private PRIDE Archive Project submitted by the user which is under review of the reviewer. User needs to be authenticated to view his private submissions",
            value = "reviewer view private submission", nickname = "getPrivateProjectForReviewer", tags = {"projects"} )
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = APIError.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = APIError.class)})
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/projects/{accession}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> getPrivateProject( Authentication authentication,
                                                      @PathVariable(value ="accession") String projectAccession) {

        User currentUser = (User) (authentication).getDetails();
        List<Project> projectsList = projectService.findReviewerProjects(currentUser.getUserReference());
        Optional<Project> privateProject = projectsList.stream().filter(project -> project.getAccession().equals(projectAccession)).findFirst();

        PrideProjectResourceAssembler assembler = new PrideProjectResourceAssembler(authentication, ProjectController.class, ProjectResource.class);
        return privateProject.<ResponseEntity<Object>>map(oracleProject -> new ResponseEntity<>(assembler.toResource(oracleProject), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(WsContastants.PX_PROJECT_NOT_FOUND + projectAccession + WsContastants.CONTACT_PRIDE, new HttpHeaders(), HttpStatus.BAD_REQUEST));

    }

    @ApiOperation(notes = "List of PRIDE Archive Projects accessible to reviewer. User needs to be authenticated to view these submissions",
            value = "reviewer projects", nickname = "getReviewerSubmissions", tags = {"projects"} )
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = APIError.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = APIError.class)})
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "projects/reviewer-submissions", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<PagedResources> getReviewerProjects(Authentication authentication,
                                                                  @RequestParam(value="pageSize", defaultValue = "100", required = false) int pageSize,
                                                                  @RequestParam(value="page", defaultValue = "0" ,  required = false) int page) {
        User currentUser = (User) (authentication).getDetails();
        List<Project> projectsList = projectService.findReviewerProjects(currentUser.getUserReference());

        PrideProjectResourceAssembler assembler = new PrideProjectResourceAssembler(authentication, ProjectController.class, ProjectResource.class);


        projectsList = projectsList.stream().filter(project -> !project.isPublicProject()).collect(Collectors.toList());
        List<ProjectResource> resources = assembler.toResources(projectsList);

        long totalElements = projectsList.size();
        long totalPages = totalElements / pageSize + 1;
        PagedResources.PageMetadata pageMetadata = new PagedResources.PageMetadata(pageSize, page, totalElements, totalPages);

        PagedResources<ProjectResource> pagedResources = new PagedResources<>(resources, pageMetadata,
                linkTo(methodOn(ProjectController.class).getReviewerProjects(authentication, pageSize, page)).withSelfRel(),
                linkTo(methodOn(ProjectController.class).getReviewerProjects(authentication, pageSize, (int) WsUtils.validatePage(page + 1, totalPages)))
                        .withRel(WsContastants.HateoasEnum.next.name()),
                linkTo(methodOn(ProjectController.class).getReviewerProjects( authentication, pageSize, (int) WsUtils.validatePage(page - 1, totalPages)))
                        .withRel(WsContastants.HateoasEnum.previous.name()),
                linkTo(methodOn(ProjectController.class).getReviewerProjects(authentication, pageSize, 0))
                        .withRel(WsContastants.HateoasEnum.first.name()),
                linkTo(methodOn(ProjectController.class).getReviewerProjects(authentication, pageSize, (int) totalPages))
                        .withRel(WsContastants.HateoasEnum.last.name())
        ) ;

        return new HttpEntity<>(pagedResources);

//
//
//        return ResponseEntity.ok().body(privateProjectsList);
    }

    @ApiOperation(notes = "Get all the Files for an specific private project in PRIDE.", value = "projects", nickname = "getFilesByProject", tags = {"projects"} )
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = APIError.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = APIError.class)
    })
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/projects/{accession}/files", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<PagedResources<PrideFileResource>> getFilesByProject(Authentication authentication,
                                                                           @PathVariable(value ="accession") String projectAccession,
                                                                           @RequestParam(value="pageSize", defaultValue = "100", required = false) int pageSize,
                                                                           @RequestParam(value="page", defaultValue = "0" ,  required = false) int page){

        Tuple<Integer, Integer> pageParams = WsUtils.validatePageLimit(page, pageSize);
        page = pageParams.getKey();
        pageSize = pageParams.getValue();

        List<ProjectFile> projectFiles = projectService.findProjectFiles(projectAccession);

        String token = aapService.getAAPToken();

        ProjectFileResourceAssembler assembler = new ProjectFileResourceAssembler(token, projectAccession, ProjectController.class, PrideFileResource.class);

        List<PrideFileResource> resources = assembler.toResources(projectFiles);

        long totalElements = projectFiles.size();
        long totalPages = totalElements / pageSize;
        PagedResources.PageMetadata pageMetadata = new PagedResources.PageMetadata(pageSize, page, totalElements, totalPages);

        PagedResources<PrideFileResource> pagedResources = new PagedResources<>(resources, pageMetadata,
                linkTo(methodOn(ProjectController.class).getFilesByProject(authentication, projectAccession, pageSize, page)).withSelfRel(),
                linkTo(methodOn(ProjectController.class).getFilesByProject(authentication, projectAccession, pageSize, (int) WsUtils.validatePage(page + 1, totalPages)))
                        .withRel(WsContastants.HateoasEnum.next.name()),
                linkTo(methodOn(ProjectController.class).getFilesByProject(authentication, projectAccession,pageSize, (int) WsUtils.validatePage(page - 1, totalPages)))
                        .withRel(WsContastants.HateoasEnum.previous.name()),
                linkTo(methodOn(ProjectController.class).getFilesByProject(authentication, projectAccession,pageSize, 0))
                        .withRel(WsContastants.HateoasEnum.first.name()),
                linkTo(methodOn(ProjectController.class).getFilesByProject(authentication, projectAccession, pageSize, (int) totalPages))
                        .withRel(WsContastants.HateoasEnum.last.name())
        ) ;

        return new HttpEntity<>(pagedResources);
    }

    /**
     * Todo: Token handling is not yet implemented.
     * @param accession
     * @param fileId
     * @param token
     * @return
     */

    @ApiOperation(notes = "Get file private file", value = "projects", nickname = "getFileByProject", tags = {"projects"} )
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = APIError.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = APIError.class)
    })
    @RequestMapping(value = "/projects/private/{projectId}/files/{fileId}", method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    public HttpEntity<Resource> getFileByProject(@PathVariable(value ="projectId") String accession,
                                                 @PathVariable(value="fileId") Long fileId,
                                                 @RequestParam(value="token", required = true) String token){

//        try{
//            Profile auth = aapService.getMyProfile(token);
//            if(auth == null){
//                throw new AuthenticationCredentialsNotFoundException("The Token is not valid");
//            }
//        }catch (Exception e){
//            throw new AuthenticationCredentialsNotFoundException("The Token is not valid");
//        }

        Optional<ProjectFile> projectFile = projectService.getFilePath(fileId);
        String fileName = null;
        if(projectFile.isPresent()){
            fileName = accession + "/" + "submitted" + "/" + projectFile.get().getFileName();
        }

        // Load file as Resource
        Resource resource = null;
        try {
            resource = fileStorageService.loadFileAsResource(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Try to determine file's content type
        String contentType = "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
