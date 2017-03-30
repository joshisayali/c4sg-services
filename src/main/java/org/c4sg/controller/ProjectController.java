package org.c4sg.controller;

import io.swagger.annotations.*;

import org.apache.tomcat.util.codec.binary.Base64;
import org.c4sg.dto.ProjectDTO;
import org.c4sg.dto.UserDTO;
import org.c4sg.entity.Project;
import org.c4sg.exception.BadRequestException;
import org.c4sg.exception.NotFoundException;
import org.c4sg.exception.UserProjectException;
import org.c4sg.service.ProjectService;
import org.c4sg.service.UserService;
import org.c4sg.util.FileUploadUtil;
import org.hibernate.jpa.criteria.predicate.ExistsPredicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;

import static org.c4sg.constant.Directory.PROJECT_UPLOAD;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/projects")
@Api(description = "Operations about Projects", tags = "project")
public class ProjectController extends GenericController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserService userService;

    @CrossOrigin
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Find all projects", notes = "Returns a collection of projects")
    public List<ProjectDTO> getProjects() {
        return projectService.findProjects();
    }

    @CrossOrigin
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Find project by ID", notes = "Returns a single project")
    public Project getProject(@ApiParam(value = "ID of project to return", required = true)
                              @PathVariable("id") int id) {
        System.out.println("**************ID**************" + id);
        return projectService.findById(id);
    }

    @CrossOrigin
    @RequestMapping(value = "/organizations/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Find projects by Organization ID", notes = "Returns a list of projects")
    public List<Project> getProjectsByOrganization(@ApiParam(value = "ID of an organization", required = true)
                                                   @PathVariable("id") int orgId) {
        System.out.println("**************OrganizationID**************" + orgId);
        return projectService.getProjectsByOrganization(orgId);
    }

    @CrossOrigin
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    @ApiOperation(value = "Find project by name or keyWord", notes = "Returns a collection of projects")
    public List<ProjectDTO> getProjects(@ApiParam(value = "Name of project to return", required = true)
                                        @RequestParam String name,
                                        @ApiParam(value = "Description of project to return")
                                        @RequestParam(required = false) String keyword) {
        return projectService.findByKeyword(name, keyword);
    }

    @CrossOrigin
    @RequestMapping(value = "/users/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Find projects by user", notes = "Returns a collection of projects")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "ID of user invalid")
    })
    public List<ProjectDTO> getProjectsByUser(@ApiParam(value = "userId of projects to return", required = true)
                                              @PathVariable("id") Integer id) {

        System.out.println("**************Search**************" + id);

        List<ProjectDTO> projects;

        try {
            projects = projectService.findByUser(id);
        } catch (Exception e) {
            throw new NotFoundException("ID of user invalid");
        }

        return projects;
    }

    @CrossOrigin
    @RequestMapping(method = RequestMethod.POST)
    @ApiOperation(value = "Add a new project")
    public Map<String, Object> createProject(@ApiParam(value = "Project object to return", required = true)
                                             @RequestBody @Valid Project project) {

        System.out.println("**************Add**************");

        Map<String, Object> responseData = null;

        try {
            Project createProject = projectService.createProject(project);
            responseData = Collections.synchronizedMap(new HashMap<>());
            responseData.put("project", createProject);
        } catch (Exception e) {
            System.out.println(e);
        }

        return responseData;
    }

    @CrossOrigin
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ApiOperation(value = "Deletes a project")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@ApiParam(value = "Project id to delete", required = true)
                              @PathVariable("id") int id) {

        System.out.println("************** Delete : id=" + id + "**************");

        try {
            projectService.deleteProject(id);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @CrossOrigin
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @ApiOperation(value = "Update an existing project")
    public Map<String, Object> updateProject(@ApiParam(value = "Updated project object", required = true)
                                             @RequestBody @Valid Project project) {

        System.out.println("**************Update : id=" + project.getId() + "**************");

        Map<String, Object> responseData = null;

        try {
            Project updateProject = projectService.updateProject(project);
            responseData = Collections.synchronizedMap(new HashMap<>());
            responseData.put("project", updateProject);
        } catch (Exception e) {
            System.out.println(e);
        }

        return responseData;
    }

    @CrossOrigin
    @RequestMapping(value = "/{id}/users/{userId}", method = RequestMethod.POST)
    @ApiOperation(value = "Create a relation between user and project")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "ID of project or user invalid")
    })
    //TODO: Replace explicit user{id} with AuthN user id.
    public ResponseEntity<?> createUserProject(@ApiParam(value = "ID of user", required = true)
                                               @PathVariable("userId") Integer userId,
                                               @ApiParam(value = "ID of project", required = true)
                                               @PathVariable("id") Integer projectId) {
        try {
            projectService.saveUserProject(userId, projectId);
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                                                      .path("/{id}/users/{userId}")
                                                      .buildAndExpand(projectId, userId).toUri();
            return ResponseEntity.created(location).build();
        } catch (NullPointerException | UserProjectException e) {
            throw new NotFoundException("ID of project or user invalid");
        }
    }

    @CrossOrigin
    @RequestMapping(value = "/applicant/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Find applicants of a given project", notes = "Returns a collection of projects")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Applicants not found")
    })
    public ResponseEntity<List<UserDTO>> getApplicants(@ApiParam(value = "ID of project", required = true)
                                                       @PathVariable("id") Integer projectId) {
        List<UserDTO> applicants = userService.getApplicants(projectId);

        if (!applicants.isEmpty()) {
            return ResponseEntity.ok().body(applicants);
        } else {
            throw new NotFoundException("Applicants not found");
        }
    }

    @CrossOrigin
    @RequestMapping(value = "/applied/users/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Find projects, with status applied, related to a given user",
            notes = "Returns a collection of projects with status applied")
    public List<ProjectDTO> getProjects(@ApiParam(value = "ID of user", required = true)
                                        @PathVariable("id") Integer userId) {
        return projectService.getAppliedProjects(userId);
    }

    @RequestMapping(value = "/{id}/image", method = RequestMethod.POST)
   	@ApiOperation(value = "Add new image file for project")
   	public String uploadImage(@ApiParam(value = "user Id", required = true) @PathVariable("id") Integer id,
   			@ApiParam(value = "Image File", required = true) @RequestPart("file") MultipartFile file) {

   		String contentType = file.getContentType();
   		if (!FileUploadUtil.isValidImageFile(contentType)) {
   			return "Invalid image File! Content Type :-" + contentType;
   		}
   		File directory = new File(PROJECT_UPLOAD.getValue());
   		if (!directory.exists()) {
   			directory.mkdir();
   		}
   		File f = new File(projectService.getImageUploadPath(id));
   		try (FileOutputStream fos = new FileOutputStream(f)) {
   			byte[] imageByte = file.getBytes();
   			fos.write(imageByte);
   			return "Success";
   		} catch (Exception e) {
   			return "Error saving image for Project " + id + " : " + e;
   		}
   	}

    @CrossOrigin
    @RequestMapping(value = "/{id}/image", method = RequestMethod.GET)
    @ApiOperation(value = "Retrieves project image")
    public String retrieveImage(@ApiParam(value = "Project id to get image for", required = true)
                                         @PathVariable("id") int id) {
        File image = new File(projectService.getImageUploadPath(id));
        try {
			FileInputStream fileInputStreamReader = new FileInputStream(image);
            byte[] bytes = new byte[(int) image.length()];
            fileInputStreamReader.read(bytes);
            fileInputStreamReader.close();
            return new String(Base64.encodeBase64(bytes));
        } catch (IOException e) {
            e.printStackTrace();
        return null;
        }
    }

    @CrossOrigin
    @RequestMapping(value = "/bookmark/projects/{projectId}/users/{userId}", method = RequestMethod.POST)
    @ApiOperation(value = "Create a bookmark for a project")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Invalid project or user")
    })
    //TODO: Replace explicit user{id} with AuthN user id.
    public ResponseEntity<?> createUserProjectBookmark(@ApiParam(value = "Project ID", required = true)
                                               @PathVariable("projectId") Integer projectId,
                                               @ApiParam(value = "User ID", required = true)
                                               @PathVariable("userId") Integer userId) {
        try {
            projectService.saveUserProjectBookmark(userId, projectId);
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                                                      .path("/bookmark/projects/{projectId}/users/{userId}")
                                                      .buildAndExpand(projectId, userId).toUri();
            return ResponseEntity.created(location).build();
        } catch (NullPointerException e) {
            throw new NotFoundException(e.getMessage());
        }
        catch(UserProjectException e){
        	throw new BadRequestException(e.getErrorMessage());
        }
    }
    
}

