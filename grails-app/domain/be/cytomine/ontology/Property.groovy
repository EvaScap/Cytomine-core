package be.cytomine.ontology

import be.cytomine.AnnotationDomain
import be.cytomine.CytomineDomain
import be.cytomine.Exception.AlreadyExistException
import be.cytomine.utils.JSONUtils
import grails.converters.JSON
import org.apache.log4j.Logger
import org.jsondoc.core.annotation.ApiObject
import org.jsondoc.core.annotation.ApiObjectField

//@ApiObject(name = "property", description = "Allow to attach any property to a domain", show = true)
class Property extends CytomineDomain implements Serializable{

    @ApiObjectField(description = "The domain class")
    String domainClassName
    @ApiObjectField(description = "The domain identifier (id)")
    Long domainIdent

    @ApiObjectField(description = "The property key")
    String key
    @ApiObjectField(description = "The property value")
    String value

    static constraints = {
        domainClassName(nullable: false, blank:  false)
        key(blank: false)
        value(blank: false)
    }

    /**
     * Set annotation (storing class + id)
     * With groovy, you can do: this.annotation = ...
     * @param domain to add
     */
    public void setDomain(CytomineDomain domain) {
        domainClassName = domain.class.getName()
        domainIdent = domain.id
    }

    /**
     * Get annotation thanks to domainClassName and domainIdent
     * @return Annotation concerned with this prediction
     */
    public CytomineDomain retrieveCytomineDomain() {
        //Class.forName(domainClassName, false, Thread.currentThread().contextClassLoader).read(domainIdent)

        def domainClass = domainClassName
        CytomineDomain domain

        if(domainClass.contains("AnnotationDomain")) {
            domain = AnnotationDomain.getAnnotationDomain(domainIdent)
        } else {
            domain = Class.forName(domainClass, false, Thread.currentThread().contextClassLoader).read(domainIdent)
        }

        domain
    }

    /**
     * Check if this domain will cause unique constraint fail if saving on database
     */
    void checkAlreadyExist(){
        Property.withNewSession {
            Property property = Property.findByDomainIdentAndKeyAndValue(domainIdent, key, value)
            if (property != null && (property.id!=id))
            {
                throw new AlreadyExistException("Property " + property.domainIdent
                        + "-" + property.key + " already exist!")
            }
        }
    }

    /**
     * Define fields available for JSON response
     * This Method is called during application start
     */
    static void registerMarshaller() {
        Logger.getLogger(this).info("Register custom JSON renderer for " + this.class)
        println "<<< mapping from Relation <<< " + getMappingFromAnnotation(Property)
        JSON.registerObjectMarshaller(Property) { domain ->
            return getDataFromDomain(domain, getMappingFromAnnotation(Property))
        }
    }


    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static Property insertDataIntoDomain(def json, def domain = new Property()){
        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)

        Long id = JSONUtils.getJSONAttrLong(json, 'domainIdent', -1)
        if (id == -1) {
            id = JSONUtils.getJSONAttrLong(json, 'domain', -1)
        }

        domain.domainIdent = id
        domain.domainClassName = JSONUtils.getJSONAttrStr(json, 'domainClassName')

        domain.key = JSONUtils.getJSONAttrStr(json,'key')
        domain.value = JSONUtils.getJSONAttrStr(json,'value')

        return domain
    }

    public CytomineDomain container() {
        return retrieveCytomineDomain().container();
    }

}
