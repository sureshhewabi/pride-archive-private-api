package uk.ac.ebi.pride.ws.pride.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.DigestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.archive.repo.client.ProjectRepoClient;
import uk.ac.ebi.pride.archive.repo.models.file.ProjectFile;
import uk.ac.ebi.pride.archive.repo.models.project.Project;
import uk.ac.ebi.pride.utilities.util.Tuple;
import uk.ac.ebi.pride.ws.pride.assemblers.PrideProjectResourceAssembler;
import uk.ac.ebi.pride.ws.pride.assemblers.ProjectFileResourceAssembler;
import uk.ac.ebi.pride.ws.pride.configs.SubmissionApiConfig;
import uk.ac.ebi.pride.ws.pride.models.dataset.ProjectResource;
import uk.ac.ebi.pride.ws.pride.models.file.PrideFileResource;
import uk.ac.ebi.pride.ws.pride.models.project.PublishProject;
import uk.ac.ebi.pride.ws.pride.models.project.PublishProjectRequest;
import uk.ac.ebi.pride.ws.pride.service.project.FileStorageService;
import uk.ac.ebi.pride.ws.pride.service.project.ProjectService;
import uk.ac.ebi.pride.ws.pride.utils.APIError;
import uk.ac.ebi.pride.ws.pride.utils.PrideSupportEmailSender;
import uk.ac.ebi.pride.ws.pride.utils.WsContastants;
import uk.ac.ebi.pride.ws.pride.utils.WsUtils;
import uk.ac.ebi.tsc.aap.client.model.User;

