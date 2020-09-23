package org.boozallen.plugins.jte.init.primitives
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive
import org.boozallen.plugins.jte.util.JTEException

/**
 * Subclasses of PrimitiveNamespace should overwrite getProperty to allow
 * the seamless interaction.
 *
 * For example, a PrimitiveNamespace identified by foo that holds a TemplatePrimitive
 * bar should be accessible via jte.foo.bar
 */
class PrimitiveNamespace implements Serializable{
    private static final long serialVersionUID = 1L
    String name
    LinkedHashMap primitives = [:]
    /**
     * Add a new primitive to the namespace
     * @param primitive the primitive to be added
     */
    void add(TemplatePrimitive primitive){
        primitives[primitive.getName()] = primitive
    }

    String getMissingPropertyMessage(String name){
        return "Primitive ${name} not found"
    }

    LinkedHashMap getPrimitives(){
        return this.@primitives
    }

    Object getProperty(String name){
        def meta = getClass().metaClass.getMetaProperty(name)
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
}
