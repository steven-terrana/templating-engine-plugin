package org.boozallen.plugins.jte.init.primitives

import org.boozallen.plugins.jte.util.JTEException
import org.boozallen.plugins.jte.util.TemplateLogger

/**
 * Subclasses of PrimitiveNamespace should overwrite getProperty to allow
 * the seamless interaction.
 *
 * For example, a PrimitiveNamespace identified by foo that holds a TemplatePrimitive
 * bar should be accessible via jte.foo.bar
 */
class PrimitiveNamespace implements Serializable{

    private static final long serialVersionUID = 1L
    private static final String TYPE_DISPLAY_NAME = "Primitive"

    /**
     * the key/name for this namespace
     */
    protected String name
    LinkedHashMap primitives = [:]
    String typeDisplayName = TYPE_DISPLAY_NAME

    TemplatePrimitiveInjector primitiveInjector

    /**
     * Add a new primitive to the namespace
     * @param primitive the primitive to be added
     */
    void add(TemplatePrimitive primitive){
        primitives[primitive.getName()] = primitive
    }

    String getName(){
        return name
    }

    String getTypeDisplayName(){
        return typeDisplayName
    }

    String getMissingPropertyMessage(String name){
        return "${getTypeDisplayName()} ${name} not found"
    }

    LinkedHashMap getPrimitives(){
        return this.@primitives
    }

    Object getProperty(String name){
        MetaProperty meta = getClass().metaClass.getMetaProperty(name)
        if(meta){
            return meta.getProperty(this)
        }
        if(!getPrimitives().containsKey(name)){
            throw new JTEException(getMissingPropertyMessage(name))
        }
        return getPrimitives()[name]
    }

    /**
     * @return the variable names of the primitives in this namespace
     */
    Set<String> getVariables(){
        return this.primitives.keySet() as Set<String>
    }

    void printAllPrimitives(TemplateLogger logger){
        // default implementation
        logger.print("Printing names for primitive type: ${getTypeDisplayName()}")
        variables.each { varName ->
            logger.print( "${varName}\n")
        }
    }

}
