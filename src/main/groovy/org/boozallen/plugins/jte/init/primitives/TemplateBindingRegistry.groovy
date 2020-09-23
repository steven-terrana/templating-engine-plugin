/*
    Copyright 2018 Booz Allen Hamilton

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package org.boozallen.plugins.jte.init.primitives

import hudson.Extension
import org.boozallen.plugins.jte.util.JTEException

/**
 * Stores a run's TemplatePrimitives on individual namespaces for each type of primitive
 */
class TemplateBindingRegistry implements Serializable{

    private static final long serialVersionUID = 1L

    /**
     * The name used to reference the namespace collector from the pipeline
     */
    static final String variableName = "jte"

    String getVariableName(){ return variableName }

    @Extension static class JTE extends ReservedVariableName{
        @Override String getName(){ return variableName }
        @Override String getExceptionMessage(){
            return "The variable name ${variableName} is reserved by JTE for the template primitive namespace"
        }
    }

    List<PrimitiveNamespace> namespaces = []

    /**
     * Allows users to access PrimitiveNamespaces via their self-defined identifiers
     * @param property
     * @return
     */
    Object getProperty(String property){
        PrimitiveNamespace namespace = namespaces.find{ n ->
            n.getName() == property
        }
        if(!namespace){
            throw new JTEException("JTE does not have a primitive namespace for ${property}")
        }
        return namespace
    }

    void add(Object primitive){
        // get the injector that created this primitive
        Class<? extends TemplatePrimitiveInjector> injector = primitive.getInjector()
        // get this injector's PrimitiveNamespace type
        Class<? extends PrimitiveNamespace> namespaceClass = injector.getPrimitiveNamespaceClass()
        // check if this type of namespace already exists
        PrimitiveNamespace namespace = namespaces.find{ n ->
            n.getClass() == namespaceClass
        }
        // if this namespace does NOT exist - create it.
        if(!namespace){
            namespace = namespaceClass.getDeclaredConstructor().newInstance()
            namespaces.push(namespace)
        }
        // add the primitive to the namespace
        namespace.add(primitive)
    }

    /**
     * @return the list of variable names corresponding to TemplatePrimitives
     */
    Set<String> getVariables(){
        return namespaces*.getVariables().flatten() as Set<String>
    }

    /**
     * Subclasses of PrimitiveNamespace should overwrite getProperty to allow
     * the seamless interaction.
     *
     * For example, a PrimitiveNamespace identified by foo that holds a TemplatePrimitive
     * bar should be accessible via jte.foo.bar
     */
    static abstract class PrimitiveNamespace implements Serializable{
        private static final long serialVersionUID = 1L
        String name
        /**
         * Add a new primitive to the namespace
         * @param primitive the primitive to be added
         */
        abstract void add(TemplatePrimitive primitive)
        /**
         * @return the variable names of the primitives in this namespace
         */
        abstract Set<String> getVariables()
    }

}

