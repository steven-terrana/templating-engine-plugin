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

import hudson.ExtensionList
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.util.JTEException
import org.boozallen.plugins.jte.util.TemplateLogger
import org.codehaus.groovy.reflection.CachedMethod
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.traverse.TopologicalOrderIterator
import org.jgrapht.alg.cycle.CycleDetector
import java.lang.reflect.Method

/**
 * creates, populates, and returns the run's {@link TemplateBinding}
 *
 */
class TemplateBindingFactory {

    static TemplateBinding create(FlowExecutionOwner flowOwner, PipelineConfigurationObject config){
        // create template binding
        TemplateBinding templateBinding = new TemplateBinding(flowOwner)

        invoke("validateConfiguration", flowOwner, config)
        invoke("injectPrimitives", flowOwner, config, templateBinding)
        invoke("validateBinding", flowOwner, config, templateBinding)

        templateBinding.lock()

        return templateBinding
    }

    private static void invoke(String phase, Object... args){
        TemplateLogger logger = new TemplateLogger(args[0].getListener())
        Graph<TemplatePrimitiveInjector, DefaultEdge> graph = buildGraph(phase, args)
        new TopologicalOrderIterator<>(graph).each{ injectorClazz ->
            logger.printWarning("${injectorClazz}: ${phase}")
            TemplatePrimitiveInjector injector = injectorClazz.getDeclaredConstructor().newInstance()
            injector.invokeMethod(phase, args)
        }
    }

    /**
     * Builds a Directed Acyclic Graph representing the order in which the {@link TemplatePrimitiveInjector's} should
     * be invoked.
     * <p>
     *
     *
     * @param name the phase of binding population to a graph of
     * @param args
     * @return
     */
    private static Graph<TemplatePrimitiveInjector, DefaultEdge> buildGraph(String name, Object... args){
        Graph<TemplatePrimitiveInjector, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge)
        ExtensionList<TemplatePrimitiveInjector> injectorInstances = TemplatePrimitiveInjector.all()
        Class<? extends TemplatePrimitiveInjector>[] injectors = injectorInstances*.class
        // add each injector as a node in the graph
        injectors.each{ injector -> graph.addVertex(injector) }

        // for each injector, connect edges
        injectors.each{ injector ->
            MetaMethod metaMethod = injector.metaClass.pickMethod(name, args*.class as Class[])
            if(metaMethod instanceof CachedMethod){
                Method method = metaMethod.getCachedMethod()
                RunAfter annotation = method.getAnnotation(RunAfter)
                if(annotation) {
                    List<TemplatePrimitiveInjector> prereqs = [annotation.value()].flatten()
                    prereqs.each { req ->
                        graph.addEdge(req, injector)
                    }
                }
            }
        }

        // check for infinite loops
        CycleDetector detector = new CycleDetector(graph)
        if(detector.detectCycles()){
            throw new JTEException("There are cyclic dependencies preventing initialization")
        }

        return graph
    }

}