import java.util.*;
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
    private final PrideSupportEmailSender prideSupportEmailSender;
    private final RestTemplate proxyRestTemplate;
    private final SubmissionApiConfig submissionApiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProjectRepoClient projectRepoClient;

    public ProjectController(ProjectService projectService,
                             FileStorageService fileStorageService,
                             PrideSupportEmailSender prideSupportEmailSender,
                             @Qualifier("proxyRestTemplate") RestTemplate proxyRestTemplate,
                             SubmissionApiConfig submissionApiConfig, ProjectRepoClient projectRepoClient) {
        this.projectService = projectService;
        this.fileStorageService = fileStorageService;
        this.prideSupportEmailSender = prideSupportEmailSender;
        this.proxyRestTemplate = proxyRestTemplate;
        this.submissionApiConfig = submissionApiConfig;
        this.projectRepoClient = projectRepoClient;
    }

    @ApiOperation(notes = "List of Private PRIDE Archive Projects submitted by the user. User needs to be authenticated to view his private submissions",
            value = "my submissions", nickname = "getMySubmissions", tags = {"projects"})
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = APIError.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = APIError.class)})
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/projects/private-submissions", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<PagedResources> getPrivateProjects(Authentication authentication,
                                                         @RequestParam(value = "pageSize", defaultValue = "100", required = false) int pageSize,
                                                         @RequestParam(value = "page", defaultValue = "0", required = false) int page) {

        User currentUser = (User) (authentication).getDetails();

        try {
            List<Project> projectsList = projectService.findUserProjects(currentUser.getUserReference(), false);
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
                    linkTo(methodOn(ProjectController.class).getReviewerProjects(authentication, pageSize, (int) WsUtils.validatePage(page - 1, totalPages)))
                            .withRel(WsContastants.HateoasEnum.previous.name()),
                    linkTo(methodOn(ProjectController.class).getReviewerProjects(authentication, pageSize, 0))
                            .withRel(WsContastants.HateoasEnum.first.name()),
                    linkTo(methodOn(ProjectController.class).getReviewerProjects(authentication, pageSize, (int) totalPages))
                            .withRel(WsContastants.HateoasEnum.last.name())
            );

            return new HttpEntity<>(pagedResources);
        } catch (Exception exception) {
            log.error("Error in getting private projects" + exception.getMessage());
            return ResponseEntity.noContent().build();
        }

    }

    @ApiOperation(notes = "Private PRIDE Archive Project submitted by the user which is under review of the reviewer. User needs to be authenticated to view his private submissions",
            value = "reviewer view private submission", nickname = "getPrivateProjectForReviewer", tags = {"projects"})
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = APIError.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = APIError.class)})
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/projects/{accession}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> getPrivateProject(Authentication authentication,
                                                    @PathVariable(value = "accession") String projectAccession) throws Exception {

        User currentUser = (User) (authentication).getDetails();
        List<Project> projectsReviewerList = projectService.findReviewerProjects(currentUser.getUserReference());
        List<Project> projectList = projectService.findUserProjects(currentUser.getUserReference(), false);
        if (projectList == null)
            projectList = new ArrayList<>();
        projectList.addAll(projectsReviewerList);
        Optional<Project> privateProject = projectList.stream().filter(project -> project.getAccession().equals(projectAccession)).findFirst();

        PrideProjectResourceAssembler assembler = new PrideProjectResourceAssembler(authentication, ProjectController.class, ProjectResource.class);
        return privateProject.<ResponseEntity<Object>>map(oracleProject -> new ResponseEntity<>(assembler.toResource(oracleProject), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(WsContastants.PX_PROJECT_NOT_FOUND + projectAccession + WsContastants.CONTACT_PRIDE, new HttpHeaders(), HttpStatus.BAD_REQUEST));

    }

    @ApiOperation(notes = "To publish private PRIDE Archive Project submitted by the user. User needs to be authenticated to publish his private submissions",
            value = "publish private project", nickname = "publishPrivateProject", tags = {"projects"})
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = APIError.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = APIError.class)})
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/projects/publish/{accession}", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> publishPrivateProject(Authentication authentication,
                                                        @PathVariable(value = "accession") String projectAccession,
                                                        @RequestBody PublishProjectRequest publishProjectRequest) throws Exception {

        User currentUser = (User) (authentication).getDetails();
        List<Project> projectList = projectService.findUserProjects(currentUser.getUserReference(), false);
        if (projectList == null)
            projectList = new ArrayList<>();
        Optional<Project> privateProject = projectList.stream().filter(project -> project.getAccession().equals(projectAccession)).findFirst();

        if (!privateProject.isPresent()) {
            return new ResponseEntity<>(WsContastants.PX_PROJECT_NOT_FOUND + projectAccession + WsContastants.CONTACT_PRIDE, new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

        PublishProject publishProject = PublishProject.builder().pubmedId(publishProjectRequest.getPubmedId())
                .doi(publishProjectRequest.getDoi())
                .referenceLine(publishProjectRequest.getReferenceLine())
                .publishJustification(publishProjectRequest.getPublishJustification())
                .authorized(true).userName(currentUser.getEmail()).build();

        try {
            sendPublicationRequestToSubmissionApi(projectAccession, publishProjectRequest);
        } catch (Exception ex) {
            log.error("Failed to login or send publication request to submission-api for accession : " + projectAccession);
            log.error(ex.getMessage(), ex);
        }
        try {
            prideSupportEmailSender.sendPublishProjectEmail(publishProject, projectAccession, prideSupportEmailSender.getpublishProjectEmailTemplate());
        } catch (Exception ex) {
            String message = "Failed to send publish project email on: " + projectAccession;
            log.error(message, ex);
            return new ResponseEntity<>(WsContastants.PUBLISH_PROJECT_NOK, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(WsContastants.PUBLISH_PROJECT_OK, HttpStatus.OK);
    }

    private void sendPublicationRequestToSubmissionApi(String projectAccession, PublishProjectRequest publishProjectRequest) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_JSON);
        String payload = objectMapper.writeValueAsString(submissionApiConfig.getCredentials());
        HttpEntity<MultiValueMap<String, String>> loginRequestEntity = new HttpEntity(payload, headers);
        ResponseEntity<String> loginResponse = proxyRestTemplate.postForEntity(submissionApiConfig.getLoginUrl(), loginRequestEntity, String.class);
        if (loginResponse.getStatusCode() == HttpStatus.OK) {
            String token = loginResponse.getBody();
            headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.ALL));
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<MultiValueMap<String, String>> publicationRequestEntity = new HttpEntity(headers);
            String publicationUrl = submissionApiConfig.getPublicationUrl() + "?force=true&verifyCommand=false&accession=" + projectAccession;
            String doi = publishProjectRequest.getDoi();
            String pubmedId = publishProjectRequest.getPubmedId();
            if (doi != null && !doi.trim().isEmpty()) {
                String doiUppercase = doi.trim().toUpperCase();
                if (doiUppercase.contains("HTTP") || doiUppercase.contains("DOI.ORG")) { // strip protocol and domain
                    doi = doiUppercase.replaceFirst("((HTTP|HTTPS)://)?(DX\\.)?DOI\\.ORG/", "");
                } else if(doiUppercase.contains("DOI:")) { // strip protocol
                    doi = doiUppercase.replaceFirst("DOI:", "");
                } else {
                    doi = doiUppercase;
                }
                String DOIREGEX = "^10\\.\\d{4,9}\\/[-._;()/:a-zA-Z0-9]+$";
                if(doi.matches(DOIREGEX)) {
                    publicationUrl += "&doi=" + doi;
                }
            }
            if (pubmedId != null && !pubmedId.trim().isEmpty()) {
                pubmedId = pubmedId.trim();
                String PUBMIDID_REGEX = "^\\d{7,8}$";
                if(pubmedId.matches(PUBMIDID_REGEX)) {
                    publicationUrl += "&pubmedId=" + pubmedId;
                }
            }
            ResponseEntity<String> response = proxyRestTemplate.postForEntity(publicationUrl, publicationRequestEntity, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Successfully sent publication request to submission-api for accession : " +
                        projectAccession + " - " + response.getBody().replaceAll("\n", " ### "));
            } else {
                log.error("Failed to send publication request to submission-api for accession : " + projectAccession + " : " + publicationUrl);
            }
        } else {
            log.error("Failed login to submission-api : " + loginResponse.getStatusCode() + " " + loginResponse.getBody());
        }
    }

