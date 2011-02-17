package be.cytomine.api.project

import grails.converters.*
import be.cytomine.project.Annotation
import com.vividsolutions.jts.io.WKTReader
import com.vividsolutions.jts.geom.Geometry
import be.cytomine.security.User
import be.cytomine.command.Command
import be.cytomine.command.Transaction
import be.cytomine.command.annotation.AddAnnotationCommand
import be.cytomine.command.UndoStack
import be.cytomine.command.annotation.DeleteAnnotationCommand
import be.cytomine.command.annotation.EditAnnotationCommand
import be.cytomine.project.Image

class RestAnnotationController {

  def springSecurityService

  def list = {
    log.info "List with id image:"+params.id
    def data = [:]

    if(params.id == null) {
      data.annotation = Annotation.list()
    }
    else {
      if(Image.findById(params.id)!=null) {
        data.annotation = Annotation.findAllByImage(Image.findById(params.id))
      }
      else {
        log.error "Image Id " + params.id+ " don't exist"
        response.status = 404
        render contentType: "application/xml", {
          errors {
            message("Annotation not found with id image: " + params.id)
          }
        }
      }
    }
    withFormat {
      json { render data as JSON }
      xml { render jsonMap as XML}
    }

  }

  //return 404 when not found
  def show = {
    //testExecuteEditAnnotation()
    log.info "Show with id:" + params.id
    def data = [:]
    data.annotation = Annotation.get(params.id)
    if(data.annotation!=null)  {
      withFormat {
        json { render data as JSON }
        xml { render data as XML}
      }
    }
    else {
      log.error "Annotation Id " + params.id+ " don't exist"
      response.status = 404
      render contentType: "application/xml", {
        errors {
          message("Annotation not found with id: " + params.id)
        }
      }
    }
  }

  def add = {

    log.info "Add"
    User currentUser = User.read(springSecurityService.principal.id)
    log.info "User:" + currentUser.username + " request:" + request.JSON.toString()

    Command addAnnotationCommand = new AddAnnotationCommand(postData : request.JSON.toString())
    def result = addAnnotationCommand.execute()
    if (result.status == 201) {
      addAnnotationCommand.save(flush : true)
      new UndoStack(command : addAnnotationCommand, user: currentUser, transactionInProgress:  currentUser.transactionInProgress).save(flush : true)
    }

    response.status = result.status
    log.debug "result.status="+result.status+" result.data=" + result.data
    withFormat {
      json { render result.data as JSON }
      xml { render result.data as XML }
    }
  }


  def delete = {

    log.info "Delete"
    User currentUser = User.get(springSecurityService.principal.id)
    log.info "User:" + currentUser.username + " params.id=" + params.id

    def postData = ([id : params.id]) as JSON

    Command deleteAnnotationCommand = new DeleteAnnotationCommand(postData : postData.toString())
    def result = deleteAnnotationCommand.execute()
    if (result.status == 204) {
      log.info "Save command on stack"
      deleteAnnotationCommand.save()
      new UndoStack(command : deleteAnnotationCommand, user: currentUser, transactionInProgress:  currentUser.transactionInProgress).save()
    }

    response.status = result.status
    withFormat {
      json { render result.data as JSON }
      xml { render result.data as XML }
    }
  }


  def update = {

    log.info "Update"
    User currentUser = User.read(springSecurityService.principal.id)
    log.info "User:" + currentUser.username + " request:" + request.JSON.toString()

    def result

    if((String)params.id!=(String)request.JSON.annotation.id) {
      log.error "Annotation id from URL and from data are different:"+ params.id + " vs " +  request.JSON.annotation.id
      result = [data : [annotation : null , errors : ["Annotation id from URL and from data are different:"+ params.id + " vs " +  request.JSON.annotation.id ]], status : 400]
    }
    else
    {
      Command editAnnotationCommand = new EditAnnotationCommand(postData : request.JSON.toString())

      result = editAnnotationCommand.execute()
      if (result.status == 200) {
        log.info "Save command on stack"
        editAnnotationCommand.save()
        new UndoStack(command : editAnnotationCommand, user: currentUser, transactionInProgress:  currentUser.transactionInProgress).save()
      }
    }

    response.status = result.status
    log.debug "result.status="+result.status+" result.data=" + result.data
    withFormat {
      json { render result.data as JSON }
      xml { render result.data as XML }
    }
  }
}
