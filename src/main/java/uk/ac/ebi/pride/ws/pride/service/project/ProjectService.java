package uk.ac.ebi.pride.ws.pride.service.project;

import org.springframework.stereotype.Service;
import uk.ac.ebi.pride.archive.repo.client.FileRepoClient;
import uk.ac.ebi.pride.archive.repo.client.ProjectRepoClient;
import uk.ac.ebi.pride.archive.repo.client.UserRepoClient;
import uk.ac.ebi.pride.archive.repo.models.file.ProjectFile;
import uk.ac.ebi.pride.archive.repo.models.project.Project;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class ProjectService {

    private UserRepoClient userRepoClient;
    private ProjectRepoClient projectRepoClient;
    private FileRepoClient fileRepoClient;

    public ProjectService(UserRepoClient userRepoClient, ProjectRepoClient projectRepoClient, FileRepoClient fileRepoClient) {
        this.userRepoClient = userRepoClient;
        this.projectRepoClient = projectRepoClient;
        this.fileRepoClient = fileRepoClient;
    }

    public List<Project> findUserProjects(String userReference, boolean isPublic) throws IOException {
        Long userId = userRepoClient.findByUserRef(userReference).getId();
        List<Project> projectsList = projectRepoClient.findBySubmitterIdAndIsPublic(userId, isPublic);
        return projectsList;
    }

    public List<Project> findReviewerProjects(String userReference) throws IOException {
        List<Project> projectsList = projectRepoClient.findByReviewer(userReference);
        return projectsList;
    }

    public Optional<ProjectFile> getFilePath(Long fileId) throws IOException {
        return fileRepoClient.findById(fileId);
    }

    public List<ProjectFile> findProjectFiles(String projectAccession) throws IOException {
        Project project = projectRepoClient.findByAccession(projectAccession);
        return fileRepoClient.findAllByProjectId(project.getId());
    }
}