//    @RequestMapping(value = "/createsubmissiontickets", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
//    public ResponseEntity<Object> createsubmissiontickets() throws Exception {
//        List<String> allAccessions = projectRepoClient.getAllAccessions();
//        List<String> allPublicAccessions = projectRepoClient.getAllPublicAccessions();
//        allAccessions.removeAll(allPublicAccessions);
//        System.out.println(allAccessions);
//        System.out.println(allAccessions.size());
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setAccept(Arrays.asList(MediaType.ALL));
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        String payload = objectMapper.writeValueAsString(submissionApiConfig.getCredentials());
//        HttpEntity<MultiValueMap<String, String>> loginRequestEntity = new HttpEntity(payload, headers);
//        try {
//            ResponseEntity<String> loginResponse = proxyRestTemplate.postForEntity(submissionApiConfig.getLoginUrl(), loginRequestEntity, String.class);
//            if (loginResponse.getStatusCode() == HttpStatus.OK) {
//                String token = loginResponse.getBody();
//                headers = new HttpHeaders();
//                headers.setAccept(Arrays.asList(MediaType.ALL));
//                headers.set("Authorization", "Bearer " + token);
//                HttpEntity<MultiValueMap<String, String>> publicationRequestEntity = new HttpEntity(headers);
//
//                for (String accession : allAccessions) {
//                    System.out.println(accession);
//                    String ticketUrl = "http://noah-login-02.ebi.ac.uk:8061/pride/submission-api/ticket/create-new-ticket?" +
//                            "ticketId=" + accession + "&submittedPath=/nfs/pride/prod/archive/" + accession +
//                            "&state=SUBMITTED&accession=" + accession;
//
//                    ResponseEntity<String> response = proxyRestTemplate.postForEntity(ticketUrl, publicationRequestEntity, String.class);
//                    System.out.println(response.getStatusCode() + " : " + response.getBody());
//                }
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//
//        return new ResponseEntity<>(WsContastants.PUBLISH_PROJECT_OK, HttpStatus.OK);
//    }

   /* @ApiOperation(notes = "To publish private PRIDE Archive Project submitted by another user",
            value = "publish others private project", nickname = "publishOtherPrivateProject", tags = {"projects"})
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = APIError.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = APIError.class)})
    @RequestMapping(value = "/projects/publishother/{accession}", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> publishOthersPrivateProject(
            @PathVariable(value = "accession") String projectAccession,
            @RequestBody PublishProjectRequest publishProjectRequest) {

        PublishProject publishProject = PublishProject.builder().pubmedId(publishProjectRequest.getPubmedId())
                .doi(publishProjectRequest.getDoi())
                .referenceLine(publishProjectRequest.getReferenceLine())
                .publishJustification(publishProjectRequest.getPublishJustification())
                .authorized(false).userName("anonymousUser").build();
        try {
            prideSupportEmailSender.sendPublishProjectEmail(publishProject, projectAccession, prideSupportEmailSender.getpublishProjectEmailTemplate());
        } catch (Exception ex) {
            String message = "Failed to send publish project email on: " + projectAccession;
            log.error(message, ex);
            return new ResponseEntity<>(WsContastants.PUBLISH_PROJECT_NOK, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(WsContastants.PUBLISH_PROJECT_OK, HttpStatus.OK);
    }*/

    @ApiOperation(notes = "List of PRIDE Archive Projects accessible to reviewer. User needs to be authenticated to view these submissions",
            value = "reviewer projects", nickname = "getReviewerSubmissions", tags = {"projects"})
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = APIError.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = APIError.class)})
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "projects/reviewer-submissions", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<PagedResources> getReviewerProjects(Authentication authentication,
                                                          @RequestParam(value = "pageSize", defaultValue = "100", required = false) int pageSize,
                                                          @RequestParam(value = "page", defaultValue = "0", required = false) int page) throws Exception {
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
                linkTo(methodOn(ProjectController.class).getReviewerProjects(authentication, pageSize, (int) WsUtils.validatePage(page - 1, totalPages)))
                        .withRel(WsContastants.HateoasEnum.previous.name()),
                linkTo(methodOn(ProjectController.class).getReviewerProjects(authentication, pageSize, 0))
                        .withRel(WsContastants.HateoasEnum.first.name()),
                linkTo(methodOn(ProjectController.class).getReviewerProjects(authentication, pageSize, (int) totalPages))
                        .withRel(WsContastants.HateoasEnum.last.name())
        );

        return new HttpEntity<>(pagedResources);

