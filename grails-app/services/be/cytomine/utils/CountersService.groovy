package be.cytomine.utils

import be.cytomine.image.ImageInstance
import be.cytomine.ontology.AlgoAnnotation
import be.cytomine.ontology.ReviewedAnnotation
import be.cytomine.ontology.UserAnnotation
import be.cytomine.project.Project
import org.hibernate.jdbc.Work

import java.sql.Connection
import java.sql.SQLException

/**
 * Cytomine @ GIGA-ULG
 * User: stevben
 * Date: 20/07/11
 * Time: 12:17
 * Service to reset all value incr/decr by trigger
 * E.g. A project has count value for "user annotation", "algo annotation",...
 * Instead of doing this:
 * value = UserAnnotation.countByProject(project)
 * We can do something like this:
 * value = project.userAnnotationNumber
 * The value is incr/decr with a trigger on user annotation table
 *
 * We may sometimes need to reset counter with correct value if there are some service/database issue
 */
class CountersService {

    def sessionFactory

    def updateCommentsCounters() {

        sessionFactory.currentSession.doWork(
                new Work() {
                    public void execute(Connection connection) throws SQLException
                    {
                        try {
                            def statement = connection.createStatement()
                            statement.execute("update project as p SET count_images = (select count(id) from image_instance where project_id = p.id);")
                        } catch (org.postgresql.util.PSQLException e) {
                            log.info e
                        }
                    }
                }
        )

    }

    /**
     * Update all counter with their correct value
     */
    def updateCounters() {

        Project.list().each { project ->
            //reset project counter
            log.info "update counter for project " + project.name
            def userAnnotations = UserAnnotation.countByProject(project)
            def algoAnnotations = AlgoAnnotation.countByProject(project)
            def reviewedAnnotations = ReviewedAnnotation.countByProject(project)
            def images = ImageInstance.findAllByProject(project)

            project.setCountAnnotations(userAnnotations)
            project.setCountJobAnnotations(algoAnnotations)
            project.setCountReviewedAnnotations(reviewedAnnotations)
            project.setCountImages(images.size())
            project.save(flush: true)

            images.each { image ->
                //reset image counter
                log.info "update counter for image " + image.id
                def userAnnotationsImage = UserAnnotation.countByProjectAndImage(project,image)
                def algoAnnotationsImage = AlgoAnnotation.countByProjectAndImage(project,image)
                def reviewedAnnotationsImage = ReviewedAnnotation.countByProjectAndImage(project,image)

                if (image.getCountImageAnnotations() != userAnnotationsImage || image.getCountImageJobAnnotations() != algoAnnotationsImage) {
                    image.setCountImageAnnotations(userAnnotationsImage)
                    image.setCountImageJobAnnotations(algoAnnotationsImage)
                    image.setCountImageReviewedAnnotations(reviewedAnnotationsImage)
                    image.save(flush: true)
                }
                //replace by SQL ? update project as p SET count_images = (select count(id) from image_instance where project_id = p.id);
            }
        }




    }
}
