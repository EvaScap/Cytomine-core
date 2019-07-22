package be.cytomine.api.processing

/*
* Copyright (c) 2009-2017. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.api.RestController
import be.cytomine.processing.Job
import be.cytomine.processing.JobRuntimeService
import be.cytomine.processing.ProcessingServerService
import be.cytomine.processing.SoftwareProject
import be.cytomine.processing.SoftwareProjectService
import be.cytomine.project.Project
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

/**
 * Controller for software project link
 * A software may be used by some project
 */
@RestApi(name = "Processing | software project services", description = "Methods for managing software, application that can be launch (job)")
class RestSoftwareProjectController extends RestController{

    def softwareProjectService
    def projectService
    def jobRuntimeService

    /**
     * List all software project links
     */
    @RestApiMethod(description="List all software project links", listing = true)
    def list() {
        responseSuccess(softwareProjectService.list())
    }

    /**
     * List all software by project
     */
    @RestApiMethod(description="List all software project links by project", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The project id")
    ])
    def listByProject() {
        Project project = projectService.read(params.long('id'))
        if (project) {
            responseSuccess(softwareProjectService.list(project))
        } else {
            responseNotFound("Project", params.id)
        }
    }

    /**
     * Get a software project link
     */
    @RestApiMethod(description="Get a software project link")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The software project id")
    ])
    def show() {
        SoftwareProject parameter = softwareProjectService.read(params.long('id'))
        if (parameter) responseSuccess(parameter)
        else responseNotFound("SoftwareProject", params.id)
    }

    /**
     * Add an existing software to a project
     */
    @RestApiMethod(description="Add an existing software to a project")
    def add () {
        add(softwareProjectService, request.JSON)
    }

    @RestApiMethod(description="")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The software project id")
    ])
    def executeAllWorkflows()
    {
        try
        {
            Project project = projectService.read(params.long('id'))
            if (project) {
                def response = softwareProjectService.executeAllWorkflows(project)
                return responseSuccess(response)
            }
            else
            {
                responseNotFound("Project", params.id)
            }
        }
        catch (Exception ex)
        {
            log.info("Error: ${ex.printStackTrace()}")
        }
    }

    /**
     * Delete the software for the project
     */
    @RestApiMethod(description="Remove the software from the project")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The software project id")
    ])
    def delete() {
        delete(softwareProjectService, JSON.parse("{id : $params.id}"),null)
    }
}