//
//
//        return ResponseEntity.ok().body(privateProjectsList);
    }

    @ApiOperation(notes = "Get all the Files for an specific private project in PRIDE.", value = "projects", nickname = "getFilesByProject", tags = {"projects"})
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = APIError.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = APIError.class)
    })
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/projects/{accession}/files", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<PagedResources<PrideFileResource>> getFilesByProject(Authentication authentication,
                                                                           @PathVariable(value = "accession") String projectAccession,
                                                                           @RequestParam(value = "pageSize", defaultValue = "100", required = false) int pageSize,
                                                                           @RequestParam(value = "page", defaultValue = "0", required = false) int page) throws Exception {

        Tuple<Integer, Integer> pageParams = WsUtils.validatePageLimit(page, pageSize);
        page = pageParams.getKey();
        pageSize = pageParams.getValue();

        User currentUser = (User) (authentication).getDetails();
        List<Project> projectsReviewerList = projectService.findReviewerProjects(currentUser.getUserReference());
        List<Project> projectList = projectService.findUserProjects(currentUser.getUserReference(), false);
        if (projectList == null)
            projectList = new ArrayList<>();

        projectList.addAll(projectsReviewerList);
        Optional<Project> privateProject = projectList.stream().filter(project -> project.getAccession().equals(projectAccession)).findFirst();

        List<ProjectFile> projectFiles = privateProject.isPresent() ? projectService.findProjectFiles(projectAccession) : new ArrayList<>();

        ProjectFileResourceAssembler assembler = new ProjectFileResourceAssembler(projectAccession, ProjectController.class, PrideFileResource.class);

        List<PrideFileResource> resources = assembler.toResources(projectFiles);

        long totalElements = projectFiles.size();
        long totalPages = totalElements / pageSize;
        if (totalElements % pageSize > 0)
            totalPages++;
        PagedResources.PageMetadata pageMetadata = new PagedResources.PageMetadata(pageSize, page, totalElements, totalPages);

        PagedResources<PrideFileResource> pagedResources = new PagedResources<>(resources, pageMetadata,
                linkTo(methodOn(ProjectController.class).getFilesByProject(authentication, projectAccession, pageSize, page)).withSelfRel(),
                linkTo(methodOn(ProjectController.class).getFilesByProject(authentication, projectAccession, pageSize, (int) WsUtils.validatePage(page + 1, totalPages)))
                        .withRel(WsContastants.HateoasEnum.next.name()),
                linkTo(methodOn(ProjectController.class).getFilesByProject(authentication, projectAccession, pageSize, (int) WsUtils.validatePage(page - 1, totalPages)))
                        .withRel(WsContastants.HateoasEnum.previous.name()),
                linkTo(methodOn(ProjectController.class).getFilesByProject(authentication, projectAccession, pageSize, 0))
                        .withRel(WsContastants.HateoasEnum.first.name()),
                linkTo(methodOn(ProjectController.class).getFilesByProject(authentication, projectAccession, pageSize, (int) totalPages))
                        .withRel(WsContastants.HateoasEnum.last.name())
        );

        return new HttpEntity<>(pagedResources);
    }

    /**
     * Todo: Token handling is not yet implemented.
     *
     * @param accession
     * @param fileId
     * @param token
     * @return
     */

    @ApiOperation(notes = "Get file private file", value = "projects", nickname = "getFileByProject", tags = {"projects"})
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = APIError.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = APIError.class)
    })
    @RequestMapping(value = "/projects/private/{projectId}/files/{fileId}", method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    public HttpEntity<Resource> getFileByProject(@PathVariable(value = "projectId") String accession,
                                                 @PathVariable(value = "fileId") Long fileId,
                                                 @RequestParam(value = "token", required = true) String token) throws Exception {

        Optional<ProjectFile> projectFile = projectService.getFilePath(fileId);

        String fileName = null;
        if (projectFile.isPresent()) {
            fileName = accession + "/" + "submitted" + "/" + projectFile.get().getFileName();
        }

        String sha3_256hex = DigestUtils.md5DigestAsHex((fileName + fileId).getBytes());
        if (!sha3_256hex.equalsIgnoreCase(token)) {
            return ResponseEntity.
                    status(HttpStatus.BAD_REQUEST).body(null);
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
