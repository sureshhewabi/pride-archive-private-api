package uk.ac.ebi.pride.ws.pride.service.project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.pride.archive.repo.repos.file.ProjectFile;
import uk.ac.ebi.pride.archive.repo.repos.file.ProjectFileRepository;
import uk.ac.ebi.pride.archive.repo.repos.project.Project;
import uk.ac.ebi.pride.archive.repo.repos.project.ProjectRepository;
import uk.ac.ebi.pride.archive.repo.repos.user.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectFileRepository fileRepository;

    public List<Project> findUserProjects(String userReference, boolean isPublic) {
        Long userId = userRepository.findByUserRef(userReference).getId();
        List<Project> projectsList = projectRepository.findFilteredBySubmitterIdAndIsPublic(userId,isPublic);
        return projectsList;
    }

    public List<Project> findReviewerProjects(String userReference) {
        List<Project> projectsList = projectRepository.findFilteredByReviewer(userReference);
        return projectsList;
    }

    public Optional<ProjectFile> getFilePath(Long fileId){
        return fileRepository.findById(fileId);
    }

    public List<ProjectFile> findProjectFiles(String projectAccession) {
        Project project = projectRepository.findByAccession(projectAccession);
        return fileRepository.findAllByProjectId(project.getId());
    }
}
